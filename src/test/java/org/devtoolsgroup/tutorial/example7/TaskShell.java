package org.devtoolsgroup.tutorial.example7;

import org.devtoolsgroup.simplespelshell.ShellExitException;
import org.devtoolsgroup.simplespelshell.ShellUtils;
import org.devtoolsgroup.simplespelshell.impl.BaseSpelShellImpl;
import org.springframework.core.annotation.Order;

import java.util.ArrayList;
import java.util.List;

public class TaskShell extends BaseSpelShellImpl {

    private final List<Task> tasks = new ArrayList<>();

    public static void main(String[] args) {
        new TaskShell().runRepl();
    }

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

    public void viewTask(String title) {
        Task task = findTask(title);
        if (task == null) {
            println("No such task: " + title);
            return;
        }
        new TaskDetailShell(this, task).runRepl();
    }

    private Task findTask(String title) {
        return tasks.stream().filter(t -> t.title.equals(title)).findFirst().orElse(null);
    }
}