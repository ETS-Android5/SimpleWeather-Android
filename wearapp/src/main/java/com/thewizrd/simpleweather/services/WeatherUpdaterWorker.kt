package com.thewizrd.simpleweather.services

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.core.location.LocationManagerCompat
import androidx.work.*
import com.google.android.gms.location.*
import com.thewizrd.shared_resources.helpers.locationPermissionEnabled
import com.thewizrd.shared_resources.location.LocationProvider
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.remoteconfig.RemoteConfig
import com.thewizrd.shared_resources.tzdb.TZDBCache
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.utils.LiveDataUtils.awaitWithTimeout
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.wearable.WearableDataSync
import com.thewizrd.shared_resources.weatherdata.WeatherDataLoader
import com.thewizrd.shared_resources.weatherdata.WeatherManager
import com.thewizrd.shared_resources.weatherdata.WeatherRequest
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.simpleweather.App
import com.thewizrd.simpleweather.services.ServiceNotificationHelper.JOB_ID
import com.thewizrd.simpleweather.services.ServiceNotificationHelper.getForegroundNotification
import com.thewizrd.simpleweather.services.ServiceNotificationHelper.initChannel
import com.thewizrd.simpleweather.wearable.WearableWorker
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.concurrent.*
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.math.absoluteValue

class WeatherUpdaterWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    companion object {
        private const val TAG = "WeatherUpdaterWorker"

        const val ACTION_UPDATEWEATHER = "SimpleWeather.Droid.Wear.action.UPDATE_WEATHER"
        const val ACTION_ENQUEUEWORK = "SimpleWeather.Droid.Wear.action.START_ALARM"
        const val ACTION_CANCELWORK = "SimpleWeather.Droid.Wear.action.CANCEL_ALARM"
        const val ACTION_REQUEUEWORK = "SimpleWeather.Droid.Wear.action.UPDATE_ALARM"

        @JvmStatic
        @JvmOverloads
        fun enqueueAction(context: Context, intentAction: String, onBoot: Boolean = false) {
            when (intentAction) {
                ACTION_REQUEUEWORK -> enqueueWork(context.applicationContext)
                ACTION_ENQUEUEWORK ->
                    GlobalScope.launch(Dispatchers.Default) {
                        if (onBoot || !isWorkScheduled(context.applicationContext)) {
                            startWork(context.applicationContext)
                        }
                    }
                ACTION_UPDATEWEATHER ->
                    // For immediate action
                    startWork(context.applicationContext)
                ACTION_CANCELWORK -> cancelWork(context.applicationContext)
            }
        }

        private fun startWork(context: Context) {
            Logger.writeLine(Log.INFO, "%s: Requesting to start work", TAG)

            val updateRequest = OneTimeWorkRequest.Builder(WeatherUpdaterWorker::class.java).apply {
                setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            }.build()

            WorkManager.getInstance(context.applicationContext).enqueue(updateRequest)

            Logger.writeLine(Log.INFO, "%s: One-time work enqueued", TAG)

            // Enqueue periodic task as well
            enqueueWork(context.applicationContext)
        }

        private fun enqueueWork(context: Context) {
            Logger.writeLine(Log.INFO, "%s: Requesting work", TAG)

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresCharging(false)
                .build()

            val updateRequest = PeriodicWorkRequest.Builder(
                WeatherUpdaterWorker::class.java,
                SettingsManager.DEFAULT_INTERVAL.toLong(),
                TimeUnit.MINUTES,
                5,
                TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .addTag(TAG)
                .build()

            WorkManager.getInstance(context.applicationContext)
                .enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.REPLACE, updateRequest)

            Logger.writeLine(Log.INFO, "%s: Work enqueued", TAG)
        }

        private suspend fun isWorkScheduled(context: Context): Boolean {
            val workMgr = WorkManager.getInstance(context.applicationContext)
            val statuses = workMgr.getWorkInfosForUniqueWorkLiveData(TAG).awaitWithTimeout(10000)
            if (statuses.isNullOrEmpty()) return false
            var running = false
            for (workStatus in statuses) {
                running = (workStatus.state == WorkInfo.State.RUNNING
                        || workStatus.state == WorkInfo.State.ENQUEUED)
            }
            return running
        }

