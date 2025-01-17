package com.thewizrd.simpleweather.widgets

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.core.graphics.ColorUtils
import androidx.navigation.fragment.NavHostFragment
import com.thewizrd.shared_resources.remoteconfig.RemoteConfig
import com.thewizrd.shared_resources.utils.ActivityUtils.setFullScreen
import com.thewizrd.shared_resources.utils.ActivityUtils.setTransparentWindow
import com.thewizrd.shared_resources.utils.AnalyticsLogger
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.ContextUtils.getAttrColor
import com.thewizrd.shared_resources.utils.UserThemeMode
import com.thewizrd.simpleweather.App
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.locale.UserLocaleActivity
import com.thewizrd.simpleweather.widgets.preferences.WeatherWidget4x3LocationFragment

class WeatherWidgetConfigActivity : UserLocaleActivity() {
    private var mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AnalyticsLogger.logEvent("WidgetConfig: onCreate")

        // Find the widget id from the intent.
        if (intent?.extras != null) {
            mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        }

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if they press the back button.
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId))

        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            // If they gave us an intent without the widget id, just bail.
            finish()
        }

        setContentView(R.layout.activity_widget_setup)

        var color = getAttrColor(android.R.attr.colorBackground)
        if (App.instance.settingsManager.getUserThemeMode() == UserThemeMode.AMOLED_DARK) {
            color = Colors.BLACK
        }

        window.setTransparentWindow(
            color,
            Colors.TRANSPARENT,
            ColorUtils.setAlphaComponent(color, 0xB3)
        )
        window.setFullScreen(true)

        val mWidgetType = WidgetUtils.getWidgetTypeFromID(mAppWidgetId)

        if (mWidgetType == WidgetType.Widget4x3Locations) {
            val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
            if (fragment == null) {
                supportFragmentManager.beginTransaction()
                    .replace(
                        R.id.fragment_container,
                        WeatherWidget4x3LocationFragment.newInstance(mAppWidgetId)
                    )
                    .commit()
            }
        } else {
            val args = Bundle().apply {
                putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId)
            }

            if (intent?.extras != null) {
                args.putAll(intent.extras)
            }

            if (intent?.extras?.containsKey(WeatherWidgetProvider.EXTRA_LOCATIONQUERY) == false && WidgetUtils.exists(
                    mAppWidgetId
                )
            ) {
                WidgetUtils.getLocationData(mAppWidgetId)?.let {
                    args.putString(WeatherWidgetProvider.EXTRA_LOCATIONNAME, it.name)
                    args.putString(WeatherWidgetProvider.EXTRA_LOCATIONQUERY, it.query)
                }
            }

            val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

            if (fragment == null) {
                val hostFragment = NavHostFragment.create(R.navigation.widget_graph, args)
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, hostFragment)
                    .setPrimaryNavigationFragment(hostFragment)
                    .commit()
            }
        }

        // Update configuration
        RemoteConfig.checkConfig()
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        mAppWidgetId = savedInstanceState.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId)
        super.onSaveInstanceState(outState)
    }
}