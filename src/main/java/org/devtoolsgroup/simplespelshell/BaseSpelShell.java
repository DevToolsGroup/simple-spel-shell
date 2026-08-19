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

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.devtoolsgroup.simplespelshell.ShellUtils.getSortOrder;
import static org.devtoolsgroup.simplespelshell.ShellUtils.getStackTrace;
import static org.devtoolsgroup.simplespelshell.ShellUtils.matches;

public class BaseSpelShell implements BaseShell {
    private final Set<String> methodsToHide;
    private final List<String> zeroArgMethods;
    private final List<String> oneArgMethods;

    private final SpelEvaluator spelEvaluator;
    private Object lastEvalResult;
    private final WorkDirectory workDirectory;
    private Consumer<Object> onExit = _ -> System.exit(1);

    private final ReplConfig replConfig;
    private final ReplConfig replConfigForScript;

    public BaseSpelShell() {
        this(Path.of(""));
    }

    public BaseSpelShell(Path initDir) {
        this(initDir, new ConsoleImpl());
    }

    public BaseSpelShell(Console console) {
        this(Path.of(""), console);
    }

    public BaseSpelShell(Path initDir, Console console) {
        methodsToHide = Set.of("equals", "getClass", "hashCode", "notify", "notifyAll", "toString", "wait");
        zeroArgMethods = getMethodsWithNumOfArgs(0);
        oneArgMethods = getMethodsWithNumOfArgs(1);

        spelEvaluator = new SpelEvaluatorImpl();

        Path absInitDir = initDir.toAbsolutePath().normalize();
        workDirectory = new WorkDirectoryImpl(absInitDir);
        workDirectory.setCurrentDirectoryValidator(absInitDir, path -> {
            if (!ShellUtils.isParentChild(absInitDir, path)) {
                throw new ShellException(false, "Cannot work outside of " + absInitDir);
            }
        });

        replConfig = new ReplConfig();
        replConfig.setConsole(console);
        replConfig.setPrompt(() -> "SpEL> ");
        replConfig.setExprHistoryFile(null);
        replConfig.setExpressionInterceptor(makeDefaultExpressionInterceptor(replConfig));
        replConfig.setIsCommentLine(str -> str.trim().startsWith("//"));
        replConfig.setPrintExpressionBeforeEval(false);
        replConfig.setPrintEvalResultLength(100);
        replConfig.setStopOnException(ShellExitException.class);


        replConfigForScript = new ReplConfig();
        replConfigForScript.setConsole(console);
        replConfigForScript.setPrompt(null);
        replConfigForScript.setExprHistoryFile(null);
        replConfigForScript.setExpressionInterceptor(makeDefaultExpressionInterceptor(replConfigForScript));
        replConfigForScript.setIsCommentLine(replConfig.getIsCommentLine());
        replConfigForScript.setPrintExpressionBeforeEval(false);
        replConfigForScript.setPrintEvalResultLength(0);
        replConfigForScript.setStopOnException(Exception.class);

    }

    @Override
    public Object runRepl() {
        Console console = replConfig.getConsole();
        if (console == null) {
            throw new ShellException("Cannot run REPL without console.");
        }
        return runRepl(replConfig, ShellUtils.expressionReader(console::read, replConfig.getIsCommentLine()));
    }

    @Override
    public Object runScript(String script) {
        return runRepl(
            replConfigForScript,
            ShellUtils.expressionReader(ShellUtils.lineReader(script), replConfig.getIsCommentLine())
        );
    }

    @Override
    public Object runScript(Path path) {
        return runRepl(
            replConfigForScript,
            ShellUtils.expressionReader(
                ShellUtils.lineReader(workDirectory.getFile(path)), replConfig.getIsCommentLine()
            )
        );
    }

    @Override
    public Object eval(Object tmpVar, String script) {
        spelEvaluator.addVariable("_", tmpVar);
        return runScript(script);
    }

