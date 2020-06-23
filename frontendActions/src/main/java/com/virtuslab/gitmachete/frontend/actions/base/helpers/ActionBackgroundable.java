package com.virtuslab.gitmachete.frontend.actions.base.helpers;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;

public abstract class ActionBackgroundable extends Task.Backgroundable{
    private boolean isCanceled;
    @Getter
    private boolean isRunning;
    @Getter
    private boolean isDone;
    private @Nullable ProgressIndicator progressIndicator;
    @Getter
    private final ActionFuture actionFuture;

    @SuppressWarnings("all")
    public ActionBackgroundable(@Nullable Project project, String title) {
        super(project, title);
        isCanceled = false;
        isRunning = false;
        isDone = false;
        actionFuture = new ActionFuture(this);
    }

    @Override
    public void run(ProgressIndicator indicator) {
        progressIndicator = indicator;
        if (!isCanceled()) {
            isRunning = true;
            doTheJob();
        }
    }

    @Override
    public void onCancel() {
        super.onCancel();
        isCanceled = true;
        actionFuture.onResult(ActionResult.Result.CANCELED, null);
    }

    @Override
    public void onSuccess() {
        super.onSuccess();
        actionFuture.onResult(ActionResult.Result.SUCCESS, null);
    }

    @Override
    public void onThrowable(@NotNull Throwable error) {
        super.onThrowable(error);
        actionFuture.onResult(ActionResult.Result.ERROR, error);
    }

    @Override
    public void onFinished() {
        super.onFinished();
        isRunning = false;
        isDone = true;
    }

    protected abstract void doTheJob();

    public boolean isCanceled() {
        if (isCanceled) {
            return true;
        }
        return progressIndicator != null && progressIndicator.isCanceled();
    }

    public boolean cancel() {
        if (progressIndicator == null) {
            return false;
        }
        progressIndicator.cancel();
        isCanceled = true;
        return true;
    }
}
