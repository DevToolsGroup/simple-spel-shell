# 13. Operator Overloading

SpEL lets you give existing operators new meaning for your own types. This page uses it for something genuinely useful in a task tracker: `` `release3` - 3 `` fuzzy-finds a task named something like "Release 3.0" and pushes its due date back three days.

## The `OperatorOverloader` SPI

Spring's `OperatorOverloader` interface has two methods: `overridesOperation(Operation, Object left, Object right)`, asked before SpEL tries anything else, and `operate(Operation, Object left, Object right)`, called if the first one said yes. The framework ships two implementations:

- **`EmptyOperatorOverloader`** — the default. `overridesOperation` always returns `false`, so every operator behaves exactly as plain SpEL defines it.
- **`BasicOperatorOverloader`** — an opt-in example that overloads `/` between two `String`/`Path` operands to join paths (`"a" / "b"` → `Path` `a/b`). It's not active by default; a shell that wants it calls `getSpelEvaluator().setOperatorOverloader(new BasicOperatorOverloader())` explicitly.

`setOperatorOverloader` takes one implementation at a time — it replaces whatever was configured before, the same way `setTypeConverters` replaces the whole converter list (page 11). If you needed both `BasicOperatorOverloader`'s path-joining *and* the overloader below at once, you'd write one class that delegates to both rather than call `setOperatorOverloader` twice.

## Shifting a due date by name

```java
public class TaskDueDateOperatorOverloader implements OperatorOverloader {
    private final TaskShell shell;

    public TaskDueDateOperatorOverloader(TaskShell shell) {
        this.shell = shell;
    }

    @Override
    public boolean overridesOperation(Operation operation, Object left, Object right) {
        return (operation == Operation.SUBTRACT || operation == Operation.ADD)
            && left instanceof NamePattern && right instanceof Number;
    }

    @Override
    public Object operate(Operation operation, Object left, Object right) {
        String title = shell.findTaskTitle(((NamePattern) left).pattern());
        long days = ((Number) right).longValue();
        LocalDate shifted = shell.getDueDate(title)
            .plusDays(operation == Operation.SUBTRACT ? -days : days);
        shell.setDueDate(title, shifted);
        return "%s due %s".formatted(title, shifted);
    }
}
```

`overridesOperation` only claims `SUBTRACT`/`ADD` when the left operand is a `NamePattern` and the right is a `Number` — everything else (`1+2`, string concatenation, and so on) falls through to SpEL's own built-in behavior untouched. `operate` does the real work: find the one task whose title fuzzy-matches the pattern, shift its due date, save it, and return a short confirmation string.

`TaskShell` needs two small helpers alongside `getDueDate`/`setDueDate` from page 11 — `findTaskTitle` reuses the exact same fuzzy `ShellUtils.matches` matcher that powers shorthand command names (page 3) and backtick patterns (page 4), applied here to task titles instead of method names, with the same "zero or multiple matches is an error" behavior as `ShellUtils`'s own internal method resolution:

```java
List<String> taskTitles() {
    return findFilesByName(new NamePattern("")).stream()
        .filter(File::isFile)
        .map(f -> f.getName().replace(".task", ""))
        .toList();
}

String findTaskTitle(String pattern) {
    List<String> found = taskTitles().stream()
        .filter(title -> ShellUtils.matches(title, pattern))
        .toList();
    if (found.isEmpty()) {
        throw new ShellException(false, "No task matches '" + pattern + "'");
    }
    if (found.size() > 1) {
        throw new ShellException(false, "Multiple tasks match '" + pattern + "': " + found);
    }
    return found.getFirst();
}

LocalDate getDueDate(String title) {
    List<String> lines = read(Path.of(title + ".task")).lines().toList();
    return lines.size() > 1 && !lines.get(1).isBlank() ? LocalDate.parse(lines.get(1)) : LocalDate.now();
}
```

Wire the overloader in during construction:

```java
getSpelEvaluator().setOperatorOverloader(new TaskDueDateOperatorOverloader(this));
```

## Trying it out

```
SpEL> addTask 'Release 3.0'
Added: Release 3.0
SpEL> setDueDate('Release 3.0', '2026-09-20')
SpEL> `release3` - 3
Release 3.0 due 2026-09-17
SpEL> `release3` + 7
Release 3.0 due 2026-09-24
```

Walking through what actually happens on the `` `release3` - 3 `` line: the backtick syntax rewrites it to `npat('release3') - 3` before shorthand rewriting even runs (page 4); that string doesn't match any of the shorthand rules from page 3, so it's evaluated as-is — a plain SpEL subtraction between the result of calling `npat('release3')` (a `NamePattern`) and the integer `3`. Since neither operand is a type SpEL subtracts natively, it asks the configured `OperatorOverloader`, which says yes and does the real work. Nothing about the shell's command-dispatch machinery is involved at all past the `npat(...)` call — it's ordinary SpEL operator resolution, pointed at code you wrote.

---
Previous: [12. REPL Hooks: Prompts, Comments, and Interceptors](12-repl-hooks.md) · Next: [14. Other Extension Points and Current Limits](14-other-extension-points.md)
