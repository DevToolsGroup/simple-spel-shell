# Multiline expressions and comments

## 1. General description

An expression can span several lines, and so can a comment.
The simplest way is per line:
a trailing `\` joins the current line with the next one, with no separator inserted,
and a line starting with `//` is a single-line comment.
For a long expression or a comment spanning many lines this gets awkward,
since every line needs its own trailing `\` or leading `//`.

Multiline blocks are a second, block-based way to write both.
An **expression block** starts with `<<` and ends with a `>>` line:

```
<< multiline
expression
that doesn't
need slashes
at the end
of each line
>>
```

A **comment block** starts with `/*` and ends with a `*/` line:

```
/* a multiline
comment
that doesn't need
a marker on every line
*/
```

Blocks work everywhere the per-line mechanisms do
— interactively and in every `runScript(...)` variant — and can be mixed freely with them.
Comments can also be used inside an expression block, both as `//` lines and as `/*` ... `*/` blocks.
All five markers (`<<`, `>>`, `//`, `/*`, `*/`) are defaults and can be changed or disabled
(see [Configuring the markers](#configuring-the-markers)).

## 2. How it can be used

### Expression blocks

Everything between the opener and the closing `>>` line becomes one expression,
concatenated with **no separator inserted**.
An expression block is a stand-in for a chain of trailing-`\` continuations and behaves exactly like one:
no whitespace is added or removed anywhere.

```
<<result = 1
+ 2
>>
```

is read as `"result = 1+ 2"`.
The same gotcha as with trailing `\` applies:
without whitespace at the end of one line or the start of the next, the two lines run together.
Leading whitespace is kept too, so indenting the continuation line is enough to separate them:

```
<<result = 1
 + 2
>>
```

is read as `"result = 1 + 2"`.

No whitespace is required after the opening `<<`
— content can start immediately (`<<foo`) or after any amount of whitespace (`<<  foo`).
Either way, the text after `<<` is taken literally, with nothing added or stripped,
unless it is a comment (see [Comments on the opener line](#comments-on-the-opener-line)).
If nothing follows `<<`, the content starts on the next line.

The closing `>>` must be on a line by itself.
Leading and trailing whitespace around it is fine, but nothing else may share that line:

```
<<
this content starts on the line after the opener
   >>
```

Expression blocks are **not** nestable:
the first line whose trimmed content is exactly `>>` always closes the block,
even if the content before it contains something that looks like a nested `<<`.

### Comment blocks

Everything from the `/*` opener up to the closing `*/` line is discarded,
including any text after `/*` on the opener line.
A comment block contributes nothing to the evaluated expression — like a `//` line, just spanning many lines.
The opener and closer follow the same rules as for expression blocks:
no whitespace is required after `/*`, and `*/` must be on a line by itself.

Unlike expression blocks, comment blocks can be nested.
The outer block only closes at its own matching `*/`, not at a `*/` that belongs to a block nested inside it:

```
/* main comment
    /* nested comment
    */
    /* another nested comment
    */
*/
```

Because of nesting, every line inside a comment block that starts with `/*` (after leading whitespace)
opens a nested block and needs its own `*/` line.
This includes one-line forms such as `/* note */`, see [Known limitations](#known-limitations).

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
2. A line starting with `/*` (after leading whitespace) opens a comment block,
   which is skipped up to its matching `*/`, nested comment blocks included.
3. A line starting with `//` (after leading whitespace) is a single-line comment and is skipped.
4. Any other line is appended to the expression as is.

Only whole lines are comments.
A `//` or `/*` after other content on the same line is literal expression text,
so something like `.url('http://example.com')` is not cut off.

### Comments on the opener line

The text right after `<<` on the opener line can be a comment too.
If it starts with `//` (after optional whitespace), it is dropped:

```
<< // this is a comment
expression
continuation
>>
```

is read as `"expressioncontinuation"`.

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

Each marker is a plain `String` field on `ReplConfig`:

| Field                     | Default |
|---------------------------|---------|
| `exprBlockOpenMarker`     | `"<<"`  |
| `exprBlockCloseMarker`    | `">>"`  |
| `commentLineMarker`       | `"//"`  |
| `commentBlockOpenMarker`  | `"/*"`  |
| `commentBlockCloseMarker` | `"*/"`  |

```java
getReplConfig().setExprBlockOpenMarker("#{");
getReplConfig().setExprBlockCloseMarker("}#");
getReplConfig().setCommentBlockOpenMarker("(*");
getReplConfig().setCommentBlockCloseMarker("*)");
```

The configured markers are used everywhere,
including for comments inside expression blocks and on their opener line.
The markers are independent of each other:
for example, a shell that replaces `//` with `#` as its single-line comment marker
still recognizes `/*` ... `*/` blocks.

Setting `exprBlockOpenMarker`, `commentLineMarker`, or `commentBlockOpenMarker` to `null`
disables that construct entirely.
A line starting with the disabled marker is then ordinary content, both at the top level and inside an expression block.
For example, with `commentLineMarker` set to `null`, a `//` line inside an expression block is literal expression text.

As with every `ReplConfig` field, `getReplConfig()` (interactive) and `getReplConfigForScript()` (scripts)
are configured independently.
The script config copies its markers from the interactive config when the shell is constructed,
so a later change to one of them doesn't affect the other — set both if scripts should also use the custom markers.
A sub-shell starts with copies of its parent shell's markers.

### Known limitations

- There is no way to include a standalone `>>` line (the configured close marker)
  as literal content of an expression block.
  Expression blocks can't be nested, and there is no escaping.
- There is no way to include a line starting with `//` or `/*` as literal content of an expression block,
  for example inside a string literal that spans several lines.
  Such a line is always treated as a comment, unless the corresponding marker is set to `null`.
- Apart from comments, content inside an expression block is taken verbatim:
  a trailing `\` inside an expression block is literal expression text, not a line continuation.
- A comment block can't be closed on its opener line.
  `/* comment */` on a single line opens a comment block that is still waiting for a `*/` line,
  so the lines after it are discarded too.
  The same happens with such a line inside a comment block (it opens a nested block)
  and inside an expression block or on its opener line.
  Use a `//` line for a single-line comment instead.
- Reaching the end of input before a closing marker line is found (for either block kind) raises a
  `ShellException` reporting the unterminated block, rather than silently returning partial content.
  This includes a comment block inside an expression block that is never closed.
  The exception is created with `printStackTrace = true`,
  so the REPL loop prints its stack trace along with the message (see [Exceptions](../reference/exceptions.md)).
- A block opener is only recognized at the start of a fresh expression
  (or, for comment blocks, also inside an expression block).
  If an open marker shows up on the continuation line of an already-started trailing-`\` expression,
  it is ordinary literal text of that continuation, not a block opener.

## 3. Implementation details

Both block forms are handled entirely inside the private `readExpr` method in
[`ShellUtils`](../reference/shell-utils.md), together with trailing-`\` continuation and `//` comment lines.
`readExpr` is the single choke point every expression source reads through
(the interactive REPL and every `runScript(...)` overload).

`readExpr` checks for a block opener only while its line accumulator is still empty,
i.e. before any trailing-`\` continuation for the current expression has started.
It checks for a comment-block opener first, then for an expression-block opener.

- A comment block is skipped by `skipCommentBlock`,
  which discards lines until a closing marker line at the same nesting depth.
  It tracks the depth by counting every line that starts with the open marker as a nested opener.
  Afterwards `readExpr` continues with the next line, so a comment block can be followed by any expression.
- An expression block is read by `readExpressionBlock`,
  which joins lines with no separator until the closing marker line,
  then returns immediately.
  No trailing-`\` processing happens afterward,
  the same way a plain, non-backslash-terminated line ends `readExpr`.

`readExpressionBlock` first looks at the rest of the opener line:
if it starts with the comment block open marker, `skipCommentBlock` is called;
if it is a comment line (`isCommentLine`), it is dropped; otherwise it starts the expression.
Each following line is then checked in order:
the expression block close marker first, then the comment block open marker (handled by `skipCommentBlock`),
then the single-line comment marker (`isCommentLine`); anything else is appended.

The markers are passed from `ReplConfig` through `ShellUtils.expressionReader(...)`
into `readExpr` and `readExpressionBlock`.
Their defaults are the `ShellUtils.DEFAULT_*_MARKER` constants,
which the single-argument `expressionReader(LineReader)` overload also uses.
