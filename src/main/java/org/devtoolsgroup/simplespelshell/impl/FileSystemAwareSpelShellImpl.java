/*
MIT License

Copyright (c) 2026-present DevToolsGroup (https://github.com/DevToolsGroup)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package org.devtoolsgroup.simplespelshell.impl;

import org.devtoolsgroup.simplespelshell.Console;
import org.devtoolsgroup.simplespelshell.ExpressionReader;
import org.devtoolsgroup.simplespelshell.FileSystemAwareSpelShell;
import org.devtoolsgroup.simplespelshell.ShellException;
import org.devtoolsgroup.simplespelshell.ShellUtils;
import org.devtoolsgroup.simplespelshell.WorkingDirectory;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.devtoolsgroup.simplespelshell.ShellUtils.matches;

public class FileSystemAwareSpelShellImpl extends BaseSpelShellImpl implements FileSystemAwareSpelShell {
    private final WorkingDirectory workingDirectory;

    public FileSystemAwareSpelShellImpl(FileSystemAwareSpelShell parentShell) {
        this(parentShell, new ConsoleImpl(), null);
    }

    public FileSystemAwareSpelShellImpl(Path initDir) {
        this(new ConsoleImpl(), initDir);
    }

    public FileSystemAwareSpelShellImpl(Console console, Path initDir) {
        this(null, console, initDir);
    }

    public FileSystemAwareSpelShellImpl(FileSystemAwareSpelShell parentShell, Console console, Path initDir) {
        super(parentShell, console);

        if (parentShell == null) {
            List<Converter<?, ?>> typeConverters = new ArrayList<>(getSpelEvaluator().getTypeConverters());
            typeConverters.add(new Converter<String, Path>() {
                @Override
                public Path convert(String first) {
                    return Path.of(first);
                }
            });
            getSpelEvaluator().setTypeConverters(typeConverters);

            Path absInitDir = initDir.toAbsolutePath().normalize();
            workingDirectory = new WorkingDirectoryImpl(absInitDir);
            workingDirectory.setCurrentDirectoryValidator(absInitDir, path -> {
                if (!ShellUtils.isParentChild(absInitDir, path)) {
                    throw new ShellException(false, "Cannot work outside of " + absInitDir);
                }
            });
        } else {
            workingDirectory = parentShell.getWorkingDirectory();
        }
    }

    @Order(-1000)
    @Override
    public WorkingDirectory getWorkingDirectory() {
        return workingDirectory;
    }

    @Order(-100)
    @Override
    public Object runScript(Path path) {
        ExpressionReader expressionReader = ShellUtils.expressionReader(
            ShellUtils.lineReader(workingDirectory.getFile(path)),
            line -> getReplConfigForScript().getIsCommentLine().apply(getRootObject(), line)
        );
        return runRepl(getReplConfigForScript(), expressionReader);
    }

    @Order(-100)
    @Override
    public Path cd(Path path) {
        workingDirectory.changeCurrentDir(path);
        return workingDirectory.getCurDirAbsolutePath();
    }

    @Order(-100)
    @Override
    public Path cd() {
        return cd(Path.of(".."));
    }

    @Order(-100)
    @Override
    public void pwd() {
        println(workingDirectory.getCurDirAbsolutePath());
    }

    @Order(-100)
    @Override
    public File getFile(Path path) {
        return workingDirectory.getFile(path);
    }

    @Order(-100)
    @Override
    public void write(Path path, String text) {
        workingDirectory.write(path, text);
    }

    @Order(-100)
    @Override
    public String read(Path path) {
        try {
            return Files.readString(getFile(path).toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ShellException(e);
        }
    }

    @Order(-100)
    @Override
    public void mkdir(boolean autoCd, Path path) {
        Path curDir = getWorkingDirectory().getCurDirAbsolutePath();
        if (!ShellUtils.isParentChild(curDir, path)) {
            throw new ShellException(false, "Cannot write outside of %s".formatted(curDir));
        }
        if (!getFile(path).mkdirs()) {
            throw new ShellException(
                "There was an error when creating one of the specified directories %s".formatted(path)
            );
        }
        if (autoCd) {
            cd(path);
            pwd();
        }
    }

    @Order(-100)
    @Override
    public void mkdir(Path path) {
        mkdir(true, path);
    }

    @Order(-100)
    @Override
    public void ll(Path path) {
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(workingDirectory.getCurDirAbsolutePath().resolve(path))) {
            List<Path> children = new ArrayList<>();
            for (Path entry : entries) {
                children.add(entry);
            }
            children.sort(Comparator.comparing(
                p -> p.getFileName().toString(),
                String.CASE_INSENSITIVE_ORDER
            ));

            printPaths(children);
        } catch (IOException e) {
            throw new ShellException(e);
        }
    }

    @Order(-100)
    @Override
    public void ll() {
        ll(Path.of("."));
    }

    @Order(-100)
    @Override
    public List<File> findFilesByName(String pattern) {
        Path curDir = workingDirectory.getCurDirAbsolutePath();
        try (Stream<Path> files = Files.walk(curDir)) {
            return files
                .filter(path -> !curDir.equals(path))
                .filter(path -> matches(curDir.relativize(path).toString(), pattern))
                .map(Path::toFile)
                .toList();
        } catch (IOException e) {
            throw new ShellException(e);
        }
    }

    @Order(-100)
    @Override
    public void listFiles(String pattern) {
        printPaths(
            findFilesByName(pattern).stream()
                .map(File::toPath)
                .filter(Files::isRegularFile)
                .toList()
        );
    }

    @Order(-100)
    @Override
    public void listFiles() {
        listFiles("");
    }

    @Order(-100)
    @Override
    public void listDirs(String pattern) {
        printPaths(
            findFilesByName(pattern).stream()
                .map(File::toPath)
                .filter(Files::isDirectory)
                .toList()
        );
    }

    @Order(-100)
    @Override
    public void listDirs() {
        listDirs("");
    }

    private void printPaths(List<Path> absPaths) {
        try {
            // Find the largest size so all sizes can be right-aligned.
            long maxSize = 0;
            for (Path absPath : absPaths) {
                if (Files.isRegularFile(absPath)) {
                    maxSize = Math.max(maxSize, Files.size(absPath));
                }
            }

            int width = String.valueOf(maxSize).length();

            for (Path absPath : absPaths) {
                Path relPath = workingDirectory.getCurDirAbsolutePath().relativize(absPath);
                if (Files.isDirectory(absPath)) {
                    printf("%" + width + "s %s/\n", "", relPath);
                } else {
                    printf("%" + width + "d %s\n", Files.size(absPath), relPath);
                }
            }
        } catch (IOException e) {
            throw new ShellException(e);
        }
    }
}
