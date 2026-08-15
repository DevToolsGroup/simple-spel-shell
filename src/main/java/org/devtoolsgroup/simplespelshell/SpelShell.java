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

import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpelShell {

    private static final Pattern SET_VAR_PAT = Pattern.compile("^\\s*([a-zA-Z$_][a-zA-Z$_0-9]*)\\s*=(.+)$");
    private static final Pattern ZERO_ARG_METHOD_PAT = Pattern.compile("^\\s*([a-zA-Z][a-zA-Z0-9]*)\\s*$");
    private static final Pattern ONE_ARG_METHOD_PAT = Pattern.compile("^\\s*([a-zA-Z][a-zA-Z0-9]*)\\s+(\\S.*)$");
    private static final Pattern TRAILING_SLASHES_PAT = Pattern.compile("^(.*)(\\\\+)\\s*$");

    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final Set<String> methodsToHide;
    private final List<String> zeroArgMethods;
    private final List<String> oneArgMethods;
    private StandardEvaluationContext spelCtx;
    private Object lastEvalResult;
    private InputStream input = System.in;
    private PrintStream output = System.out;
    private String prompt = ">>> ";
    private boolean printEvalResult = true;
    private Object globalFunctions = this;

    public SpelShell() {
        methodsToHide = new HashSet<>();
        methodsToHide.addAll(Set.of("equals", "getClass", "hashCode", "notify", "notifyAll", "toString", "wait",
            "setInput", "setOutput", "setGlobalFunctions"));
        zeroArgMethods = getMethodsWithNumOfArgs(0);
        oneArgMethods = getMethodsWithNumOfArgs(1);
        initSpelCtx();
    }

    public void runScript(InputStream scriptInp, boolean stopOnException) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(scriptInp));
        while (true) {
            String expr = null;
            String rewrittenExpr = null;
            try {
                output.print(prompt);
                expr = readExpr(reader);
                rewrittenExpr = rewriteExpr(expr);
                Object res = eval(rewrittenExpr);
                if (printEvalResult && res != null) {
                    output.println(res);
                }
            } catch (Exception ex) {
                output.println(ex.getMessage());
                ex.printStackTrace(output);
                if (stopOnException) {
                    if (rewrittenExpr != null || expr != null) {
                        throw new SpelShellException(
                            "An error occurred while evaluating expression\nExpression: %s\nError: %s".formatted(
                                rewrittenExpr != null ? rewrittenExpr : expr, ex.getMessage()
                            )
                        );
                    }
                    throw new SpelShellException(ex);
                }
            }
        }
    }

    private Object eval(String expr) {
        lastEvalResult = parser.parseExpression(expr).getValue(spelCtx, globalFunctions, Object.class);
        setLastEvalResult(lastEvalResult);
        return lastEvalResult;
    }

    private String readExpr(BufferedReader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                return sb.toString();
            }
            Matcher matcher = TRAILING_SLASHES_PAT.matcher(line);
            if (matcher.matches() && matcher.group(2).length() % 2 == 1) {
                sb.append(matcher.group(1)).append(" ");
            } else {
                sb.append(line);
                return sb.toString();
            }
        }
    }

    private String rewriteExpr(String expr) {
        Matcher matcher = SET_VAR_PAT.matcher(expr);
        if (matcher.matches()) {
            return "var('%s',%s)".formatted(matcher.group(1), matcher.group(2));
        }
        matcher = ZERO_ARG_METHOD_PAT.matcher(expr);
        if (matcher.matches()) {
            return "%s()".formatted(findByPattern(matcher.group(1), zeroArgMethods));
        }
        matcher = ONE_ARG_METHOD_PAT.matcher(expr);
        if (matcher.matches()) {
            return "%s(%s)".formatted(findByPattern(matcher.group(1), oneArgMethods), matcher.group(2));
        }
        return expr;
    }

    private String findByPattern(String patStr, List<String> options) {
        Pattern pattern = makePattern(patStr);
        List<String> found = options.stream()
            .filter(option -> pattern.matcher(option).matches())
            .toList();
        if (found.isEmpty()) {
            throw new SpelShellException("Cannot find a method by pattern '%s'".formatted(patStr));
        }
        if (found.size() > 1) {
            throw new SpelShellException("Multiple methods match the pattern '%s':\n%s".formatted(
                patStr,
                String.join("\n", found)
            ));
        }
        return found.getFirst();
    }

    private List<String> getMethodsWithNumOfArgs(int numOfArgs) {
        return Arrays.stream(globalFunctions.getClass().getMethods())
            .filter(m -> m.getGenericParameterTypes().length == numOfArgs)
            .map(Method::getName)
            .filter(this::isMethodToShow)
            .distinct()
            .map(String::toLowerCase)
            .toList();
    }

    private void initSpelCtx() {
        spelCtx = new StandardEvaluationContext();
        setLastEvalResult(lastEvalResult);
    }

    private void setLastEvalResult(Object res) {
        lastEvalResult = res;
        spelCtx.setVariable("_", res);
    }

    private Pattern makePattern(String pat) {
        return Pattern.compile(".*" + String.join(".*", pat.toLowerCase().split("")) + ".*");
    }

    public void setInput(InputStream input) {
        this.input = input;
    }

    public void setOutput(PrintStream output) {
        this.output = output;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public void setPrintEvalResult(boolean printEvalResult) {
        this.printEvalResult = printEvalResult;
    }

    public void setGlobalFunctions(Object globalFunctions) {
        this.globalFunctions = globalFunctions;
    }

    private boolean isMethodToShow(String name) {
        return !methodsToHide.contains(name);
    }

}
