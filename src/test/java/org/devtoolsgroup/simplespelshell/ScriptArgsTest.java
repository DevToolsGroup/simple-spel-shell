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

class ScriptArgsTest {

    @Test
    void scriptReceivesArgsViaUnderscoreVariable() {
        BaseSpelShellImpl shell = new BaseSpelShellImpl();

        Object result = shell.runScript("#_", "hello");

        Assertions.assertEquals("hello", result);
    }

    @Test
    void noArgsScriptSeesNullArgs() {
        BaseSpelShellImpl shell = new BaseSpelShellImpl();

        Object result = shell.runScript("#_");

        Assertions.assertNull(result);
    }

    @Test
    void nestedScriptGetsOwnArgsAndOuterIsRestoredAfterItReturns() {
        BaseSpelShellImpl shell = new BaseSpelShellImpl();

        Object result = shell.runScript(
            "runScript('#_', 'inner-args')\n#_",
            "outer-args"
        );

        Assertions.assertEquals("outer-args", result);
    }

    @Test
    void mutatingArgsBeforeNestedCallIsPreservedAcrossIt() {
        BaseSpelShellImpl shell = new BaseSpelShellImpl();

        Object result = shell.runScript(
            "_ = 'mutated-outer-args'\nrunScript('#_', 'inner-args')\n#_",
            "original-outer-args"
        );

        Assertions.assertEquals("mutated-outer-args", result);
    }

    @Test
    void noArgsNestedCallSeesNullNotParentArgs() {
        BaseSpelShellImpl shell = new BaseSpelShellImpl();

        Object result = shell.runScript("runScript('#_')", "outer-args");

        Assertions.assertNull(result);
    }

    @Test
    void exceptionInNestedScriptStillRestoresOuterArgs() {
        CatchingShell shell = new CatchingShell();

        Object result = shell.runScript(
            "runScriptCatchingException('1/0', 'inner-args')\n#_",
            "outer-args"
        );

        Assertions.assertEquals("outer-args", result);
    }

    @Test
    void exceptionPropagatingOutOfTopLevelScriptLeavesArgsUnset() {
        BaseSpelShellImpl shell = new BaseSpelShellImpl();

        Assertions.assertThrows(
            ArithmeticException.class,
            () -> shell.runScript("runScript('1/0', 'inner-args')", "outer-args")
        );

        Assertions.assertNull(shell.getSpelEvaluator().getVariable("_"));
    }

    @Test
    void scriptArgsVarNameIsConfigurable() {
        BaseSpelShellImpl shell = new BaseSpelShellImpl();
        shell.getSpelEvaluator().setScriptArgsVarName("args");

        Object result = shell.runScript("#args", "hello");

        Assertions.assertEquals("hello", result);
        Assertions.assertEquals("args", shell.getSpelEvaluator().getScriptArgsVarName());
    }

    @Test
    void replReceivesArgsViaUnderscoreVariable() {
        BaseSpelShellImpl shell = new BaseSpelShellImpl();
        connectConsole(shell, "#_");

        Object result = shell.runRepl("hello");

        Assertions.assertEquals("hello", result);
    }

    @Test
    void nestedScriptInsideReplRestoresReplArgs() {
        BaseSpelShellImpl shell = new BaseSpelShellImpl();
        connectConsole(shell, "runScript('#_', 'inner-args')\n#_");

        Object result = shell.runRepl("repl-args");

        Assertions.assertEquals("repl-args", result);
    }

    private static void connectConsole(BaseSpelShellImpl shell, String script) {
        LineReader lineReader = ShellUtils.lineReader(script);
        TestConsole console = new TestConsole(
            lineReader,
            line -> shell.getReplConfig().getIsCommentLine().apply(null, line)
        );
        shell.setConsole(console);
    }

    public static class CatchingShell extends BaseSpelShellImpl {
        public Object runScriptCatchingException(String script, Object args) {
            try {
                return runScript(script, args);
            } catch (Exception e) {
                return null;
            }
        }
    }
}
