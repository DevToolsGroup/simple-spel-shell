# Multiline expressions and comments

## 1. General description

An expression can span several lines, and so can a comment.
The simplest way is per line:
a trailing `\` joins the current line with the next one, with no separator inserted,
and a `//`-prefixed line (by default; configurable via `ReplConfig.setCommentLineMarker(...)`) is a single-line comment.
For a genuinely long expression or a comment spanning many lines this gets awkward,
since every line needs its own trailing `\` or leading `//`.

Multiline blocks are a second, block-based way to write both.
They work everywhere the per-line mechanisms do
— interactively and in every `runScript(...)` variant — and can be mixed freely with them:

```
<< multiline
expression
that doesn't
need slashes
at the end
of each line
>>
```

```
/* a multiline
comment
that doesn't need
a marker on every line
*/
```

A block starting with `<<` is an expression block:
everything between the opener and the closing `>>` line becomes one expression,
concatenated with **no separator inserted**.
`<<` is essentially a stand-in for a whole chain of trailing-`\` continuations, and behaves exactly like one:
no whitespace is added or removed anywhere,
and the same gotcha applies (a missing trailing space merges the last word of one line into the first word of the next).
A block starting with `/*` is a comment block:
everything between the opener and the closing `*/` line is discarded,
contributing nothing to the evaluated expression — like a `//` line, just spanning many lines.

