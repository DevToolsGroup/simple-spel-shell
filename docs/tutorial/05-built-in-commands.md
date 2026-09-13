# 5. Built-in Commands: help, print, prompt, exit

`BaseSpelShellImpl` — the class `TaskShell` extends —
comes with a set of commands beyond `var`, which we already used on the last page.
This page covers the rest: `help`, the `print` family, `prompt`, and `exit`.

## `help`

`help()` lists every exposed command with its signature. Try it now, before we add anything else:

```
SpEL> help
eval(rootObject: Object, expression: String): Object
exit(): void
exit(result: Object): void
exn(msg: String): void
exnf(format: String, args: Object[]): void
format(format: String, args: Object[]): String
help(): void
help(pattern: NamePattern): void
help(pattern: String): void
hist(): void
hist(num: int): void
hist(substring: String): void
npat(str: String): NamePattern
print(obj: Object): void
printf(format: String, args: Object[]): void
println(obj: Object): void
prompt(prompt: String): String
runScript(script: String): Object
runScript(scriptLineReader: LineReader): Object
setLastEvalResultMaxPrintLength(lastEvalResultMaxPrintLength: int): void
var(): void
var(name: String): Object
var(pattern: NamePattern): void
var(name: String, value: Object): Object
---
addTask(title: String): void
completeTask(title: String): void
listTasks(): void
```

That's every built-in framework command, followed by a `---` divider, followed by our own three commands.
The divider marks the transition from negative-`@Order` framework commands to your own (order `0` by default)
— page 6 explains exactly why,
and how to hide the built-ins from this list entirely.

`help` also takes a filter:
`help(String)` and `help(NamePattern)` restrict the listing to commands whose name fuzzy-matches.
Using the backtick syntax from page 4:

```
SpEL> he `task
addTask(title: String): void
completeTask(title: String): void
listTasks(): void
```

(`he` is a fuzzy match for `help` itself
— the framework's shorthand and fuzzy-matching apply to its own commands too, not just yours.)

## print, println, printf, format

`TaskShell.addTask` already uses `println`. The full family:

- `print(Object)` — writes without a trailing newline.
- `println(Object)` — writes with one.
- `printf(String format, Object... args)` — `String.format`-style, written straight to the console.
- `format(String format, Object... args)` — same formatting,
  but *returns* the string instead of printing it
  (useful when you want to build a string for `println` or `var`).

## `prompt` — asking the user for input

`prompt(String)` prints a message and blocks for one line of input, returning it as a `String`.
Let's add an interactive way to create a task:

```java
public void addTaskInteractive() {
    String title = prompt("Task title: ");
    addTask(title);
}
```

```shell
mvn test-compile exec:java -Dexec.classpathScope=test -Dexec.mainClass=org.devtoolsgroup.tutorial.example3.TaskShell
```

```
SpEL> addTaskInteractive
Task title: Buy milk
Added: Buy milk
```

## `exit`

`exit()` (or `exit(result)` to carry a value out) ends the current `runRepl()` loop.
Under the hood it doesn't just `return`
— it calls a configurable `Consumer<Object>` (`getOnExit()`/`setOnExit(...)`)
whose default implementation is `System.exit(0)`.
We'll override that default in [page 8](08-submenus.md),
where a nested shell's `exit()` needs to mean "go back one level," not "kill the process."

---
Previous: [4. Name Patterns and Variables](04-name-patterns-and-variables.md) · Next: [6. Controlling Help and Shorthand Visibility with @Order](06-order-and-help-visibility.md)
