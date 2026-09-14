package org.devtoolsgroup.tutorial.example7;

import org.devtoolsgroup.simplespelshell.BaseSpelShell;
import org.devtoolsgroup.simplespelshell.ShellUtils;
import org.devtoolsgroup.simplespelshell.impl.BaseSpelShellImpl;

public class TaskDetailShell extends BaseSpelShellImpl {
    private final Task task;

    TaskDetailShell(BaseSpelShell parent, Task task) {
        super(parent);
        this.task = task;
        getReplConfig().setPrompt(_ ->
                "-------------------------------\n" +
                        "Task: " + task.title + (task.done ? " [done]" : "") + "\n" +
                        "[task] SpEL> "
        );
        setOnExit(ShellUtils.exnExit(false));
    }

    public void rename(String newTitle) {
        task.title = newTitle;
    }

    public void markDone() {
        task.done = true;
    }

    public void addNote(String note) {
        task.notes = note;
        var("lastEdited", task.title);
    }
}
