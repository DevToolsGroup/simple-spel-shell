# Script arguments

## 1. General description

Today, `runScript(...)` runs a sequence of SpEL expressions read from a string, a
`LineReader`, or (via `FileSystemAwareSpelShell`) a file `Path`, but a script has no way
to receive input from its caller. Any data has to flow in through global shell state
(variables set with `var(...)` before the call), which is awkward and leaks state the
script didn't ask for.

This feature adds an *args* parameter to every `runScript(...)` overload. The value
passed in is made available inside the script as the SpEL variable `#_`, without
clobbering the args of whichever script (if any) is already running when the nested
call is made. Scripts can therefore be written as small, parameterized units of work and
composed by calling one script from another, each with its own args, the same way a
Java method call passes arguments down a call stack without disturbing its caller's
locals.

`args` is untyped (`Object`) — a single value, a `Map`, a custom object, whatever the
caller finds convenient — and it is entirely up to the script to know what shape to
expect.

## 2. How it can be used

Every existing `runScript` overload gains a sibling that takes an `Object args`
parameter:

```java
// CoreSpelShell
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

## 3. Implementation details

### Where the args live

A script's args are exposed through the *same* `#_` SpEL variable regardless of
nesting depth — there's only ever one `_` in the evaluation context at a time. What
changes across nested `runScript(...)` calls is *what value* `_` currently holds, and
that's tracked with an explicit stack, one entry per currently-running script, with the
top of the stack always mirroring the live value of `#_`.

The stack cannot live on `CoreSpelShellImpl` (or any subclass) as a plain instance
field. Sub-shells opened for menus/sub-shells are separate shell objects, each with
their own instance fields, but they all share a single `SpelEvaluator` instance —
handed down as `spelEvaluator = parentShell.getSpelEvaluator()` in
`CoreSpelShellImpl`'s constructor. If `runScript(...)` is called from within such a
sub-shell while a running script (started on a different shell instance) is on the
call stack, a stack field on the shell class would not be the same stack the outer
script pushed onto. So the args stack is added to `SpelEvaluatorImpl`, next to
`variables`/`spelCtx`, which is the one thing already shared correctly across the whole
shell tree. It is not exposed as another shell-visible "variable" — it is private
state, separate from the `variables` map used by `var()`/`getVariable()`/
`getAllVariables()`.

`SpelEvaluator` gets two new methods to encapsulate it:

```java
void pushArgs(Object args);
void popArgs();
```

`SpelEvaluatorImpl` adds a `Deque<Object> argsStack` and a constant
`SCRIPT_ARGS_VAR_NAME = "_"`:

- **`pushArgs(args)`** — if the stack is non-empty, first writes the *current* value of
  `#_` (read back via `getVariable(SCRIPT_ARGS_VAR_NAME)`) into the entry that's
  currently on top, so that any mutation or reassignment the calling script made to its
  own args isn't lost. Then pushes `args` as the new top and calls
  `addVariable(SCRIPT_ARGS_VAR_NAME, args)` to make it visible as `#_`.
- **`popArgs()`** — pops the top entry (the just-finished script's args, discarded) and
  calls `addVariable(SCRIPT_ARGS_VAR_NAME, stack.isEmpty() ? null : stack.peek())` to
  restore `#_` to whatever the caller (if any) had.

This is the exact push/resync/pop behavior described at the start of this exercise,
just relocated to `SpelEvaluatorImpl` so it works uniformly whether or not a sub-shell
is involved.

### Every overload manages the stack, uniformly

All four `runScript` overloads — `runScript(String)`, `runScript(LineReader)`,
`runScript(Path)`, and their new `Object args` siblings — must push and pop, including
the no-args ones. A no-args call pushes `null` explicitly rather than leaving the
caller's args silently visible to the nested script. Without this, a script called
without args from inside a script that has args would still see the *outer* script's
`#_` (since nothing overrode it), which would be a surprising, easy-to-miss form of
state leakage.

To avoid duplicating (and risking inconsistently implementing) the push/pop logic in
four different methods, each no-args overload simply delegates to its args-taking
sibling with `null`, and the args-taking overloads share one private helper that wraps
the actual script run:

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
    return runScriptWithArgs(args, () -> runRepl(replConfigForScript, expressionReader));
}

// shared by all *(..., Object args)* overloads, incl. the one in FileSystemAwareSpelShellImpl
protected Object runScriptWithArgs(Object args, Supplier<Object> scriptRunner) {
    getSpelEvaluator().pushArgs(args);
    try {
        return scriptRunner.get();
    } finally {
        getSpelEvaluator().popArgs();
    }
}
```

`runScript(LineReader, Object)` (in `CoreSpelShellImpl`) and `runScript(Path, Object)`
(in `FileSystemAwareSpelShellImpl`) follow the same shape: build the
`ExpressionReader` as before, then run it through `runScriptWithArgs`.

### Exception safety

`replConfigForScript` is configured with `setStopOnException(Exception.class)`, so
`runRepl(replConfigForScript, ...)` rethrows on *any* exception raised while evaluating
a script line, rather than swallowing it the way the interactive REPL loop does. That
means a failing script unwinds straight out of `runScript(...)` (and, for nested calls,
back out through every enclosing `runScript(...)` on the Java call stack). The
`try { ... } finally { popArgs(); }` in `runScriptWithArgs` is what keeps the args stack
correct in that case: no matter how a script's execution ends — normal completion or an
exception propagating out — its args entry is always popped and the caller's `#_` is
always restored before control leaves `runScript(...)`.

### Why `#_` is a safe choice of name

- SpEL's tokenizer accepts a lone `_` as a variable identifier (only `javac` treats a
  bare `_` as reserved; that restriction doesn't apply to a runtime SpEL parser), so
  `#_` parses as an ordinary variable reference.
- The shell's shorthand-rewrite regexes in `ShellUtils` (e.g. `IDENTIFIER_PAT` and the
  patterns built from it) only match expressions that don't start with `#`, so `#_` is
  passed straight through to the SpEL evaluator rather than being misinterpreted as a
  zero-arg command invocation.
- `_` doesn't collide with any existing built-in variable — `lastEvalResultVarName`
  defaults to `$`, and nothing else in the codebase reserves `_`.
