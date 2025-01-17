package com.thewizrd.simpleweather.widgets.remoteviews

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.thewizrd.shared_resources.controls.WeatherNowViewModel
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.icons.WeatherIconsManager
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.StringUtils.removeNonDigitChars
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.widgets.WeatherWidgetProvider2x2MaterialYou
import com.thewizrd.simpleweather.widgets.WidgetProviderInfo
import com.thewizrd.simpleweather.widgets.WidgetUtils

class WeatherWidget2x2MaterialYouCreator(context: Context) : WidgetRemoteViewCreator(context) {
    private fun generateRemoteViews() =
        RemoteViews(context.packageName, R.layout.app_widget_2x2_materialu)

    override val info: WidgetProviderInfo
        get() = WeatherWidgetProvider2x2MaterialYou.Info.getInstance()

    override suspend fun buildUpdate(
        appWidgetId: Int,
        weather: WeatherNowViewModel,
        location: LocationData,
        newOptions: Bundle
    ): RemoteViews {
        return buildLayout(appWidgetId, weather, location, newOptions)
    }

    private suspend fun buildLayout(
        appWidgetId: Int,
        weather: WeatherNowViewModel, location: LocationData,
        newOptions: Bundle
    ): RemoteViews {
        val updateViews = generateRemoteViews()

        // WeatherIcon
        val wim = WeatherIconsManager.getInstance()
        val weatherIconResId = wim.getWeatherIconResource(weather.weatherIcon)

        updateViews.setImageViewResource(R.id.weather_icon, weatherIconResId)
        if (!wim.isFontIcon) {
            // Remove tint
            updateViews.setInt(R.id.weather_icon, "setColorFilter", 0x0)
        }

        updateViews.setTextViewText(
            R.id.condition_hi,
            if (weather.hiTemp?.isNotBlank() == true) weather.hiTemp else WeatherIcons.PLACEHOLDER
        )
        updateViews.setTextViewText(
            R.id.condition_lo,
            if (weather.loTemp?.isNotBlank() == true) weather.loTemp else WeatherIcons.PLACEHOLDER
        )

        val temp = weather.curTemp?.removeNonDigitChars()
        updateViews.setTextViewText(
            R.id.condition_temp,
            if (temp.isNullOrBlank()) {
                WeatherIcons.PLACEHOLDER
            } else {
                "$temp°"
            }
        )

        // Location Name
        updateViews.setTextViewText(R.id.location_name, weather.location)

        updateViews.setViewVisibility(
            R.id.location_name,
            if (WidgetUtils.isLocationNameHidden(appWidgetId)) View.GONE else View.VISIBLE
        )
        updateViews.setViewVisibility(
            R.id.settings_button,
            if (WidgetUtils.isSettingsButtonHidden(appWidgetId)) View.GONE else View.VISIBLE
        )

        setOnClickIntent(location, updateViews)
        setOnSettingsClickIntent(updateViews, location, appWidgetId)

        return updateViews
    }

    override fun resizeWidget(
        info: WidgetProviderInfo,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        // no-op
    }
}