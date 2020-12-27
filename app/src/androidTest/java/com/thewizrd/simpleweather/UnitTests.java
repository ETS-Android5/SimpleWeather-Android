package com.thewizrd.simpleweather;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.gms.tasks.Tasks;
import com.google.common.base.Predicate;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Iterables;
import com.jakewharton.threetenabp.AndroidThreeTen;
import com.thewizrd.shared_resources.AppState;
import com.thewizrd.shared_resources.ApplicationLib;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.locationdata.LocationProviderImpl;
import com.thewizrd.shared_resources.locationdata.google.GoogleLocationProvider;
import com.thewizrd.shared_resources.locationdata.weatherapi.WeatherApiLocationProvider;
import com.thewizrd.shared_resources.tasks.AsyncTask;
import com.thewizrd.shared_resources.tasks.CallableEx;
import com.thewizrd.shared_resources.tzdb.TimeZoneProvider;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.utils.here.HEREOAuthUtils;
import com.thewizrd.shared_resources.weatherdata.AirQuality;
import com.thewizrd.shared_resources.weatherdata.Astronomy;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.shared_resources.weatherdata.WeatherAlert;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.shared_resources.weatherdata.WeatherProviderImpl;
import com.thewizrd.shared_resources.weatherdata.aqicn.AQICNProvider;
import com.thewizrd.shared_resources.weatherdata.images.ImageDatabase;
import com.thewizrd.shared_resources.weatherdata.nws.SolCalcAstroProvider;
import com.thewizrd.shared_resources.weatherdata.nws.alerts.NWSAlertProvider;
import com.thewizrd.shared_resources.weatherdata.smc.SunMoonCalcProvider;

import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

@RunWith(AndroidJUnit4.class)
public class UnitTests {
    @Before
    public void init() {
        // Context of the app under test.
        final Context appContext = ApplicationProvider.getApplicationContext();

        ApplicationLib app = new ApplicationLib() {
            @Override
            public Context getAppContext() {
                return appContext.getApplicationContext();
            }

            @Override
            public SharedPreferences getPreferences() {
                return PreferenceManager.getDefaultSharedPreferences(getAppContext());
            }

            @Override
            public SharedPreferences.OnSharedPreferenceChangeListener getSharedPreferenceListener() {
                return null;
            }

            @Override
            public AppState getAppState() {
                return null;
            }

            @Override
            public boolean isPhone() {
                return true;
            }
        };

        SimpleLibrary.init(app);
        AndroidThreeTen.init(appContext);

        // Start logger
        Logger.init(appContext);
        Settings.loadIfNeeded();
    }

    private Weather getWeather(WeatherProviderImpl providerImpl) throws WeatherException {
        LocationQueryViewModel location = providerImpl.getLocation(new WeatherUtils.Coordinate(47.6721646, -122.1706614));
        LocationData locData = new LocationData(location);
        return providerImpl.getWeather(locData);
    }

    @Test
    public void getHEREWeather() throws WeatherException {
        WeatherProviderImpl provider = WeatherManager.getProvider(WeatherAPI.HERE);
        Weather weather = getWeather(provider);
        Assert.assertTrue(weather != null && weather.isValid());
    }

    @Test
    public void getYahooWeather() throws WeatherException {
        WeatherProviderImpl provider = WeatherManager.getProvider(WeatherAPI.YAHOO);
        Weather weather = getWeather(provider);
        Assert.assertTrue(weather != null && weather.isValid());
    }

    @Test
    public void getMetNoWeather() throws WeatherException {
        WeatherProviderImpl provider = WeatherManager.getProvider(WeatherAPI.METNO);
        Weather weather = getWeather(provider);
        Assert.assertTrue(weather != null && weather.isValid());
    }

    @Test
    public void getNWSAlerts() throws WeatherException {
        LocationQueryViewModel location = WeatherManager.getProvider(WeatherAPI.OPENWEATHERMAP)
                .getLocation(new WeatherUtils.Coordinate(47.6721646, -122.1706614));
        LocationData locData = new LocationData(location);
        Collection<WeatherAlert> alerts = new NWSAlertProvider().getAlerts(locData);
        Assert.assertNotNull(alerts);
    }

    @Test
    public void getNWSWeather() throws WeatherException {
        WeatherProviderImpl provider = WeatherManager.getProvider(WeatherAPI.NWS);
        Weather weather = getWeather(provider);
        Assert.assertTrue(weather != null && weather.getForecast() != null && weather.getForecast().size() > 0 && weather.getHrForecast() != null && weather.getHrForecast().size() > 0);
        Assert.assertTrue(weather != null && weather.isValid());
    }

    @Test
    public void getOWMWeather() throws WeatherException {
        WeatherProviderImpl provider = WeatherManager.getProvider(WeatherAPI.OPENWEATHERMAP);
        Weather weather = getWeather(provider);
        Assert.assertTrue(weather != null && weather.isValid());
    }

    @Test
    public void getHEREOAuthToken() {
        String token = AsyncTask.await(new Callable<String>() {
            @Override
            public String call() {
                return HEREOAuthUtils.getBearerToken(true);
            }
        });
        Assert.assertFalse(StringUtils.isNullOrWhitespace(token));
    }

    @Test
    public void getTimeZone() {
        String tz = new TimeZoneProvider().getTimeZone(0, 0);
        Log.d("TZTest", "tz = " + tz);
        Assert.assertFalse(StringUtils.isNullOrWhitespace(tz));
    }

    @Test
    public void getAQIData() throws WeatherException {
        String tz_long = "America/Los_Angeles";
        final LocationData locationData = new LocationData();
        locationData.setLatitude(47.6721646);
        locationData.setLongitude(-122.1706614);
        locationData.setTzLong(tz_long);
        AirQuality aqi = new AQICNProvider().getAirQualityData(locationData);
        Assert.assertNotNull(aqi);
    }

