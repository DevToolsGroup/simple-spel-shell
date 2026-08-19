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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ConsoleImpl implements Console {
    private final Consumer<String> print;
    private final Consumer<String> println;
    private final BiConsumer<String, Object[]> printf;
    private final Supplier<String> read;

    public ConsoleImpl() {
        print = System.out::print;
        println = System.out::println;
        printf = System.out::printf;
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        read = () -> {
            try {
                return bufferedReader.readLine();
            } catch (IOException e) {
                throw new ShellException(e);
            }
        };
    }

    @Override
    public void print(String text) {
        print.accept(text);
    }

    @Override
    public void println(String text) {
        println.accept(text);
    }

    @Override
    public void printf(String format, Object... args) {
        printf.accept(format, args);
    }

    @Override
    public String read() {
        return read.get();
    }
}
