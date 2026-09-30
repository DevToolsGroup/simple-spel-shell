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
        Assertions.assertEquals("he npat('len')", ShellUtils.replaceAllNamePatterns("he `len"));
        Assertions.assertEquals("he npat('len')", ShellUtils.replaceAllNamePatterns("he `len`"));
        Assertions.assertEquals("he npat('len')+123", ShellUtils.replaceAllNamePatterns("he `len`+123"));
    }

    @Test
    void expressionReaderJoinsTrailingBackslashLinesWithoutInsertingASpace() {
        Assertions.assertEquals("abcdef", readOneExpression("abc\\\ndef"));
        Assertions.assertEquals("abc def", readOneExpression("abc \\\ndef"));
        Assertions.assertEquals("abc  def", readOneExpression("abc \\\n def"));
        Assertions.assertEquals("abcdefghi", readOneExpression("abc\\\ndef\\\nghi"));
    }

    @Test
    void expressionReaderJoinsExpressionBlockLinesWithNoSeparatorJustLikeTrailingBackslash() {
        Assertions.assertEquals("abcdef", readOneExpression("<<abc\ndef\n>>"));
        Assertions.assertEquals("abc def", readOneExpression("<<abc \ndef\n>>"));
        Assertions.assertEquals("abc  def", readOneExpression("<<abc \n def\n>>"));
        Assertions.assertEquals("abcdefghi", readOneExpression("<<abc\ndef\nghi\n>>"));
    }

    @Test
    void expressionReaderRequiresNoWhitespaceAfterOpenerAndTakesContentFullyLiterally() {
        // no space after '<<': content starts immediately, exactly as typed, nothing stripped.
        Assertions.assertEquals("foo", readOneExpression("<<foo\n>>"));
        // whitespace right after '<<', if any, is literal content too - none of it is stripped as a separator.
        Assertions.assertEquals(" foo", readOneExpression("<< foo\n>>"));
        Assertions.assertEquals("  foo", readOneExpression("<<  foo\n>>"));
    }

    @Test
    void expressionReaderTreatsBareOpenerAsContributingNoContentOfItsOwn() {
        Assertions.assertEquals("foobar", readOneExpression("<<\nfoo\nbar\n>>"));
        Assertions.assertEquals("", readOneExpression("<<\n>>"));
    }

    @Test
    void expressionReaderTreatsCloseMarkerLineTakingSurroundingWhitespace() {
        Assertions.assertEquals(" foo", readOneExpression("<< foo\n   >>  "));
    }

    @Test
    void expressionReaderToleratesLeadingWhitespaceBeforeOpenerButNotAfterIt() {
        Assertions.assertEquals("foo", readOneExpression("  <<foo\n>>"));
        Assertions.assertEquals(" foo", readOneExpression("  << foo\n>>"));
    }

    @Test
    void expressionReaderTreatsOpenerLineRemainderAndBackslashesInsideExpressionBlockVerbatim() {
        Assertions.assertEquals(
            " // not a comment in herestill \\ literal backslash",
            readOneExpression("<< // not a comment in here\nstill \\ literal backslash\n>>")
        );
    }

    @Test
    void expressionReaderSkipsSingleLineCommentsInsideExpressionBlock() {
        Assertions.assertEquals("abcghi", readOneExpression("<<abc\n//def\nghi\n>>"));
        Assertions.assertEquals("abcghi", readOneExpression("<<abc\n    // def\nghi\n>>"));
        // '//' not at the start of a line is literal content.
        Assertions.assertEquals("url('http://x')", readOneExpression("<<url('http://x')\n>>"));
        Assertions.assertEquals("a.url('http://x')", readOneExpression("<<a\n.url('http://x')\n>>"));
    }

    @Test
    void expressionReaderSkipsCommentBlocksInsideExpressionBlock() {
        String text = String.join("\n",
            "<<config = configBuilder()",
            "    //.url('some-url')",
            "    .url('test-url')",
            "    //<<.username('abc')",
            "    .password('def')",
            "    >>",
            "    .username('test-abc')",
            "    .password('test-def')",
            ">>"
        );
        Assertions.assertEquals(
            "config = configBuilder()    .url('test-url')    .username('test-abc')    .password('test-def')",
            readOneExpression(text)
        );
    }

    @Test
    void expressionReaderSkipsNestedCommentBlocksInsideExpressionBlock() {
        String text = String.join("\n",
            "<<abc",
            "//<< outer",
            "    //<< inner",
            "    >>",
            "    still comment",
            ">>",
            "def",
            ">>"
        );
        Assertions.assertEquals("abcdef", readOneExpression(text));
    }

    @Test
    void expressionReaderThrowsOnUnterminatedCommentBlockInsideExpressionBlock() {
        ShellException ex = Assertions.assertThrows(
            ShellException.class, () -> readOneExpression("<<abc\n//<< comment\nbar")
        );
        Assertions.assertEquals(
            "Unterminated comment block: reached end of input before a closing '>>' line.", ex.getMessage()
        );
    }

    @Test
    void expressionReaderUsesConfiguredCommentMarkersInsideExpressionBlock() {
        String result = ShellUtils.expressionReader(
            ShellUtils.lineReader("#{foo\n# line comment\n//still content\n/*\nblock comment\n*/\nbar\n}#"),
            "#{",
            "}#",
            "#",
            "/*",
            "*/"
        ).readExpression();
        Assertions.assertEquals("foo//still contentbar", result);
    }

    @Test
    void expressionReaderKeepsCommentLikeLinesInsideExpressionBlockWhenCommentMarkersDisabled() {
        String result = ShellUtils.expressionReader(
            ShellUtils.lineReader("<<foo\n//bar\n>>"),
            ShellUtils.DEFAULT_EXPR_BLOCK_OPEN_MARKER,
            ShellUtils.DEFAULT_EXPR_BLOCK_CLOSE_MARKER,
            null,
            null,
            null
        ).readExpression();
        Assertions.assertEquals("foo//bar", result);
    }

    @Test
    void expressionReaderThrowsOnUnterminatedExpressionBlock() {
        ShellException ex = Assertions.assertThrows(ShellException.class, () -> readOneExpression("<< foo\nbar"));
        Assertions.assertTrue(ex.isPrintStackTrace());
        Assertions.assertEquals(
            "Unterminated expression block: reached end of input before a closing '>>' line.", ex.getMessage()
        );
    }

    @Test
    void expressionReaderSkipsCommentBlockAndContinuesToNextExpression() {
        Assertions.assertEquals(
            "1+2",
            readOneExpression("//<< a multiline comment\nthat uses a single //\nat the very beginning only\n>>\n1+2")
        );
    }

    @Test
    void expressionReaderRequiresNoWhitespaceAfterCommentBlockOpener() {
        Assertions.assertEquals("1+2", readOneExpression("//<<no space here\nstill discarded\n>>\n1+2"));
    }

    @Test
    void expressionReaderRecognizesCommentBlockIndependentlyOfCustomCommentLineMarker() {
        String result = ShellUtils.expressionReader(
            ShellUtils.lineReader("//<< comment\nstill comment\n>>\n1+2"),
            ShellUtils.DEFAULT_EXPR_BLOCK_OPEN_MARKER,
            ShellUtils.DEFAULT_EXPR_BLOCK_CLOSE_MARKER,
            "#",
            ShellUtils.DEFAULT_COMMENT_BLOCK_OPEN_MARKER,
            ShellUtils.DEFAULT_COMMENT_BLOCK_CLOSE_MARKER
        ).readExpression();
        Assertions.assertEquals("1+2", result);
    }

    @Test
    void expressionReaderThrowsOnUnterminatedCommentBlock() {
        ShellException ex = Assertions.assertThrows(ShellException.class, () -> readOneExpression("//<< foo\nbar"));
        Assertions.assertTrue(ex.isPrintStackTrace());
        Assertions.assertEquals("Unterminated comment block: reached end of input before a closing '>>' line.", ex.getMessage());
    }

    @Test
    void expressionReaderSupportsNestedCommentBlocks() {
        String text = String.join(
            "\n",
            "//<< main comment",
            "    //<< nested comment",
            "    >>",
            "    //<< another nested comment",
            "    >>",
            ">>",
            "1+1"
        );
        Assertions.assertEquals("1+1", readOneExpression(text));
    }

    @Test
    void expressionReaderThrowsOnUnterminatedNestedCommentBlock() {
        String text = String.join(
            "\n",
            "//<< main comment",
            "    //<< nested comment, never closed",
            "1+1"
        );
        ShellException ex = Assertions.assertThrows(ShellException.class, () -> readOneExpression(text));
        Assertions.assertTrue(ex.isPrintStackTrace());
        Assertions.assertEquals(
            "Unterminated comment block: reached end of input before a closing '>>' line.", ex.getMessage()
        );
    }

    @Test
    void expressionReaderDoesNotTreatOpenerAsBlockWhenItIsAContinuationLine() {
        Assertions.assertEquals("abc<< def", readOneExpression("abc\\\n<< def"));
    }

    @Test
    void expressionReaderSkipsLeadingSingleLineCommentsBeforeDetectingABlockOpener() {
        String result = ShellUtils.expressionReader(
            ShellUtils.lineReader("// leading comment\n<<foo\n>>"),
            ShellUtils.DEFAULT_EXPR_BLOCK_OPEN_MARKER,
            ShellUtils.DEFAULT_EXPR_BLOCK_CLOSE_MARKER,
            "//",
            ShellUtils.DEFAULT_COMMENT_BLOCK_OPEN_MARKER,
            ShellUtils.DEFAULT_COMMENT_BLOCK_CLOSE_MARKER
        ).readExpression();
        Assertions.assertEquals("foo", result);
    }

    @Test
    void expressionReaderUsesConfiguredCustomBlockMarkersInsteadOfDefaults() {
        String result = ShellUtils.expressionReader(
            ShellUtils.lineReader("#{foo\nbar\n}#"),
            "#{",
            "}#",
            null,
            "//#{",
            "}#"
        ).readExpression();
        Assertions.assertEquals("foobar", result);
    }

    @Test
    void expressionReaderDoesNotTreatDefaultMarkersSpeciallyWhenCustomMarkersConfigured() {
        String result = ShellUtils.expressionReader(
            ShellUtils.lineReader("<< foo\n>>"),
            "#{",
            "}#",
            null,
            "//#{",
            "}#"
        ).readExpression();
        Assertions.assertEquals("<< foo", result);
    }

    private String readOneExpression(String text) {
        return ShellUtils.expressionReader(ShellUtils.lineReader(text)).readExpression();
    }

    private void testSplit(String name, String... expectedSplit) {
        Assertions.assertArrayEquals(expectedSplit, ShellUtils.splitForMatch(name));
    }
}