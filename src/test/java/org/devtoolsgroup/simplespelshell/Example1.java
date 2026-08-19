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

import java.lang.reflect.Method;
import java.util.Map;
import java.util.function.Supplier;

import static org.devtoolsgroup.simplespelshell.Example1.MainShell.DELIMITER;
import static org.devtoolsgroup.simplespelshell.ShellUtils.getSortOrder;

public class Example1 {
    static void main() {
        new MainShell();
    }

    public static class MainShell extends BaseSpelShellImpl {
        public static final String DELIMITER = "-------------------------------\n";

        public MainShell() {
            getReplConfig().setPrompt(() -> "[main menu] SpEL> ");
            setOnExit(_ -> {
                throw new ShellExitException();
            });
        }

        @Order(-1000)
        @Override
        public Object runRepl() {
            while (true) {
                try {
                    super.runRepl();
                } catch (ShellExitException ex) {
                    if (ex.getResult() != null) {
                        //copy all variables from the child shell to the parent shell
                        ((Map<String, Object>) ex.getResult()).forEach(this::var);
                    } else {
                        return null;
                    }
                }
            }
        }

        @Override
        protected boolean isMethodToHideInHelp(Method method) {
            //show only custom methods in help
            return super.isMethodToHideInHelp(method) || getSortOrder(method) < 0;
        }

        public void command1() {
            Cmd1Shell cmd1Shell = new Cmd1Shell(getSpelEvaluator().getAllVariables());
            Supplier<String> origPrompt = cmd1Shell.getReplConfig().getPrompt();
            cmd1Shell.setConsole(getConsole());
            cmd1Shell.setReplConfig(getReplConfig());
            cmd1Shell.getReplConfig().setPrompt(origPrompt);
            cmd1Shell.runRepl();
        }

        public void command2() {
            Cmd2Shell cmd2Shell = new Cmd2Shell(getSpelEvaluator().getAllVariables());
            Supplier<String> origPrompt = cmd2Shell.getReplConfig().getPrompt();
            cmd2Shell.setConsole(getConsole());
            cmd2Shell.setReplConfig(getReplConfig());
            cmd2Shell.getReplConfig().setPrompt(origPrompt);
            cmd2Shell.runRepl();
        }
    }

    private static class Cmd1Shell extends BaseSpelShellImpl {
        private int number1;
        private int number2;

        public Cmd1Shell(Map<String, Object> variables) {
            variables.forEach(this::var);
            getReplConfig().setPrompt(() ->
                DELIMITER +
                    "Adding numbers\n" +
                    "number1=" + number1 + "\n" +
                    "number2=" + number2 + "\n" +
                    "[adder] SpEL> "
            );
            setOnExit(_ -> {
                //returning all variables to the parent shell
                throw new ShellExitException(getSpelEvaluator().getAllVariables());
            });
        }

        @Override
        protected boolean isMethodToHideInHelp(Method method) {
            //show only custom methods in help
            return super.isMethodToHideInHelp(method) || getSortOrder(method) < 0;
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

        public Cmd2Shell(Map<String, Object> variables) {
            variables.forEach(this::var);
            getReplConfig().setPrompt(() ->
                DELIMITER +
                    "Multiplying numbers\n" +
                    "number1=" + number1 + "\n" +
                    "number2=" + number2 + "\n" +
                    "[multiplier] SpEL> "
            );
            setOnExit(_ -> {
                //returning all variables to the parent shell
                throw new ShellExitException(getSpelEvaluator().getAllVariables());
            });
        }

        @Override
        protected boolean isMethodToHideInHelp(Method method) {
            //show only custom methods in help
            return super.isMethodToHideInHelp(method) || getSortOrder(method) < 0;
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
