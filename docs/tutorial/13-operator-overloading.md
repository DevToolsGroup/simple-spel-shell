# 13. Operator Overloading

SpEL lets you give existing operators new meaning for your own types.
This page uses it for something genuinely useful in a task tracker:
`` `rel3` + 10 `` fuzzy-finds a task named something like "Release 3.0"
and computes what its due date would be ten days later.

## The `OperatorOverloader` SPI

Spring's `OperatorOverloader` interface has two methods:
`overridesOperation(Operation, Object left, Object right)`, asked before SpEL tries anything else,
and `operate(Operation, Object left, Object right)`, called if the first one said yes.
The framework ships two implementations:

- **`EmptyOperatorOverloader`** — the default in BaseSpelShellImpl.
`overridesOperation` always returns `false`, so every operator behaves exactly as plain SpEL defines it.
- **`BasicOperatorOverloader`** — the default in FileSystemAwareSpelShellImpl.
It overloads `/` between two `String`/`Path` operands to join paths
(`"a" / "b"` → `Path` `a/b`).

`setOperatorOverloader` takes one implementation at a time
— it replaces whatever was configured before,
the same way `setTypeConverters` replaces the whole converter list (page 11).
`TaskShell` extends `FileSystemAwareSpelShellImpl`,
which already installs `BasicOperatorOverloader` as its default, giving `/` its path-joining behavior.
Calling `setOperatorOverloader` with a class that only knows about due-date shifting would silently turn that off.
The overloader below avoids that by extending `BasicOperatorOverloader` itself
and delegating to `super` for anything it doesn't recognize,
so path-joining and date-shifting both stay available at once.

## Shifting a due date by name

Here's an operator overloader that recognizes a `NamePattern` shifted by a `Number` of days,
and falls back to `BasicOperatorOverloader` for everything else:

```java
package org.devtoolsgroup.tutorial.example12;

import org.devtoolsgroup.simplespelshell.BasicOperatorOverloader;
import org.devtoolsgroup.simplespelshell.NamePattern;
import org.springframework.expression.Operation;

public class TaskDueDateOperatorOverloader extends BasicOperatorOverloader {
    private final TaskShell shell;

    public TaskDueDateOperatorOverloader(TaskShell shell) {
        this.shell = shell;
    }

    private boolean isDueDateShift(Operation operation, Object left, Object right) {
        return (operation == Operation.SUBTRACT || operation == Operation.ADD)
            && left instanceof NamePattern && right instanceof Number;
    }

    @Override
    public boolean overridesOperation(Operation operation, Object left, Object right) {
        return isDueDateShift(operation, left, right) || super.overridesOperation(operation, left, right);
    }

    @Override
    public Object operate(Operation operation, Object left, Object right) {
        if (isDueDateShift(operation, left, right)) {
            long days = ((Number) right).longValue();
            return shell.getDueDate((NamePattern) left).plusDays(operation == Operation.SUBTRACT ? -days : days);
        }
        return super.operate(operation, left, right);
    }
}
```

`overridesOperation` claims `SUBTRACT`/`ADD` between a `NamePattern` and a `Number` itself,
and defers to `super.overridesOperation` for everything else
— which is how `/` between two `String`/`Path` operands keeps working unchanged.

`TaskShell` registers the new operator overloader using
`getSpelEvaluator().setOperatorOverloader(new TaskDueDateOperatorOverloader(this))`:

```java
package org.devtoolsgroup.tutorial.example12;

public class TaskShell extends FileSystemAwareSpelShellImpl {

    ...
    
    public TaskShell(Path tasksDir) {
        super(tasksDir);
        // Add a converter to seamlessly go from String to LocalDate
        List<Converter<?, ?>> converters = new ArrayList<>(getSpelEvaluator().getTypeConverters());
        converters.add(new Converter<String, LocalDate>() {
            @Override
            public LocalDate convert(String source) {
                return LocalDate.parse(source);
            }
        });
        getSpelEvaluator().setTypeConverters(converters);
        // Wire the overloader
        getSpelEvaluator().setOperatorOverloader(new TaskDueDateOperatorOverloader(this));
    }

    public void setDueDate(NamePattern pattern, LocalDate date) {
        Path taskFile = findTaskByPattern(pattern).toPath();
        String status = read(taskFile).lines().findFirst().orElse("pending");
        write(taskFile, status + "\n" + date);
    }

    public LocalDate getDueDate(NamePattern pattern) {
        Path taskFile = findTaskByPattern(pattern).toPath();
        List<String> lines = read(taskFile).lines().toList();
        return lines.size() > 1 && !lines.get(1).isBlank() ? LocalDate.parse(lines.get(1)) : LocalDate.now();
    }
    
    ...

    private File findTaskByPattern(NamePattern pattern) {
        List<File> found = findFilesByName(pattern).stream()
            .filter(File::isFile)
            .filter(file -> file.getName().endsWith(".task"))
            .toList();
        if (found.isEmpty()) {
            throw new ShellException(false, "Could not find a task by pattern '" + pattern.pattern() + "'.");
        }
        if (found.size() > 1) {
            throw new ShellException(false, "More than one task was found by pattern '" + pattern.pattern() + "'.");
        }
        return found.getFirst();
    }
}
```

## Trying it out

```shell
mvn test-compile exec:java -Dexec.classpathScope=test -Dexec.mainClass=org.devtoolsgroup.tutorial.example12.TaskShell
```

```
SpEL> addTask 'Release 3.0'
Added: Release 3.0
SpEL> addTask 'Release 4.0'
Added: Release 4.0
SpEL> setDueDate(`rel3`, '2026-09-20')
SpEL> setDueDate(`rel4`, `rel3` + 10)
SpEL> getDueDate `rel4
2026-09-30
```

---
Previous: [12. REPL Hooks: Prompts, Comments, and Interceptors](12-repl-hooks.md) · Next: [14. Other Extension Points and Current Limits](14-other-extension-points.md)
