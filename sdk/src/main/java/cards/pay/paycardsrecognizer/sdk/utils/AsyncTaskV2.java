package cards.pay.paycardsrecognizer.sdk.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.AnyThread;
import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AsyncTaskV2<Params, Progress, Result> {

    public enum Status {
        FINISHED,
        PENDING,
        RUNNING
    }

    // This handler will be used to communicate with main thread
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    private Result result;
    private Future<Result> resultFuture;
    private ExecutorService executor;
    private Status status = Status.PENDING;

    // Base class must implement this method
    protected abstract Result doInBackground(Params... params);

    // Methods with default implementation
    // Base class can optionally override these methods.
    protected void onPreExecute() {}
    protected void onPostExecute(Result result) {}
    @SafeVarargs
    protected final void onProgressUpdate(Progress... progress) {}
    protected void onCancelled() {}
    protected void onCancelled(Result result) {
        onCancelled();
    }

    @SafeVarargs
    @MainThread
    public final void execute(@Nullable Params... params) {
        status = Status.RUNNING;
        onPreExecute();
        try {
            executor = Executors.newSingleThreadExecutor();
            Callable<Result> backgroundCallableTask = () ->  doInBackground(params);
            // Execute the background task
            resultFuture = executor.submit(backgroundCallableTask);

            // On the worker thread - wait for the background task to complete
            executor.execute(getResult());
        } finally {
            if (executor != null) {
                executor.shutdown();
            }
        }
    }

    private Runnable getResult() {
        return () -> {
            try {
                if (!isCancelled()) {
                    // This will block the worker thread, till the result is available
                    // Since our executor is a single thread, we will have results by now
                    result = resultFuture.get();

                    // Post the result to main thread
                    mainThreadHandler.post(() -> onPostExecute(result));
                } else {
                    // User cancelled the operation, ignore the result
                    mainThreadHandler.post(this::onCancelled);
                }
                status = Status.FINISHED;
            } catch (InterruptedException | ExecutionException e) {
                Log.e("AsyncTask2", "Exception while trying to get result ", e);
            }
        };
    }

    @SafeVarargs
    @WorkerThread
    public final void publishProgress(Progress... progress) {
        if (!isCancelled()) {
            mainThreadHandler.post(() -> onProgressUpdate(progress));
        }
    }

    @MainThread
    public final void cancel(boolean mayInterruptIfRunning) {
        cancelled.set(true);
        if (resultFuture!= null) {
            resultFuture.cancel(mayInterruptIfRunning);
        }
    }

    @AnyThread
    public final boolean isCancelled() {
        return cancelled.get();
    }

    @AnyThread
    public final Status getStatus() {
        return status;
    }

}