    @Test
    public void serializationTest() throws WeatherException, IOException {
        WeatherProviderImpl provider = WeatherManager.getProvider(WeatherAPI.YAHOO);

        Stopwatch s = Stopwatch.createStarted();
        Weather weather = getWeather(provider);
        s.stop();
        Log.d("Serialzer", "JSON GetWeather Test: " + s.toString());

        for (int i = 0; i < 30; i++) {
            Stopwatch s2 = Stopwatch.createStarted();
            String json2 = JSONParser.serializer(weather, Weather.class);
            Weather desW2 = JSONParser.deserializer(json2, Weather.class);
            s2.stop();

            Log.d("Serialzer", "JSON2 Test " + (i + 1) + ": " + s2.toString());
        }
    }

    @Test
    public void getSunriseSetTime() throws WeatherException {
        final ZonedDateTime date = ZonedDateTime.now();
        String tz_long = "America/Los_Angeles";
        final LocationData locationData = new LocationData();
        locationData.setLatitude(47.6721646);
        locationData.setLongitude(-122.1706614);
        locationData.setTzLong(tz_long);
        Astronomy astro = AsyncTask.await(new CallableEx<Astronomy, WeatherException>() {
            @Override
            public Astronomy call() throws WeatherException {
                return new SolCalcAstroProvider().getAstronomyData(locationData, date);
            }
        });

        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        Log.d("SolCalc", String.format(Locale.ROOT,
                "Sunrise: %s; Sunset: %s", astro.getSunrise().format(fmt), astro.getSunset().format(fmt)));
        Assert.assertTrue(astro.getSunrise() != LocalDateTime.MIN && astro.getSunset() != LocalDateTime.MIN);
    }

    @Test
    public void firebaseDBTest() throws ExecutionException, InterruptedException {
        long updateTime = Tasks.await(ImageDatabase.getLastUpdateTime());
        Assert.assertTrue(updateTime > 0);
    }

    @Test
    public void simpleAstroTest() throws Exception {
        final ZonedDateTime date = ZonedDateTime.now();
        String tz_long = "America/Los_Angeles";
        final LocationData locationData = new LocationData();
        locationData.setLatitude(47.6721646);
        locationData.setLongitude(-122.1706614);
        locationData.setTzLong(tz_long);
        Astronomy astro = new SunMoonCalcProvider().getAstronomyData(locationData, date);

        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        Log.d("SMC", String.format(Locale.ROOT,
                "Sunrise: %s; Sunset: %s, Moonrise: %s; Moonset: %s", astro.getSunrise().format(fmt), astro.getSunset().format(fmt), astro.getMoonrise().format(fmt), astro.getMoonset().format(fmt)));
        if (astro.getMoonPhase() != null) {
            Log.d("SMC", String.format(Locale.ROOT,
                    "Moonphase: %s", astro.getMoonPhase().getPhase().name()));
        }

        Assert.assertTrue(astro.getSunrise() != LocalDateTime.MIN && astro.getSunset() != LocalDateTime.MIN && astro.getMoonrise() != LocalDateTime.MIN && astro.getMoonset() != LocalDateTime.MIN);
    }

    @Test
    public void androidAutoCompleteLocTest() throws IOException {
        Assert.assertTrue(Geocoder.isPresent());
        Geocoder geocoder = new Geocoder(App.getInstance().getAppContext(), Locale.getDefault());
        List<Address> addressList = geocoder.getFromLocationName("Redmon", 5); // Redmond
        Assert.assertTrue(addressList != null && !addressList.isEmpty());
    }

    @Test
    public void androidGeocoderTest() throws IOException {
        Assert.assertTrue(Geocoder.isPresent());
        Geocoder geocoder = new Geocoder(App.getInstance().getAppContext(), Locale.getDefault());
        //List<Address> addressList = geocoder.getFromLocation(47.6721646, -122.1706614, 1); // Washington
        List<Address> addressList = geocoder.getFromLocation(51.5073884, -0.1334347, 1); // London
        Assert.assertTrue(addressList != null && !addressList.isEmpty());
        Address result = addressList.get(0);
        Assert.assertNotNull(result);
    }

    @Test
    public void placesAPITest() throws WeatherException {
        LocationProviderImpl locationProvider = new GoogleLocationProvider();
        Collection<LocationQueryViewModel> locations = locationProvider.getLocations("Berlin, Germany", null);
        Assert.assertTrue(locations != null && !locations.isEmpty());

        LocationQueryViewModel queryVM = Iterables.getFirst(locations, null);
        Assert.assertNotNull(queryVM);

        LocationQueryViewModel idModel = locationProvider.getLocationFromID(queryVM);
        /*
        LocationQueryViewModel nameModel = locationProvider.getLocationFromName(queryVM);
         */
        Assert.assertNotNull(idModel);
        //Assert.assertNotNull(nameModel);
    }

    @Test
    public void weatherAPILocationTest() throws WeatherException {
        LocationProviderImpl locationProvider = new WeatherApiLocationProvider();
        Collection<LocationQueryViewModel> locations = locationProvider.getLocations("Redmond, WA", null);
        Assert.assertTrue(locations != null && !locations.isEmpty());

        LocationQueryViewModel queryVM = Iterables.find(locations, new Predicate<LocationQueryViewModel>() {
            @Override
            public boolean apply(@NullableDecl LocationQueryViewModel input) {
                return input != null && input.getLocationName().startsWith("Redmond, ");
            }
        });
        Assert.assertNotNull(queryVM);

        LocationQueryViewModel nameModel = locationProvider.getLocationFromName(queryVM);
        Assert.assertNotNull(nameModel);
    }
}
