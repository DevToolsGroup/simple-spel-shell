# Reference Index

A flat lookup across everything in the reference section. See [documentation index](../index.md) for the tutorial this all comes from.

| Name | What it is | Details | Tutorial |
|---|---|---|---|
| `CoreSpelShellImpl` | Bare REPL engine, no built-in commands | [Shell Classes](shell-classes.md) | [1](../tutorial/01-getting-started.md) |
| `BaseSpelShellImpl` | Adds `help`/`var`/`hist`/`print*`/`prompt`/`exit` | [Shell Classes](shell-classes.md) | [1](../tutorial/01-getting-started.md), [5](../tutorial/05-built-in-commands.md) |
| `FileSystemAwareSpelShellImpl` | Adds a sandboxed working directory | [Shell Classes](shell-classes.md) | [9](../tutorial/09-filesystem-shells.md) |
| Raw SpEL at the prompt | Every line is evaluated as a SpEL expression against the shell | — | [2](../tutorial/02-raw-spel.md) |
| `ShellUtils.rewriteExpr` | Shorthand syntax: `cmd arg`, `x = value`, bare `cmd` | [ShellUtils](shell-utils.md) | [3](../tutorial/03-shorthand-and-fuzzy-matching.md) |
| `ShellUtils.matches` | Fuzzy/abbreviated name matching | [ShellUtils](shell-utils.md) | [3](../tutorial/03-shorthand-and-fuzzy-matching.md), [13](../tutorial/13-operator-overloading.md) |
| Backtick name patterns / `NamePattern` | `` `pat` `` → `npat('pat')` | [ShellUtils](shell-utils.md) | [4](../tutorial/04-name-patterns-and-variables.md) |
| `var` | Set/read/list SpEL variables | [Built-in Commands](built-in-commands.md) | [4](../tutorial/04-name-patterns-and-variables.md) |
| `help` | List exposed commands, with fuzzy filtering | [Built-in Commands](built-in-commands.md) | [5](../tutorial/05-built-in-commands.md) |
| `print` / `println` / `printf` / `format` | Console output | [Built-in Commands](built-in-commands.md) | [5](../tutorial/05-built-in-commands.md) |
| `prompt` | Ask the user for a line of input | [Built-in Commands](built-in-commands.md) | [5](../tutorial/05-built-in-commands.md) |
| `exit` / `setOnExit` | End (or redirect) the REPL loop | [Built-in Commands](built-in-commands.md) | [5](../tutorial/05-built-in-commands.md), [8](../tutorial/08-submenus.md) |
| `@Order` / `setMinOrderForHelp` | Command sort order, help visibility, shorthand eligibility | [ShellUtils](shell-utils.md) | [6](../tutorial/06-order-and-help-visibility.md) |
| `hist` / `exprHistoryFile` | Expression history logging and replay | [ReplConfig](repl-config.md), [ShellUtils](shell-utils.md) | [7](../tutorial/07-history-and-scripting.md) |
| `runScript` | Run a batch of expressions non-interactively | [Shell Classes](shell-classes.md) | [7](../tutorial/07-history-and-scripting.md) |
| Sub-shells | Nested shells sharing a `Console`/`SpelEvaluator` | [Shell Classes](shell-classes.md) | [8](../tutorial/08-submenus.md) |
| `cd` / `pwd` / `ll` / `mkdir` / `read` / `write` | Sandboxed filesystem commands | [Built-in Commands](built-in-commands.md) | [9](../tutorial/09-filesystem-shells.md) |
| `ShellException` | Recoverable error — caught, printed, loop continues | [Exceptions](exceptions.md) | [10](../tutorial/10-error-handling.md) |
| `ShellExitException` / `stopOnException` | What ends the REPL loop | [Exceptions](exceptions.md), [ReplConfig](repl-config.md) | [10](../tutorial/10-error-handling.md) |
| `SpelEvaluator.setTypeConverters` | Coerce typed strings into your own types | [Shell Classes](shell-classes.md) | [11](../tutorial/11-custom-type-converters.md) |
| `ReplConfig` (all five hooks) | `prompt`, `isCommentLine`, `expressionInterceptor`, `exprBeforeEvalInterceptor`, `evalResultInterceptor` | [ReplConfig](repl-config.md) | [12](../tutorial/12-repl-hooks.md) |
| `SpelEvaluator.setOperatorOverloader` | Give SpEL operators new meaning for your own types | [Shell Classes](shell-classes.md) | [13](../tutorial/13-operator-overloading.md) |
| `getRootObject` / `isMethodToHideInRewrite` / `isMethodToHideInHelp` | Lower-level overridable hooks | [Shell Classes](shell-classes.md) | [14](../tutorial/14-other-extension-points.md) |
| `addVariable` / `getVariable` / `getAllVariables` | Direct SpEL-variable access, bypassing `var` | [Shell Classes](shell-classes.md) | [14](../tutorial/14-other-extension-points.md) |

## Pages

- [Shell Classes](shell-classes.md)
- [Built-in Commands](built-in-commands.md)
- [ReplConfig](repl-config.md)
- [ShellUtils](shell-utils.md)
- [Exceptions](exceptions.md)
