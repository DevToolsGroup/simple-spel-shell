# 8. Sub-shells and Menu-Driven CLIs

So far every command has acted on the whole task list.
This page adds a `viewTask` command that opens a focused sub-shell for editing one task
— the pattern the framework uses for menu-driven CLIs.

## Tasks become objects

Editing a single task's fields needs more than a `String` title, so `tasks` becomes a list of small `Task` objects:

```java
public class Task {
    String title;
    boolean done;
    String notes = "";

    Task(String title) {
        this.title = title;
    }
}
```

```java
private final List<Task> tasks = new ArrayList<>();

public void addTask(String title) {
    tasks.add(new Task(title));
    println("Added: " + title);
}

public void listTasks() {
    if (tasks.isEmpty()) {
        println("No tasks yet.");
        return;
    }
    tasks.forEach(t -> println((t.done ? "[x] " : "[ ] ") + t.title));
}

public void completeTask(String title) {
    Task task = findTask(title);
    if (task == null) {
        println("No such task: " + title);
        return;
    }
    task.done = true;
}

private Task findTask(String title) {
    return tasks.stream().filter(t -> t.title.equals(title)).findFirst().orElse(null);
}
```

(We'll harden that "no such task" handling with `ShellException` in [page 10](10-error-handling.md)
— for now, a plain message is enough.)

## A nested shell for one task

`viewTask(String title)` opens a dedicated shell scoped to a single `Task`,
with its own commands (`rename`, `markDone`, `addNote`)
and its own dynamic prompt:

```java
public void viewTask(String title) {
    Task task = findTask(title);
    if (task == null) {
        println("No such task: " + title);
        return;
    }
    new TaskDetailShell(this, task).runRepl();
}

private static class TaskDetailShell extends BaseSpelShellImpl {
    private final Task task;

    TaskDetailShell(BaseSpelShell parent, Task task) {
        super(parent);
        this.task = task;
        getReplConfig().setPrompt(_ ->
            "-------------------------------\n" +
                "Task: " + task.title + (task.done ? " [done]" : "") + "\n" +
                "[task] SpEL> "
        );
        setOnExit(ShellUtils.exnExit(false));
    }

    public void rename(String newTitle) {
        task.title = newTitle;
    }

    public void markDone() {
        task.done = true;
    }

    public void addNote(String note) {
        task.notes = note;
        var("lastEdited", task.title);
    }
}
```

`super(parent)` (the `BaseSpelShellImpl(BaseSpelShell parentShell)` constructor)
is what makes this a genuine *sub*-shell rather than an unrelated shell instance:
it shares the parent's `Console` and, crucially, its `SpelEvaluator`
— so SpEL variables set in one are visible in the other.
`getReplConfig()` on the child is still its own separate `ReplConfig` object
(inherited by copy from the parent at construction time),
which is exactly what lets it have its own prompt without disturbing the parent's.

## Exit vs. "go back"

`exit()` is a built-in command
— but inside `TaskDetailShell`, we don't want it to end the whole program,
just return to the task list.
`setOnExit(ShellUtils.exnExit(false))` makes `exit()` throw a `ShellExitException(false)`
instead of calling the default `System.exit(0)`.
That exception unwinds out of `TaskDetailShell.runRepl()`, back through `viewTask(...)`,
and into `TaskShell`'s own `runRepl()` loop
— where, by default, `ShellExitException` is exactly the exception class configured to stop *that* loop too.
Left alone, "going back" from the sub-shell would silently exit `TaskShell` as well.

The fix is to override `runRepl()` on `TaskShell` itself to catch that signal and loop instead of returning
— the same pattern the framework's own `Example2` fixture uses for its main menu:

```java
public TaskShell() {
    setMinOrderForHelp(0);
    setOnExit(ShellUtils.exnExit(true));
}

@Order(-1000)
@Override
public Object runRepl() {
    while (true) {
        try {
            super.runRepl();
        } catch (ShellExitException ex) {
            if ((boolean) ex.getResult()) {
                return null;
            }
        }
    }
}
```

`TaskShell`'s own `exit()` now throws `ShellExitException(true)`
— caught here, `true` means "really exit," so we return.
`TaskDetailShell`'s `exit()` throws `ShellExitException(false)`
— caught here too, but `false` means "just fall through,"
so the `while (true)` loop calls `super.runRepl()` again,
re-entering the interactive loop right where the task list left off.

## Trying it out

```
SpEL> at 'Buy milk'
Added: Buy milk
SpEL> viewTask 'Buy milk'
-------------------------------
Task: Buy milk
[task] SpEL> markDone
-------------------------------
Task: Buy milk [done]
[task] SpEL> addNote 'Get oat milk'
-------------------------------
Task: Buy milk [done]
[task] SpEL> exit
SpEL> lt
[x] Buy milk
SpEL> #lastEdited
Buy milk
```

`#lastEdited` — set inside the sub-shell via `var("lastEdited", ...)` —
is still visible after returning to the main menu,
because both shells share the same underlying `SpelEvaluator` and its variable table.

---
Previous: [7. History and Scripting](07-history-and-scripting.md) · Next: [9. Sandboxed Filesystem Shells](09-filesystem-shells.md)
