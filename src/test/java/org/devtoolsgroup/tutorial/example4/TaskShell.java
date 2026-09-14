package org.devtoolsgroup.tutorial.example4;

import org.devtoolsgroup.simplespelshell.impl.BaseSpelShellImpl;

import java.util.ArrayList;
import java.util.List;

public class TaskShell extends BaseSpelShellImpl {

    private final List<String> tasks = new ArrayList<>();

    public static void main(String[] args) {
        new TaskShell().runRepl();
    }

    public TaskShell() {
        setMinOrderForHelp(0);
    }

    public void addTask(String title) {
        tasks.add(title);
        println("Added: " + title);
    }

    public void addTaskInteractive() {
        String title = prompt("Task title: ");
        addTask(title);
    }

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
}