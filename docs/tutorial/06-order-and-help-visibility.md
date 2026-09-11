# 6. Controlling Help and Shorthand Visibility with @Order

The `help` output on the last page mixed the framework's own commands in with ours, separated by a `---` divider. This page explains what drives that, and how to clean it up.

## Where the numbers come from

`help` sorts commands by an `@Order` value (`org.springframework.core.annotation.Order`), read via `ShellUtils.getSortOrder`:

```java
public static int getSortOrder(Method method) {
    Order order = method.getAnnotation(Order.class);
    return order == null ? 0 : order.value();
}
```

Unannotated methods — including every command you write, like `addTask` — default to order `0`. The framework annotates its own commands at two levels:

- **`@Order(-100)`** — "public utility" commands like `help`, `hist`, `var`, `print*`, `exit`. These show up in `help` by default and are shorthand-invocable.
- **`@Order(-1000)`** — internal plumbing accessors like `getConsole`, `setReplConfig`, `runRepl` itself. These are hidden from `help` under any realistic setting, **and** — this is a separate, fixed rule, not affected by anything in this page — methods ordered below `-100` are excluded from the shorthand rewrite lists entirely, so they're only reachable by their full name in raw SpEL (`shell.getConsole()`), never via `cmd` or `cmd arg`.

`help`'s divider line appears exactly where the sort order crosses from negative to non-negative — which is why, by default, you see all the `-100` built-ins, then `---`, then your own `0`-order commands.

## Hiding the built-ins

`setMinOrderForHelp(int)` changes the *display* threshold `help` uses (it has no effect on what's callable — only on what's listed). Set it to `0` and only your own commands remain visible:

```java
public TaskShell() {
    setMinOrderForHelp(0);
}
```

```
SpEL> help
addTask(title: String): void
addTaskInteractive(): void
completeTask(title: String): void
listTasks(): void
```

This is the same technique used by the framework's own `Example2` test fixture, with the same comment: *"show custom methods only in help."* The built-ins are all still fully callable (`hi` still resolves to `hist`, `he` still resolves to `help`) — they've just been curated out of the listing.

## Giving your own commands an order

`@Order` isn't reserved for the framework. Put it on your own methods to change where they sort relative to each other — lower values sort first:

```java
@Order(-10)
@Override
public void listTasks() {
    ...
}
```

With that in place, `listTasks` sorts before `addTask` and `completeTask` in `help`, since `-10 < 0`. One thing to watch for: if you accidentally give one of your own commands an order below `-100`, it silently drops out of shorthand eligibility too — `lt` would stop working and you'd need to type `listTasks()` in full. There's no warning when this happens, so it's worth remembering the `-100` boundary is a hard cutoff, not just a display convention.

---
Previous: [5. Built-in Commands: help, print, prompt, exit](05-built-in-commands.md) · Next: [7. History and Scripting](07-history-and-scripting.md)
