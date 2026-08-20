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

import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

public class TestConsole implements Console {
    private final List<String> lines = new LinkedList<>();
    private final Predicate<String> isCommentLine;
    private final StringBuilder sb = new StringBuilder();

    public TestConsole(LineReader lineReader, Predicate<String> isCommentLine) {
        this.isCommentLine = isCommentLine;
        String line = lineReader.readLine();
        while (line != null) {
            lines.add(line);
            line = lineReader.readLine();
        }
        readAndPrintComments();
    }

    @Override
    public String read() {
        if (lines.isEmpty()) {
            return null;
        }
        return lines.removeFirst();
    }

    @Override
    public void print(String text) {
        sb.append(text);
    }

    public String getCollectedOutput() {
        return sb.toString();
    }

    public void readAndPrintComments() {
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank() || isCommentLine.test(line)) {
                println(line);
            } else {
                return;
            }
        }
    }
}
