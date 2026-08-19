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
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.devtoolsgroup.simplespelshell.ShellUtils.getSortOrder;
import static org.devtoolsgroup.simplespelshell.ShellUtils.getStackTrace;
import static org.devtoolsgroup.simplespelshell.ShellUtils.loadHistory;
import static org.devtoolsgroup.simplespelshell.ShellUtils.matches;

public class BaseSpelShellImpl implements BaseSpelShell {
    protected final Set<String> internalMethods;
    protected final List<String> zeroArgMethodsForRewrite;
    protected final List<String> oneArgMethodsForRewrite;

    private final SpelEvaluator spelEvaluator;
    protected Object lastEvalResult;
    protected Consumer<Object> onExit = _ -> System.exit(1);

    private Console console;
    private final ReplConfig replConfig;
    private final ReplConfig replConfigForScript;

    public BaseSpelShellImpl() {
        this(new ConsoleImpl());
    }

    public BaseSpelShellImpl(Console console) {
        this.console = console;

        internalMethods = Set.of("equals", "getClass", "hashCode", "notify", "notifyAll", "toString", "wait");
        zeroArgMethodsForRewrite = getMethodsWithNumOfArgs(0, method -> !isMethodToHideInRewrite(method));
        oneArgMethodsForRewrite = getMethodsWithNumOfArgs(1, method -> !isMethodToHideInRewrite(method));

        spelEvaluator = new SpelEvaluatorImpl();

        replConfig = new ReplConfig();
        replConfig.setPrompt(() -> "SpEL> ");
        replConfig.setExprHistoryFile(null);
        replConfig.setExpressionInterceptor(makeDefaultExpressionInterceptor(replConfig));
        replConfig.setIsCommentLine(str -> str.trim().startsWith("//"));
        replConfig.setPrintExpressionBeforeEval(false);
        replConfig.setPrintEvalResultLength(100);
        replConfig.setStopOnException(ShellExitException.class);

        replConfigForScript = new ReplConfig();
        replConfigForScript.setPrompt(null);
        replConfigForScript.setExprHistoryFile(null);
        replConfigForScript.setExpressionInterceptor(makeDefaultExpressionInterceptor(replConfigForScript));
        replConfigForScript.setIsCommentLine(replConfig.getIsCommentLine());
        replConfigForScript.setPrintExpressionBeforeEval(false);
        replConfigForScript.setPrintEvalResultLength(0);
        replConfigForScript.setStopOnException(Exception.class);
    }

    @Order(-1000)
    @Override
    public Object runRepl() {
        return runRepl(replConfig, ShellUtils.expressionReader(console::read, replConfig.getIsCommentLine()));
    }

    @Order(-100)
    @Override
    public Object runScript(String script) {
        return runRepl(
            replConfigForScript,
            ShellUtils.expressionReader(ShellUtils.lineReader(script), replConfig.getIsCommentLine())
        );
    }

    @Order(-100)
    @Override
    public Object eval(Object arg, String script) {
        spelEvaluator.addVariable("_", arg);
        return runScript(script);
    }

    @Order(-1000)
    @Override
    public Console getConsole() {
        return console;
    }

    @Order(-1000)
    @Override
    public void setConsole(Console console) {
        this.console = console;
    }

    @Order(-1000)
    @Override
    public ReplConfig getReplConfig() {
        return replConfig;
    }

    @Order(-1000)
    @Override
    public ReplConfig getReplConfigForScript() {
        return replConfigForScript;
    }

    @Order(-1000)
    @Override
    public void setOnExit(Consumer<Object> onExit) {
        this.onExit = onExit;
    }

    @Order(-1000)
    @Override
    public SpelEvaluator getSpelEvaluator() {
        return spelEvaluator;
    }

    @Order(-100)
    @Override
    public void exit(Object result) {
        if (onExit != null) {
            onExit.accept(result);
        }
    }

    @Order(-100)
    @Override
    public void exit() {
        exit(null);
    }

    @Order(-100)
    @Override
    public Object var(String name, Object value) {
        if (name == null) {
            throw new ShellException(false, "Variable name must not be null.");
        }
        spelEvaluator.addVariable(name, value);
        return value;
    }

    @Order(-100)
    @Override
    public Object var(String name) {
        return spelEvaluator.getVariable(name);
    }

