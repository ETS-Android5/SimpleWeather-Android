package com.thewizrd.shared_resources.controls;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.Transformations;

import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.SettingsManager;
import com.thewizrd.shared_resources.weatherdata.WeatherAlert;
import com.thewizrd.shared_resources.weatherdata.WeatherAlerts;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WeatherAlertsViewModel extends ObservableViewModel {
    private final SettingsManager settingsMgr;

    private LocationData locationData;

    private MutableLiveData<List<WeatherAlertViewModel>> alerts;

    private LiveData<List<WeatherAlertViewModel>> currentAlertsData;

    public WeatherAlertsViewModel() {
        settingsMgr = SimpleLibrary.getInstance().getApp().getSettingsManager();
        alerts = new MutableLiveData<>();
    }

    public LiveData<List<WeatherAlertViewModel>> getAlerts() {
        return alerts;
    }

    @MainThread
    public void updateAlerts(@NonNull LocationData location) {
        if (this.locationData == null || !ObjectsCompat.equals(this.locationData.getQuery(), location.getQuery())) {
            // Clone location data
            this.locationData = new LocationData(new LocationQueryViewModel(location));

            if (currentAlertsData != null) {
                currentAlertsData.removeObserver(alertObserver);
            }

            LiveData<WeatherAlerts> weatherAlertsLiveData = settingsMgr.getWeatherDAO().getLiveWeatherAlertData(location.getQuery());

            currentAlertsData = Transformations.map(weatherAlertsLiveData, weatherAlerts -> {
                List<WeatherAlertViewModel> alerts;

                if (weatherAlerts != null && weatherAlerts.getAlerts() != null && !weatherAlerts.getAlerts().isEmpty()) {
                    alerts = new ArrayList<>(weatherAlerts.getAlerts().size());
                    final ZonedDateTime now = ZonedDateTime.now();

                    for (WeatherAlert alert : weatherAlerts.getAlerts()) {
                        // Skip if alert has expired
                        if (!alert.getExpiresDate().isAfter(now) || alert.getDate().isAfter(now))
                            continue;

                        WeatherAlertViewModel alertView = new WeatherAlertViewModel(alert);
                        alerts.add(alertView);
                    }

                    return alerts;
                }

                return Collections.emptyList();
            });

            currentAlertsData.observeForever(alertObserver);

            if (alerts != null) {
                alerts.postValue(currentAlertsData.getValue());
            }
        }
    }

    private final Observer<List<WeatherAlertViewModel>> alertObserver = new Observer<List<WeatherAlertViewModel>>() {
        @Override
        public void onChanged(List<WeatherAlertViewModel> alertViewModels) {
            if (alerts != null) {
                alerts.postValue(alertViewModels);
            }
        }
    };

    @Override
    protected void onCleared() {
        super.onCleared();

        locationData = null;

        if (currentAlertsData != null)
            currentAlertsData.removeObserver(alertObserver);

        currentAlertsData = null;

        alerts = null;
    }
}
