package com.thewizrd.simpleweather.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.simpleweather.App
import com.thewizrd.simpleweather.services.*
import com.thewizrd.simpleweather.services.UpdaterUtils.Companion.updateAlarm
import com.thewizrd.simpleweather.wearable.WearableWorker
import com.thewizrd.simpleweather.wearable.WearableWorkerActions
import com.thewizrd.simpleweather.widgets.WidgetUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.ZoneOffset
import java.time.ZonedDateTime

class CommonActionsBroadcastReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "CommonActionsBroadcastReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (CommonActions.ACTION_SETTINGS_UPDATEAPI == intent.action) {
            WearableWorker.enqueueAction(context, WearableWorkerActions.ACTION_SENDSETTINGSUPDATE)
            WeatherUpdaterWorker.enqueueAction(context, WeatherUpdaterWorker.ACTION_UPDATEWEATHER)
        } else if (CommonActions.ACTION_SETTINGS_UPDATEGPS == intent.action) {
            WearableWorker.enqueueAction(context, WearableWorkerActions.ACTION_SENDUPDATE)
            // Reset notification time for new location
            App.instance.settingsManager.setLastPoPChanceNotificationTime(ZonedDateTime.of(DateTimeUtils.getLocalDateTimeMIN(), ZoneOffset.UTC))
        } else if (CommonActions.ACTION_SETTINGS_UPDATEUNIT == intent.action) {
            WearableWorker.enqueueAction(context, WearableWorkerActions.ACTION_SENDSETTINGSUPDATE)
            WidgetUpdaterWorker.enqueueAction(context, WidgetUpdaterWorker.ACTION_UPDATEWIDGETS)
        } else if (CommonActions.ACTION_SETTINGS_UPDATEREFRESH == intent.action) {
            WearableWorker.enqueueAction(context, WearableWorkerActions.ACTION_SENDSETTINGSUPDATE)
            updateAlarm(context)
        } else if (CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE == intent.action) {
            WearableWorker.enqueueAction(context, WearableWorkerActions.ACTION_SENDLOCATIONUPDATE)
            if (intent.getBooleanExtra(CommonActions.EXTRA_FORCEUPDATE, true)) {
                WeatherUpdaterWorker.enqueueAction(context, WeatherUpdaterWorker.ACTION_UPDATEWEATHER)
            }
            // Reset notification time for new location
            App.instance.settingsManager.setLastPoPChanceNotificationTime(ZonedDateTime.of(DateTimeUtils.getLocalDateTimeMIN(), ZoneOffset.UTC))
        } else if (CommonActions.ACTION_WEATHER_UPDATEWIDGETLOCATION == intent.action) {
            val oldKey = intent.getStringExtra(Constants.WIDGETKEY_OLDKEY)
            val locationJson = intent.getStringExtra(Constants.WIDGETKEY_LOCATION)

            GlobalScope.launch(Dispatchers.Default) {
                val location = JSONParser.deserializer(locationJson, LocationData::class.java)

                if (WidgetUtils.exists(oldKey)) {
                    WidgetUtils.updateWidgetIds(oldKey, location)
                }
            }
        } else if (CommonActions.ACTION_WEATHER_SENDWEATHERUPDATE == intent.action) {
            WearableWorker.enqueueAction(context, WearableWorkerActions.ACTION_SENDWEATHERUPDATE)
        } else if (CommonActions.ACTION_SETTINGS_SENDUPDATE == intent.action) {
            WearableWorker.enqueueAction(context, WearableWorkerActions.ACTION_SENDSETTINGSUPDATE)
        } else if (CommonActions.ACTION_WIDGET_RESETWIDGETS == intent.action) {
            WidgetWorker.enqueueResetGPSWidgets(context)
        } else if (CommonActions.ACTION_WIDGET_REFRESHWIDGETS == intent.action) {
            WidgetWorker.enqueueRefreshGPSWidgets(context)
        } else if (CommonActions.ACTION_IMAGES_UPDATEWORKER == intent.action) {
            ImageDatabaseWorker.enqueueAction(
                context,
                ImageDatabaseWorkerActions.ACTION_UPDATEALARM
            )
        } else if (CommonActions.ACTION_SETTINGS_UPDATEDAILYNOTIFICATION == intent.action) {
            UpdaterUtils.enableDailyNotificationService(
                context,
                SettingsManager(context.applicationContext).isDailyNotificationEnabled()
            )
        }

        Logger.writeLine(Log.INFO, "%s: Intent Action = %s", TAG, intent.action)
    }
}