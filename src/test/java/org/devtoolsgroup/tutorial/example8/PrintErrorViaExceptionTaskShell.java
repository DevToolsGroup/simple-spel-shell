package org.devtoolsgroup.tutorial.example8;

import org.devtoolsgroup.simplespelshell.NamePattern;
import org.devtoolsgroup.simplespelshell.ShellException;
import org.devtoolsgroup.simplespelshell.impl.FileSystemAwareSpelShellImpl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class PrintErrorViaExceptionTaskShell extends FileSystemAwareSpelShellImpl {

    public static void main(String[] args) throws IOException {
        Path tasksDir = Path.of("target/tasks");
        Files.createDirectories(tasksDir);
        new PrintErrorViaExceptionTaskShell(tasksDir).runRepl();
    }

    public PrintErrorViaExceptionTaskShell(Path tasksDir) {
        super(tasksDir);
    }

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
            throw new ShellException(false, "No such task: " + title);
        }
        write(taskFile, "done");
    }
}