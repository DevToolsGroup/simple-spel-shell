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

import org.springframework.core.convert.converter.Converter;
import org.springframework.expression.OperatorOverloader;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface Shell {

    Object runScript(String script);

    Object runScript(Path path) throws IOException;

    Object runScript(Path path, Charset cs) throws IOException;

    Object runShell(Supplier<String> lineReader);

    Object eval(Object newVar, String script);

    Object eval(String script);

    void print(Object obj);

    void println(Object obj);

    void printf(String format, Object... args);

    String format(String format, Object... args);

    String prompt(String prompt);

    void exit(String msg);

    void exit();

    void exn(String msg);

    void exnf(String format, Object... args);

    void exnStackTrace(String msg);

    void help(String filter);

    void help();

    void hist(int num) throws IOException;

    void hist() throws IOException;

    void hist(String filter) throws IOException;

    Object var(String name, Object value);

    Object var(String name);

    void var();

    Map<String, Object> getAllVariables();

    Path cd(Path path);

    Path cd();

    void pwd();

    File getFile(Path path);

    void write(Path path, String text) throws IOException;

    String read(Path path) throws IOException;

    void mkdir(boolean autoCd, Path path);

    void mkdir(Path path);

    void ll(Path path) throws IOException;

    void listFiles(String pattern) throws IOException;

    void listFiles() throws IOException;

    void ll() throws IOException;

    void setPromptFn(Function<String, String> promptFn);

    void setPrintFn(Consumer<String> printFn);

    void setPrintExpression(boolean printExpression);

    void setPrompt(Supplier<String> prompt);

    void setExpressionInterceptor(BiFunction<String, Consumer<String>, String> expressionInterceptor);

    void setPrompt(String prompt);

    void setPrintEvalResultLength(int printEvalResultLength);

    void setExprHistoryFile(File commandHistoryFile);

    void setRootObject(Object rootObject);

    void addTypeConverter(Converter<?, ?> typeConverter);

    void setOperatorOverloader(OperatorOverloader operatorOverloader);

    void setCurrentDirectoryValidator(Path newCurDir, Consumer<Path> currentDirectoryValidator);

    void setOnExit(Consumer<String> onExit);
}
