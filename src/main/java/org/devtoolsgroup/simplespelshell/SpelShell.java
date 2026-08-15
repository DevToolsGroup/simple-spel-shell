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

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SpelShell implements Shell {

    private static final Pattern SET_VAR_PAT = Pattern.compile("^\\s*([a-zA-Z$_][a-zA-Z$_0-9]*)\\s*=(.+)$");
    private static final Pattern ZERO_ARG_METHOD_PAT = Pattern.compile("^\\s*([a-zA-Z][a-zA-Z0-9]*)\\s*$");
    private static final Pattern ONE_ARG_METHOD_PAT = Pattern.compile("^\\s*([a-zA-Z][a-zA-Z0-9]*)\\s+(\\S.*)$");
    private static final Pattern TRAILING_SLASHES_PAT = Pattern.compile("^(.*)(\\\\+)\\s*$");

    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final Set<String> methodsToHide;
    private List<String> zeroArgMethods;
    private List<String> oneArgMethods;
    private StandardEvaluationContext spelCtx;
    private Object lastEvalResult;
    private BufferedReader input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    private PrintStream output = System.out;
    private boolean printExpression;
    private Supplier<String> prompt = () -> ">>> ";
    private int printEvalResultLength = 100;
    private File exprHistoryFile;
    private List<Converter<?, ?>> typeConverters = new ArrayList<>();
    private Object rootObject;
    private final Map<String, Object> variables = new ConcurrentHashMap<>();
    private final AtomicLong variablesHash = new AtomicLong(0);
    private Path curDir;
    private Consumer<Path> currentDirectoryValidator;

    public SpelShell(Path initialDir) {
        methodsToHide = new HashSet<>();
        methodsToHide.addAll(Set.of("equals", "getClass", "hashCode", "notify", "notifyAll", "toString", "wait",
            "setInput", "setOutput", "setGlobalFunctions", "setCurrentDirectoryValidator", "addTypeConverter",
            "setRootObject"));
        setRootObject(this);
        typeConverters.add(new Converter<String, Path>() {
            @Override
            public Path convert(String first) {
                return Path.of(first);
            }
        });
        initSpelCtx();

        Path absInitialDir = initialDir.toAbsolutePath().normalize();
        cd(absInitialDir);
        setCurrentDirectoryValidator(path -> {
            if (!path.toAbsolutePath().normalize().startsWith(absInitialDir)) {
                exn("Cannot work outside of " + absInitialDir);
            }
        });
    }

    @Override
    public void runScript(InputStream scriptInp, Charset cs, boolean stopOnException) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(scriptInp, cs));
        while (true) {
            String expr = null;
            try {
                print(prompt.get());
                expr = readExpr(reader);
                if (expr == null) {
                    return;
                }
                if (expr.isBlank()) {
                    continue;
                }
                expr = rewriteExpr(expr, zeroArgMethods, oneArgMethods);
                if (printExpression) {
                    println(expr);
                }
                Object res = eval(expr);
                if (exprHistoryFile != null) {
                    saveExprToHistFile(expr, exprHistoryFile);
                }
                if (printEvalResultLength > 0 && res != null) {
                    String resStr = res.toString();
                    String ellipsis = resStr.length() > printEvalResultLength ? "..." : "";
                    println(resStr.substring(0, Math.min(resStr.length(), printEvalResultLength)) + ellipsis);
                }
            } catch (Exception ex) {
                println(ex.getMessage());
                ex.printStackTrace(output);
                if (stopOnException) {
                    if (expr != null) {
                        throw new SpelShellException(
                            "An error occurred while evaluating expression\nExpression: %s\nError: %s".formatted(
                                expr, ex.getMessage()
                            ),
                            ex
                        );
                    }
                    throw new SpelShellException(ex.getMessage(), ex);
                }
            }
        }
    }

    @Override
    public void runScript(InputStream scriptInp, boolean stopOnException) {
        runScript(scriptInp, StandardCharsets.UTF_8, stopOnException);
    }

    @Override
    public void runScript(String script, boolean stopOnException) {
        runScript(
            new ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8)),
            StandardCharsets.UTF_8,
            stopOnException
        );
    }

    @Override
    public void runScript(Path path, Charset cs, boolean stopOnException) throws IOException {
        runScript(Files.readString(path, cs), stopOnException);
    }

    @Override
    public void runScript(Path path, boolean stopOnException) throws IOException {
        runScript(Files.readString(path, StandardCharsets.UTF_8), stopOnException);
    }

    @Override
    public void print(Object obj) {
        output.print(obj.toString());
    }

    @Override
    public void println(Object obj) {
        output.println(obj.toString());
    }

    @Override
    public String format(String format, Object... args) {
        return String.format(format, args);
    }

    @Override
    public String prompt(String prompt) throws IOException {
        print(prompt);
        return input.readLine();
    }

    @Override
    public void exit(int code) {
        System.exit(code);
    }

    @Override
    public void exit() {
        System.exit(0);
    }

    @Override
    public void exn(String msg) {
        throw new SpelShellException(msg);
    }

    @Override
    public void help(String filter) {
        Pattern pattern = makePattern(filter);
        Arrays.stream(rootObject.getClass().getMethods())
            .filter(method -> {
                String methodName = method.getName();
                return isMethodToShow(methodName) && pattern.matcher(methodName.toLowerCase()).matches();
            })
            .map(method -> {
                String params = Arrays.stream(method.getParameterTypes())
                    .map(Class::getName)
                    .collect(Collectors.joining(", "));
                String returnType = method.getReturnType().getName();
                String name = method.getName();
                return "%s(%s): %s".formatted(name, params, returnType);
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
        String finalFilter = filter.toLowerCase();
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
        variablesHash.incrementAndGet();
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
    public Path cd(Path path) {
        if (path.toString().isEmpty()) {
            return curDir;
        }
        if (path.isAbsolute()) {
            path = path.normalize();
        } else {
            path = curDir.resolve(path).toAbsolutePath().normalize();
        }
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
        Path pathToWriteTo = curDir.resolve(path).toAbsolutePath().normalize();
        if (!isParentChild(curDir, pathToWriteTo)) {
            exn("Cannot write outside of the current directory " + curDir);
        }
        Path parentPath = pathToWriteTo.getParent();
        if (!Files.exists(parentPath) && !parentPath.toFile().mkdirs()) {
            exn("There was an error when creating parent directories " + parentPath);
        }
        Files.writeString(pathToWriteTo, text, StandardCharsets.UTF_8);
    }

    @Override
    public String read(Path path) throws IOException {
        return Files.readString(curDir.resolve(path), StandardCharsets.UTF_8);
    }

    @Override
    public void mkdir(boolean autoCd, Path path) {
        if (!getFile(path).mkdirs()) {
            exn("There was an error when creating one of the specified directories");
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
    public void ll() throws IOException {
        List<Path> children;
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(curDir)) {
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
            String name = child.getFileName().toString();

            if (Files.isDirectory(child)) {
                System.out.printf("%" + width + "s %s/%n", "", name);
            } else {
                long size = Files.size(child);
                System.out.printf("%" + width + "d %s%n", size, name);
            }
        }
    }

    @Override
    public void setInput(InputStream input, Charset cs) {
        this.input = new BufferedReader(new InputStreamReader(input, cs));
    }

    @Override
    public void setInput(InputStream input) {
        setInput(input, StandardCharsets.UTF_8);
    }

    @Override
    public void setOutput(PrintStream output) {
        this.output = output;
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
    public void setCurrentDirectoryValidator(Consumer<Path> currentDirectoryValidator) {
        this.currentDirectoryValidator = currentDirectoryValidator;
        if (currentDirectoryValidator != null) {
            currentDirectoryValidator.accept(curDir);
        }
    }

    private Object eval(String expr) {
        long varHash = variablesHash.get();
        setLastEvalResult(parser.parseExpression(expr).getValue(spelCtx, rootObject, Object.class));
        if (variablesHash.get() != varHash) {
            initSpelCtx();
        }
        return lastEvalResult;
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

        DefaultConversionService defaultConversionService = new DefaultConversionService();
        typeConverters.forEach(defaultConversionService::addConverter);
        spelCtx.setTypeConverter(new StandardTypeConverter(defaultConversionService));

        spelCtx.setVariables(variables);
        setLastEvalResult(lastEvalResult);
    }

    private void setLastEvalResult(Object res) {
        lastEvalResult = res;
        spelCtx.setVariable("_", res);
    }

    private static Pattern makePattern(String pat) {
        return Pattern.compile(".*" + String.join(".*", pat.toLowerCase().split("")) + ".*");
    }

    private boolean isMethodToShow(String name) {
        return !methodsToHide.contains(name);
    }

    private static boolean isParentChild(Path parent, Path child) {
        return child.toAbsolutePath().normalize().startsWith(parent.toAbsolutePath().normalize());
    }

    private static void saveExprToHistFile(String expr, File histFile) throws IOException {
        try (FileWriter wr = new FileWriter(histFile, StandardCharsets.UTF_8, true)) {
            wr.append("\n").append(Instant.now().truncatedTo(ChronoUnit.SECONDS).toString())
                .append("\n").append(expr);
        }
    }

    private static List<String> loadHistory(File exprHistoryFile) throws IOException {
        if (exprHistoryFile == null) {
            throw new SpelShellException("exprHistoryFile is not set.");
        }
        return Files.readAllLines(exprHistoryFile.toPath(), StandardCharsets.UTF_8);
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
