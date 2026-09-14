package org.devtoolsgroup.tutorial.example1;

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