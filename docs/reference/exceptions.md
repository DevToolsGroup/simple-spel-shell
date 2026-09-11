# Reference: Exceptions

Two exception types the REPL loop treats specially. Full walkthrough: [tutorial page 10](../tutorial/10-error-handling.md).

## `ShellException`

`org.devtoolsgroup.simplespelshell.ShellException extends RuntimeException` — a recoverable error. Not fatal by default: the loop catches it, prints its message, and continues.

| Constructor | `isPrintStackTrace()` |
|---|---|
| `ShellException(String message)` | `true` |
| `ShellException(String message, Throwable cause)` | `true` |
| `ShellException(boolean printStackTrace, String message)` | as given |
| `ShellException(Exception ex)` | `true` |

`isPrintStackTrace()` controls whether the loop's default handler prints a full stack trace alongside the message — pass `false` explicitly (as the built-in filesystem and shorthand-matching commands do) for a clean, single-line error.

## `ShellExitException`

`org.devtoolsgroup.simplespelshell.ShellExitException extends RuntimeException` — the default `stopOnException` type for the interactive loop ([ReplConfig reference](repl-config.md)); throwing one ends `runRepl()`.

| Constructor | Notes |
|---|---|
| `ShellExitException()` | `result` is `null`. |
| `ShellExitException(Object result)` | `getResult()` returns whatever was passed — used in [tutorial page 8](../tutorial/08-submenus.md) to distinguish "go back one level" (`false`) from "exit everything" (`true`) across nested shells. |

Built via `ShellUtils.exnExit(Object)` and passed to `setOnExit(...)` — see the [ShellUtils reference](shell-utils.md). A custom exception can `extend ShellExitException` to get "clean exit" behavior for free without touching `stopOnException` at all — see [tutorial page 10](../tutorial/10-error-handling.md) for why that's usually preferable to replacing `stopOnException` directly.

---
Back to [reference index](index.md) · [documentation index](../index.md)
