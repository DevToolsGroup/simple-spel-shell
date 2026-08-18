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
import java.util.function.Function;
import java.util.function.Supplier;

public class ReplImpl implements Repl {
    private Supplier<String> input;
    private Function<String, Object> evaluator;
    private Function<Object, String> resultPrinter;
    private Consumer<String> output;
    private Consumer<Exception> errorOutput;
    private Class<Exception> stopOnException;

    @Override
    public void run() {
        while (true) {
            try {
                String expr = input.get();
                Object res = evaluator.apply(expr);
                String resStr = resultPrinter.apply(res);
                output.accept(resStr);
            } catch (Exception ex) {
                if (stopOnException != null && stopOnException.isAssignableFrom(ex.getClass())) {
                    throw ex;
                }
                errorOutput.accept(ex);
            }
        }
    }

    @Override
    public void setInput(Supplier<String> input) {
        this.input = input;
    }

    @Override
    public void setEvaluator(Function<String, Object> evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public void setResultPrinter(Function<Object, String> resultPrinter) {
        this.resultPrinter = resultPrinter;
    }

    @Override
    public void setOutput(Consumer<String> output) {
        this.output = output;
    }

    @Override
    public void setErrorOutput(Consumer<Exception> errorOutput) {
        this.errorOutput = errorOutput;
    }

    @Override
    public void setStopOnException(Class<Exception> stopOnException) {
        this.stopOnException = stopOnException;
    }
}
