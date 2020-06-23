package com.virtuslab.gitmachete.frontend.actions.base.helpers;

import io.vavr.control.Option;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;

@Data
@RequiredArgsConstructor(staticName = "of")
public class ActionResult {
    public enum Result {
        SUCCESS,
        ERROR,
        CANCELED
    }

    private final Result result;
    private final @Nullable Throwable exception;

    public static ActionResult of(Result result) {
        return new ActionResult(result, null);
    }

    public Option<Throwable> getException() {
        return Option.of(exception);
    }
}
