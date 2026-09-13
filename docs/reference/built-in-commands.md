# Reference: Built-in Commands

Every command available without writing a line of code, grouped by the class that introduces it.

## From `BaseSpelShellImpl` — introduced on [page 5](../tutorial/05-built-in-commands.md)

| Command                                              | Description                                                                                                                              |
|------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------|
| `help()`                                             | List every exposed command.                                                                                                              |
| `help(String pattern)` / `help(NamePattern pattern)` | List commands whose name fuzzy-matches `pattern`.                                                                                        |
| `var(String name, Object value)`                     | Set a SpEL variable; returns `value`. Reachable via the `x = value` shorthand — [page 4](../tutorial/04-name-patterns-and-variables.md). |
| `var(String name)`                                   | Read a variable's value back.                                                                                                            |
| `var()`                                              | List all variables and their runtime types.                                                                                              |
| `var(NamePattern pattern)`                           | List variables whose name fuzzy-matches `pattern`.                                                                                       |
| `hist()` / `hist(int num)`                           | Print the last `num` (default 100) history entries.                                                                                      |
| `hist(String substring)`                             | Print history entries containing `substring`.                                                                                            |
| `print(Object)` / `println(Object)`                  | Write to the console, with or without a trailing newline.                                                                                |
| `printf(String format, Object... args)`              | `String.format`-style write.                                                                                                             |
| `format(String format, Object... args)`              | Same formatting, returned as a `String` instead of printed.                                                                              |
| `prompt(String message)`                             | Print `message`, block for one line of input, return it.                                                                                 |
| `exit()` / `exit(Object result)`                     | Invoke `getOnExit()` (default `System.exit(0)`) — see [page 8](../tutorial/08-submenus.md) for overriding it.                            |
| `npat(String str)`                                   | Construct a `NamePattern` — same as the `` `str` `` backtick syntax.                                                                     |
| `exn(String msg)`                                    | Throw `new ShellException(msg)`.                                                                                                         |
| `exnf(String format, Object... args)`                | Throw a `ShellException` with a formatted message.                                                                                       |
| `setOnExit(Consumer<Object>)` / `getOnExit()`        | What `exit(...)` invokes.                                                                                                                |
| `setMinOrderForHelp(int)` / `getMinOrderForHelp()`   | Display filter for `help` — see [page 6](../tutorial/06-order-and-help-visibility.md).                                                   |

## From `FileSystemAwareSpelShellImpl` — introduced on [page 9](../tutorial/09-filesystem-shells.md)

All at order −100 unless noted.

| Command                                  | Description                                                                                           |
|------------------------------------------|-------------------------------------------------------------------------------------------------------|
| `cd(Path)` / `cd()`                      | Change directory (bare form goes up one level); returns the new absolute path.                        |
| `pwd()`                                  | Print the current absolute path.                                                                      |
| `ll(Path)` / `ll()`                      | List the *immediate* children of a directory (default: current), with file sizes.                     |
| `mkdir(Path)`                            | Create a directory and `cd` into it.                                                                  |
| `mkdir(boolean autoCd, Path)`            | Same, with `cd` behavior controllable.                                                                |
| `getFile(Path)`                          | Resolve a sandboxed `Path` to a `java.io.File`.                                                       |
| `write(Path, String text)`               | Write `text` to a file, creating parent directories as needed.                                        |
| `read(Path)`                             | Read a file's full contents as a `String`.                                                            |
| `findFilesByName(NamePattern)`           | Recursively find files and directories under the current directory whose relative path fuzzy-matches. |
| `listFiles(NamePattern)` / `listFiles()` | Like `findFilesByName`, filtered to regular files.                                                    |
| `listDirs(NamePattern)` / `listDirs()`   | Like `findFilesByName`, filtered to directories.                                                      |
| `runScript(Path)`                        | Run a script file, resolved within the sandbox.                                                       |
| `getWorkingDirectory()`                  | The underlying `WorkingDirectory`.                                                                    |

---
Back to [reference index](index.md) · [documentation index](../index.md)
