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
        //given
        BaseSpelShellImpl shell = new BaseSpelShellImpl();

        //when
        Object result = shell.runScript("#_", "hello");

        //then
        Assertions.assertEquals("hello", result);
    }

    @Test
    void noArgsScriptSeesNullArgs() {
        //given
        BaseSpelShellImpl shell = new BaseSpelShellImpl();

        //when
        Object result = shell.runScript("#_");

        //then
        Assertions.assertNull(result);
    }

    @Test
    void nestedScriptGetsOwnArgsAndOuterIsRestoredAfterItReturns() {
        //given
        BaseSpelShellImpl shell = new BaseSpelShellImpl();

        //when
        Object result = shell.runScript(
            "runScript('#_', 'inner-args')\n#_",
            "outer-args"
        );

        //then
        Assertions.assertEquals("outer-args", result);
    }

    @Test
    void mutatingArgsBeforeNestedCallIsPreservedAcrossIt() {
        //given
        BaseSpelShellImpl shell = new BaseSpelShellImpl();

        //when
        Object result = shell.runScript(
            "_ = 'mutated-outer-args'\nrunScript('#_', 'inner-args')\n#_",
            "original-outer-args"
        );

        //then
        Assertions.assertEquals("mutated-outer-args", result);
    }

    @Test
    void noArgsNestedCallSeesNullNotParentArgs() {
        //given
        BaseSpelShellImpl shell = new BaseSpelShellImpl();

        //when
        Object result = shell.runScript("runScript('#_')", "outer-args");

        //then
        Assertions.assertNull(result);
    }

    @Test
    void exceptionInNestedScriptStillRestoresOuterArgs() {
        //given
        CatchingShell shell = new CatchingShell();

        //when
        Object result = shell.runScript(
            "runScriptCatchingException('1/0', 'inner-args')\n#_",
            "outer-args"
        );

        //then
        Assertions.assertEquals("outer-args", result);
    }

    @Test
    void exceptionPropagatingOutOfTopLevelScriptLeavesArgsUnset() {
        //given
        BaseSpelShellImpl shell = new BaseSpelShellImpl();

        //when/then
        Assertions.assertThrows(
            ArithmeticException.class,
            () -> shell.runScript("runScript('1/0', 'inner-args')", "outer-args")
        );

        //then
        Assertions.assertNull(shell.getSpelEvaluator().getVariable("_"));
    }

    @Test
    void scriptArgsVarNameIsConfigurable() {
        //given
        BaseSpelShellImpl shell = new BaseSpelShellImpl();
        shell.getSpelEvaluator().setScriptArgsVarName("args");

        //when
        Object result = shell.runScript("#args", "hello");

        //then
        Assertions.assertEquals("hello", result);
        Assertions.assertEquals("args", shell.getSpelEvaluator().getScriptArgsVarName());
    }

    @Test
    void replReceivesArgsViaUnderscoreVariable() {
        //given
        BaseSpelShellImpl shell = new BaseSpelShellImpl();
        connectConsole(shell, "#_");

        //when
        Object result = shell.runRepl("hello");

        //then
        Assertions.assertEquals("hello", result);
    }

    @Test
    void nestedScriptInsideReplRestoresReplArgs() {
        //given
        BaseSpelShellImpl shell = new BaseSpelShellImpl();
        connectConsole(shell, "runScript('#_', 'inner-args')\n#_");

        //when
        Object result = shell.runRepl("repl-args");

        //then
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
