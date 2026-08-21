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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShellUtils {
    private static final String IDENTIFIER_PAT = "[a-zA-Z_$][a-zA-Z_$0-9]*";
    private static final Pattern SET_VAR_PAT = pat("^\\s*(%s)\\s*=(.+)$".formatted(IDENTIFIER_PAT));
    private static final Pattern ZERO_ARG_METHOD_PAT = pat("^\\s*(%s)\\s*$".formatted(IDENTIFIER_PAT));
    private static final Pattern ONE_ARG_METHOD_PAT = pat("^\\s*(%s)\\s+(.*)$".formatted(IDENTIFIER_PAT));
    private static final Pattern SET_VAR_EQ_ZERO_ARG_METHOD_PAT = pat(
        "^\\s*(%s)\\s*=\\s*(%s)\\s*$".formatted(IDENTIFIER_PAT, IDENTIFIER_PAT)
    );
    private static final Pattern SET_VAR_EQ_ONE_ARG_METHOD_PAT = pat(
        "^\\s*(%s)\\s*=\\s*(%s)\\s+(.*)$".formatted(IDENTIFIER_PAT, IDENTIFIER_PAT)
    );
    private static final Pattern TRAILING_SLASHES_PAT = pat("^(.*)\\\\\\s*$");
    private static final Pattern NAME_SPLIT_PAT = Pattern.compile(
        "(?<=[a-z])(?=[A-Z])" +
            "|(?<=[^_])(?=_)|(?<=_)(?=[^_])" +
            "|(?<=[^-])(?=-)|(?<=-)(?=[^-])" +
            "|(?<=[^.])(?=\\.)|(?<=\\.)(?=[^.])" +
            "|(?<=[^$])(?=\\$)|(?<=\\$)(?=[^$])" +
            "|(?<=[^/])(?=/)|(?<=/)(?=[^/])" +
            "|(?<=[^\\\\])(?=\\\\)|(?<=\\\\)(?=[^\\\\])" +
            "|(?<=[0-9])(?=[^0-9])|(?<=[^0-9])(?=[0-9])"
    );
    private static final Pattern NAME_PATTERN_PAT = pat("`([^\\s`]+)`?");

    public static LineReader lineReader(String text) {
        return lineReader(new BufferedReader(new StringReader(text)));
    }

    public static LineReader lineReader(InputStream inputStream) {
        return lineReader(new BufferedReader(new InputStreamReader(inputStream)));
    }

    public static LineReader lineReader(File file, Charset cs) {
        try {
            return lineReader(new BufferedReader(new FileReader(file, cs)));
        } catch (IOException e) {
            throw new ShellException(e);
        }
    }

    public static LineReader lineReader(File file) {
        return lineReader(file, StandardCharsets.UTF_8);
    }

    public static ExpressionReader expressionReader(LineReader lineReader, Function<String, Boolean> isCommentLine) {
        return () -> readExpr(lineReader, isCommentLine);
    }

    public static ExpressionReader expressionReader(LineReader lineReader) {
        return () -> readExpr(lineReader, null);
    }

    public static String[] splitForMatch(String name) {
        return NAME_SPLIT_PAT.split(name);
    }

    public static boolean matches(String name, String pattern) {
        String patternL = pattern.trim().toLowerCase();
        if (patternL.isEmpty()) {
            return true;
        }
        if (name.length() < pattern.length()) {
            return false;
        }
        String[] parts = splitForMatch(name);
        int patLen = pattern.length();
        // how many first characters of the pattern can be formed using the parts processed so far.
        int patPrefixLen = 0;
        for (String part : parts) {
            int maxP = patPrefixLen;
            int partLen = part.length();
            for (int p = Math.max(0, patPrefixLen - partLen + 1); p <= maxP && patPrefixLen < patLen; p++) {
                int maxLen = Math.min(partLen, patLen - p);
                for (int i = 0; i < maxLen; i++) {
                    int pi = p + i;
                    if (Character.toLowerCase(part.charAt(i)) == patternL.charAt(pi)) {
                        if (pi + 1 > patPrefixLen) {
                            patPrefixLen = pi + 1;
                        }
                    } else {
                        break;
                    }
                }
            }
            if (patPrefixLen == patLen) {
                return true;
            }
        }
        return false;
    }

    public static String rewriteExpr(String expr, List<String> zeroArgMethods, List<String> oneArgMethods) {
        if (expr == null || expr.isEmpty()) {
            return expr;
        }
        Matcher matcher;
        matcher = SET_VAR_EQ_ONE_ARG_METHOD_PAT.matcher(expr);
        if (matcher.matches()) {
            return "var('%s',%s(%s))".formatted(
                matcher.group(1), findMethodByPattern(oneArgMethods, matcher.group(2)), matcher.group(3)
            );
        }
        matcher = SET_VAR_EQ_ZERO_ARG_METHOD_PAT.matcher(expr);
        if (matcher.matches()) {
            return "var('%s',%s())".formatted(matcher.group(1), findMethodByPattern(zeroArgMethods, matcher.group(2)));
        }
        matcher = SET_VAR_PAT.matcher(expr);
        if (matcher.matches()) {
            return "var('%s',%s)".formatted(matcher.group(1), matcher.group(2));
        }
        matcher = ONE_ARG_METHOD_PAT.matcher(expr);
        if (matcher.matches()) {
            return "%s(%s)".formatted(findMethodByPattern(oneArgMethods, matcher.group(1)), matcher.group(2));
        }
        matcher = ZERO_ARG_METHOD_PAT.matcher(expr);
        if (matcher.matches()) {
            return "%s()".formatted(findMethodByPattern(zeroArgMethods, matcher.group(1)));
        }
        return expr.trim();
    }

    public static int getSortOrder(Method method) {
        Order order = method.getAnnotation(Order.class);
        return order == null ? 0 : order.value();
    }

    public static boolean isParentChild(Path parent, Path child) {
        return child.toAbsolutePath().normalize().startsWith(parent.toAbsolutePath().normalize());
    }

    public static void saveExprToHistFile(String expr, File histFile) {
        try (FileWriter wr = new FileWriter(histFile, StandardCharsets.UTF_8, true)) {
            wr.append("\n").append(Instant.now().truncatedTo(ChronoUnit.SECONDS).toString())
                .append(" ").append(expr);
        } catch (IOException ex) {
            throw new ShellException(ex);
        }
    }

    public static List<String> loadHistory(File exprHistoryFile) {
        if (exprHistoryFile == null) {
            throw new ShellException(false, "exprHistoryFile is not set.");
        }
        if (!exprHistoryFile.exists()) {
            return List.of();
        }
        try {
            return Files.readAllLines(exprHistoryFile.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ShellException(e);
        }
    }

    public static String getStackTrace(Throwable th) {
        StringWriter sw = new StringWriter();
        th.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    public static String truncateWithEllipsis(String str, int maxLength) {
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    public static Consumer<Object> exnExit(Object result) {
        return _ -> {
            throw new ShellExitException(result);
        };
    }

    public static Consumer<Object> exnExit() {
        return exnExit(null);
    }

    public static String replaceAllNamePatterns(String str) {
        if (str == null) {
            return null;
        }
        return NAME_PATTERN_PAT.matcher(str).replaceAll("npat('$1')");
    }

    private static String findMethodByPattern(List<String> methods, String pattern) {
        List<String> found = methods.stream()
            .filter(method -> matches(method, pattern))
            .toList();
        if (found.isEmpty()) {
            throw new ShellException(false, "Cannot find a method by pattern '%s'".formatted(pattern));
        }
        if (found.size() > 1) {
            throw new ShellException(false, "Multiple methods match the pattern '%s':\n%s".formatted(
                pattern,
                String.join("\n", found)
            ));
        }
        return found.getFirst();
    }

    private static String readExpr(LineReader lineReader, Function<String, Boolean> isCommentLine) {
        StringBuilder sb = new StringBuilder();
        while (true) {
            String line = lineReader.readLine();
            if (line == null) {
                return sb.isEmpty() ? null : sb.toString();
            }
            if (isCommentLine != null && isCommentLine.apply(line)) {
                continue;
            }
            Matcher matcher = TRAILING_SLASHES_PAT.matcher(line);
            if (matcher.matches()) {
                sb.append(matcher.group(1)).append(" ");
            } else {
                sb.append(line);
                return sb.toString();
            }
        }
    }

    private static LineReader lineReader(BufferedReader bufferedReader) {
        AtomicBoolean closed = new AtomicBoolean(false);
        return () -> {
            try {
                String res = bufferedReader.readLine();
                if (res == null && !closed.get()) {
                    bufferedReader.close();
                    closed.set(true);
                }
                return res;
            } catch (IOException e) {
                throw new ShellException(e);
            }
        };
    }

    private static Pattern pat(String regex) {
        return Pattern.compile(regex, Pattern.DOTALL);
    }
}
