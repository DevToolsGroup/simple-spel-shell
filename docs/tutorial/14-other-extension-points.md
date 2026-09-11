# 14. Other Extension Points and Current Limits

A short closing tour of the remaining hooks — smaller and more situational than the ones covered so far, shown here as standalone snippets rather than folded into `TaskShell` — plus an honest note on what isn't customizable yet.

## `getRootObject()` — evaluating against a different object

Every expression is evaluated with `getRootObject()` as SpEL's root, and `CoreSpelShellImpl`'s default implementation is simply `return this;`. Overriding it lets commands, prompts, and interceptors keep running on the shell instance while SpEL expressions themselves resolve against something else entirely — useful if you're wrapping an existing domain object rather than putting all your commands directly on the shell class:

```java
private final Inventory inventory = new Inventory();

@Override
protected Object getRootObject() {
    return inventory;
}
```

With this override, typed expressions call methods on `inventory`, not on the shell — while `help`, shorthand rewriting, and history still work the same way, since those look at `getRootObject().getClass()` rather than assuming it's always `this`.

## Custom visibility policy

`@Order` (page 6) is the normal way to control what's shorthand-eligible and what shows in `help`, but both checks go through overridable methods you can replace outright:

```java
@Override
protected boolean isMethodToHideInRewrite(Method method) {
    return super.isMethodToHideInRewrite(method) || method.getName().startsWith("internal");
}

@Override
protected boolean isMethodToHideInHelp(Method method) {
    return super.isMethodToHideInHelp(method) || method.isAnnotationPresent(Deprecated.class);
}
```

This is the mechanism `@Order`-based filtering is itself built on — `isMethodToHideInRewrite` lives on `CoreSpelShellImpl`, `isMethodToHideInHelp` on `BaseSpelShellImpl` — so a subclass can layer arbitrary additional rules (naming conventions, annotations, anything reflectable) on top of the default order-based ones.

## Working with variables directly

`var(...)` (page 4) is a thin shell command wrapping three methods on `SpelEvaluator` itself: `addVariable(String name, Object value)`, `getVariable(String name)`, and `getAllVariables()` (which returns a defensive copy — mutating it has no effect on the shell's actual variable table). Calling these directly from your own Java code, rather than through the `var` command, is occasionally useful for wiring up state from outside the REPL loop entirely — for instance, seeding a variable before the first prompt is ever shown:

```java
getSpelEvaluator().addVariable("startedAt", Instant.now());
```

## The auto-printed result

Two more small knobs on `CoreSpelShellImpl`, both defaulted in its constructor: `lastEvalResultVarName` (default `"$"`) is the SpEL variable name the previous result is stashed under after every evaluation — so `#$` always refers to whatever you last evaluated, the way `#sum`/`#prod` did across submenus in page 8. `lastEvalResultMaxPrintLength` (default `100`) controls how much of a result's `toString()` the default `evalResultInterceptor` (page 12) prints before truncating with `...`. Both have setters (`setLastEvalResultVarName`, `setLastEvalResultMaxPrintLength`) if the defaults don't suit your shell.

## What isn't pluggable yet

Worth knowing before you go looking for it: `SpelEvaluator` currently exposes exactly two extension points into SpEL's `StandardEvaluationContext` — `TypeConverter` (page 11) and `OperatorOverloader` (page 13). There's no way, as of this version, to register a custom `PropertyAccessor`, `MethodResolver`, `ConstructorResolver`, or to tune `SpelParserConfiguration` (compiler mode, for instance). If your use case needs one of those, you'd currently have to build your own `SpelEvaluator` implementation from scratch rather than configure the shipped one.

---
Previous: [13. Operator Overloading](13-operator-overloading.md) · Back to [documentation index](../index.md)
