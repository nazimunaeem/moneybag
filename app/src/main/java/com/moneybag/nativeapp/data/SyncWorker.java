package com.moneybag.nativeapp.data;

import android.app.Application;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.util.concurrent.CountDownLatch;

public class SyncWorker extends Worker {

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        MoneyBagRepository repository = new MoneyBagRepository((Application) getApplicationContext());
        SyncManager syncManager = new SyncManager(getApplicationContext(), repository);
        
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] success = {false};

        syncManager.sync(new SyncManager.SyncCallback() {
            @Override
            public void onSuccess() {
                success[0] = true;
                latch.countDown();
            }

            @Override
            public void onError(String message) {
                success[0] = false;
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            return Result.retry();
        }

        return success[0] ? Result.success() : Result.retry();
    }
}
