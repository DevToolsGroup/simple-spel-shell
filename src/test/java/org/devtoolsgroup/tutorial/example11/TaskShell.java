package org.devtoolsgroup.tutorial.example11;

import org.devtoolsgroup.simplespelshell.impl.BaseSpelShellImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class TaskShell extends BaseSpelShellImpl {

    private final List<String> tasks = new ArrayList<>();

    public static void main(String[] args) {
        new TaskShell().runRepl();
    }

    public TaskShell() {
        BiFunction<Object, String, String> defaultInterceptor = getReplConfig().getExpressionInterceptor();
        getReplConfig().setExpressionInterceptor((root, expr) -> {
            String rewritten = defaultInterceptor.apply(root, expr);
            System.err.println("[audit] " + expr + " -> " + rewritten);
            return rewritten;
        });
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