    @Order(-100)
    @Override
    public void var() {
        spelEvaluator.getAllVariables().entrySet().stream()
            .filter(entry -> !"$".equals(entry.getKey()))
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry ->
                console.printf(
                    "%s: %s\n",
                    entry.getKey(),
                    entry.getValue() == null ? "null" : entry.getValue().getClass().getName()
                )
            );
    }

    @Order(-100)
    @Override
    public void help(String pattern) {
        AtomicInteger prevOrder = new AtomicInteger(Integer.MAX_VALUE);
        Function<Method, String> methodNameGetter = Method::getName;
        getExposedMethods(method -> !isMethodToHideInHelp(method))
            .filter(method -> matches(method.getName(), pattern))
            .sorted(Comparator.comparing(ShellUtils::getSortOrder).thenComparing(methodNameGetter))
            .map(method -> {
                String params = Arrays.stream(method.getParameters())
                    .map(param -> "%s: %s".formatted(param.getName(), param.getType().getName()))
                    .collect(Collectors.joining(", "));
                String returnType = method.getReturnType().getName();
                String name = method.getName();
                int thisOrder = getSortOrder(method);
                String delim;
                if (prevOrder.get() < 0 && thisOrder >= 0) {
                    delim = "---\n";
                } else {
                    delim = "";
                }
                prevOrder.set(thisOrder);
                return (String.format("%s%s(%s): %s", delim, name, params, returnType));
            })
            .forEach(console::println);
    }

    @Order(-100)
    @Override
    public void help() {
        help("");
    }

    @Order(-100)
    @Override
    public void hist(int num) {
        List<String> allHist = loadHistory(replConfig.getExprHistoryFile());
        for (int i = Math.max(0, allHist.size() - num); i < allHist.size(); i++) {
            console.println(allHist.get(i));
        }
    }

    @Order(-100)
    @Override
    public void hist() {
        hist(100);
    }

    @Order(-100)
    @Override
    public void hist(String substring) {
        String finalFilter = substring.trim().toLowerCase();
        loadHistory(replConfig.getExprHistoryFile()).stream()
            .filter(line -> line.toLowerCase().contains(finalFilter))
            .forEach(console::println);
    }

    @Order(-100)
    @Override
    public void print(Object obj) {
        console.print(String.valueOf(obj));
    }

    @Order(-100)
    @Override
    public void println(Object obj) {
        console.println(String.valueOf(obj));
    }

    @Order(-100)
    @Override
    public void printf(String format, Object... args) {
        console.printf(format, args);
    }

    @Order(-100)
    @Override
    public String format(String format, Object... args) {
        return String.format(format, args);
    }

    @Order(-100)
    @Override
    public String prompt(String prompt) {
        console.print(prompt);
        return console.read();
    }

    @Order(-100)
    @Override
    public void exn(String msg) {
        throw new ShellException(msg);
    }

    @Order(-100)
    @Override
    public void exnf(String format, Object... args) {
        throw new ShellException(format(format, args));
    }

    protected Object getRootObject() {
        return this;
    }

    protected boolean isMethodToHideInHelp(Method method) {
        return internalMethods.contains(method.getName()) || getSortOrder(method) < -100;
    }

    protected boolean isMethodToHideInRewrite(Method method) {
        return internalMethods.contains(method.getName());
    }

    protected Stream<Method> getExposedMethods(Predicate<Method> predicate) {
        return Arrays.stream(getRootObject().getClass().getMethods())
            .filter(method -> !Modifier.isStatic(method.getModifiers()) && predicate.test(method));
    }

    protected Object runRepl(ReplConfig config, ExpressionReader expressionReader) {
        while (true) {
            Supplier<String> prompt = config.getPrompt();
            Function<String, String> expressionInterceptor = config.getExpressionInterceptor();
            boolean printExpressionBeforeEval = config.isPrintExpressionBeforeEval();
            int printEvalResultLength = config.getPrintEvalResultLength();
            Class<? extends Exception> stopOnException = config.getStopOnException();
            try {
                if (prompt != null) {
                    console.print(prompt.get());
                }
                String expr = expressionReader.readExpression();
                if (expressionInterceptor != null) {
                    expr = expressionInterceptor.apply(expr);
                }
                if (expr == null) {
                    return lastEvalResult;
                }
                if (expr.isBlank()) {
                    continue;
                }
                if (printExpressionBeforeEval) {
                    console.println(expr);
                }
                lastEvalResult = spelEvaluator.evaluate(getRootObject(), expr);
                var("$", lastEvalResult);
                if (printEvalResultLength > 0 && lastEvalResult != null) {
                    String resStr = lastEvalResult.toString();
                    if (resStr.length() <= printEvalResultLength) {
                        console.println(resStr);
                    } else {
                        console.println(resStr.substring(0, printEvalResultLength));
                    }
                }
            } catch (Exception ex) {
                if (stopOnException != null && stopOnException.isAssignableFrom(ex.getClass())) {
                    throw ex;
                }
                if (ex.getMessage() != null) {
                    console.println(ex.getMessage());
                }
                if (!(ex instanceof ShellException se) || se.isPrintStackTrace()) {
                    console.println(getStackTrace(ex));
                }
            }
        }
    }

    private Function<String, String> makeDefaultExpressionInterceptor(ReplConfig config) {
        return expr -> {
            if (config.getExprHistoryFile() != null) {
                ShellUtils.saveExprToHistFile(expr, config.getExprHistoryFile());
            }
            return ShellUtils.rewriteExpr(expr, zeroArgMethodsForRewrite, oneArgMethodsForRewrite);
        };
    }

    private List<String> getMethodsWithNumOfArgs(int numOfArgs, Predicate<Method> predicate) {
        return getExposedMethods(predicate)
            .filter(m -> m.getGenericParameterTypes().length == numOfArgs)
            .map(Method::getName)
            .distinct()
            .toList();
    }
}
