package com.virtuslab.gitmachete.frontend.actions.base.helpers;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ActionFuture implements Future<ActionResult> {
    private final @Nullable ActionBackgroundable actionBackgroundable;
    private @MonotonicNonNull ActionResult actionResult;
    private final Object lock = new Object();

    ActionFuture(ActionBackgroundable actionBackgroundable) {
        this.actionBackgroundable = actionBackgroundable;
    }

    private ActionFuture (ActionResult actionResult) {
        this.actionResult = actionResult;
        this.actionBackgroundable = null;
    }

    public static ActionFuture ofResult(ActionResult actionResult) {
        return new ActionFuture(actionResult);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (actionBackgroundable == null) {
            return false;
        }
        if (!mayInterruptIfRunning && actionBackgroundable.isRunning()) {
            return false;
        }
        return actionBackgroundable.cancel();
    }

    @Override
    public boolean isCancelled() {
        if (actionBackgroundable == null) {
            return false;
        }
        return actionBackgroundable.isCanceled();
    }

    @Override
    public boolean isDone() {
        if (actionBackgroundable == null) {
            return true;
        }
        return actionBackgroundable.isDone();
    }

    @Override
    @SuppressWarnings("all")
    public ActionResult get() throws InterruptedException, ExecutionException {
        if (isDone()) {
            return actionResult;
        }

        synchronized (this) {
            lock.wait();
        }

        return actionResult;
    }

    @Override
    @SuppressWarnings("all")
    public ActionResult get(long l, @NotNull TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
        if (isDone()) {
            return actionResult;
        }

        synchronized (this) {
            lock.wait(timeUnit.toMillis(l));
        }

        return actionResult;
    }

    void onResult(ActionResult.Result result, @Nullable Throwable exception) {
        actionResult = ActionResult.of(result, exception);
        lock.notifyAll();
    }
}
