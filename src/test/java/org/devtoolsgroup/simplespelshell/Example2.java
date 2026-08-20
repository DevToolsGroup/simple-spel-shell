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

import org.springframework.core.annotation.Order;

public class Example2 extends BaseSpelShellImpl {
    public static final String DELIMITER = "-------------------------------\n";

    static void main() {
        new Example2().runRepl();
    }

    public Example2() {
        getReplConfig().setPrompt(_ -> "[main menu] SpEL> ");
        //show custom methods only in help
        setMinOrderForHelp(0);
        setOnExit(ShellUtils.exnExit(true));
    }

    @Order(-1000)
    @Override
    public Object runRepl() {
        //overriding runRepl() to handle child command exit signals
        while (true) {
            try {
                super.runRepl();
            } catch (ShellExitException ex) {
                if ((boolean) ex.getResult()) {
                    //exit the main menu
                    return null;
                }
                //return back to the main menu
            }
        }
    }

    public void command1() {
        new Cmd1Shell(this).runRepl();
    }

    public void command2() {
        new Cmd2Shell(this).runRepl();
    }

    private static class Cmd1Shell extends BaseSpelShellImpl {
        private int number1;
        private int number2;

        public Cmd1Shell(BaseSpelShell parent) {
            super(parent);
            getReplConfig().setPrompt(_ ->
                DELIMITER +
                    "Adding numbers\n" +
                    "number1=" + number1 + "\n" +
                    "number2=" + number2 + "\n" +
                    "[adder] SpEL> "
            );
            setOnExit(ShellUtils.exnExit(false));
        }

        public void setNumber1(int number1) {
            this.number1 = number1;
        }

        public void setNumber2(int number2) {
            this.number2 = number2;
        }

        public int calculate() {
            int result = number1 + number2;
            printf(
                DELIMITER +
                    "%s + %s = %s\n",
                number1, number2, number1 + number2
            );
            return result;
        }
    }

    private static class Cmd2Shell extends BaseSpelShellImpl {
        private int number1;
        private int number2;

        public Cmd2Shell(BaseSpelShell parent) {
            super(parent, parent.getConsole());
            getReplConfig().setPrompt(_ ->
                DELIMITER +
                    "Multiplying numbers\n" +
                    "number1=" + number1 + "\n" +
                    "number2=" + number2 + "\n" +
                    "[multiplier] SpEL> "
            );
            setOnExit(ShellUtils.exnExit(false));
        }

        public void setNumber1(int number1) {
            this.number1 = number1;
        }

        public void setNumber2(int number2) {
            this.number2 = number2;
        }

        public int calculate() {
            int result = number1 * number2;
            printf(
                DELIMITER +
                    "%s * %s = %s\n",
                number1, number2, result
            );
            return result;
        }
    }
}
