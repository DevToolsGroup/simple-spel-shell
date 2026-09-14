package org.devtoolsgroup.tutorial.example5;

import org.devtoolsgroup.simplespelshell.impl.BaseSpelShellImpl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TaskShell extends BaseSpelShellImpl {

    private final List<String> tasks = new ArrayList<>();

    public static void main(String[] args) {
        TaskShell shell = new TaskShell();
        shell.getReplConfig().setExprHistoryFile(new File("tasks-history.log"));
        shell.runRepl();
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