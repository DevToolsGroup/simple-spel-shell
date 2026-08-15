package org.devtoolsgroup.simplespelshell;

public class SpelShellException extends RuntimeException {
    public SpelShellException(String message) {
        super(message);
    }

    public SpelShellException(Throwable cause) {
        super(cause);
    }

    public SpelShellException(String message, Throwable cause) {
        super(message, cause);
    }
}
