# Reference: ShellUtils

`org.devtoolsgroup.simplespelshell.ShellUtils` — static helpers used internally by the framework and safe to call from your own code.

## Shorthand rewrite rules — `rewriteExpr`

Covered in [tutorial page 3](../tutorial/03-shorthand-and-fuzzy-matching.md). Tried in this order; the first match wins:

| Pattern | Rewritten to |
|---|---|
| `IDENT` | `IDENT()` |
| `IDENT rest` | `IDENT(rest)` |
| `IDENT = rest` | `var('IDENT', rest)` |
| `IDENT1 = IDENT2` | `var('IDENT1', IDENT2())` |
| `IDENT1 = IDENT2 rest` | `var('IDENT1', IDENT2(rest))` |
| *(no match)* | Returned unchanged, evaluated as raw SpEL. |

`IDENT` (and `IDENT2`, the method name) is fuzzy-matched against the shell's exposed zero-arg or one-arg method names, as appropriate, via `matches(...)` below — throwing `ShellException` if zero or more than one method matches.

## Fuzzy matching — `matches` / `splitForMatch`

`matches(String name, String pattern)` — used for shorthand command names, backtick name patterns, and (in [tutorial page 13](../tutorial/13-operator-overloading.md)) application-level lookups. A pattern matches if its characters, in order, form the start of successive segments of `name`, where `splitForMatch(String)` first splits `name` on camelCase boundaries, `_`, `-`, `.`, `$`, `/`, `\`, and digit transitions. Matching is case-insensitive; an empty pattern matches everything.

Examples: `matches("addTask", "at")` → `true`; `matches("setLastEvalResultMaxPrintLength", "llen")` → `true`; `matches("ab-cd-ef", "aef")` → `true`.

## Name patterns — `replaceAllNamePatterns`

Rewrites `` `pat `` or `` `pat` `` to `npat('pat')` anywhere in a string — run before shorthand rewriting, by `BaseSpelShellImpl`. See [tutorial page 4](../tutorial/04-name-patterns-and-variables.md).

## History — `saveExprToHistFile` / `loadHistory`

`saveExprToHistFile(String expr, File histFile)` appends `\n<ISO-8601 instant, second precision> <expr>`; `loadHistory(File)` reads all lines back (empty list if the file doesn't exist yet). Driven automatically by the default `expressionInterceptor` — see [tutorial page 7](../tutorial/07-history-and-scripting.md).

## Exiting — `exnExit`

`exnExit(Object result)` / `exnExit()` return a `Consumer<Object>` that throws `new ShellExitException(result)` — meant to be passed to `setOnExit(...)`. See [tutorial page 8](../tutorial/08-submenus.md).

## Readers — `lineReader` / `expressionReader`

`lineReader(String|File|InputStream|...)` builds a `LineReader` from various sources; `expressionReader(LineReader, Function<String,Boolean> isCommentLine)` builds the `ExpressionReader` `runRepl()` reads from, handling comment-line skipping and trailing-`\` continuation (`readExpr`, private).

## Other

`getSortOrder(Method)` reads a method's `@Order` value (`0` if unannotated) — see [tutorial page 6](../tutorial/06-order-and-help-visibility.md). `isParentChild(Path parent, Path child)` checks sandbox containment — used by `FileSystemAwareSpelShellImpl`. `truncateWithEllipsis(String, int)` is what the default `evalResultInterceptor` uses to cap printed results. `getStackTrace(Throwable)` formats a stack trace as a `String`.

---
Back to [reference index](index.md) · [documentation index](../index.md)
