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

import org.jspecify.annotations.Nullable;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Operation;
import org.springframework.expression.OperatorOverloader;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeConverter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SpelShell implements Shell {

    private static final String IDENTIFIER_PAT = "[a-zA-Z_$][a-zA-Z_$0-9]*";
    private static final Pattern SET_VAR_PAT = pat("^\\s*(%s)\\s*=(.+)$".formatted(IDENTIFIER_PAT));
    private static final Pattern ZERO_ARG_METHOD_PAT = pat("^\\s*(%s)\\s*$".formatted(IDENTIFIER_PAT));
    private static final Pattern ONE_ARG_METHOD_PAT = pat("^\\s*(%s)\\s+(.*)$".formatted(IDENTIFIER_PAT));
    private static final Pattern TRAILING_SLASHES_PAT = pat("^(.*)(\\\\+)\\s*$");
    private static final Pattern NAME_SPLIT_PAT =
        Pattern.compile("(?<=[a-z0-9])(?=[A-Z])|_|-|(?<=[0-9])(?=[^0-9])|(?<=[^0-9])(?=[0-9])");

    private Supplier<String> prompt = () -> "SpEL> ";
    private BiFunction<String, Consumer<String>, String> expressionInterceptor;
    private boolean printExpression;

    private final SpelExpressionParser parser = new SpelExpressionParser();
    private StandardEvaluationContext spelCtx;
    private List<Converter<?, ?>> typeConverters = new ArrayList<>();
    private OperatorOverloader operatorOverloader;
    private final Map<String, Object> variables = new ConcurrentHashMap<>();
    private Object rootObject;

    private Object lastEvalResult;
    private int printEvalResultLength = 100;
    private File exprHistoryFile;

    private BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    private PrintStream userOutput = System.out;
    private Consumer<String> onExit = _ -> System.exit(1);
    private Path curDir;
    private Consumer<Path> currentDirectoryValidator;

    private final Set<String> methodsToHide;
    private List<String> zeroArgMethods;
    private List<String> oneArgMethods;

    public SpelShell(Path initialDir) {
        methodsToHide = new HashSet<>();
        methodsToHide.addAll(Set.of("equals", "getClass", "hashCode", "notify", "notifyAll", "toString", "wait",
            "setInput", "setOutput", "setGlobalFunctions", "setCurrentDirectoryValidator", "addTypeConverter",
            "setRootObject"));

        expressionInterceptor = (expr, _) -> SpelShell.rewriteExpr(expr, zeroArgMethods, oneArgMethods);

        typeConverters.add(new Converter<String, Path>() {
            @Override
            public Path convert(String first) {
                return Path.of(first);
            }
        });
        operatorOverloader = new BasicOperatorOverloader();
        initSpelCtx();
        setRootObject(this);

        Path absInitialDir = initialDir.toAbsolutePath().normalize();
        curDir = Path.of(".").toAbsolutePath().normalize();
        setCurrentDirectoryValidator(absInitialDir, path -> {
            if (!path.toAbsolutePath().normalize().startsWith(absInitialDir)) {
                throw new SpelShellException(false, "Cannot work outside of " + absInitialDir);
            }
        });
    }

    @Override
    public Object runScript(InputStream scriptInput, Charset cs, boolean stopOnException) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(scriptInput, cs));
        // @formatter:off
        Consumer<String> saveToHist = exprHistoryFile != null
            ? expr -> SpelShell.saveExprToHistFile(expr, exprHistoryFile)
            : _ -> {};
        // @formatter:on
        while (true) {
            String expr;
            try {
                print(prompt.get());
                expr = readExpr(reader);
                if (expressionInterceptor != null) {
                    expr = expressionInterceptor.apply(expr, saveToHist);
                }
                if (expr == null) {
                    return lastEvalResult;
                }
                if (expr.isBlank()) {
                    continue;
                }
                if (printExpression) {
                    println(expr);
                }
                Object res = evaluate(expr);
                saveToHist.accept(expr);
                if (printEvalResultLength > 0 && res != null) {
                    String resStr = res.toString();
                    if (resStr.length() <= printEvalResultLength) {
                        println(resStr);
                    } else {
                        println(resStr.substring(0, printEvalResultLength) + "...");
                    }
                }
            } catch (SpelShellExitException ex) {
                throw ex;
            } catch (Exception ex) {
                if (stopOnException) {
                    throw new SpelShellException(ex.getMessage(), ex);
                }
                if (ex.getMessage() != null) {
                    println(ex.getMessage());
                }
                if (!(ex instanceof SpelShellException sse) || sse.isPrintStackTrace()) {
                    ex.printStackTrace(userOutput);
                }
            }
        }
    }

    @Override
    public Object runScript(InputStream scriptInp, boolean stopOnException) {
        return runScript(scriptInp, StandardCharsets.UTF_8, stopOnException);
    }

    @Override
    public Object runScript(String script, boolean stopOnException) {
        return runScript(
            new ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8)),
            StandardCharsets.UTF_8,
            stopOnException
        );
    }

    @Override
    public Object runScript(Path path, Charset cs, boolean stopOnException) throws IOException {
        return runScript(Files.readString(path, cs), stopOnException);
    }

    @Override
    public Object runScript(Path path, boolean stopOnException) throws IOException {
        return runScript(Files.readString(path, StandardCharsets.UTF_8), stopOnException);
    }

    @Override
    public Object runShell() {
        return runScript(System.in, false);
    }

    @Override
    public Object eval(Object newVar, String script) {
        Supplier<String> promptBefore = this.prompt;
        int lengthBefore = this.printEvalResultLength;
        setPrompt("");
        setPrintEvalResultLength(0);
        var("$", newVar);
        Object res = runScript(script, true);
        setPrompt(promptBefore);
        setPrintEvalResultLength(lengthBefore);
        return res;
    }

    @Override
    public Object eval(String script) {
        return eval(null, script);
    }

    @Override
    public void print(Object obj) {
        userOutput.print(obj.toString());
    }

    @Override
    public void println(Object obj) {
        userOutput.println(obj);
    }

    @Override
    public void printf(String format, Object... args) {
        userOutput.printf(format, args);
    }

    @Override
    public String format(String format, Object... args) {
        return String.format(format, args);
    }

    @Override
    public String prompt(String prompt) throws IOException {
        print(prompt);
        return userInput.readLine();
    }

    @Override
    public void exit(String msg) {
        if (onExit != null) {
            onExit.accept(msg);
        }
    }

    @Override
    public void exit() {
        exit("");
    }

    @Override
    public void exn(String msg) {
        throw new SpelShellException(false, msg);
    }

    @Override
    public void exnf(String format, Object... args) {
        exn(format(format, args));
    }

    @Override
    public void exnStackTrace(String msg) {
        throw new SpelShellException(true, msg);
    }

    @Override
    public void help(String filter) {
        getExposedMethods()
            .filter(method -> matches(method.getName(), filter))
            .map(method -> {
                String params = Arrays.stream(method.getParameterTypes())
                    .map(Class::getName)
                    .collect(Collectors.joining(", "));
                String returnType = method.getReturnType().getName();
                String name = method.getName();
                return format("%s(%s): %s", name, params, returnType);
            })
            .distinct()
            .sorted()
            .forEach(this::println);
    }

    @Override
    public void help() {
        help("");
    }

    @Override
    public void hist(int num) throws IOException {
        List<String> allHist = loadHistory(exprHistoryFile);
        for (int i = Math.max(0, allHist.size() - num); i < allHist.size(); i++) {
            println(allHist.get(i));
        }
    }

    @Override
    public void hist() throws IOException {
        hist(200);
    }

    @Override
    public void hist(String filter) throws IOException {
        String finalFilter = filter.trim().toLowerCase();
        loadHistory(exprHistoryFile).stream()
            .filter(line -> line.toLowerCase().contains(finalFilter))
            .forEach(this::println);
    }

    @Override
    public Object var(String name, Object value) {
        if (name == null) {
            exn("Variable name must not be null.");
        }
        if (value == null) {
            variables.remove(name);
        } else {
            variables.put(name, value);
        }
        initSpelCtx();
        return value;
    }

    @Override
    public Object var(String name) {
        return variables.get(name);
    }

    @Override
    public void var() {
        variables.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry ->
                print(format(
                    "%s: %s\n",
                    entry.getKey(),
                    entry.getValue() == null ? "null" : entry.getValue().getClass().getName()
                ))
            );
    }

    @Override
    public Map<String, Object> allVariables() {
        return new HashMap<>(variables);
    }

    @Override
    public Path cd(Path path) {
        if (path.toString().isBlank()) {
            return curDir;
        }
        path = curDir.resolve(path).toAbsolutePath().normalize();
        if (!Files.exists(path)) {
            exn("The directory %s doesn't exist.".formatted(path));
        }
        if (!Files.isDirectory(path)) {
            exn("%s is not a directory.".formatted(path));
        }
        if (currentDirectoryValidator != null) {
            currentDirectoryValidator.accept(path);
        }
        curDir = path;
        return curDir;
    }

    @Override
    public Path cd() {
        return cd(Path.of(".."));
    }

    @Override
    public void pwd() {
        println(curDir);
    }

    @Override
    public File getFile(Path path) {
        return curDir.resolve(path).toAbsolutePath().normalize().toFile();
    }

    @Override
    public void write(Path path, String text) throws IOException {
        File fileToWriteTo = getFile(path);
        if (!isParentChild(curDir, fileToWriteTo.toPath())) {
            exnf("Cannot write outside of the current directory %s", curDir);
        }
        if (fileToWriteTo.exists() && fileToWriteTo.isDirectory()) {
            exnf("Cannot write text to %s because it is a directory", fileToWriteTo);
        }
        File parentDir = fileToWriteTo.getParentFile();
        if (!parentDir.exists() && !parentDir.mkdirs()) {
            exnf("There was an error when creating parent directories %s", parentDir);
        }
        Files.writeString(fileToWriteTo.toPath(), text, StandardCharsets.UTF_8);
    }

    @Override
    public String read(Path path) throws IOException {
        return Files.readString(getFile(path).toPath(), StandardCharsets.UTF_8);
    }

    @Override
    public void mkdir(boolean autoCd, Path path) {
        if (!getFile(path).mkdirs()) {
            exnf("There was an error when creating one of the specified directories %s", path);
        }
        if (autoCd) {
            cd(path);
            pwd();
        }
    }

    @Override
    public void mkdir(Path path) {
        mkdir(true, path);
    }

    @Override
    public void ll(Path path) throws IOException {
        List<Path> children;
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(curDir.resolve(path))) {
            children = new ArrayList<>();
            for (Path entry : entries) {
                children.add(entry);
            }
        }
        children.sort(Comparator.comparing(
            p -> p.getFileName().toString(),
            String.CASE_INSENSITIVE_ORDER
        ));

        // Find the largest size so all sizes can be right-aligned.
        long maxSize = 0;
        for (Path child : children) {
            if (Files.isRegularFile(child)) {
                maxSize = Math.max(maxSize, Files.size(child));
            }
        }

        int width = String.valueOf(maxSize).length();

        for (Path child : children) {
            if (Files.isDirectory(child)) {
                printf("%" + width + "s %s/\n", "", child.getFileName());
            } else {
                long size = Files.size(child);
                printf("%" + width + "d %s\n", size, child.getFileName());
            }
        }
    }

    @Override
    public void ll() throws IOException {
        ll(Path.of(""));
    }

    @Override
    public void setUserInput(InputStream userInput, Charset cs) {
        this.userInput = new BufferedReader(new InputStreamReader(userInput, cs));
    }

    @Override
    public void setUserInput(InputStream userInput) {
        setUserInput(userInput, StandardCharsets.UTF_8);
    }

    @Override
    public void setUserOutput(PrintStream userOutput) {
        this.userOutput = userOutput;
    }

    @Override
    public void setPrintExpression(boolean printExpression) {
        this.printExpression = printExpression;
    }

    @Override
    public void setPrompt(Supplier<String> prompt) {
        this.prompt = prompt;
    }

    @Override
    public void setPrompt(String prompt) {
        setPrompt(() -> prompt);
    }

    @Override
    public void setExpressionInterceptor(BiFunction<String, Consumer<String>, String> expressionInterceptor) {
        this.expressionInterceptor = expressionInterceptor;
    }

    @Override
    public void setPrintEvalResultLength(int printEvalResultLength) {
        this.printEvalResultLength = printEvalResultLength;
    }

    @Override
    public void setExprHistoryFile(File exprHistoryFile) {
        this.exprHistoryFile = exprHistoryFile;
    }

    @Override
    public void setRootObject(Object rootObject) {
        this.rootObject = rootObject;
        zeroArgMethods = getMethodsWithNumOfArgs(rootObject, 0);
        oneArgMethods = getMethodsWithNumOfArgs(rootObject, 1);
    }

    @Override
    public void addTypeConverter(Converter<?, ?> typeConverter) {
        typeConverters.add(typeConverter);
        initSpelCtx();
    }

    @Override
    public void setOperatorOverloader(OperatorOverloader operatorOverloader) {
        this.operatorOverloader = operatorOverloader;
        initSpelCtx();
    }

    @Override
    public void setCurrentDirectoryValidator(Path newCurDir, Consumer<Path> currentDirectoryValidator) {
        this.currentDirectoryValidator = null;
        cd(newCurDir);
        this.currentDirectoryValidator = currentDirectoryValidator;
        if (currentDirectoryValidator != null) {
            currentDirectoryValidator.accept(curDir);
        }
    }

    @Override
    public void setOnExit(Consumer<String> onExit) {
        this.onExit = onExit;
    }

    private Object evaluate(String expr) {
        return setLastEvalResult(parser.parseExpression(expr).getValue(spelCtx, rootObject, Object.class));
    }

    private static String readExpr(BufferedReader reader) throws IOException {
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

    private static String rewriteExpr(String expr, List<String> zeroArgMethods, List<String> oneArgMethods) {
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

    private static String findByPattern(String patStr, List<String> options) {
        List<String> found = options.stream()
            .filter(option -> matches(option, patStr))
            .toList();
        if (found.isEmpty()) {
            throw new SpelShellException(false, "Cannot find a method by pattern '%s'".formatted(patStr));
        }
        if (found.size() > 1) {
            throw new SpelShellException(false, "Multiple methods match the pattern '%s':\n%s".formatted(
                patStr,
                String.join("\n", found)
            ));
        }
        return found.getFirst();
    }

    private List<String> getMethodsWithNumOfArgs(Object rootObject, int numOfArgs) {
        return getExposedMethods()
            .filter(m -> m.getGenericParameterTypes().length == numOfArgs)
            .map(Method::getName)
            .filter(this::isMethodToShow)
            .distinct()
            .toList();
    }

    private void initSpelCtx() {
        spelCtx = new StandardEvaluationContext();

        DefaultConversionService defaultConversionService = new DefaultConversionService();
        typeConverters.forEach(defaultConversionService::addConverter);
        spelCtx.setTypeConverter(new StandardTypeConverter(defaultConversionService));
        if (operatorOverloader != null) {
            spelCtx.setOperatorOverloader(operatorOverloader);
        }

        spelCtx.setVariables(variables);
        setLastEvalResult(lastEvalResult);
    }

    private Object setLastEvalResult(Object res) {
        lastEvalResult = res;
        spelCtx.setVariable("_", res);
        return res;
    }

    private boolean isMethodToShow(String name) {
        return !methodsToHide.contains(name);
    }

    private static boolean isParentChild(Path parent, Path child) {
        return child.toAbsolutePath().normalize().startsWith(parent.toAbsolutePath().normalize());
    }

    private static void saveExprToHistFile(String expr, File histFile) {
        try (FileWriter wr = new FileWriter(histFile, StandardCharsets.UTF_8, true)) {
            wr.append("\n").append(Instant.now().truncatedTo(ChronoUnit.SECONDS).toString())
                .append("\n").append(expr);
        } catch (IOException ex) {
            throw new SpelShellException(ex.getMessage(), ex);
        }
    }

    private static List<String> loadHistory(File exprHistoryFile) throws IOException {
        if (exprHistoryFile == null) {
            throw new SpelShellException(false, "exprHistoryFile is not set.");
        }
        return Files.readAllLines(exprHistoryFile.toPath(), StandardCharsets.UTF_8);
    }

    public static boolean matches(String name, String pattern) {
        String patternL = pattern.trim().toLowerCase();
        if (patternL.isEmpty()) {
            return true;
        }
        String[] parts = NAME_SPLIT_PAT.split(name);
        String nameL = name.toLowerCase();
        int partIdx = 0;
        int partBeginIdx = 0;
        int n = 0;
        int p = 0;
        int maxN = name.length() - 1;
        int maxP = pattern.length() - 1;
        while (n <= maxN && p <= maxP) {
            if (nameL.charAt(n) == patternL.charAt(p)) {
                p++;
                n++;
            } else {
                while (partBeginIdx <= n && partIdx < parts.length) {
                    partBeginIdx += parts[partIdx++].length();
                }
                n = partBeginIdx;
            }
        }
        return p > maxP;
    }

    private static Pattern pat(String regex) {
        return Pattern.compile(regex, Pattern.DOTALL);
    }

    private Stream<Method> getExposedMethods() {
        return Arrays.stream(rootObject.getClass().getMethods())
            .filter(method -> !Modifier.isStatic(method.getModifiers()) && isMethodToShow(method.getName()));
    }

    public static class SpelShellException extends RuntimeException {
        private final boolean printStackTrace;

        public SpelShellException(String message) {
            super(message);
            printStackTrace = true;
        }

        public SpelShellException(String message, Throwable cause) {
            super(message, cause);
            printStackTrace = true;
        }

        public SpelShellException(boolean printStackTrace, String message) {
            super(message);
            this.printStackTrace = printStackTrace;
        }

        public boolean isPrintStackTrace() {
            return printStackTrace;
        }
    }

    public static class SpelShellExitException extends RuntimeException {
        public SpelShellExitException() {
        }

        public SpelShellExitException(String message) {
            super(message);
        }
    }

    public static class BasicOperatorOverloader implements OperatorOverloader {

        @Override
        public boolean overridesOperation(
            Operation operation, @Nullable Object leftOperand, @Nullable Object rightOperand
        ) throws EvaluationException {
            if (operation == Operation.DIVIDE) {
                if (leftOperand != null && rightOperand != null) {
                    return (leftOperand instanceof String || leftOperand instanceof Path)
                        && (rightOperand instanceof String || rightOperand instanceof Path);
                }
            }
            return false;
        }

        @Override
        public Object operate(
            Operation operation, @Nullable Object leftOperand, @Nullable Object rightOperand
        ) throws EvaluationException {
            if (operation == Operation.DIVIDE) {
                if (leftOperand != null && rightOperand != null) {
                    return Path.of(leftOperand + "/" + rightOperand).normalize();
                }
            }
            throw new EvaluationException("Cannot operate: %s %s %s.".formatted(leftOperand, operation, rightOperand));
        }
    }
}
