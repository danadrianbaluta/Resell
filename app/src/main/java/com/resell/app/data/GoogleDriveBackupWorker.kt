package com.resell.app.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class GoogleDriveBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repository = ProductRepository(applicationContext)
        return if (repository.exportBackupToDriveFolder()) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}
