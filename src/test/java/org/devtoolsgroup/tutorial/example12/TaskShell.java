package org.devtoolsgroup.tutorial.example12;

import org.devtoolsgroup.simplespelshell.NamePattern;
import org.devtoolsgroup.simplespelshell.ShellException;
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
        getSpelEvaluator().setOperatorOverloader(new TaskDueDateOperatorOverloader(this));
    }

    public void setDueDate(NamePattern pattern, LocalDate date) {
        Path taskFile = findTaskByPattern(pattern).toPath();
        String status = read(taskFile).lines().findFirst().orElse("pending");
        write(taskFile, status + "\n" + date);
    }

    public LocalDate getDueDate(NamePattern pattern) {
        Path taskFile = findTaskByPattern(pattern).toPath();
        List<String> lines = read(taskFile).lines().toList();
        return lines.size() > 1 && !lines.get(1).isBlank() ? LocalDate.parse(lines.get(1)) : LocalDate.now();
    }

    public void addTask(String title) {
        write(Path.of(title + ".task"), "pending");
        println("Added: " + title);
    }

    public void listTasks() {
        List<File> files = findFilesByName(new NamePattern("")).stream()
            .filter(File::isFile)
            .filter(file -> file.getName().endsWith(".task"))
            .toList();
        if (files.isEmpty()) {
            println("No tasks yet.");
            return;
        }
        for (File file : files) {
            String title = file.getName().replace(".task", "");
            String status = read(file.toPath()).lines().findFirst().orElse("pending");
            println((status.equals("done") ? "[x] " : "[ ] ") + title);
        }
    }

    public void completeTask(NamePattern pattern) {
        write(findTaskByPattern(pattern).toPath(), "done");
    }

    private File findTaskByPattern(NamePattern pattern) {
        List<File> found = findFilesByName(pattern).stream()
            .filter(File::isFile)
            .filter(file -> file.getName().endsWith(".task"))
            .toList();
        if (found.isEmpty()) {
            throw new ShellException(false, "Could not find a task by pattern '" + pattern.pattern() + "'.");
        }
        if (found.size() > 1) {
            throw new ShellException(false, "More than one task was found by pattern '" + pattern.pattern() + "'.");
        }
        return found.getFirst();
    }
}