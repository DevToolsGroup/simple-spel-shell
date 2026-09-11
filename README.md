# simple-spel-shell

A small Java library for building interactive CLI dev tools, where every command you type is evaluated as a [Spring Expression Language (SpEL)](https://docs.spring.io/spring-framework/reference/core/expressions.html) expression.

## What is it?

To build a shell, you subclass one of the framework's base shell classes and add plain public Java methods. Each method is automatically exposed as a shell command via reflection — no annotations, no registration boilerplate. When a user types a command, it's rewritten into a SpEL expression and evaluated with your shell object as the root, so alongside your own methods, the full power of SpEL is available at the prompt: arithmetic, variables, static type access (`T(Math).PI`), collection literals, and more.

The library has a single real dependency, `spring-expression` — there's no Spring Boot, no DI container, just the expression engine.

## Features

- **Reflection-based commands** — any public method on your shell subclass becomes a command automatically.
- **Shorthand syntax** — write `cmd arg` instead of `cmd(arg)`, and `x = 5` instead of `var('x', 5)`; the framework rewrites shell-like input into SpEL for you.
- **Fuzzy name matching** — abbreviate command and variable names (e.g. `c1` for `command1`) and the shell resolves them.
- **Auto-generated help** — `help` lists every available command with its signature.
- **History & scripting** — expressions can be logged to a history file, replayed, or run in bulk from a script file.
- **Sub-shell menus** — commands can launch nested shells, making it easy to build menu-driven CLIs.
- **Sandboxed filesystem helpers** — `FileSystemAwareSpelShellImpl` adds `cd`, `pwd`, `ll`, `mkdir`, `read`/`write`, and fuzzy file search, all constrained to a root directory.

## A minimal example

```java
import org.devtoolsgroup.simplespelshell.impl.BaseSpelShellImpl;

public class MyShell extends BaseSpelShellImpl {

    public static void main(String[] args) {
        new MyShell().runRepl();
    }

    public void sayHi() {
        String name = prompt("What is your name? ");
        print(format("Hi, %s!\n", name));
    }
}
```

Running it gives you an interactive prompt where `sayHi`, `help`, `var`, and any other exposed method can be invoked directly — and so can raw SpEL, like `1+2` or `{1,2,3}`.

## Requirements

- Java 25+
- Maven coordinates: `org.devtoolsgroup.simplespelshell:simple-spel-shell`

## License

[MIT](LICENSE.txt)