Comments can also be used inside an expression block, both as `//` lines and as `/*` ... `*/` blocks.
See [Comments inside expression blocks](#comments-inside-expression-blocks) below.

## 2. How it can be used

```
<< result = 1
+ 2
>>
```

is read as the expression `"result = 1+ 2"`
— concatenated flat, exactly like trailing-`\` continuation would join the same two lines.
As with trailing-`\`, remember the space before the line break if the two lines shouldn't run together:
`<< result = 1 \n+ 2\n>>` (mind the trailing space after `1`) reads as `"result = 1 + 2"` instead.

No whitespace is required after the opening marker (`<<` or `/*`)
— content can start immediately (`<<foo`) or after any amount of whitespace (`<<  foo`).
Either way, everything right after an expression block opener is taken literally, with nothing added or stripped,
unless it is a comment (see [Comments on the opener line](#comments-on-the-opener-line)).
The closing marker must be on a line by itself
— optional leading/trailing whitespace around it is fine, but nothing else may share that line:

```
<<
this content starts on the line after the opener, since nothing followed '<<'
   >>
```

Both examples close cleanly.
Comment blocks follow the exact same opening/closing rules (including "no whitespace required" after `/*`).
Only the markers and the "content is discarded" behavior differ from an expression block.

### Nesting comment blocks

A comment block can contain further comment blocks nested inside it
— the outer block only closes at its own matching `*/`, not at a `*/` that belongs to a block nested inside it:

```
/* main comment
    /* nested comment
    */
    /* another nested comment
    */
*/
```

Nesting is comment-block-only.
Expression blocks are **not** nestable:
the first line whose trimmed content is exactly `>>` always closes an expression block,
even if the block's content happens to contain something that looks like a nested `<<`/`>>` pair.

### Comments inside expression blocks

Lines inside an expression block can be commented out, which is handy for switching between alternatives:

```
<<config = configBuilder()
    //.url('some-url')
    .url('test-url')
    /*.username('abc')
    .password('def')
    */
    .username('test-abc')
    .password('test-def')
>>
```

is read as `"config = configBuilder()    .url('test-url')    .username('test-abc')    .password('test-def')"`.
The remaining lines are still concatenated with no separator, so their indentation stays in the expression.

Inside an expression block, each line is handled as follows:

1. A line whose trimmed content is exactly `>>` closes the expression block.
2. A line starting with `/*` (after leading whitespace) opens a comment block, which is skipped up to its matching `*/`.
   Comment blocks nested inside it work as described in [Nesting comment blocks](#nesting-comment-blocks).
3. A line starting with `//` (after leading whitespace) is a single-line comment and is skipped.
4. Any other line is appended to the expression as is.

Only whole lines are comments.
A `//` or `/*` after other content on the same line is literal expression text,
so something like `.url('http://example.com')` is not cut off.

### Comments on the opener line

The text right after `<<` on the opener line can be a comment too:

```
<< // this is a comment
expression
continuation
>>
```

is read as `"expressioncontinuation"`.
If the text after `<<` starts with `//` (after optional whitespace), it is dropped.
If it starts with `/*`, a comment block is opened and skipped up to its matching `*/` line,
after which the expression block continues until its own `>>`:

```
<< /* a comment block
started on the opener line
*/
expression
continuation
>>
```

is also read as `"expressioncontinuation"`.
Note that this form needs two closing lines: `*/` closes the comment block, and `>>` closes the expression block.

### Configuring the markers

`<<`, `>>`, `/*`, and `*/` are defaults, not fixed syntax
— each is a plain `String` field on `ReplConfig`, alongside the single-line `commentLineMarker`:

```java
getReplConfig().setExprBlockOpenMarker("#{");
getReplConfig().setExprBlockCloseMarker("}#");
getReplConfig().setCommentBlockOpenMarker("(*");
getReplConfig().setCommentBlockCloseMarker("*)");
```

Expression blocks and comment blocks each have their own close marker
(`exprBlockCloseMarker` and `commentBlockCloseMarker`).
They default to `">>"` and `"*/"` respectively.

The configured markers apply inside expression blocks as well:
comments inside an expression block (including on its opener line)
are recognized by the configured `commentLineMarker` and `commentBlockOpenMarker`.

As with every `ReplConfig` field, `getReplConfig()` (interactive) and `getReplConfigForScript()` (batch)
are configured independently — set both if scripts should also use the custom markers.
Setting `exprBlockOpenMarker` or `commentBlockOpenMarker` to `null` disables that block kind entirely
(a line starting with the disabled marker is then read as ordinary content,
exactly as if block syntax weren't supported), the same way `null` disables single-line comments via `commentLineMarker`.
This also applies inside expression blocks:
with `commentLineMarker` set to `null`, a `//` line inside an expression block is literal expression text.

### Known limitations

- No nesting for expression blocks, and no escaping:
  an expression block ends at the *first* line whose trimmed content is exactly the configured close marker,
  regardless of what appears inside the block.
  There is no way to include a standalone close-marker line as literal content of an expression block.
  Comment blocks are the exception — see [Nesting comment blocks](#nesting-comment-blocks) above.
- There is no way to include a line starting with `//` or `/*` as literal content of an expression block,
  for example inside a string literal that spans several lines.
  Such a line is always treated as a comment, unless the corresponding marker is set to `null`.
- Apart from comments, content inside an expression block is taken verbatim:
  no backslash-continuation processing happens on lines inside the block.
  A trailing `\` inside an expression block is literal expression text.
- A comment block can't be closed on its opener line.
  `/* comment */` on a single line opens a comment block that is still waiting for a `*/` line,
  so the lines after it are discarded too.
  Use a `//` line for a single-line comment instead.
- A comment block is recognized by its own open marker,
  independent of the single-line `commentLineMarker` a shell has configured via `ReplConfig.setCommentLineMarker(...)`.
  Even a shell that has replaced `//` with, say, `#` as its single-line comment marker still recognizes a
  `/*` ... `*/` block (assuming default block markers).
- Reaching the end of input before a closing marker line is found (for either block kind) raises a
  `ShellException` reporting the unterminated block, rather than silently returning partial content.
  This includes a comment block inside an expression block that is never closed.
  The exception is created with `printStackTrace = true`,
  so the REPL loop prints its stack trace along with the message (see [Exceptions](../reference/exceptions.md)).
- A block opener is only recognized as the very first line contributing to a fresh expression
  (or, for comment blocks, as a line inside an expression block).
  If the open marker shows up as the continuation line of an already-in-progress trailing-`\` expression,
  it is treated as ordinary literal text of that continuation, not as a block opener.

## 3. Implementation details

Both block forms are detected and handled entirely inside the private `readExpr` method in
[`ShellUtils`](../reference/shell-utils.md) — the single choke point every expression source
(the interactive REPL and every `runScript(...)` overload) reads through,
the same way trailing-`\` continuation is implemented as a fixed part of that method.

`readExpr` checks for a comment-block or expression-block opener only while its own line accumulator is still empty
— i.e., only before any trailing-`\` continuation for the current expression has started.
A comment block is skipped by reading and discarding lines until a closing marker line at the same nesting depth,
tracking depth by also recognizing nested opener lines while skipping (`skipCommentBlock`).
An expression block is read by `readExpressionBlock`, which joins lines (with no separator inserted, ever)
until the closing marker line, then returns immediately.
No further trailing-`\` processing is checked afterward,
the same way a plain, non-backslash-terminated line ends `readExpr`.

`readExpressionBlock` first looks at the rest of the opener line:
if it starts with the comment block open marker, `skipCommentBlock` is called;
if it is a comment line (`isCommentLine`), it is dropped; otherwise it starts the expression.
Each following line is then checked in order:
the expression block close marker first, then the comment block open marker (handled by `skipCommentBlock`),
then the single-line comment marker (`isCommentLine`); anything else is appended.

The four block markers travel alongside the single-line `commentLineMarker` as `String` fields on `ReplConfig`
— `exprBlockOpenMarker`, `exprBlockCloseMarker`, `commentBlockOpenMarker`, `commentBlockCloseMarker` —
defaulting to `"<<"`, `">>"`, `"/*"`, and `"*/"` respectively (the `ShellUtils.DEFAULT_*_MARKER` constants),
and are threaded through `ShellUtils.expressionReader(...)` into `readExpr` and `readExpressionBlock`
exactly like `commentLineMarker` is.
