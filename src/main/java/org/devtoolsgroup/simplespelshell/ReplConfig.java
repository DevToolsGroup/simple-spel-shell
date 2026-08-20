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
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ReplConfig {

    private File exprHistoryFile;
    private BiFunction<Object, String, Boolean> isCommentLine;
    private Function<Object, String> prompt;
    private BiFunction<Object, String, String> expressionInterceptor;
    private BiConsumer<Object, String> exprBeforeEvalInterceptor;
    private BiConsumer<Object, Object> evalResultInterceptor;
    private Class<? extends Exception> stopOnException;

    public Function<Object, String> getPrompt() {
        return prompt;
    }

    public void setPrompt(Function<Object, String> prompt) {
        this.prompt = prompt;
    }

    public BiFunction<Object, String, String> getExpressionInterceptor() {
        return expressionInterceptor;
    }

    public void setExpressionInterceptor(BiFunction<Object, String, String> expressionInterceptor) {
        this.expressionInterceptor = expressionInterceptor;
    }

    public BiFunction<Object, String, Boolean> getIsCommentLine() {
        return isCommentLine;
    }

    public void setIsCommentLine(BiFunction<Object, String, Boolean> isCommentLine) {
        this.isCommentLine = isCommentLine;
    }

    public BiConsumer<Object, String> getExprBeforeEvalInterceptor() {
        return exprBeforeEvalInterceptor;
    }

    public void setExprBeforeEvalInterceptor(BiConsumer<Object, String> exprBeforeEvalInterceptor) {
        this.exprBeforeEvalInterceptor = exprBeforeEvalInterceptor;
    }

    public BiConsumer<Object, Object> getEvalResultInterceptor() {
        return evalResultInterceptor;
    }

    public void setEvalResultInterceptor(BiConsumer<Object, Object> evalResultInterceptor) {
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
