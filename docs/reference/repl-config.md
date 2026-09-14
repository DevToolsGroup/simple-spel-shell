# Reference: ReplConfig

`org.devtoolsgroup.simplespelshell.ReplConfig` — every shell has two instances:
`getReplConfig()` (used by `runRepl()`)
and `getReplConfigForScript()` (used by `runScript(...)`), independently configurable.
Full walkthrough: [tutorial page 12](../tutorial/12-repl-hooks.md).

| Field | Type | Default | Fires |
|---|---|---|---|
| `prompt` | `Function<Object, String>` | `_ -> "SpEL> "` (REPL); `null` (script) | Before reading a line, if non-`null`. |
| `isCommentLine` | `BiFunction<Object, String, Boolean>` | `(_, s) -> s.trim().startsWith("//")` | While reading — matching lines are skipped. |
| `expressionInterceptor` | `BiFunction<Object, String, String>` | shorthand rewrite + history logging (wrapped once more by `BaseSpelShellImpl` for backtick expansion) | After a full expression is read. |
| `exprBeforeEvalInterceptor` | `BiConsumer<Object, String>` | `null` | Right before evaluation, with the final expression. |
| `evalResultInterceptor` | `BiConsumer<Object, Object>` | prints the truncated, non-`null` result | Right after evaluation, with the result. |
| `exprHistoryFile` | `File` | `null` (off) | N/A — read by `hist(...)`, written by the default `expressionInterceptor`. See [page 7](../tutorial/07-history-and-scripting.md). |
| `stopOnException` | `Class<? extends Exception>` | `ShellExitException.class` (REPL); `Exception.class` (script) | An exception whose class this is assignable from ends `runRepl()` instead of being caught and printed. See [page 10](../tutorial/10-error-handling.md). |

Accessors follow the standard `getX()`/`setX(...)` pattern for every field above.

## Order of operations inside `runRepl()`

1. `prompt.apply(root)` is printed (if non-`null`).
2. A line is read  (or several lines with trailing `\`).
3. `expressionInterceptor.apply(root, expr)` runs; its return value is what gets evaluated.
A `null` ends the loop (EOF); a blank line is skipped.
4. `exprBeforeEvalInterceptor.accept(root, expr)` runs, if non-`null`.
5. `SpelEvaluator.evaluate(root, expr)` runs;
the result is stored under the `$` SpEL variable (configurable, [page 14](../tutorial/14-other-extension-points.md)).
6. `evalResultInterceptor.accept(root, result)` runs, if non-`null`.
7. Any exception thrown during 1–6 is caught:
if its class matches `stopOnException`, it's rethrown (ending the loop);
otherwise its message and stack trace is printed,
and the loop continues.

---
Back to [reference index](index.md) · [documentation index](../index.md)
