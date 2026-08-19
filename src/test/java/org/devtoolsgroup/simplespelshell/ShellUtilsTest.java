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

    private void testSplit(String name, String... expectedSplit) {
        Assertions.assertArrayEquals(expectedSplit, ShellUtils.splitForMatch(name));
    }
}