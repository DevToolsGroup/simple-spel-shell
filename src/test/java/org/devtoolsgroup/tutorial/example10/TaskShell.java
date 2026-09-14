package org.devtoolsgroup.tutorial.example10;

import org.devtoolsgroup.simplespelshell.impl.BaseSpelShellImpl;

import java.util.ArrayList;
import java.util.List;

public class TaskShell extends BaseSpelShellImpl {

    private final List<String> tasks = new ArrayList<>();

    public static void main(String[] args) {
        new TaskShell().runRepl();
    }

    public TaskShell() {
        getReplConfig().setPrompt(_ -> "tasks(" + countTasks() + ")> ");
    }

    public void addTask(String title) {
        tasks.add(title);
        println("Added: " + title);
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

    private int countTasks() {
        return tasks.size();
    }
}