# Multiline expressions and comments

## 1. General description

Two mechanisms already let one line "continue" onto the next.
A trailing `\` joins the current line with the next one, with no separator inserted.
A `//`-prefixed line (by default; configurable via `ReplConfig.setIsCommentLine(...)`) is a single-line comment.
Both apply per physical line, which makes them awkward for a genuinely long expression or a comment spanning many
lines — every line needs its own trailing `\` or leading `//`.

This feature adds a second, block-based way to write both, usable anywhere the existing mechanisms are
— interactively and in every `runScript(...)` variant — without replacing either of them:

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
//<< a multiline
comment
that uses a single //
at the very beginning only
>>
```

A block starting with `<<` is an expression block: everything between the opener and the closing `>>` line
becomes one expression, concatenated with **no separator inserted** — `<<` is essentially a stand-in for a
whole chain of trailing-`\` continuations, and behaves exactly like one: no whitespace is added or removed
anywhere, and the same gotcha applies (a missing trailing space merges the last word of one line into the
first word of the next).
A block starting with `//<<` is a comment block: everything between the opener and the closing `>>` line
is discarded, contributing nothing to the evaluated expression — like a `//` line, just spanning many lines.

## 2. How it can be used

```
<< result = 1
+ 2
>>
```

is read as the expression `"result = 1+ 2"` — concatenated flat, exactly like trailing-`\` continuation
would join the same two lines.
As with trailing-`\`, remember the space before the line break if the two lines shouldn't run together:
`<< result = 1 \n+ 2\n>>` (mind the trailing space after `1`) reads as `"result = 1 + 2"` instead.

No whitespace is required after the opening marker (`<<` or `//<<`) — content can start immediately
(`<<foo`) or after any amount of whitespace (`<<  foo`); either way, everything right after the marker is
taken completely literally, with nothing added or stripped.
The closing `>>` must be on a line by itself — optional leading/trailing whitespace around it is fine,
but nothing else may share that line:

```
<<
this content starts on the line after the opener, since nothing followed '<<'
   >>
```

Both examples close cleanly.
Comment blocks follow the exact same opening/closing rules (including "no whitespace required" after
`//<<`); only the prefix and the "content is discarded" behavior differ from an expression block.

### Nesting comment blocks

A comment block can contain further comment blocks nested inside it — the outer block only closes at its own
matching `>>`, not at a `>>` that belongs to a block nested inside it:

```
//<< main comment
    //<< nested comment
    >>
    //<< another nested comment
    >>
>>
```

Nesting is comment-block-only.
Expression blocks are **not** nestable: the first line whose trimmed content is exactly `>>` always closes
an expression block, even if the block's (fully verbatim) content happens to contain something that looks
like a nested `<<`/`>>` pair.

### Configuring the markers

`<<`, `//<<`, and `>>` are defaults, not fixed syntax — each is a plain `String` field on `ReplConfig`:

```java
getReplConfig().setExprBlockOpenMarker("#{");
getReplConfig().setCommentBlockOpenMarker("//#{");
getReplConfig().setBlockCloseMarker("}#");
```

As with every `ReplConfig` field, `getReplConfig()` (interactive) and `getReplConfigForScript()` (batch)
are configured independently — set both if scripts should also use the custom markers.
Setting `exprBlockOpenMarker` or `commentBlockOpenMarker` to `null` disables that block kind entirely
(a line starting with the disabled marker is then read as ordinary content, exactly as if this feature
didn't exist), the same way `null` already disables `isCommentLine`.

### Known limitations

- No nesting for expression blocks, and no escaping: an expression block ends at the *first* line whose
  trimmed content is exactly the configured close marker, regardless of what appears inside the block.
  There is no way to include a standalone close-marker line as literal content of an expression block.
  Comment blocks are the exception — see [Nesting comment blocks](#nesting-comment-blocks) above.
- Content inside an expression block is taken completely verbatim: no backslash-continuation processing
  and no `//`-comment stripping happens on lines inside the block.
  A line that looks like `// not a comment` inside an expression block is literal expression text,
  not a stripped comment.
- A comment block is recognized by its literal open-marker prefix, independent of whatever `isCommentLine`
  predicate a shell has configured via `ReplConfig.setIsCommentLine(...)`.
  Even a shell that has replaced `//` entirely with, say, `#` as its single-line comment marker still
  recognizes a literal `//<<...>>` block (assuming default markers) — because `isCommentLine` is a per-line
  boolean predicate and cannot express "keep reading until a closing marker shows up several lines later."
  This mirrors how trailing-`\` continuation is itself unconditional and not pluggable.
- Reaching the end of input before a closing marker line is found (for either block kind) raises a
  `ShellException` reporting the unterminated block, rather than silently returning partial content.
- A block opener is only recognized as the very first line contributing to a fresh expression.
  If the open marker shows up as the continuation line of an already-in-progress trailing-`\` expression,
  it is treated as ordinary literal text of that continuation, not as a block opener.

## 3. Implementation details

Both block forms are detected and handled entirely inside the private `readExpr` method in
[`ShellUtils`](../reference/shell-utils.md) — the single choke point every expression source
(the interactive REPL and every `runScript(...)` overload) already reads through, the same way
trailing-`\` continuation is already implemented as a fixed part of that method.

`readExpr` checks for a comment-block or expression-block opener only while its own line accumulator
is still empty — i.e., only before any trailing-`\` continuation for the current expression has started.
A comment block is skipped by reading and discarding lines until a closing marker line at the same nesting
depth, tracking depth by also recognizing nested opener lines while skipping (`skipCommentBlock`).
An expression block reads and joins lines verbatim (with no separator inserted, ever) until the closing
marker line, then returns immediately — no further trailing-`\` processing is checked afterward, the same
way a plain, non-backslash-terminated line already ends `readExpr` today.

The three markers travel alongside `isCommentLine` as three new `ReplConfig` fields —
`exprBlockOpenMarker`, `commentBlockOpenMarker`, `blockCloseMarker` — defaulting to `"<<"`, `"//<<"`,
and `">>"` respectively, and are threaded through `ShellUtils.expressionReader(...)` into `readExpr`
exactly like `isCommentLine` already is.
