package com.thewizrd.simpleweather.preferences

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.IntDef
import androidx.annotation.StringRes
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceRecyclerViewAccessibilityDelegate
import androidx.preference.PreferenceScreen
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.wear.widget.SwipeDismissFrameLayout
import androidx.wear.widget.WearableLinearLayoutManager
import com.thewizrd.shared_resources.helpers.SpacerItemDecoration
import com.thewizrd.shared_resources.utils.ContextUtils.dpToPx
import com.thewizrd.shared_resources.utils.SettingsManager
import com.thewizrd.simpleweather.App
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.adapters.PreferenceListHeaderAdapter
import com.thewizrd.simpleweather.adapters.SpacerAdapter
import com.thewizrd.simpleweather.databinding.ActivitySettingsBinding
import com.thewizrd.simpleweather.helpers.CustomScrollingLayoutCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

abstract class SwipeDismissPreferenceFragment : PreferenceFragmentCompat() {
    companion object {
        private const val DIALOG_FRAGMENT_TAG =
            "com.thewizrd.simpleweather.preferences.SwipeDismissPreferenceFragment.DIALOG"
    }

    @IntDef(Toast.LENGTH_SHORT, Toast.LENGTH_LONG)
    @Retention(AnnotationRetention.SOURCE)
    annotation class ToastDuration

    var parentActivity: Activity? = null
        private set
    protected val settingsManager: SettingsManager = App.instance.settingsManager

    private lateinit var binding: ActivitySettingsBinding
    private var swipeCallback: SwipeDismissFrameLayout.Callback? = null

    @StringRes
    protected abstract fun getTitle(): Int

    override fun onAttach(context: Context) {
        super.onAttach(context)
        parentActivity = context as Activity
    }

    override fun onDetach() {
        parentActivity = null
        super.onDetach()
    }

    override fun onDestroy() {
        parentActivity = null
        super.onDestroy()
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ActivitySettingsBinding.inflate(inflater, container, false)
        val inflatedView = super.onCreateView(inflater, container, savedInstanceState)

        binding.swipeLayout.addView(inflatedView)
        binding.swipeLayout.isSwipeable = true
        swipeCallback = object : SwipeDismissFrameLayout.Callback() {
            override fun onDismissed(layout: SwipeDismissFrameLayout) {
                layout.visibility = View.GONE
                parentActivity?.onBackPressed()
            }
        }
        binding.swipeLayout.addCallback(swipeCallback)
        binding.swipeLayout.requestFocus()

        return binding.swipeLayout
    }

    override fun onDestroyView() {
        binding.swipeLayout.removeCallback(swipeCallback)
        super.onDestroyView()
    }

    fun showToast(@StringRes resId: Int, @ToastDuration duration: Int) {
        lifecycleScope.launch {
            if (parentActivity != null && isVisible) {
                Toast.makeText(parentActivity, resId, duration).show()
            }
        }
    }

    fun showToast(message: CharSequence?, @ToastDuration duration: Int) {
        lifecycleScope.launch {
            if (parentActivity != null && isVisible) {
                Toast.makeText(parentActivity, message, duration).show()
            }
        }
    }

    /**
     * Launches and runs the given runnable if the fragment is at least initialized
     * The action will be signalled to cancel if the fragment goes into the destroyed state
     * Note: This will run on the UI thread
     *
     * @param context additional to [CoroutineScope.coroutineContext] context of the coroutine.
     * @param block the coroutine code which will be invoked in the context of the viewLifeCycleOwner lifecycle scope.
     */
    fun runWithView(context: CoroutineContext = EmptyCoroutineContext,
                    block: suspend CoroutineScope.() -> Unit
    ) {
        runCatching {
            viewLifecycleOwner.lifecycleScope
        }.onFailure {
            // no-op
        }.onSuccess {
            it.launch(context = context, block = block)
        }
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateRecyclerView(
        inflater: LayoutInflater,
        parent: ViewGroup,
        savedInstanceState: Bundle?
    ): RecyclerView {
        val recyclerView =
            inflater.inflate(R.layout.preference_recyclerview, parent, false) as RecyclerView

        recyclerView.setHasFixedSize(true)
        recyclerView.addItemDecoration(
            SpacerItemDecoration(
                recyclerView.context.dpToPx(16f).toInt(),
                recyclerView.context.dpToPx(4f).toInt()
            )
        )
        recyclerView.layoutManager = onCreateLayoutManager()
        recyclerView.setAccessibilityDelegateCompat(
            PreferenceRecyclerViewAccessibilityDelegate(
                recyclerView
            )
        )

        return recyclerView
    }

    override fun onCreateLayoutManager(): RecyclerView.LayoutManager {
        return WearableLinearLayoutManager(context, CustomScrollingLayoutCallback())
    }

    override fun onCreateAdapter(preferenceScreen: PreferenceScreen): RecyclerView.Adapter<*> {
        return ConcatAdapter(
            PreferenceListHeaderAdapter(requireContext().getString(getTitle())),
            super.onCreateAdapter(preferenceScreen),
            SpacerAdapter(requireContext().dpToPx(48f).toInt())
        )
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        // check if dialog is already showing
        if (parentFragmentManager.findFragmentByTag(DIALOG_FRAGMENT_TAG) != null) {
            return
        }

        if (preference is WearEditTextPreference) {
            val f = WearEditTextPreferenceDialogFragment.newInstance(preference.key)
            f.setTargetFragment(this, 0)
            f.show(parentFragmentManager, DIALOG_FRAGMENT_TAG)
        } else if (preference is WearListPreference) {
            val f = WearListPreferenceDialogFragment.newInstance(preference.key)
            f.setTargetFragment(this, 0)
            f.show(parentFragmentManager, DIALOG_FRAGMENT_TAG)
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }
}