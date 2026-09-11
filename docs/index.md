# simple-spel-shell documentation

simple-spel-shell turns a plain Java class into an interactive CLI shell: every public method you add becomes a command, and everything you type at the prompt is evaluated as a [Spring Expression Language (SpEL)](https://docs.spring.io/spring-framework/reference/core/expressions.html) expression against your shell object. This documentation set has two parts:

- **[The tutorial](#tutorial)** — a step-by-step guide, in reading order, that builds one small CLI tool (a task tracker) from an empty class up through every feature the framework offers.
- **[The reference](#reference)** — short, skimmable pages you come back to once you already know the concepts and just need a signature.

If you haven't yet, start with the root [README](../README.md) for a one-page overview, then come back here.

## Tutorial

The tutorial is meant to be read in order — each page builds directly on the code from the last one. Pages 1–13 all evolve the same running example, a little task-tracker shell, so you can see how the pieces compound instead of looking at disconnected snippets.

1. [Getting Started](tutorial/01-getting-started.md) — your first shell: subclassing, your first command, `runRepl()`.
2. [Talking to SpEL Directly](tutorial/02-raw-spel.md) — why typed input is just SpEL, and what that buys you for free.
3. [Shorthand Syntax and Fuzzy Command Names](tutorial/03-shorthand-and-fuzzy-matching.md) — `cmd arg` instead of `cmd(arg)`, and abbreviating command names.
4. [Name Patterns and Variables](tutorial/04-name-patterns-and-variables.md) — backtick name patterns, `var`, and `#name` SpEL variables.
5. [Built-in Commands: help, print, prompt, exit](tutorial/05-built-in-commands.md) — the commands every shell gets for free.
6. [Controlling Help and Shorthand Visibility with @Order](tutorial/06-order-and-help-visibility.md) — curating what `help` shows and how commands sort.
7. [History and Scripting](tutorial/07-history-and-scripting.md) — logging expressions to a file, and running batches of them from a script.
8. [Sub-shells and Menu-Driven CLIs](tutorial/08-submenus.md) — nesting shells to build multi-level menus.
9. [Sandboxed Filesystem Shells](tutorial/09-filesystem-shells.md) — persisting data as files with `FileSystemAwareSpelShellImpl`.
10. [Error Handling: ShellException vs ShellExitException](tutorial/10-error-handling.md) — recoverable errors vs. exiting the loop.
11. [Custom Type Converters](tutorial/11-custom-type-converters.md) — teaching SpEL to turn typed strings into your own types.
12. [REPL Hooks: Prompts, Comments, and Interceptors](tutorial/12-repl-hooks.md) — the five `ReplConfig` hooks that drive the loop.
13. [Operator Overloading](tutorial/13-operator-overloading.md) — giving SpEL operators new meaning for your own types.
14. [Other Extension Points and Current Limits](tutorial/14-other-extension-points.md) — the remaining hooks, and what isn't pluggable yet.

## Reference

Thin, lookup-oriented pages — signatures and one-line descriptions, with links back to the tutorial page that teaches each concept in context.

- [Shell Classes](reference/shell-classes.md) — `CoreSpelShellImpl` / `BaseSpelShellImpl` / `FileSystemAwareSpelShellImpl`.
- [Built-in Commands](reference/built-in-commands.md) — every command your shell gets without writing a line of code.
- [ReplConfig](reference/repl-config.md) — all five REPL hooks in one table.
- [ShellUtils](reference/shell-utils.md) — the shorthand rewrite rules, fuzzy matching, and other static helpers.
- [Exceptions](reference/exceptions.md) — `ShellException` and `ShellExitException`.

See [reference/index.md](reference/index.md) for a single flat table across all of the above.
