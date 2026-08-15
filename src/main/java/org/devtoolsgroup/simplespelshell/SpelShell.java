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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
    private List<String> allMethods;
    private List<String> zeroArgMethods;
    private List<String> oneArgMethods;
    private StandardEvaluationContext spelCtx;
    private Object lastEvalResult;
    private BufferedReader input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    private PrintStream output = System.out;
    private String prompt = ">>> ";
    private int printEvalResultLength = 100;
    private Object rootObject;

    public SpelShell() {
        methodsToHide = new HashSet<>();
        methodsToHide.addAll(Set.of("equals", "getClass", "hashCode", "notify", "notifyAll", "toString", "wait",
            "setInput", "setOutput", "setGlobalFunctions"));
        setRootObject(this);
        initSpelCtx();
    }

    public void runScript(InputStream scriptInp, Charset cs, boolean stopOnException) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(scriptInp, cs));
        while (true) {
            String expr = null;
            String rewrittenExpr = null;
            try {
                print(prompt);
                expr = readExpr(reader);
                if (expr == null) {
                    return;
                }
                rewrittenExpr = rewriteExpr(expr);
                Object res = eval(rewrittenExpr);
                if (printEvalResultLength > 0 && res != null) {
                    String resStr = res.toString();
                    String ellipsis = resStr.length() > printEvalResultLength ? "..." : "";
                    println(resStr.substring(0, Math.min(resStr.length(), printEvalResultLength)) + ellipsis);
                }
            } catch (Exception ex) {
                println(ex.getMessage());
                ex.printStackTrace(output);
                if (stopOnException) {
                    if (rewrittenExpr != null || expr != null) {
                        throw new SpelShellException(
                            "An error occurred while evaluating expression\nExpression: %s\nError: %s".formatted(
                                rewrittenExpr != null ? rewrittenExpr : expr, ex.getMessage()
                            ),
                            ex
                        );
                    }
                    throw new SpelShellException(ex.getMessage(), ex);
                }
            }
        }
    }

    public void runScript(InputStream scriptInp, boolean stopOnException) {
        runScript(scriptInp, StandardCharsets.UTF_8, stopOnException);
    }

    public void runScript(String script, boolean stopOnException) {
        runScript(
            new ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8)),
            StandardCharsets.UTF_8,
            stopOnException
        );
    }

    public void runScript(Path path, Charset cs, boolean stopOnException) throws IOException {
        runScript(Files.readString(path, cs), stopOnException);
    }

    public void runScript(Path path, boolean stopOnException) throws IOException {
        runScript(Files.readString(path, StandardCharsets.UTF_8), stopOnException);
    }

    public void print(Object obj) {
        output.print(obj.toString());
    }

    public void println(Object obj) {
        output.println(obj.toString());
    }

    public String format(String format, Object... args) {
        return String.format(format, args);
    }

    public String prompt(String prompt) throws IOException {
        print(prompt);
        return input.readLine();
    }

    public void exit(int code) {
        System.exit(code);
    }

    public void exit() {
        System.exit(0);
    }

    public void help() {
        allMethods.forEach(this::println);
    }

    private Object eval(String expr) {
        lastEvalResult = parser.parseExpression(expr).getValue(spelCtx, rootObject, Object.class);
        setLastEvalResult(lastEvalResult);
        return lastEvalResult;
    }

    private String readExpr(BufferedReader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                return sb.isEmpty() ? null : sb.toString();
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
            .filter(option -> pattern.matcher(option.toLowerCase()).matches())
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

    private List<String> getMethodsWithNumOfArgs(Object rootObject, int numOfArgs) {
        return Arrays.stream(rootObject.getClass().getMethods())
            .filter(m -> m.getGenericParameterTypes().length == numOfArgs)
            .map(Method::getName)
            .filter(this::isMethodToShow)
            .distinct()
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

    public void setInput(InputStream input, Charset cs) {
        this.input = new BufferedReader(new InputStreamReader(input, cs));
    }

    public void setInput(InputStream input) {
        setInput(input, StandardCharsets.UTF_8);
    }

    public void setOutput(PrintStream output) {
        this.output = output;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public void setPrintEvalResultLength(int printEvalResultLength) {
        this.printEvalResultLength = printEvalResultLength;
    }

    public void setRootObject(Object rootObject) {
        this.rootObject = rootObject;
        allMethods = Arrays.stream(rootObject.getClass().getMethods())
            .map(Method::getName)
            .filter(this::isMethodToShow)
            .distinct()
            .sorted()
            .toList();
        zeroArgMethods = getMethodsWithNumOfArgs(rootObject, 0);
        oneArgMethods = getMethodsWithNumOfArgs(rootObject, 1);
    }

    private boolean isMethodToShow(String name) {
        return !methodsToHide.contains(name);
    }

    public static class SpelShellException extends RuntimeException {
        public SpelShellException(String message) {
            super(message);
        }

        public SpelShellException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}
