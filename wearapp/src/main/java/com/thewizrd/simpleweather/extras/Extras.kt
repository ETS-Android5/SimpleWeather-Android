@file:JvmMultifileClass
@file:JvmName("ExtrasKt")

package com.thewizrd.simpleweather.extras

import android.content.Intent
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.preferences.SettingsActivity
import com.thewizrd.simpleweather.wearable.WearableListenerActivity

fun isIconPackSupported(packKey: String?): Boolean {
    return com.thewizrd.extras.isIconPackSupported(packKey)
}

fun isWeatherAPISupported(api: String?): Boolean {
    return com.thewizrd.extras.isWeatherAPISupported(api)
}

fun SettingsActivity.SettingsFragment.navigateToPremiumFragment() {
    // Navigate to premium page
    showToast(R.string.message_premium_required, Toast.LENGTH_SHORT);
    LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(
        Intent(WearableListenerActivity.ACTION_OPENONPHONE)
            .putExtra(WearableListenerActivity.EXTRA_SHOWANIMATION, true)
    )
    return
}

fun SettingsActivity.IconsFragment.navigateUnsupportedIconPack() {
    // Navigate to premium page
    showToast(R.string.message_premium_required, Toast.LENGTH_SHORT);
    LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(
        Intent(WearableListenerActivity.ACTION_OPENONPHONE)
            .putExtra(WearableListenerActivity.EXTRA_SHOWANIMATION, true)
    )
    return
}