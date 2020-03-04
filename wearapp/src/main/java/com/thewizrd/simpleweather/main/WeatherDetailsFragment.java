package com.thewizrd.simpleweather.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.databinding.Observable;
import androidx.wear.widget.WearableLinearLayoutManager;

import com.thewizrd.shared_resources.controls.WeatherNowViewModel;
import com.thewizrd.simpleweather.adapters.DetailItemAdapter;
import com.thewizrd.simpleweather.databinding.FragmentWeatherListBinding;
import com.thewizrd.simpleweather.fragments.SwipeDismissFragment;

public class WeatherDetailsFragment extends SwipeDismissFragment {
    private WeatherNowViewModel weatherView = null;

    private FragmentWeatherListBinding binding;
    private DetailItemAdapter mAdapter;

    public static WeatherDetailsFragment newInstance(WeatherNowViewModel weatherViewModel) {
        WeatherDetailsFragment fragment = new WeatherDetailsFragment();
        if (weatherViewModel != null) {
            fragment.weatherView = weatherViewModel;
        }

        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        // Use this to return your custom view for this Fragment
        View outerView = super.onCreateView(inflater, container, savedInstanceState);
        binding = FragmentWeatherListBinding.inflate(inflater, (ViewGroup) outerView, true);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        binding.recyclerView.setHasFixedSize(true);
        binding.recyclerView.setEdgeItemsCenteringEnabled(true);
        binding.recyclerView.setLayoutManager(new WearableLinearLayoutManager(mActivity));

        return outerView;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Don't resume if fragment is hidden
        if (!this.isHidden()) {
            initialize();
            if (weatherView != null) {
                weatherView.addOnPropertyChangedCallback(propertyChangedCallback);
            }
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);

        if (!hidden && this.isVisible()) {
            initialize();
        }
    }

    @Override
    public void onPause() {
        weatherView.removeOnPropertyChangedCallback(propertyChangedCallback);
        super.onPause();
    }

    private Observable.OnPropertyChangedCallback propertyChangedCallback = new Observable.OnPropertyChangedCallback() {
        @Override
        public void onPropertyChanged(Observable sender, int propertyId) {
            if (mActivity != null) {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        initialize();
                    }
                });
            }
        }
    };

    public void initialize() {
        if (weatherView != null && mActivity != null) {
            if (getView() != null)
                getView().setBackgroundColor(weatherView.getPendingBackground());

            // specify an adapter (see also next example)
            if (mAdapter == null) {
                mAdapter = new DetailItemAdapter(weatherView);
                binding.recyclerView.setAdapter(mAdapter);
            } else {
                mAdapter.updateItems(weatherView);
            }
        }
    }
}