        private fun cancelWork(context: Context): Boolean {
            // Cancel alarm if dependent features are turned off
            WorkManager.getInstance(context.applicationContext).cancelUniqueWork(TAG)
            Logger.writeLine(Log.INFO, "%s: Canceled work", TAG)
            return true
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        initChannel(applicationContext)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                JOB_ID, getForegroundNotification(applicationContext),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            ForegroundInfo(JOB_ID, getForegroundNotification(applicationContext))
        }
    }

    override suspend fun doWork(): Result {
        Logger.writeLine(Log.INFO, "%s: Work started", TAG)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            runCatching {
                setForeground(getForegroundInfo())
            }
        }

        if (!WeatherUpdaterHelper.executeWork(applicationContext))
            return Result.failure()

        return Result.success()
    }

    private object WeatherUpdaterHelper {
        suspend fun executeWork(context: Context): Boolean {
            val wm = WeatherManager.instance
            val settingsManager = App.instance.settingsManager
            var locationChanged = false

            if (settingsManager.getDataSync() == WearableDataSync.OFF) {
                runCatching {
                    // Update configuration
                    RemoteConfig.checkConfigAsync()
                }
            }

            if (settingsManager.isWeatherLoaded()) {
                if (settingsManager.getDataSync() == WearableDataSync.OFF && settingsManager.useFollowGPS()) {
                    try {
                        locationChanged = updateLocation()
                        Timber.tag(TAG).i("locationChanged = $locationChanged...")
                    } catch (e: CancellationException) {
                        // ignore
                    } catch (e: Exception) {
                        Logger.writeLine(Log.ERROR, e, "Error updating location")
                    }
                }

                // Update for home
                val weather = getWeather()

                if (weather != null) {
                    WidgetUpdaterWorker.requestWidgetUpdate(context)
                } else {
                    if (settingsManager.getDataSync() != WearableDataSync.OFF) {
                        // Check if data has been updated
                        WearableWorker.enqueueAction(context, WearableWorker.ACTION_REQUESTWEATHERUPDATE)
                    }
                    Timber.tag(TAG).i("Work failed...")
                    return false
                }
            }

            Timber.tag(TAG).i("Work completed successfully...")
            return true
        }

        private suspend fun getWeather(): Weather? = withContext(Dispatchers.IO) {
            val settingsManager = App.instance.settingsManager

            Timber.tag(TAG).d("Getting weather data...")

            val weather = try {
                val locData = settingsManager.getHomeData() ?: return@withContext null
                val wloader = WeatherDataLoader(locData)
                val request = WeatherRequest.Builder()
                if (settingsManager.getDataSync() == WearableDataSync.OFF) {
                    request.forceRefresh(false).loadAlerts()
                } else {
                    request.forceLoadSavedData()
                }
                wloader.loadWeatherData(request.build())
            } catch (ex: Exception) {
                Logger.writeLine(Log.ERROR, ex, "%s: getWeather error", TAG)
                null
            }
            weather
        }

        @SuppressLint("MissingPermission")
        private suspend fun updateLocation(): Boolean = withContext(Dispatchers.Default) {
            val context = App.instance.appContext
            val wm = WeatherManager.instance
            val settingsManager = App.instance.settingsManager
            val locationProvider = LocationProvider(context)

            if (settingsManager.useFollowGPS()) {
                if (!context.locationPermissionEnabled()) {
                    return@withContext false
                }

                val locMan = context.getSystemService(LocationManager::class.java)
                if (locMan == null || !LocationManagerCompat.isLocationEnabled(locMan)) {
                    return@withContext false
                }

                var location: Location? = try {
                    withTimeoutOrNull(10 * 1000) {
                        locationProvider.getLastLocation()
                    }
                } catch (e: Exception) {
                    Logger.writeLine(Log.ERROR, e)
                    null
                }

                if (location == null) {
                    if (!isActive) return@withContext false

                    val handlerThread = HandlerThread("location")
                    handlerThread.start()
                    // Handler for timeout callback
                    val handler = Handler(handlerThread.looper)

                    Timber.tag(TAG).i("Requesting location updates...")

                    location = suspendCancellableCoroutine { continuation ->
                        val locationCallback = object : LocationProvider.Callback {
                            override fun onLocationChanged(location: Location?) {
                                handler.removeCallbacksAndMessages(null)
                                locationProvider.stopLocationUpdates()

                                Timber.tag(TAG).i("Location update received...")
                                if (continuation.isActive) {
                                    continuation.resume(location)
                                }
                                handlerThread.quitSafely()
                            }

                            override fun onRequestTimedOut() {
                                Timber.tag(TAG).i("Location update timed out...")
                                continuation.cancel()
                            }
                        }

                        continuation.invokeOnCancellation {
                            locationProvider.stopLocationUpdates()
                            handler.removeCallbacksAndMessages(null)
                            handlerThread.quitSafely()
                        }

                        // Timeout after 60s
                        locationProvider.requestSingleUpdate(
                            locationCallback,
                            handlerThread.looper,
                            60000
                        )
                    }
                }

                if (location != null) {
                    val lastGPSLocData = settingsManager.getLastGPSLocData()

                    // Check previous location difference
                    if (lastGPSLocData?.query != null && ConversionMethods.calculateHaversine(
                                    lastGPSLocData.latitude, lastGPSLocData.longitude, location.latitude, location.longitude).absoluteValue < 1600) {
                        return@withContext false
                    }

                    val query_vm = try {
                        withContext(Dispatchers.IO) {
                            wm.getLocation(location)
                        }
                    } catch (e: WeatherException) {
                        Logger.writeLine(Log.ERROR, e)
                        return@withContext false
                    }

                    if (query_vm == null || query_vm.locationQuery.isNullOrBlank()) {
                        // Stop since there is no valid query
                        return@withContext false
                    } else if (query_vm.locationTZLong.isNullOrBlank() && query_vm.locationLat != 0.0 && query_vm.locationLong != 0.0) {
                        val tzId =
                            TZDBCache.getTimeZone(query_vm.locationLat, query_vm.locationLong)

                        if ("unknown" != tzId) {
                            query_vm.locationTZLong = tzId
                        }
                    }

                    // Save location as last known
                    settingsManager.saveLastGPSLocData(LocationData(query_vm, location))
                    return@withContext true
                }
            }
            false
        }
    }
}