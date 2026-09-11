# 3. Shorthand Syntax and Fuzzy Command Names

Typing `addTask('Buy milk')` at a prompt every time is tedious. This page introduces the rewriting layer that lets you type `addTask 'Buy milk'` instead — and abbreviate `addTask` further still.

## Two new commands

First, let's give `TaskShell` a couple more methods to play with:

```java
public void listTasks() {
    if (tasks.isEmpty()) {
        println("No tasks yet.");
        return;
    }
    tasks.forEach(this::println);
}

public void completeTask(String title) {
    tasks.remove(title);
}
```

## The rewrite rules

Before your typed line reaches the SpEL evaluator, it passes through `ShellUtils.rewriteExpr`, which tries a handful of patterns in order and rewrites the first one that matches:

| You type | Gets rewritten to |
|---|---|
| `cmd` (bare identifier) | `cmd()` |
| `cmd arg` | `cmd(arg)` |
| `x = value` | `var('x', value)` |
| `x = cmd` | `var('x', cmd())` |
| `x = cmd arg` | `var('x', cmd(arg))` |
| anything else | left unchanged, evaluated as raw SpEL |

The `arg` part is inserted verbatim as SpEL — it isn't quoted for you. That means a bare word like `1` or `#someVar` works as-is, but a string needs SpEL's own quotes:

```
SpEL> addTask 'Buy milk'
Added: Buy milk
SpEL> addTask 'Walk the dog'
Added: Walk the dog
SpEL> listTasks
Buy milk
Walk the dog
```

`addTask 'Buy milk'` became `addTask('Buy milk')` — a normal, valid SpEL method call — before being evaluated exactly the way page 2 described. Note that this shorthand only applies to methods with **zero or one parameter**; a method like `write(Path, String)` (which we'll meet in [page 9](09-filesystem-shells.md)) always needs the full `write(...)` call with real SpEL arguments.

## Fuzzy command names

The framework doesn't require the exact method name either. When rewriting `cmd` or `cmd arg`, it fuzzy-matches whatever you typed against the set of exposed zero-arg or one-arg method names, and — if exactly one method matches — uses that method. The matcher treats camelCase boundaries the way an IDE's "go to symbol" search does: the letters you type must appear, in order, as the start of successive camelCase segments.

```
SpEL> lt
Buy milk
Walk the dog
SpEL> at 'Read a book'
Added: Read a book
SpEL> ct 'Walk the dog'
SpEL> lt
Buy milk
Read a book
```

`lt` matched `listTasks` (**l**ist**T**asks), `at` matched `addTask`, and `ct` matched `completeTask` — each unambiguously, since no other exposed command starts the same way. If your abbreviation matches more than one command, or none at all, the shell throws a `ShellException` listing the candidates (or telling you nothing matched) instead of guessing.

---
Previous: [2. Talking to SpEL Directly](02-raw-spel.md) · Next: [4. Name Patterns and Variables](04-name-patterns-and-variables.md)
