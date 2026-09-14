# 9. Sandboxed Filesystem Shells

Up to now, tasks have lived only in memory — they disappear when the shell exits.
This page switches `TaskShell` to extend `FileSystemAwareSpelShellImpl` instead of `BaseSpelShellImpl`,
so each task becomes a file on disk.
We're trading the `Task` object and the `viewTask` sub-shell from [page 8](08-submenus.md) for plain files;
the submenu technique from that page is not needed for what follows here.

## Switching base classes

`FileSystemAwareSpelShellImpl` extends `BaseSpelShellImpl`,
so every command from the earlier pages (`help`, `var`, `exit`, and so on) is still there
— this class adds a *sandboxed working directory* and the commands that go with it:
`cd`, `pwd`, `ll`, `mkdir`, `read`, `write`, `findFilesByName`, `listFiles`, `listDirs`.

```java
package org.devtoolsgroup.tutorial.example8;

public class TaskShell extends FileSystemAwareSpelShellImpl {

    public static void main(String[] args) throws IOException {
        Path tasksDir = Path.of("target/tasks");
        Files.createDirectories(tasksDir);
        new TaskShell(tasksDir).runRepl();
    }

    public TaskShell(Path tasksDir) {
        super(tasksDir);
    }

    // addTask / listTasks / completeTask below
}
```

One important detail: the constructor of `FileSystemAwareSpelShellImpl`
requires the directory you pass it to **already exist**
— it fails fast with a `ShellException` otherwise —
so `main` creates it first.

## Tasks as files

Each task becomes one file, named after its title, holding a single word (`pending` or `done`) as its content:

```java
package org.devtoolsgroup.tutorial.example8;

public class TaskShell extends FileSystemAwareSpelShellImpl {

    ...

    public void addTask(String title) {
        write(Path.of(title + ".task"), "pending");
        println("Added: " + title);
    }

    public void listTasks() {
        List<File> files = findFilesByName(new NamePattern("")).stream()
                .filter(File::isFile)
                .toList();
        if (files.isEmpty()) {
            println("No tasks yet.");
            return;
        }
        for (File file : files) {
            String title = file.getName().replace(".task", "");
            String status = read(file.toPath());
            println((status.equals("done") ? "[x] " : "[ ] ") + title);
        }
    }

    public void completeTask(String title) {
        Path taskFile = Path.of(title + ".task");
        if (!getFile(taskFile).exists()) {
            println("No such task: " + title);
            return;
        }
        write(taskFile, "done");
    }
}
```

`write(Path, String)` and `read(Path)` are two of the built-in filesystem commands
— both resolve their `Path` argument against the shell's *current* working directory
and refuse to touch anything outside the sandbox root you passed to the constructor.
`findFilesByName(NamePattern)` walks the current directory recursively and fuzzy-matches names against the pattern;
an empty pattern (`new NamePattern("")`) matches everything,
which is how `listTasks` finds every task file regardless of what it's named.

## `cd`, `mkdir`, and `ll`

Because tasks are just files now, the built-in directory commands work on them directly
— including organizing tasks into subfolders:

```shell
mvn test-compile exec:java -Dexec.classpathScope=test -Dexec.mainClass=org.devtoolsgroup.tutorial.example8.TaskShell
```

```
SpEL> at 'Buy milk'
Added: Buy milk
SpEL> at 'Walk the dog'
Added: Walk the dog
SpEL> ct 'Walk the dog'
SpEL> ll
7 Buy milk.task
4 Walk the dog.task
SpEL> mkdir 'urgent'
/home/dev/Projects/simple-spel-shell/target/tasks/urgent
SpEL> at 'Call dentist'
Added: Call dentist
SpEL> lt
[ ] Call dentist
SpEL> cd
/home/dev/Projects/simple-spel-shell/target/tasks
SpEL> lt
[ ] Call dentist
[ ] Buy milk
[x] Walk the dog
SpEL> ll
  urgent/
7 Buy milk.task
4 Walk the dog.task
```

A couple of things worth noticing:

- `mkdir 'urgent'` both creates the directory and `cd`s into it;
its output is the new directory's absolute path.
- Once inside `urgent/`, `lt` only shows `Call dentist` — `findFilesByName` walks from the *current* directory,
so `cd` changes what `listTasks` can see.
- Bare `cd` (no argument) goes up one level (`cd(Path.of(".."))`),
and its printed output is simply the return value of `cd(Path)` — a `Path` —
being auto-printed like any other non-`null` result.
- Back at the root, `lt` shows all three tasks, including `Call dentist`
— `findFilesByName` walks recursively,
so a task nested inside `urgent/` is still found.
`ll`, in contrast, only lists the *immediate* children of the current directory,
which is why it shows `urgent/` as a single entry rather than reaching into it.

Any attempt to `cd` or `write` outside the `tasks` directory you constructed the shell with throws a `ShellException`
— that sandbox boundary is enforced on every path, not just the ones typed at the prompt.

---
Previous: [8. Sub-shells and Menu-Driven CLIs](08-submenus.md) · Next: [10. Error Handling: ShellException vs ShellExitException](10-error-handling.md)
