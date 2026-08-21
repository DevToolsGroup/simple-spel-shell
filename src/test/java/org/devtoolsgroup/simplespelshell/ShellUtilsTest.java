package org.devtoolsgroup.simplespelshell;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ShellUtilsTest {

    @Test
    void splitForMatch() {
        testSplit("getProperty", "get", "Property");
        testSplit("get_property", "get", "_", "property");
        testSplit("max-length", "max", "-", "length");
        testSplit("setProp1", "set", "Prop", "1");
        testSplit("setProp12New", "set", "Prop", "12", "New");
        testSplit("archive.tar.gz", "archive", ".", "tar", ".", "gz");
        testSplit("SpelShell$BasicOperatorOverloader.class",
            "Spel", "Shell", "$", "Basic", "Operator", "Overloader", ".", "class");
        testSplit("path/to/file", "path", "/", "to", "/", "file");
        testSplit("path\\to\\file", "path", "\\", "to", "\\", "file");
    }

    @Test
    void matches() {
        Assertions.assertTrue(ShellUtils.matches("setLastEvalResultMaxPrintLength", "len"));
        Assertions.assertTrue(ShellUtils.matches("setLastEvalResultMaxPrintLength", "llen"));
        Assertions.assertTrue(ShellUtils.matches("setLastEvalResultAbsPrintLength", "lablen"));
        Assertions.assertTrue(ShellUtils.matches("setLastEvalResultAbsPrintLength", "lablen"));
        Assertions.assertTrue(ShellUtils.matches("ab-cd-ef", "aef"));
        Assertions.assertTrue(ShellUtils.matches("ab-cd-ef", ""));
        Assertions.assertTrue(ShellUtils.matches("abc-cef", "abce"));
        Assertions.assertFalse(ShellUtils.matches("abc-cef", "abe"));
        Assertions.assertTrue(ShellUtils.matches("ab-cd-ef", "abcdef"));
        Assertions.assertFalse(ShellUtils.matches("ab-cd-ef", "abcdef1"));
    }

    @Test
    void replaceAllNamePatterns() {
        Assertions.assertEquals(
            "he npat('len')",
            ShellUtils.replaceAllNamePatterns("he `len")
        );
    }

    private void testSplit(String name, String... expectedSplit) {
        Assertions.assertArrayEquals(expectedSplit, ShellUtils.splitForMatch(name));
    }
}