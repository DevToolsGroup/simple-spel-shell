# 12. REPL Hooks: Prompts, Comments, and Interceptors

Several earlier pages already used one `ReplConfig` hook or another without naming the mechanism: `Example2`'s dynamic submenu prompt in page 8, the history file in page 7. This page names all five, in the order `CoreSpelShellImpl.runRepl()` actually calls them, and shows how to layer behavior onto the default without breaking it.

## The five hooks, in firing order

Each `ReplConfig` holds these fields (there are two `ReplConfig`s per shell — one for `runRepl()`, one for `runScript(...)` — see page 7):

| Hook | Type | Called with | When |
|---|---|---|---|
| `prompt` | `Function<Object, String>` | the root object | before reading a line, to render the prompt (skipped entirely if `null`, as it is for scripts) |
| `isCommentLine` | `BiFunction<Object, String, Boolean>` | root object, raw line | while reading — matching lines are skipped |
| `expressionInterceptor` | `BiFunction<Object, String, String>` | root object, raw expression | after a full expression is read; returns the (possibly rewritten) expression to evaluate |
| `exprBeforeEvalInterceptor` | `BiConsumer<Object, String>` | root object, final expression | right before evaluation — a side-effect-only hook, `null` by default |
| `evalResultInterceptor` | `BiConsumer<Object, Object>` | root object, the result | right after evaluation, with whatever `evaluate(...)` returned |

`expressionInterceptor` is the one doing all the heavy lifting we've relied on throughout this tutorial: its default implementation (`CoreSpelShellImpl.makeDefaultExpressionInterceptor`) is what calls `ShellUtils.rewriteExpr` for shorthand syntax and logs to the history file. `BaseSpelShellImpl` wraps that default once more, to expand backtick name patterns before the shorthand rewrite runs. `evalResultInterceptor`'s default is what auto-prints a non-`null` result, truncated — the behavior we've been relying on since page 2.

## A dynamic prompt

`Example2`'s submenus (page 8) set a `Function<Object, String>` prompt that reads live fields off the shell. Let's do the same at the top level — show how many tasks exist right in the prompt:

```java
getReplConfig().setPrompt(_ -> "tasks(" + countTasks() + ")> ");
```

```java
private int countTasks() {
    return (int) findFilesByName(new NamePattern("")).stream().filter(File::isFile).count();
}
```

```
tasks(0)> at 'Buy milk'
Added: Buy milk
tasks(1)> at 'Walk the dog'
Added: Walk the dog
tasks(2)>
```

Since `prompt` is re-evaluated every iteration of the loop, the count updates on its own — no manual refresh needed.

## Recognizing a second comment style

`isCommentLine` is a plain predicate — safe to replace outright, since it isn't doing anything besides the one check:

```java
getReplConfig().setIsCommentLine((_, line) -> {
    String trimmed = line.trim();
    return trimmed.startsWith("//") || trimmed.startsWith("#");
});
```

Now both `// like this` and `# like this` are treated as comments in scripts (page 7) and skipped while reading.

## Wrapping — not replacing — the expression interceptor

`isCommentLine` was safe to overwrite because it does one small thing. `expressionInterceptor` is different: it's what makes shorthand syntax and history logging work at all, so replacing it outright would silently turn both off. The pattern to reach for — the same one `BaseSpelShellImpl` itself uses internally to layer backtick expansion on top of the default — is to capture the existing interceptor and call it from inside your replacement:

```java
BiFunction<Object, String, String> defaultInterceptor = getReplConfig().getExpressionInterceptor();
getReplConfig().setExpressionInterceptor((root, expr) -> {
    String rewritten = defaultInterceptor.apply(root, expr);
    System.err.println("[audit] " + expr + " -> " + rewritten);
    return rewritten;
});
```

This adds a simple audit trail to stderr while leaving shorthand rewriting and history logging fully intact — `defaultInterceptor.apply(root, expr)` still does that work; we're only observing its result on the way past.

## `exprBeforeEvalInterceptor`

Unlike `expressionInterceptor`, this one is `null` by default, so there's nothing to preserve — set it directly:

```java
getReplConfig().setExprBeforeEvalInterceptor((_, expr) -> System.err.println("[eval] " + expr));
```

It fires once per expression, after all rewriting is done and right before `SpelEvaluator.evaluate(...)` runs — useful for logging exactly what's about to execute, as opposed to `expressionInterceptor`, which logs (and can still change) what was typed.

---
Previous: [11. Custom Type Converters](11-custom-type-converters.md) · Next: [13. Operator Overloading](13-operator-overloading.md)
