# 2. Talking to SpEL Directly

We're still using the `TaskShell` from [page 1](01-getting-started.md) unchanged — this page is entirely about what already works at the prompt without writing any more code.

## The evaluator

Every time you press enter, `CoreSpelShellImpl.runRepl()` hands your line to a `SpelEvaluator`, which does exactly this:

```java
public Object evaluate(Object rootObject, String expression) {
    return parser.parseExpression(expression).getValue(spelCtx, rootObject, Object.class);
}
```

`rootObject` is your shell instance. That's the whole mechanism: your typed line is parsed as a SpEL expression and evaluated with the shell as the implicit `this`. `addTask('Buy milk')` works because SpEL resolves `addTask` as a method on the root object — same as it would resolve `foo.addTask(...)` if `foo` were the root.

Because it's *real* SpEL, not a lookalike, everything else SpEL can do is available too, with no extra setup:

```
SpEL> 1+2
3
SpEL> T(Math).PI
3.141592653589793
SpEL> {1,2,3}
[1, 2, 3]
SpEL> addTask('Buy milk')
Added: Buy milk
```

A few things happened there worth calling out:

- `1+2` evaluated to `3`, and the shell printed it automatically. After every expression, the shell stores the result and — if it isn't `null` — prints it (truncated if very long). `addTask` returns `void`, so nothing extra was printed for that line beyond what `println` inside the method already wrote.
- `T(Math).PI` is SpEL's syntax for referencing a static field via a type reference (`T(...)`). No import, no wiring — it's built into the expression language itself.
- `{1,2,3}` is a SpEL inline list literal, evaluating to a real `java.util.List`.

## Why this matters

Nothing you'll see in the rest of this tutorial — shorthand syntax, fuzzy command names, variables, sub-shells — replaces this mechanism. It's all built *on top of* it: convenience layers that rewrite what you type into a plain SpEL expression before handing it to the same `evaluate(rootObject, expression)` call shown above. When something behaves unexpectedly, it usually helps to ask "what SpEL expression did this actually turn into?" — a question the next page answers directly.

---
Previous: [1. Getting Started](01-getting-started.md) · Next: [3. Shorthand Syntax and Fuzzy Command Names](03-shorthand-and-fuzzy-matching.md)
