# 11. Custom Type Converters

So far, every command parameter has been a `String`, `int`, or `Path`
— types SpEL either handles natively or that the framework already knows how to convert from a typed string.
This page adds a due date to each task,
typed as a plain string at the prompt but received as a real `java.time.LocalDate`.

## Where the existing conversions come from

Typing `cd 'tasks'` works because `cd(Path)` takes a `Path`,
you typed a `String`,
and something coerces one into the other before the method is invoked.
That "something" is a `List<Converter<?, ?>>` on the `SpelEvaluator`,
exposed via `getTypeConverters()`/`setTypeConverters(...)`.
`FileSystemAwareSpelShellImpl` registers exactly this kind of converter in its own constructor:

```java
List<Converter<?, ?>> typeConverters = new ArrayList<>(getSpelEvaluator().getTypeConverters());
typeConverters.add(new Converter<String, Path>() {
    @Override
    public Path convert(String first) {
        return Path.of(first);
    }
});
typeConverters.add(new Converter<String, NamePattern>() {
    @Override
    public NamePattern convert(String pattern) {
        return new NamePattern(pattern);
    }
});
getSpelEvaluator().setTypeConverters(typeConverters);
```

That's the exact mechanism `TaskShell` has been relying on since page 9,
just written by the framework instead of by you.
Adding your own converter uses the same pattern.

## A `String → LocalDate` converter

`setTypeConverters` *replaces* the whole list rather than appending to it
— call it with a fresh list containing only your new converter,
and you'd silently lose the `Path`/`NamePattern` converters `FileSystemAwareSpelShellImpl` already registered,
breaking `cd`, `mkdir`, and every other command that relies on them.
So, you need to **read the existing list before writing a new one**
as shown above: fetch the current list, add to a copy, write the copy back.

```java
package org.devtoolsgroup.tutorial.example9;

public class TaskShell extends FileSystemAwareSpelShellImpl {

    ...

    public TaskShell(Path tasksDir) {
        super(tasksDir);
        List<Converter<?, ?>> converters = new ArrayList<>(getSpelEvaluator().getTypeConverters());
        converters.add(new Converter<String, LocalDate>() {
            @Override
            public LocalDate convert(String source) {
                return LocalDate.parse(source);
            }
        });
        getSpelEvaluator().setTypeConverters(converters);
    }

    ...
    
}
```

## Using it

Add a command that takes a `LocalDate`, and store the due date as a second line in the task file:

```java
package org.devtoolsgroup.tutorial.example9;

public class TaskShell extends FileSystemAwareSpelShellImpl {

    ...

    public void setDueDate(String title, LocalDate date) {
        Path taskFile = Path.of(title + ".task");
        if (!getFile(taskFile).exists()) {
            println("No such task: " + title);
            return;
        }
        String status = read(taskFile).lines().findFirst().orElse("pending");
        write(taskFile, status + "\n" + date);
    }

    ...
}
```

`setDueDate` takes two parameters,
so — as noted back on page 3 — it isn't eligible for the `cmd arg` shorthand;
call it with a full, parenthesized SpEL expression.
The second argument is still typed as a plain quoted string:

```shell
mvn test-compile exec:java -Dexec.classpathScope=test -Dexec.mainClass=org.devtoolsgroup.tutorial.example9.TaskShell
```

```
SpEL> setDueDate('Buy milk', '2026-09-20')
```

SpEL matches this call against `setDueDate(String, LocalDate)`,
sees the second argument is a `String` where a `LocalDate` is needed,
and reaches for the registered `TypeConverter` to bridge the gap
— the same conversion step that already makes `cd('tasks')` work for a `Path` parameter,
just with a converter you wrote instead of one the framework shipped.

---
Previous: [10. Error Handling: ShellException vs ShellExitException](10-error-handling.md) · Next: [12. REPL Hooks: Prompts, Comments, and Interceptors](12-repl-hooks.md)
