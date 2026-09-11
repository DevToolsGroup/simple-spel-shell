# 10. Error Handling: ShellException vs ShellExitException

`completeTask` currently prints "No such task" and moves on when given a bad title. This page replaces that with a real exception, and explains the two exception types the framework treats specially, plus a subtlety in customizing which errors are fatal.

## `ShellException`: a recoverable error

```java
public void completeTask(String title) {
    Path taskFile = Path.of(title + ".task");
    if (!getFile(taskFile).exists()) {
        throw new ShellException(false, "No such task: " + title);
    }
    write(taskFile, "done");
}
```

`ShellException(boolean printStackTrace, String message)` is an unchecked exception the REPL loop already knows how to handle without your help. By default, throwing one doesn't end `runRepl()` — the loop's own `try`/`catch` prints the message and keeps going:

```
SpEL> completeTask 'Nonexistent'
No such task: Nonexistent
SpEL> lt
[ ] Buy milk
```

Passing `false` for `printStackTrace` is what keeps that output to a single clean line — the loop's catch block checks `ShellException.isPrintStackTrace()` and skips the stack trace when it's `false`. Any other exception type (a `NullPointerException` from a bug in your own code, for instance) gets its message *and* a full stack trace printed the same way, then the loop continues regardless — as long as it isn't the configured `stopOnException` type.

## `ShellExitException`: the one that stops the loop

`ReplConfig.stopOnException` names a single exception class; whenever a thrown exception is an instance of that class, `runRepl()` rethrows it instead of catching it, ending the loop. The defaults, set in `CoreSpelShellImpl`'s constructor, are:

- **Interactive `runRepl()`**: `ShellExitException.class` — which is exactly what the built-in `exit()` command throws (via `getOnExit()`), so typing `exit` naturally ends the loop, and nothing else does.
- **Scripts** (`runScript(...)`): plain `Exception.class` — any exception at all aborts a script, since there's no user at the prompt to read an error message and try again.

This is why `ShellException` from `completeTask` doesn't end the interactive session: it isn't a `ShellExitException`, so it doesn't match the default `stopOnException`.

## Customizing `stopOnException` — and its sharp edge

`ReplConfig.setStopOnException(Class<? extends Exception>)` lets you change what's fatal. The subtlety: it's a *single* class, checked with `isAssignableFrom` — setting it doesn't add to the default, it **replaces** it.

Suppose you tried this directly:

```java
class TaskNotFoundException extends RuntimeException {
    TaskNotFoundException(String title) {
        super("No such task: " + title);
    }
}
```

```java
getReplConfig().setStopOnException(TaskNotFoundException.class);
```

Now throwing `TaskNotFoundException` from `completeTask` does stop the loop — but so does calling `exit()`, in effect, *stop working*: `exit()` still throws `ShellExitException`, but that's no longer the configured `stopOnException`, so the loop's `catch (Exception ex)` block treats it like any other uncaught exception instead of rethrowing it — printing a full stack trace (since `ShellExitException` isn't a `ShellException`) and looping again, leaving you stuck at the prompt with `exit` seemingly not working.

The clean way to make a domain error end the session *without* breaking `exit()` is to let it flow through the existing mechanism rather than replacing it — by making the error itself a kind of `ShellExitException`:

```java
class FatalTaskStoreException extends ShellExitException {
    FatalTaskStoreException() {
        super(true);
    }
}
```

Since `FatalTaskStoreException` *is* a `ShellExitException`, it already matches the default `stopOnException` — no `setStopOnException` call needed — and `TaskShell`'s own `runRepl()` override from [page 8](08-submenus.md) sees `ex.getResult()` as `true`, so it exits the whole program cleanly, exactly the way typing `exit` at the top-level menu does. Reach for `setStopOnException` when you genuinely want to change what's fatal *instead of* the default (for example, in a shell that doesn't rely on `exit()`'s default wiring at all); reach for a `ShellExitException` subclass when you want to add a new way to trigger the exit path that's already there.

---
Previous: [9. Sandboxed Filesystem Shells](09-filesystem-shells.md) · Next: [11. Custom Type Converters](11-custom-type-converters.md)
