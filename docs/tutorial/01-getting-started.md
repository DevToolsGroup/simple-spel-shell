# 1. Getting Started

This tutorial builds one small CLI tool — a task tracker — from an empty class all the way up through every feature simple-spel-shell offers. Each page adds to the code from the previous one, so it's worth reading in order at least once.

## The two base classes

Everything in simple-spel-shell starts with a shell class. There are two you can extend:

- `org.devtoolsgroup.simplespelshell.impl.CoreSpelShellImpl` — the bare REPL engine. It knows how to print a prompt, read a line, evaluate it as SpEL, and loop. It has no built-in commands at all — not even `help` or `exit`.
- `org.devtoolsgroup.simplespelshell.impl.BaseSpelShellImpl` — `CoreSpelShellImpl` plus a set of built-in commands every practical shell wants: `help`, `var`, `hist`, `print`/`println`/`printf`, `prompt`, `exit`. This is what you'll extend for almost anything you build.

There's a third class, `FileSystemAwareSpelShellImpl`, which adds a sandboxed working directory on top of `BaseSpelShellImpl` — we'll get to it in [page 9](09-filesystem-shells.md).

## Your first shell

A shell is just a class with public methods. Each public method becomes a command automatically — there's no registration step, no annotation required to opt in.

```java
package com.example.taskshell;

import org.devtoolsgroup.simplespelshell.impl.BaseSpelShellImpl;

import java.util.ArrayList;
import java.util.List;

public class TaskShell extends BaseSpelShellImpl {

    private final List<String> tasks = new ArrayList<>();

    public static void main(String[] args) {
        new TaskShell().runRepl();
    }

    public void addTask(String title) {
        tasks.add(title);
        println("Added: " + title);
    }
}
```

`runRepl()` starts the interactive loop: it prints a prompt, reads a line, evaluates it, prints the result if there is one, and repeats until you call `exit()`. `println` is one of the built-in commands from `BaseSpelShellImpl` — since your shell class extends it, you can call it directly from your own methods just like any other inherited method.

## Running it

```
$ mvn compile exec:java -Dexec.mainClass=com.example.taskshell.TaskShell
SpEL> addTask('Buy milk')
Added: Buy milk
SpEL> addTask('Walk the dog')
Added: Walk the dog
SpEL> exit
```

That `addTask('Buy milk')` line is worth pausing on: it's not a custom command grammar being parsed. It's a genuine SpEL expression — a method call on the shell object — being evaluated with your `TaskShell` instance as SpEL's *root object*. The next page digs into exactly what that means and why it matters.

---
Next: [2. Talking to SpEL Directly](02-raw-spel.md)
