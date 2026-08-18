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
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Supplier;

import static org.devtoolsgroup.simplespelshell.Example1.MainShell.DELIMITER;

public class Example1 {
    static void main() {
        new MainShell(Path.of(new File("target").exists() ? "target" : "."));
    }

    public static class MainShell extends SpelShell {
        public static final String DELIMITER = "-------------------------------\n";
        private final Path initialDir;
        private final Supplier<String> terminalLineReader;

        public MainShell(Path initialDir) {
            super(initialDir);
            this.initialDir = initialDir;
            this.terminalLineReader = Main.terminalLineReader();
            setPrompt("[main menu] SpEL> ");
            while (true) {
                try {
                    runShell(terminalLineReader);
                } catch (SpelShellExitException ex) {
                    //copy all variables from the child shell to the parent shell
                    ((Map<String, Object>) ex.getResult()).forEach(this::var);
                }
            }
        }

        public void command1() {
            new Cmd1Shell(initialDir, getAllVariables()).runShell(terminalLineReader);
        }

        public void command2() {
            new Cmd2Shell(initialDir, getAllVariables()).runShell(terminalLineReader);
        }
    }

    private static class Cmd1Shell extends SpelShell {
        private int number1;
        private int number2;

        public Cmd1Shell(Path initialDir, Map<String, Object> variables) {
            super(initialDir);
            variables.forEach(this::var);
            setPrompt(() ->
                DELIMITER +
                    "Adding numbers\n" +
                    "number1=" + number1 + "\n" +
                    "number2=" + number2 + "\n" +
                    "[adder] SpEL> "
            );
            setOnExit(_ -> {
                //returning all variables to the parent shell
                throw new SpelShellExitException(getAllVariables());
            });
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

    private static class Cmd2Shell extends SpelShell {
        private int number1;
        private int number2;

        public Cmd2Shell(Path initialDir, Map<String, Object> variables) {
            super(initialDir);
            variables.forEach(this::var);
            setPrompt(() ->
                DELIMITER +
                    "Multiplying numbers\n" +
                    "number1=" + number1 + "\n" +
                    "number2=" + number2 + "\n" +
                    "[multiplier] SpEL> "
            );
            setOnExit(_ -> {
                //returning all variables to the parent shell
                throw new SpelShellExitException(getAllVariables());
            });
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
