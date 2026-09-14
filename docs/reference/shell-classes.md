# Reference: Shell Classes

Three classes, each extending the last.
Pick the lowest one that has what you need — see [tutorial page 1](../tutorial/01-getting-started.md).

## `CoreSpelShellImpl`

`package org.devtoolsgroup.simplespelshell.impl`

The bare REPL engine — no built-in commands, not even `help` or `exit`.
Introduced: [page 1](../tutorial/01-getting-started.md).

| Constructor | Notes |
|---|---|
| `CoreSpelShellImpl(CoreSpelShell parentShell, Console console)` | `parentShell == null` creates a root shell (own `SpelEvaluator`, own default `ReplConfig`s); non-`null` creates a sub-shell sharing the parent's `SpelEvaluator` and copying its `ReplConfig`s. |

| Method                                                            | Notes                                                                                                                                           |
|-------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| `runRepl()`                                                       | Starts the interactive loop. Overridable — see [page 8](../tutorial/08-submenus.md).                                                            |
| `runScript(String script)`                                        | Runs the given text as a batch of expressions.                                                                                                  |
| `runScript(LineReader reader)`                                    | Same, from an arbitrary line source.                                                                                                            |
| `eval(Object rootObject, String expression)`                      | Evaluates one expression directly against a given root object.                                                                                  |
| `getSpelEvaluator()`                                              | The underlying `SpelEvaluator` — see [tutorial page 11](../tutorial/11-custom-type-converters.md)/[13](../tutorial/13-operator-overloading.md). |
| `getConsole()` / `setConsole(Console)`                            | The I/O abstraction (`read`/`print`/`println`/`printf`).                                                                                        |
| `getReplConfig()` / `setReplConfig(ReplConfig)`                   | Interactive-loop configuration — see [ReplConfig reference](repl-config.md).                                                                    |
| `getReplConfigForScript()` / `setReplConfigForScript(ReplConfig)` | Separate configuration used by `runScript(...)`.                                                                                                |
| `setLastEvalResultVarName(String)`                                | Default `"$"` — see [page 14](../tutorial/14-other-extension-points.md).                                                                        |
| `setLastEvalResultMaxPrintLength(int)`                            | Default `100` — see [page 14](../tutorial/14-other-extension-points.md).                                                                        |

Protected, overridable: `isMethodToHideInRewrite(Method)` ([page 14](../tutorial/14-other-extension-points.md)).

## `BaseSpelShellImpl`

`extends CoreSpelShellImpl` — adds the commands covered on [page 5](../tutorial/05-built-in-commands.md):
`help`, `var`, `hist`, `print`/`println`/`printf`/`format`, `prompt`, `exit`, `npat`, `exn`/`exnf`.
Full list: [Built-in Commands reference](built-in-commands.md).

| Constructor | Notes |
|---|---|
| `BaseSpelShellImpl()` | Root shell, default `Console`. |
| `BaseSpelShellImpl(Console console)` | Root shell, custom `Console`. |
| `BaseSpelShellImpl(BaseSpelShell parentShell)` | Sub-shell sharing the parent's console — see [page 8](../tutorial/08-submenus.md). |
| `BaseSpelShellImpl(BaseSpelShell parentShell, Console console)` | Sub-shell with its own console. |

Also: `setMinOrderForHelp(int)`/`getMinOrderForHelp()` (default `-100`,
see [page 6](../tutorial/06-order-and-help-visibility.md)),
`setOnExit(Consumer<Object>)`/`getOnExit()` (default `System.exit(0)`),
and the overridable `isMethodToHideInHelp(Method)` ([page 14](../tutorial/14-other-extension-points.md)).

## `FileSystemAwareSpelShellImpl`

`extends BaseSpelShellImpl` — adds a sandboxed working directory
and the filesystem commands covered on [page 9](../tutorial/09-filesystem-shells.md):
`cd`, `pwd`, `ll`, `mkdir`, `read`, `write`, `findFilesByName`, `listFiles`, `listDirs`,
plus a `runScript(Path)` overload.
Registers `String → Path`
and `String → NamePattern` converters automatically (see [page 11](../tutorial/11-custom-type-converters.md)).

| Constructor | Notes |
|---|---|
| `FileSystemAwareSpelShellImpl(Path initDir)` | Root shell; `initDir` **must already exist**. |
| `FileSystemAwareSpelShellImpl(Console console, Path initDir)` | Root shell, custom console. |
| `FileSystemAwareSpelShellImpl(FileSystemAwareSpelShell parentShell)` | Sub-shell sharing the parent's working directory. |
| `FileSystemAwareSpelShellImpl(FileSystemAwareSpelShell parentShell, Console console, Path initDir)` | The constructor the others delegate to; if `parentShell` is non-`null` it's a sub-shell with a custom console that still inherits the parent's working directory (`initDir` is then ignored). |

`getWorkingDirectory()` exposes the underlying `WorkingDirectory`,
which enforces the sandbox and throws `ShellException` on any attempt to `cd` or write outside the root directory.

---
Back to [reference index](index.md) · [documentation index](../index.md)
