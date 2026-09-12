# Script arguments

## 1. General description

Today, `runScript(...)` runs a sequence of SpEL expressions read from a string, a
`LineReader`, or (via `FileSystemAwareSpelShell`) a file `Path`, but a script has no way
to receive input from its caller. Any data has to flow in through global shell state
(variables set with `var(...)` before the call), which is awkward and leaks state the
script didn't ask for.

This feature adds an *args* parameter to every `runScript(...)` overload, and to
`runRepl()` as well. The value passed in is made available inside the script (or the
interactive session) as the SpEL variable `#_` by default, without clobbering the args
of whichever script/REPL (if any) is already running when the nested call is made.
Scripts can therefore be written as small, parameterized units of work and composed by
calling one script from another, each with its own args, the same way a Java method call
passes arguments down a call stack without disturbing its caller's locals.
`runRepl(Object)` lets an interactive session started from within a script or a command
(e.g. a sub-shell menu) receive args the same way.

`args` is untyped (`Object`) — a single value, a `Map`, a custom object, whatever the
caller finds convenient — and it is entirely up to the script to know what shape to
expect.

The name of the variable used to expose the args (`_` by default) is configurable, via
`SpelEvaluator.setScriptArgsVarName(String)`, in case `_` collides with something a
particular application wants to use for its own purposes.

## 2. How it can be used

Every existing `runScript` overload gains a sibling that takes an `Object args`
parameter, and so does `runRepl`:

```java
// CoreSpelShell
Object runRepl(Object args);
Object runScript(String script, Object args);
Object runScript(LineReader scriptLineReader, Object args);

// FileSystemAwareSpelShell
Object runScript(Path path, Object args);
```

