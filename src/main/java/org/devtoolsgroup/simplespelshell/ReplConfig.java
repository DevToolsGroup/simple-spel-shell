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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ReplConfig {

    private Supplier<String> prompt;
    private Function<String, String> expressionInterceptor;
    private Function<String, Boolean> isCommentLine;
    private Consumer<String> exprBeforeEvalInterceptor;
    private Consumer<Object> evalResultInterceptor;
    private File exprHistoryFile;
    private Class<? extends Exception> stopOnException;

    public Supplier<String> getPrompt() {
        return prompt;
    }

    public void setPrompt(Supplier<String> prompt) {
        this.prompt = prompt;
    }

    public Function<String, String> getExpressionInterceptor() {
        return expressionInterceptor;
    }

    public void setExpressionInterceptor(Function<String, String> expressionInterceptor) {
        this.expressionInterceptor = expressionInterceptor;
    }

    public Function<String, Boolean> getIsCommentLine() {
        return isCommentLine;
    }

    public void setIsCommentLine(Function<String, Boolean> isCommentLine) {
        this.isCommentLine = isCommentLine;
    }

    public Consumer<String> getExprBeforeEvalInterceptor() {
        return exprBeforeEvalInterceptor;
    }

    public void setExprBeforeEvalInterceptor(Consumer<String> exprBeforeEvalInterceptor) {
        this.exprBeforeEvalInterceptor = exprBeforeEvalInterceptor;
    }

    public Consumer<Object> getEvalResultInterceptor() {
        return evalResultInterceptor;
    }

    public void setEvalResultInterceptor(Consumer<Object> evalResultInterceptor) {
        this.evalResultInterceptor = evalResultInterceptor;
    }

    public File getExprHistoryFile() {
        return exprHistoryFile;
    }

    public void setExprHistoryFile(File exprHistoryFile) {
        this.exprHistoryFile = exprHistoryFile;
    }

    public Class<? extends Exception> getStopOnException() {
        return stopOnException;
    }

    public void setStopOnException(Class<? extends Exception> stopOnException) {
        this.stopOnException = stopOnException;
    }
}
