# 7. History and Scripting

Two independent features on this page: logging everything you type to a file, and running a batch of expressions non-interactively from a script.

## Turning on history

History is off by default. Turn it on by pointing `getReplConfig().setExprHistoryFile(...)` at a file:

```java
public static void main(String[] args) {
    TaskShell shell = new TaskShell();
    shell.getReplConfig().setExprHistoryFile(new File("tasks-history.log"));
    shell.runRepl();
}
```

Every evaluated expression gets appended to that file as `<ISO-8601 timestamp> <what you typed>` — specifically, what you typed *before* shorthand rewriting (but after backtick name-pattern expansion), so the history stays readable rather than filling up with expanded `var('x', ...)` calls. Three kinds of expressions are deliberately excluded from the log: anything that rewrites to `help(...)`, `hist(...)`, or `exit(...)` — so browsing your own history doesn't clutter the history itself.

Read it back with the `hist` command: `hist()`/`hist(int)` prints the last *N* entries (100 by default), and `hist(String)` filters by substring:

```
SpEL> addTask 'Read a book'
Added: Read a book
SpEL> lt
Buy milk
Read a book
SpEL> hist 2
2026-09-11T10:15:32Z addTask 'Read a book'
2026-09-11T10:15:41Z lt
```

**A gotcha worth knowing about:** `getReplConfig()` returns the `ReplConfig` used for the *interactive* loop. There's a second, separate `ReplConfig` — `getReplConfigForScript()` — used when you call `runScript(...)`, and it has its own independent `exprHistoryFile`. Setting one doesn't set the other. If you want script-run expressions logged too, set both explicitly.

## Running a script

`runScript(String)`, `runScript(Path)` (covered in [page 9](09-filesystem-shells.md)), and `runScript(LineReader)` all feed a batch of expressions through the exact same evaluation pipeline as the interactive loop — shorthand rewriting included — just without printing a prompt or echoing each result by default, and with a stricter default: any `Exception` (not just `ShellExitException`) stops the script.

A common use: seed some starter data before dropping into the interactive prompt, the same way the framework's own `Example1` fixture runs an init script before calling `runRepl()`. Create `init-tasks.txt`:

```
// seed a couple of starter tasks
addTask 'Buy milk'
addTask 'Walk the dog'
```

`//`-prefixed lines are comments by default (`ReplConfig.getIsCommentLine()`), and — as shown here — shorthand syntax works inside scripts exactly as it does interactively, since it's the same `expressionInterceptor` doing the rewriting either way.

```java
public static void main(String[] args) {
    TaskShell shell = new TaskShell();
    shell.getReplConfig().setExprHistoryFile(new File("tasks-history.log"));
    shell.runScript(ShellUtils.lineReader(new File("init-tasks.txt")));
    shell.runRepl();
}
```

(`runScript(LineReader)` is the most direct option here since `TaskShell` doesn't yet know about the filesystem-aware `runScript(Path)` — that comes with `FileSystemAwareSpelShellImpl` in the next page. `ShellUtils.lineReader(File)` is the same helper the framework uses internally to turn a file into a line-by-line source.)

```
SpEL> lt
Buy milk
Walk the dog
```

Both starter tasks are already there by the time the interactive prompt appears.

---
Previous: [6. Controlling Help and Shorthand Visibility with @Order](06-order-and-help-visibility.md) · Next: [8. Sub-shells and Menu-Driven CLIs](08-submenus.md)