The existing no-arg overloads are kept as-is and simply mean "no args" (see
[Implementation details](#3-implementation-details) for exactly what that implies).

Inside a script, the args are read via `#_`, like any other SpEL variable:

```
// greet.spel
print('Hello, ' + #_.name + '!')
```

```java
shell.runScript(Path.of("greet.spel"), Map.of("name", "Ada"));
```

A script can also reassign `#_`, e.g. to unwrap or default it:

```
_ = #_ == null ? {} : #_
print(_.get('count'))
```

Because each `runScript(...)` call gets its own slot for `#_`, a script can safely call
another script with different args, and — however deep the nesting goes — regain its
own original args (as it last left them) the moment the nested call returns:

```
// outer.spel
print('outer args: ' + #_)
runScript('inner.spel', 'inner-args')
print('outer args again: ' + #_)   // still 'outer-args', unaffected by the nested call
```

### Using a variable name other than `_`

If `_` isn't a good fit (e.g. it's already used for something else in a particular
application), the variable name can be changed via the shared `SpelEvaluator`:

```java
shell.getSpelEvaluator().setScriptArgsVarName("args");
shell.runScript(Path.of("greet.spel"), Map.of("name", "Ada")); // now read inside as #args
```

`getScriptArgsVarName()` returns whatever name is currently configured (`"_"` unless
changed). The name is expected to be set once, up front — e.g. right after constructing
the root shell — rather than changed while scripts are already running; see
[Implementation details](#3-implementation-details) for why.

## 3. Implementation details

### Where the args live

A script's args are exposed through a *single* SpEL variable regardless of nesting
depth — there's only ever one active args variable in the evaluation context at a time.
What changes across nested `runScript(...)` calls is *what value* that variable
currently holds, and that's tracked with an explicit stack, one entry per
currently-running script, with the top of the stack always mirroring the live value of
the args variable.

The stack (and the name of the variable it's synced to) cannot live on
`CoreSpelShellImpl` (or any subclass) as a plain instance field. Sub-shells opened for
menus/sub-shells are separate shell objects, each with their own instance fields, but
they all share a single `SpelEvaluator` instance — handed down as
`spelEvaluator = parentShell.getSpelEvaluator()` in `CoreSpelShellImpl`'s constructor.
If `runScript(...)` is called from within such a sub-shell while a running script
(started on a different shell instance) is on the call stack, a stack field on the
shell class would not be the same stack the outer script pushed onto, and a configured
variable-name field on the shell class would not be visible to the sub-shell either. So
both the args stack and the configured variable name live on `SpelEvaluatorImpl`, next
to `variables`/`spelCtx`, which is the one thing already shared correctly across the
whole shell tree — meaning a sub-shell automatically sees whatever name (and whatever
in-progress args) the rest of the tree is using, with no copying needed. The stack
itself is not exposed as another shell-visible "variable" — it is private state,
separate from the `variables` map used by `var()`/`getVariable()`/`getAllVariables()`.

`SpelEvaluator` gets four new methods:

```java
void pushArgs(Object args);
void popArgs();
String getScriptArgsVarName();
void setScriptArgsVarName(String varName);
```

`SpelEvaluatorImpl` adds a `Deque<Object> argsStack` and a mutable
`scriptArgsVarName` field, defaulting to `"_"`:

- **`pushArgs(args)`** — if the stack is non-empty, first writes the *current* value of
  the args variable (read back via `getVariable(scriptArgsVarName)`) into the entry
  that's currently on top, so that any mutation or reassignment the calling script made
  to its own args isn't lost. Then pushes `args` as the new top and calls
  `addVariable(scriptArgsVarName, args)` to make it visible under that name.
- **`popArgs()`** — pops the top entry (the just-finished script's args, discarded) and
  calls `addVariable(scriptArgsVarName, stack.isEmpty() ? null : stack.peek())` to
  restore the args variable to whatever the caller (if any) had.

This is the exact push/resync/pop behavior described at the start of this exercise,
just relocated to `SpelEvaluatorImpl` so it works uniformly whether or not a sub-shell
is involved.

`scriptArgsVarName` is read fresh on every `pushArgs`/`popArgs` call rather than fixed
once per script run, but the stack itself holds only values, not the name each value
was pushed under. So it's assumed to be configured once, before any scripts start
running, rather than changed in the middle of a nested `runScript(...)` call chain — if
it were changed mid-chain, a frame pushed under the old name and popped under the new
one would resync against (and restore to) the wrong variable.

### Every overload manages the stack, uniformly

All five entry points — `runRepl()`, `runScript(String)`, `runScript(LineReader)`,
`runScript(Path)`, and their new `Object args` siblings — must push and pop, including
the no-args ones. A no-args call pushes `null` explicitly rather than leaving the
caller's args silently visible to the nested script/REPL. Without this, a script (or a
nested REPL) called without args from inside a script that has args would still see the
*outer* script's `#_` (since nothing overrode it), which would be a surprising,
easy-to-miss form of state leakage.

To avoid duplicating (and risking inconsistently implementing) the push/pop logic in
five different methods, each no-args overload simply delegates to its args-taking
sibling with `null`, and every args-taking overload shares one private helper that wraps
the actual run:

```java
// CoreSpelShellImpl
public Object runScript(String script) {
    return runScript(script, null);
}

public Object runScript(String script, Object args) {
    ExpressionReader expressionReader = ShellUtils.expressionReader(
        ShellUtils.lineReader(script),
        line -> replConfigForScript.getIsCommentLine().apply(getRootObject(), line)
    );
    return runWithArgs(args, () -> runRepl(replConfigForScript, expressionReader));
}

// shared by runRepl(Object) and every runScript(..., Object) overload,
// incl. the one in FileSystemAwareSpelShellImpl
protected Object runWithArgs(Object args, Supplier<Object> action) {
    getSpelEvaluator().pushArgs(args);
    try {
        return action.get();
    } finally {
        getSpelEvaluator().popArgs();
    }
}
```

`runRepl(Object)`, `runScript(LineReader, Object)` (both in `CoreSpelShellImpl`), and
`runScript(Path, Object)` (in `FileSystemAwareSpelShellImpl`) follow the same shape:
build the `ExpressionReader` (or reuse the console, for `runRepl`) as before, then run
it through `runWithArgs`.

### Exception safety

`replConfigForScript` is configured with `setStopOnException(Exception.class)`, so
`runRepl(replConfigForScript, ...)` rethrows on *any* exception raised while evaluating
a script line, rather than swallowing it the way the interactive REPL loop does. That
means a failing script unwinds straight out of `runScript(...)` (and, for nested calls,
back out through every enclosing `runScript(...)`/`runRepl(...)` on the Java call
stack). The `try { ... } finally { popArgs(); }` in `runWithArgs` is what keeps the args
stack correct in that case: no matter how a run ends — normal completion or an
exception propagating out — its args entry is always popped and the caller's `#_` is
always restored before control leaves `runScript(...)`/`runRepl(...)`.

### Why `#_` is a safe choice of default name

- SpEL's tokenizer accepts a lone `_` as a variable identifier (only `javac` treats a
  bare `_` as reserved; that restriction doesn't apply to a runtime SpEL parser), so
  `#_` parses as an ordinary variable reference.
- The shell's shorthand-rewrite regexes in `ShellUtils` (e.g. `IDENTIFIER_PAT` and the
  patterns built from it) only match expressions that don't start with `#`, so `#_` is
  passed straight through to the SpEL evaluator rather than being misinterpreted as a
  zero-arg command invocation.
- `_` doesn't collide with any existing built-in variable — `lastEvalResultVarName`
  defaults to `$`, and nothing else in the codebase reserves `_`.

These points are specific to the default; they don't necessarily hold for every name a
caller might configure via `setScriptArgsVarName(...)`. In particular, a name that
happens to match an existing zero/one-arg exposed method could be shadowed by the
shorthand-rewrite rules in `ShellUtils` when referenced *without* the `#` prefix — but
since the args variable is always meant to be read as `#<name>`, not bare `<name>`, this
doesn't come up in normal use.
