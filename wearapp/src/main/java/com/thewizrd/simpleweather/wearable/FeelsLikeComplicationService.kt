package com.thewizrd.simpleweather.wearable

import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.*
import com.thewizrd.shared_resources.utils.ContextUtils.getThemeContextOverride
import com.thewizrd.shared_resources.utils.Units
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecast
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.simpleweather.R
import kotlin.math.roundToInt

class FeelsLikeComplicationService : WeatherHourlyForecastComplicationService() {
    companion object {
        private const val TAG = "WindComplicationService"
    }

    override val supportedComplicationTypes: Set<ComplicationType> =
        setOf(ComplicationType.SHORT_TEXT, ComplicationType.LONG_TEXT)
    private val complicationIconResId = R.drawable.wi_thermometer

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (!supportedComplicationTypes.contains(type)) {
            return NoDataComplicationData()
        }

        return when (type) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    PlainComplicationText.Builder("75°").build(),
                    PlainComplicationText.Builder("Feels like: 75°").build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(
                        Icon.createWithResource(
                            getThemeContextOverride(false),
                            complicationIconResId
                        )
                    ).build()
                ).setTitle(
                    PlainComplicationText.Builder(getString(R.string.label_feelslike)).build()
                ).build()
            }
            ComplicationType.LONG_TEXT -> {
                LongTextComplicationData.Builder(
                    PlainComplicationText.Builder(getString(R.string.label_feelslike)).build(),
                    PlainComplicationText.Builder("Feels like: 75°").build()
                ).setTitle(
                    PlainComplicationText.Builder("75°").build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(
                        Icon.createWithResource(
                            getThemeContextOverride(false),
                            complicationIconResId
                        )
                    ).build()
                ).build()
            }
            else -> {
                null
            }
        }
    }

    override fun buildUpdate(
        dataType: ComplicationType,
        weather: Weather?,
        hourlyForecast: HourlyForecast?
    ): ComplicationData? {
        if (weather == null || !weather.isValid || !supportedComplicationTypes.contains(dataType)) {
            return null
        }

        val feelsLikeF =
            weather.condition?.feelslikeF ?: hourlyForecast?.extras?.feelslikeF ?: return null
        val feelsLikeC =
            weather.condition?.feelslikeC ?: hourlyForecast?.extras?.feelslikeC ?: return null

        if (feelsLikeF == feelsLikeC) return null

        val tempUnit = settingsMgr.getTemperatureUnit()
        val tempVal =
            if (tempUnit == Units.FAHRENHEIT) feelsLikeF.roundToInt() else feelsLikeC.toInt()
        val tempStr = String.format("$tempVal°$tempUnit")

        return when (dataType) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    PlainComplicationText.Builder(tempStr).build(),
                    PlainComplicationText.Builder(
                        String.format("%s: %s", getString(R.string.label_feelslike), tempStr)
                    ).build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(
                        Icon.createWithResource(
                            getThemeContextOverride(false),
                            complicationIconResId
                        )
                    ).build()
                ).setTitle(
                    PlainComplicationText.Builder(getString(R.string.label_feelslike)).build()
                ).build()
            }
            ComplicationType.LONG_TEXT -> {
                LongTextComplicationData.Builder(
                    PlainComplicationText.Builder(getString(R.string.label_feelslike)).build(),
                    PlainComplicationText.Builder(
                        String.format("%s: %s", getString(R.string.label_feelslike), tempStr)
                    ).build()
                ).setTitle(
                    PlainComplicationText.Builder(tempStr).build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(
                        Icon.createWithResource(
                            getThemeContextOverride(false),
                            complicationIconResId
                        )
                    ).build()
                ).build()
            }
            else -> {
                null
            }
        }
    }
}