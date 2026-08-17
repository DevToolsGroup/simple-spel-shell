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

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.widget.AutopairWidgets;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Supplier;

public class Main extends SpelShell {

    public Main(String initDir) {
        super(Path.of(initDir));
    }

    static void main(String[] args) throws IOException {
        String initDir = new File("target").exists() ? "target/" : "./";
        Main shell = new Main(initDir);

        shell.setPrompt("");
        shell.setPrintEvalResultLength(0);
        shell.runScript(Path.of(initDir, "../src/test/resources/init_script.txt"));

        shell.setPrompt("SpEL> ");
        shell.setPrintEvalResultLength(100);
        shell.setExprHistoryFile(new File(initDir + "history.log"));
        shell.runShell(terminalLineReader());
    }

    public void sayHi() throws IOException {
        String name = prompt("What is your name? ");
        print(format("Hi, %s!\n", name));
    }

    public static Supplier<String> terminalLineReader() {
        LineReader reader = LineReaderBuilder.builder().build();
        AutopairWidgets autopairWidgets = new AutopairWidgets(reader, true);
        autopairWidgets.enable();
        return reader::readLine;
    }
}
