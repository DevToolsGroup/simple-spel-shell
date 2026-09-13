package org.devtoolsgroup.tutorial.example12;

import org.devtoolsgroup.simplespelshell.BasicOperatorOverloader;
import org.devtoolsgroup.simplespelshell.NamePattern;
import org.springframework.expression.Operation;

public class TaskDueDateOperatorOverloader extends BasicOperatorOverloader {
    private final TaskShell shell;

    public TaskDueDateOperatorOverloader(TaskShell shell) {
        this.shell = shell;
    }

    private boolean isDueDateShift(Operation operation, Object left, Object right) {
        return (operation == Operation.SUBTRACT || operation == Operation.ADD)
            && left instanceof NamePattern && right instanceof Number;
    }

    @Override
    public boolean overridesOperation(Operation operation, Object left, Object right) {
        return isDueDateShift(operation, left, right) || super.overridesOperation(operation, left, right);
    }

    @Override
    public Object operate(Operation operation, Object left, Object right) {
        if (isDueDateShift(operation, left, right)) {
            long days = ((Number) right).longValue();
            return shell.getDueDate((NamePattern) left).plusDays(operation == Operation.SUBTRACT ? -days : days);
        }
        return super.operate(operation, left, right);
    }
}
