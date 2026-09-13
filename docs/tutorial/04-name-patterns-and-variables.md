# 4. Name Patterns and Variables

No changes to `TaskShell`'s Java code on this page
— just two more built-in pieces of syntax:
SpEL variables via the `var` command, and backtick name patterns for fuzzy-searching them.

## `var` and `#name`

`BaseSpelShellImpl` gives every shell a `var` command with four overloads:
`var(name, value)` to set one, `var(name)` to read one back,
`var()` to list everything, and `var(NamePattern)` to list a fuzzy-filtered subset.
Combined with the `x = value` shorthand from the last page, setting a variable is just:

```
SpEL> due1='2026-09-20'
2026-09-20
SpEL> due2='2026-10-01'
2026-10-01
SpEL> dueSoon='2026-09-15'
2026-09-15
```

Each line prints its own value back
— `var(name, value)` returns the value it was given,
and (as covered on page 2) any non-`null` result gets auto-printed.
Once a variable is set, SpEL's own `#name` syntax reads it back anywhere in an expression,
exactly like a local variable:

```
SpEL> #due1
2026-09-20
```

Always use the `#` prefix to *read* a variable.
A bare `due1` with nothing else on the line matches the zero-arg-method shorthand rule from page 3 instead,
and — since no command named `due1` exists —
fails with a `ShellException` ("Cannot find a method by pattern 'due1'").
Bare names are for *setting* a variable (`due1 = value`, via the `x = value` rule);
`#due1` is for reading one back inside an expression.

## Backtick name patterns

Once you have more than a couple of variables, finding them again by name gets old.
Wrapping a fragment in backticks — `` `pat `` or `` `pat` `` —
gets rewritten (before shorthand rewriting even runs) into `npat('pat')`, a `NamePattern` object.
`help` and `var` both accept a `NamePattern` and use it to fuzzy-filter what they list,
using the same matching algorithm from page 3.

```
SpEL> var `due`
due1: java.lang.String
due2: java.lang.String
dueSoon: java.lang.String
```

All three variables contain "due" as a literal prefix,
so all three show up, each with the runtime type of the value stored.

This groundwork — variables and name patterns —
comes back in a bigger way on [page 13](13-operator-overloading.md),
where a name pattern is used to fuzzy-find a *task* by title, not just a variable.

---
Previous: [3. Shorthand Syntax and Fuzzy Command Names](03-shorthand-and-fuzzy-matching.md) · Next: [5. Built-in Commands: help, print, prompt, exit](05-built-in-commands.md)
