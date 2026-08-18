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
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class SpelShellConfig {

    private ExpressionReader exprInput;
    private LineReader userInput;
    private Consumer<String> userOutput;

    private Supplier<String> prompt = () -> "SpEL> ";
    private Function<String, Boolean> isCommentLine = str -> str.trim().startsWith("//");
    private BiFunction<String, Consumer<String>, String> expressionInterceptor;
    private File exprHistoryFile;
    private boolean printExpressionBeforeEval;

    private int printEvalResultLength = 100;

    private Consumer<Object> onExit = _ -> System.exit(1);

    public ExpressionReader getExprInput() {
        return exprInput;
    }

    public SpelShellConfig setExprInput(ExpressionReader exprInput) {
        this.exprInput = exprInput;
        return this;
    }

    public LineReader getUserInput() {
        return userInput;
    }

    public SpelShellConfig setUserInput(LineReader userInput) {
        this.userInput = userInput;
        return this;
    }

    public Consumer<String> getUserOutput() {
        return userOutput;
    }

    public SpelShellConfig setUserOutput(Consumer<String> userOutput) {
        this.userOutput = userOutput;
        return this;
    }

    public Supplier<String> getPrompt() {
        return prompt;
    }

    public SpelShellConfig setPrompt(Supplier<String> prompt) {
        this.prompt = prompt;
        return this;
    }

    public Function<String, Boolean> getIsCommentLine() {
        return isCommentLine;
    }

    public SpelShellConfig setIsCommentLine(Function<String, Boolean> isCommentLine) {
        this.isCommentLine = isCommentLine;
        return this;
    }

    public BiFunction<String, Consumer<String>, String> getExpressionInterceptor() {
        return expressionInterceptor;
    }

    public SpelShellConfig setExpressionInterceptor(BiFunction<String, Consumer<String>, String> expressionInterceptor) {
        this.expressionInterceptor = expressionInterceptor;
        return this;
    }

    public File getExprHistoryFile() {
        return exprHistoryFile;
    }

    public SpelShellConfig setExprHistoryFile(File exprHistoryFile) {
        this.exprHistoryFile = exprHistoryFile;
        return this;
    }

    public boolean isPrintExpressionBeforeEval() {
        return printExpressionBeforeEval;
    }

    public SpelShellConfig setPrintExpressionBeforeEval(boolean printExpressionBeforeEval) {
        this.printExpressionBeforeEval = printExpressionBeforeEval;
        return this;
    }

    public int getPrintEvalResultLength() {
        return printEvalResultLength;
    }

    public SpelShellConfig setPrintEvalResultLength(int printEvalResultLength) {
        this.printEvalResultLength = printEvalResultLength;
        return this;
    }

    public Consumer<Object> getOnExit() {
        return onExit;
    }

    public SpelShellConfig setOnExit(Consumer<Object> onExit) {
        this.onExit = onExit;
        return this;
    }
}
