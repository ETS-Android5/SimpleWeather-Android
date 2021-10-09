package com.thewizrd.simpleweather.wearable

import android.widget.RemoteViews
import com.thewizrd.shared_resources.controls.WeatherNowViewModel
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.icons.WeatherIconsManager
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.ContextUtils.getThemeContextOverride
import com.thewizrd.shared_resources.utils.ImageUtils
import com.thewizrd.shared_resources.utils.getColorFromTempF
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.simpleweather.R

class CurrentWeatherGoogleTileProviderService : WeatherTileProviderService() {
    companion object {
        private const val TAG = "CurrentWeatherGoogleTileProviderService"
    }

    override val LOGTAG = TAG

    override suspend fun buildUpdate(weather: Weather): RemoteViews {
        val wim = WeatherIconsManager.getInstance()
        val updateViews = RemoteViews(packageName, R.layout.tile_layout_currentweather_google)
        val viewModel = WeatherNowViewModel(weather)
        val mDarkIconCtx = getThemeContextOverride(false)

        updateViews.setOnClickPendingIntent(R.id.tile, getTapIntent(applicationContext))

        updateViews.setImageViewBitmap(
            R.id.weather_icon,
            ImageUtils.bitmapFromDrawable(
                mDarkIconCtx,
                wim.getWeatherIconResource(viewModel.weatherIcon)
            )
        )

        updateViews.setTextViewText(
            R.id.condition_temp,
            viewModel.curTemp?.replace(viewModel.tempUnit ?: "", "") ?: WeatherIcons.PLACEHOLDER
        )
        updateViews.setTextColor(
            R.id.condition_temp,
            getColorFromTempF(weather.condition.tempF, Colors.WHITE)
        )
        updateViews.setTextViewText(R.id.condition_weather, viewModel.curCondition)
        updateViews.setTextViewText(R.id.location_name, viewModel.location)

        updateViews.setTextViewText(R.id.condition_hi, viewModel.hiTemp ?: WeatherIcons.PLACEHOLDER)
        updateViews.setTextViewText(R.id.condition_lo, viewModel.loTemp ?: WeatherIcons.PLACEHOLDER)


        return updateViews
    }
}