    @Override
    public ReplConfig getReplConfig() {
        return replConfig;
    }

    @Override
    public ReplConfig getReplConfigForScript() {
        return replConfigForScript;
    }

    @Override
    public void setOnExit(Consumer<Object> onExit) {
        this.onExit = onExit;
    }

    @Override
    public SpelEvaluator getSpelEvaluator() {
        return spelEvaluator;
    }

    @Override
    public WorkDirectory getWorkDirectory() {
        return workDirectory;
    }

    @Override
    public void exit(Object result) {
        if (onExit != null) {
            onExit.accept(result);
        }
    }

    @Override
    public void exit() {
        exit(null);
    }

    @Override
    public Object var(String name, Object value) {
        if (name == null) {
            throw new ShellException(false, "Variable name must not be null.");
        }
        spelEvaluator.addVariable(name, value);
        return value;
    }

    @Override
    public Object var(String name) {
        return spelEvaluator.getVariable(name);
    }

    @Override
    public void var() {
        spelEvaluator.getAllVariables().entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry ->
                getReplConsole().printf(
                    "%s: %s\n",
                    entry.getKey(),
                    entry.getValue() == null ? "null" : entry.getValue().getClass().getName()
                )
            );
    }

    @Override
    public void help(String filter) {
        Console console = getReplConsole();
        AtomicInteger prevOrder = new AtomicInteger(Integer.MIN_VALUE);
        Function<Method, String> methodNameGetter = Method::getName;
        getExposedMethods()
            .filter(method -> matches(method.getName(), filter))
            .sorted(Comparator.comparing(SpelShell::getSortOrder).thenComparing(methodNameGetter))
            .map(method -> {
                String params = Arrays.stream(method.getParameterTypes())
                    .map(Class::getName)
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

    @Override
    public void help() {

    }

    @Override
    public void hist(int num) throws IOException {

    }

    @Override
    public void hist() throws IOException {

    }

    @Override
    public void hist(String filter) throws IOException {

    }

    protected Object getRootObject() {
        return this;
    }

    protected boolean isMethodToHide(Method method) {
        return methodsToHide.contains(method.getName()) || getSortOrder(method) < -100;
    }

    protected Stream<Method> getExposedMethods() {
        return Arrays.stream(getRootObject().getClass().getMethods())
            .filter(method -> !Modifier.isStatic(method.getModifiers()) && isMethodToShow(method));
    }

    private boolean isMethodToShow(Method method) {
        return !isMethodToHide(method);
    }

    private Object runRepl(ReplConfig config, ExpressionReader expressionReader) {
        while (true) {
            Console console = config.getConsole();
            Supplier<String> prompt = config.getPrompt();
            Function<String, String> expressionInterceptor = config.getExpressionInterceptor();
            boolean printExpressionBeforeEval = config.isPrintExpressionBeforeEval();
            int printEvalResultLength = config.getPrintEvalResultLength();
            Class<? extends Exception> stopOnException = config.getStopOnException();
            try {
                if (console != null && prompt != null) {
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
                if (printExpressionBeforeEval && console != null) {
                    console.println(expr);
                }
                Object res = spelEvaluator.evaluate(getRootObject(), expr);
                if (printEvalResultLength > 0 && res != null && console != null) {
                    String resStr = res.toString();
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
                if (console == null) {
                    throw new ShellException("Need to print error, but no console set.", ex);
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
            return ShellUtils.rewriteExpr(expr, zeroArgMethods, oneArgMethods);
        };
    }

    private List<String> getMethodsWithNumOfArgs(int numOfArgs) {
        return getExposedMethods()
            .filter(m -> m.getGenericParameterTypes().length == numOfArgs)
            .filter(this::isMethodToShow)
            .map(Method::getName)
            .distinct()
            .toList();
    }

    private Console getReplConsole() {
        Console console = replConfig.getConsole();
        if (console == null) {
            throw new ShellException("REPL console is not set.");
        }
        return console;
    }
}
