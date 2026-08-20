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

import org.devtoolsgroup.simplespelshell.ShellException;
import org.devtoolsgroup.simplespelshell.ShellUtils;
import org.devtoolsgroup.simplespelshell.WorkingDirectory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

public class WorkingDirectoryImpl implements WorkingDirectory {
    private Path curDir;
    private Consumer<Path> currentDirectoryValidator;

    public WorkingDirectoryImpl(Path curDir) {
        this.curDir = curDir.toAbsolutePath().normalize();
        changeCurrentDir(curDir);
    }

    @Override
    public void changeCurrentDir(Path path) {
        if (path.toString().isBlank()) {
            return;
        }
        path = curDir.resolve(path).toAbsolutePath().normalize();
        if (!Files.exists(path)) {
            throw new ShellException("The directory %s doesn't exist.".formatted(path));
        }
        if (!Files.isDirectory(path)) {
            throw new ShellException("%s is not a directory.".formatted(path));
        }
        if (currentDirectoryValidator != null) {
            currentDirectoryValidator.accept(path);
        }
        curDir = path;
    }

    @Override
    public void setCurrentDirectoryValidator(Path newCurDir, Consumer<Path> validator) {
        this.currentDirectoryValidator = null;
        changeCurrentDir(newCurDir);
        this.currentDirectoryValidator = validator;
        if (currentDirectoryValidator != null) {
            currentDirectoryValidator.accept(curDir);
        }
    }

    @Override
    public Path getCurDirAbsolutePath() {
        return curDir;
    }

    @Override
    public File getFile(Path path) {
        return curDir.resolve(path).toAbsolutePath().normalize().toFile();
    }

    @Override
    public void write(Path path, String text) {
        File fileToWriteTo = getFile(path);
        if (!ShellUtils.isParentChild(curDir, fileToWriteTo.toPath())) {
            throw new ShellException("Cannot write outside of the current directory %s".formatted(curDir));
        }
        if (fileToWriteTo.exists() && fileToWriteTo.isDirectory()) {
            throw new ShellException("Cannot write text to %s because it is a directory".formatted(fileToWriteTo));
        }
        File parentDir = fileToWriteTo.getParentFile();
        if (parentDir.exists() || parentDir.mkdirs()) {
            try {
                Files.writeString(fileToWriteTo.toPath(), text, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new ShellException(e);
            }
        }
        throw new ShellException("There was an error when creating parent directories %s".formatted(parentDir));
    }
}
