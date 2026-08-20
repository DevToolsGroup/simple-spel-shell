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

import java.util.function.Consumer;

public interface BaseSpelShell extends CoreSpelShell {

    void setOnExit(Consumer<Object> onExit);

    Consumer<Object> getOnExit();

    void setMinOrderForHelp(int minOrderForHelp);

    int getMinOrderForHelp();

    void exit(Object result);

    void exit();

    Object var(String name, Object value);

    Object var(String name);

    void var();

    void help(String filter);

    void help();

    void hist(int num);

    void hist();

    void hist(String filter);

    void print(Object obj);

    void println(Object obj);

    void printf(String format, Object... args);

    String format(String format, Object... args);

    String prompt(String prompt);

    void exn(String msg);

    void exnf(String format, Object... args);
}
