package org.devtoolsgroup.tutorial.example9;

import org.devtoolsgroup.simplespelshell.NamePattern;
import org.devtoolsgroup.simplespelshell.impl.FileSystemAwareSpelShellImpl;
import org.springframework.core.convert.converter.Converter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TaskShell extends FileSystemAwareSpelShellImpl {

    public static void main(String[] args) throws IOException {
        Path tasksDir = Path.of("target/tasks");
        Files.createDirectories(tasksDir);
        new TaskShell(tasksDir).runRepl();
    }

    public TaskShell(Path tasksDir) {
        super(tasksDir);
        List<Converter<?, ?>> converters = new ArrayList<>(getSpelEvaluator().getTypeConverters());
        converters.add(new Converter<String, LocalDate>() {
            @Override
            public LocalDate convert(String source) {
                return LocalDate.parse(source);
            }
        });
        getSpelEvaluator().setTypeConverters(converters);
    }

    public void setDueDate(String title, LocalDate date) {
        Path taskFile = Path.of(title + ".task");
        if (!getFile(taskFile).exists()) {
            println("No such task: " + title);
            return;
        }
        String status = read(taskFile).lines().findFirst().orElse("pending");
        write(taskFile, status + "\n" + date);
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
            println("No such task: " + title);
            return;
        }
        write(taskFile, "done");
    }
}