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
    private boolean debug;
    private int numberOfReads = 0;
    private int maxNumberOfReads = 1000;
    private int numberOfPrints = 0;
    private int maxNumberOfPrints = 1000;

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
        numberOfReads++;
        if (numberOfReads > maxNumberOfReads) {
            throw new RuntimeException("numberOfReads > " + maxNumberOfReads);
        }
        if (lines.isEmpty()) {
            return null;
        }
        String read = lines.removeFirst();
        while (read != null && read.isBlank()) {
            numberOfReads++;
            read = lines.removeFirst();
        }
        if (debug) {
            System.out.printf("\n--------------------------------------------------\nTestConsole.read:\n%s", read);
        }
        return read;
    }

    @Override
    public void print(String text) {
        numberOfPrints++;
        if (numberOfPrints > maxNumberOfPrints) {
            throw new RuntimeException("numberOfPrints > " + maxNumberOfPrints);
        }
        if (debug) {
            System.out.printf("\n--------------------------------------------------\nTestConsole.print:\n%s", text);
        }
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

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void setMaxNumberOfReads(int maxNumberOfReads) {
        this.maxNumberOfReads = maxNumberOfReads;
    }

    public void setMaxNumberOfPrints(int maxNumberOfPrints) {
        this.maxNumberOfPrints = maxNumberOfPrints;
    }
}
