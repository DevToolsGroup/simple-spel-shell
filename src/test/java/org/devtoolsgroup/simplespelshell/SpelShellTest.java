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

import org.devtoolsgroup.simplespelshell.impl.BaseSpelShellImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.BiFunction;

class SpelShellTest {

    @Test
    void test1() {
        testShell(
            new BaseSpelShellImpl(),
            "test_script_01.txt",
            "test_script_01_expected_output.txt"
        );
    }

    @Test
    void test2_submenus() {
        testShell(
            new Example2(),
            "test_script_02_submenus.txt",
            "test_script_02_submenus_expected_output.txt"
        );
    }

    private static void testShell(BaseSpelShell shell, String scriptPath, String expectedOutputPath) {
        String script = readStringFromClasspath(scriptPath);
        TestConsole console = reconfigureShellForTest(shell, script, false);

        shell.runRepl();

        Assertions.assertEquals(
            readStringFromClasspath(expectedOutputPath).trim(),
            console.getCollectedOutput().trim()
        );
    }

    private static TestConsole reconfigureShellForTest(BaseSpelShell shell, String script, boolean debug) {
        LineReader scriptLineReader = ShellUtils.lineReader(script);
        BiFunction<Object, String, Boolean> isCommentLine = shell.getReplConfig().getIsCommentLine();
        TestConsole console = new TestConsole(scriptLineReader, line -> isCommentLine.apply(null, line));
        console.setDebug(debug);
        shell.setConsole(console);

        BiFunction<Object, String, String> expressionInterceptorOrig = shell.getReplConfig().getExpressionInterceptor();
        shell.getReplConfig().setExpressionInterceptor((rootObj, expr) -> {
            if (expr != null && !expr.isBlank()) {
                console.println(expr);
            }
            return expressionInterceptorOrig.apply(rootObj, expr);
        });
        shell.getReplConfig().setExprBeforeEvalInterceptor((rootObj, expr) ->
            console.printf("%" + getPromptLastLineLength(rootObj) + "s├ evaluating: %s\n", "", expr)
        );
        shell.getReplConfig().setEvalResultInterceptor((rootObj, res) -> {
            console.printf("%" + getPromptLastLineLength(rootObj) + "s└ result: %s\n", "", res);
            console.readAndPrintComments();
        });

        return console;
    }

    private static int getPromptLastLineLength(Object rootObj) {
        BaseSpelShell ro = (BaseSpelShell) rootObj;
        String prompt = ro.getReplConfig().getPrompt().apply(rootObj);
        String[] promptParts = prompt.split("\n");
        return promptParts[promptParts.length - 1].length();
    }

    private static String readStringFromClasspath(String filePath) {
        try (InputStream inputStream = SpelShellTest.class.getClassLoader().getResourceAsStream(filePath)) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ShellException(e);
        }
    }
}