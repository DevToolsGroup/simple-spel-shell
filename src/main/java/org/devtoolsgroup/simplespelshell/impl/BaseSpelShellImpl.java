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

package org.devtoolsgroup.simplespelshell.impl;

import org.devtoolsgroup.simplespelshell.BaseSpelShell;
import org.devtoolsgroup.simplespelshell.Console;
import org.devtoolsgroup.simplespelshell.NamePattern;
import org.devtoolsgroup.simplespelshell.ReplConfig;
import org.devtoolsgroup.simplespelshell.ShellException;
import org.devtoolsgroup.simplespelshell.ShellUtils;
import org.springframework.core.annotation.Order;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.devtoolsgroup.simplespelshell.ShellUtils.getSortOrder;
import static org.devtoolsgroup.simplespelshell.ShellUtils.loadHistory;
import static org.devtoolsgroup.simplespelshell.ShellUtils.matches;

public class BaseSpelShellImpl extends CoreSpelShellImpl implements BaseSpelShell {
    private Consumer<Object> onExit = _ -> System.exit(0);
    private int minOrderForHelp = -100;

    public BaseSpelShellImpl() {
        this(new ConsoleImpl());
    }

    public BaseSpelShellImpl(Console console) {
        this(null, console);
    }

    public BaseSpelShellImpl(BaseSpelShell parentShell) {
        this(parentShell, parentShell.getConsole());
    }

    public BaseSpelShellImpl(BaseSpelShell parentShell, Console console) {
        super(parentShell, console);
        if (parentShell == null) {
            addNamePatternRewriter(getReplConfig());
            addNamePatternRewriter(getReplConfigForScript());
        } else {
            minOrderForHelp = parentShell.getMinOrderForHelp();
        }
    }

    @Order(-1000)
    @Override
    public void setOnExit(Consumer<Object> onExit) {
        this.onExit = onExit;
    }

    @Order(-1000)
    @Override
    public Consumer<Object> getOnExit() {
        return onExit;
    }

    @Order(-1000)
    @Override
    public void setMinOrderForHelp(int minOrderForHelp) {
        this.minOrderForHelp = minOrderForHelp;
    }

    @Order(-1000)
    @Override
    public int getMinOrderForHelp() {
        return minOrderForHelp;
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
    public NamePattern npat(String str) {
        return new NamePattern(str);
    }

    @Order(-100)
    @Override
    public Object var(String name, Object value) {
        if (name == null) {
            throw new ShellException(false, "Variable name must not be null.");
        }
        getSpelEvaluator().addVariable(name, value);
        return value;
    }

    @Order(-100)
    @Override
    public Object var(String name) {
        return getSpelEvaluator().getVariable(name);
    }

    @Order(-100)
    @Override
    public void var(NamePattern pattern) {
        getConsole().println(
            printVariables(
                getSpelEvaluator().getAllVariables().keySet().stream()
                    .filter(varName -> matches(varName, pattern.pattern()))
                    .sorted()
                    .toList()
            )
        );
    }

    @Order(-100)
    @Override
    public void var() {
        getConsole().println(printVariables(
                getSpelEvaluator().getAllVariables().keySet().stream()
                    .filter(varName -> !varName.equals(lastEvalResultVarName))
                    .sorted()
                    .toList()
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
            .forEach(getConsole()::println);
    }

    @Order(-100)
    @Override
    public void help(NamePattern pattern) {
        help(pattern.pattern());
    }

    @Order(-100)
    @Override
    public void help() {
        help("");
    }

    @Order(-100)
    @Override
    public void hist(int num) {
        List<String> allHist = loadHistory(getReplConfig().getExprHistoryFile());
        for (int i = Math.max(0, allHist.size() - num); i < allHist.size(); i++) {
            getConsole().println(allHist.get(i));
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
        loadHistory(getReplConfig().getExprHistoryFile()).stream()
            .filter(line -> line.toLowerCase().contains(finalFilter))
            .forEach(getConsole()::println);
    }

    @Order(-100)
    @Override
    public void print(Object obj) {
        getConsole().print(String.valueOf(obj));
    }

    @Order(-100)
    @Override
    public void println(Object obj) {
        getConsole().println(String.valueOf(obj));
    }

    @Order(-100)
    @Override
    public void printf(String format, Object... args) {
        getConsole().printf(format, args);
    }

    @Order(-100)
    @Override
    public String format(String format, Object... args) {
        return String.format(format, args);
    }

    @Order(-100)
    @Override
    public String prompt(String prompt) {
        getConsole().print(prompt);
        return getConsole().read();
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

    protected boolean isMethodToHideInHelp(Method method) {
        return internalMethods.contains(method.getName()) || getSortOrder(method) < minOrderForHelp;
    }

    private void addNamePatternRewriter(ReplConfig replConfig) {
        BiFunction<Object, String, String> origExpressionInterceptor = replConfig.getExpressionInterceptor();
        replConfig.setExpressionInterceptor((rootObj, expr) ->
            origExpressionInterceptor.apply(rootObj, ShellUtils.replaceAllNamePatterns(expr))
        );
    }

    private String printVariables(List<String> varNames) {
        Map<String, Object> allVariables = getSpelEvaluator().getAllVariables();
        return varNames.stream()
            .filter(allVariables::containsKey)
            .map(varName -> {
                Object varValue = allVariables.get(varName);
                return format("%s: %s", varName, varValue == null ? "null" : varValue.getClass().getName());
            })
            .collect(Collectors.joining("\n"));
    }
}
