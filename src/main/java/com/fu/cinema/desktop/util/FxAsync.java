package com.fu.cinema.desktop.util;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Node;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Component
public class FxAsync implements DisposableBean {
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final Set<Task<?>> pending = ConcurrentHashMap.newKeySet();

    public <T> Task<T> run(Callable<T> work, Consumer<T> onSuccess) {
        return run(work, onSuccess, AlertUtil::showError, null);
    }

    public <T> Task<T> run(Callable<T> work, Consumer<T> onSuccess, Node busy) {
        return run(work, onSuccess, AlertUtil::showError, busy);
    }

    /** Invoke on the FX thread. Snapshot control values before passing work. */
    public <T> Task<T> run(Callable<T> work, Consumer<T> onSuccess,
                           Consumer<Throwable> onError, Node busy) {
        if (!Platform.isFxApplicationThread()) {
            throw new IllegalStateException("FxAsync.run must be called on the FX thread");
        }
        if (busy != null && busy.disableProperty().isBound()) {
            throw new IllegalArgumentException("Pass busy=null for a bound control and bind the task running state instead");
        }
        Task<T> task = new Task<>() {
            @Override protected T call() throws Exception { return work.call(); }
        };
        boolean wasDisabled = busy != null && busy.isDisable();
        Runnable release = () -> {
            pending.remove(task);
            if (busy != null) busy.setDisable(wasDisabled);
        };
        task.setOnSucceeded(event -> { release.run(); onSuccess.accept(task.getValue()); });
        task.setOnFailed(event -> { release.run(); onError.accept(task.getException()); });
        task.setOnCancelled(event -> release.run());
        if (busy != null) busy.setDisable(true);
        pending.add(task);
        try {
            executor.execute(task);
        } catch (RuntimeException ex) {
            release.run();
            onError.accept(ex);
        }
        return task;
    }

    public Task<Void> runVoid(Runnable work, Runnable onSuccess, Node busy) {
        return run(() -> { work.run(); return null; }, ignored -> onSuccess.run(), AlertUtil::showError, busy);
    }

    @Override public void destroy() {
        pending.forEach(task -> task.cancel(true));
        executor.shutdownNow();
    }
}
