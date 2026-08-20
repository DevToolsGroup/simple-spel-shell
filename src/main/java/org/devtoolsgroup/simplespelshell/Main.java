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

package org.devtoolsgroup.simplespelshell;

import java.io.File;
import java.nio.file.Path;

public class Main extends FileSystemAwareSpelShellImpl {

    static void main(String[] args) {
        File initDir = new File(new File("target").exists() ? "target" : ".");
        Main shell = new Main(initDir.getAbsolutePath());
        shell.getReplConfig().setExprHistoryFile(new File(initDir, "history.log"));

        shell.runScript(Path.of("../src/test/resources/init_script.txt"));
        shell.runRepl();
    }

    public Main(String initDir) {
        super(Path.of(initDir));
    }

    public void sayHi() {
        String name = prompt("What is your name? ");
        print(format("Hi, %s!\n", name));
    }
}
