package com.thewizrd.shared_resources.controls;

import android.location.Address;

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;

import com.thewizrd.shared_resources.R;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.locationdata.here.AdditionalDataItem;
import com.thewizrd.shared_resources.locationdata.here.ResultItem;
import com.thewizrd.shared_resources.locationdata.here.SuggestionsItem;
import com.thewizrd.shared_resources.locationdata.locationiq.AutoCompleteQuery;
import com.thewizrd.shared_resources.locationdata.locationiq.GeoLocation;
import com.thewizrd.shared_resources.locationdata.weatherapi.LocationItem;
import com.thewizrd.shared_resources.utils.LocationUtils;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;

import java.text.DecimalFormat;
import java.util.Locale;

public class LocationQueryViewModel {
    private String locationName;
    private String locationRegion;
    private String locationCountry;
    private String locationQuery;

    private double locationLat;
    private double locationLong;

    private String locationTZLong;

    private String locationSource;
    private String weatherSource;

    public LocationQueryViewModel() {
        locationName = SimpleLibrary.getInstance().getAppContext().getString(R.string.error_noresults);
        locationCountry = "";
        locationQuery = "";
    }

    public LocationQueryViewModel(LocationData data) {
        locationQuery = data.getQuery();
        locationName = data.getName();
        locationLat = data.getLatitude();
        locationLong = data.getLongitude();
        locationTZLong = data.getTzLong();
        weatherSource = data.getWeatherSource();
        locationSource = data.getLocationSource();
        locationCountry = data.getCountryCode();
    }

    /* HERE Autocomplete */
    public LocationQueryViewModel(SuggestionsItem location, String weatherAPI) {
        setLocation(location, weatherAPI);
    }

    /* HERE Autocomplete */
    public void setLocation(SuggestionsItem location, String weatherAPI) {
        if (location == null || location.getAddress() == null)
            return;

        String town, region, country;

        // Try to get district name or fallback to city name
        town = location.getAddress().getDistrict();

        if (StringUtils.isNullOrEmpty(town))
            town = location.getAddress().getCity();

        region = location.getAddress().getState();

        if (StringUtils.isNullOrEmpty(region) && !ObjectsCompat.equals(town, location.getAddress().getCounty()))
            region = location.getAddress().getCounty();

        country = location.getAddress().getCountry();

        if (!StringUtils.isNullOrWhitespace(town) && !StringUtils.isNullOrWhitespace(region) &&
                !StringUtils.isNullOrEmpty(location.getAddress().getCounty())
                && !(ObjectsCompat.equals(location.getAddress().getCounty(), region) ||
                ObjectsCompat.equals(location.getAddress().getCounty(), town))) {
            locationName = String.format("%s, %s, %s", town, location.getAddress().getCounty(), region);
        } else if (!StringUtils.isNullOrWhitespace(town) && !StringUtils.isNullOrWhitespace(region)) {
            if (ObjectsCompat.equals(town, region)) {
                locationName = String.format("%s, %s", town, country);
            } else {
                locationName = String.format("%s, %s", town, region);
            }
        } else {
            if (StringUtils.isNullOrWhitespace(town) || StringUtils.isNullOrWhitespace(region)) {
                if (StringUtils.isNullOrWhitespace(town)) {
                    locationName = String.format("%s, %s", region, country);
                } else {
                    locationName = String.format("%s, %s", town, country);
                }
            }
        }

        locationCountry = location.getCountryCode();
        locationQuery = location.getLocationId();

        locationLat = -1;
        locationLong = -1;

        locationTZLong = null;

        locationSource = WeatherAPI.HERE;
        weatherSource = weatherAPI;
    }

    /* HERE Geocoder */
    public LocationQueryViewModel(ResultItem location, String weatherAPI) {
        setLocation(location, weatherAPI);
    }

    /* HERE Geocoder */
    public void setLocation(ResultItem location, String weatherAPI) {
        if (location == null || location.getLocation() == null || location.getLocation().getAddress() == null)
            return;

        String country = null, countryCode = null, region = null, town = null;

        if (location.getLocation().getAddress().getAdditionalData() != null) {
            for (AdditionalDataItem item : location.getLocation().getAddress().getAdditionalData()) {
                if ("Country2".equals(item.getKey()))
                    countryCode = item.getValue();
                else if ("CountryName".equals(item.getKey()))
                    country = item.getValue();

                if (countryCode != null && country != null)
                    break;
            }
        }

        // Try to get district name or fallback to city name
        town = location.getLocation().getAddress().getDistrict();

        if (StringUtils.isNullOrEmpty(town))
            town = location.getLocation().getAddress().getCity();

        region = location.getLocation().getAddress().getState();

        if (StringUtils.isNullOrEmpty(region) && !ObjectsCompat.equals(town, location.getLocation().getAddress().getCounty()))
            region = location.getLocation().getAddress().getCounty();

        if (StringUtils.isNullOrEmpty(country))
            country = location.getLocation().getAddress().getCountry();

        if (StringUtils.isNullOrEmpty(countryCode))
            countryCode = location.getLocation().getAddress().getCountry();

        if (!StringUtils.isNullOrWhitespace(town) && !StringUtils.isNullOrWhitespace(region) &&
                !StringUtils.isNullOrEmpty(location.getLocation().getAddress().getCounty())
                && !(ObjectsCompat.equals(location.getLocation().getAddress().getCounty(), region) ||
                ObjectsCompat.equals(location.getLocation().getAddress().getCounty(), town))) {
            locationName = String.format("%s, %s, %s", town, location.getLocation().getAddress().getCounty(), region);
        } else if (!StringUtils.isNullOrWhitespace(town) && !StringUtils.isNullOrWhitespace(region)) {
            if (ObjectsCompat.equals(town, region)) {
                locationName = String.format("%s, %s", town, country);
            } else {
                locationName = String.format("%s, %s", town, region);
            }
        } else {
            if (StringUtils.isNullOrWhitespace(town) || StringUtils.isNullOrWhitespace(region)) {
                if (!StringUtils.isNullOrWhitespace(location.getLocation().getAddress().getLabel())) {
                    locationName = location.getLocation().getAddress().getLabel();

                    if (locationName != null && locationName.contains(", " + country)) {
                        locationName = locationName.replace(", " + country, "");
                    }
                } else {
                    if (StringUtils.isNullOrWhitespace(town)) {
                        locationName = String.format("%s, %s", region, country);
                    } else {
                        locationName = String.format("%s, %s", town, country);
                    }
                }
            }
        }

        locationCountry = countryCode;

        locationLat = location.getLocation().getDisplayPosition().getLatitude();
        locationLong = location.getLocation().getDisplayPosition().getLongitude();

        locationTZLong = location.getLocation().getAdminInfo().getTimeZone().getId();

        locationSource = WeatherAPI.HERE;
        weatherSource = weatherAPI;

        updateLocationQuery();
    }

    /* LocationIQ AutoComplete */
    public LocationQueryViewModel(AutoCompleteQuery result, String weatherAPI) {
        setLocation(result, weatherAPI);
    }

    /* LocationIQ AutoComplete */
    private void setLocation(AutoCompleteQuery result, String weatherAPI) {
        if (result == null || result.getAddress() == null)
            return;

        String town, region;

        // Try to get district name or fallback to city name
        if (!StringUtils.isNullOrEmpty(result.getAddress().getNeighbourhood()))
            town = result.getAddress().getNeighbourhood();
        else if (!StringUtils.isNullOrEmpty(result.getAddress().getHamlet()))
            town = result.getAddress().getHamlet();
        else if (!StringUtils.isNullOrEmpty(result.getAddress().getSuburb()))
            town = result.getAddress().getSuburb();
        else if (!StringUtils.isNullOrEmpty(result.getAddress().getVillage()))
            town = result.getAddress().getVillage();
        else if (!StringUtils.isNullOrEmpty(result.getAddress().getTown()))
            town = result.getAddress().getTown();
        else if (!StringUtils.isNullOrWhitespace(result.getAddress().getCity()))
            town = result.getAddress().getCity();
        else
            town = result.getAddress().getName();

        // Try to get district name or fallback to city name
        if (!StringUtils.isNullOrEmpty(result.getAddress().getRegion()))
            region = result.getAddress().getRegion();
        else if (!StringUtils.isNullOrEmpty(result.getAddress().getCounty()))
            region = result.getAddress().getCounty();
        else if (!StringUtils.isNullOrEmpty(result.getAddress().getStateDistrict()))
            region = result.getAddress().getStateDistrict();
        else if (!StringUtils.isNullOrEmpty(result.getAddress().getState()))
            region = result.getAddress().getState();
        else
            region = result.getAddress().getCountry();

        if (!StringUtils.isNullOrEmpty(result.getAddress().getName()) && !(result.getAddress().getName().equals(town)))
            locationName = String.format("%s, %s, %s", result.getAddress().getName(), town, region);
        else
            locationName = String.format("%s, %s", town, region);

        if (!StringUtils.isNullOrWhitespace(result.getAddress().getCountryCode()))
            locationCountry = result.getAddress().getCountryCode().toUpperCase(Locale.ROOT);
        else
            locationCountry = result.getAddress().getCountry();

        locationLat = Double.parseDouble(result.getLat());
        locationLong = Double.parseDouble(result.getLon());

        locationTZLong = null;

        locationSource = WeatherAPI.LOCATIONIQ;
        weatherSource = weatherAPI;

        updateLocationQuery();
    }

    /* LocationIQ Geocoder */
    public LocationQueryViewModel(GeoLocation result, String weatherAPI) {
        setLocation(result, weatherAPI);
    }

    /* LocationIQ Geocoder */
    private void setLocation(GeoLocation result, String weatherAPI) {
        if (result == null || result.getAddress() == null)
            return;

        String town, region;

        // Try to get district name or fallback to city name
        if (!StringUtils.isNullOrEmpty(result.getAddress().getNeighbourhood()))
            town = result.getAddress().getNeighbourhood();
        else if (!StringUtils.isNullOrEmpty(result.getAddress().getHamlet()))
            town = result.getAddress().getHamlet();
        else if (!StringUtils.isNullOrEmpty(result.getAddress().getSuburb()))
            town = result.getAddress().getSuburb();
        else if (!StringUtils.isNullOrEmpty(result.getAddress().getVillage()))
            town = result.getAddress().getVillage();
        else if (!StringUtils.isNullOrEmpty(result.getAddress().getTown()))
            town = result.getAddress().getTown();
        else if (!StringUtils.isNullOrWhitespace(result.getAddress().getCity()))
            town = result.getAddress().getCity();
        else
            town = result.getAddress().getName();

        // Try to get district name or fallback to city name
        if (!StringUtils.isNullOrEmpty(result.getAddress().getRegion()))
            region = result.getAddress().getRegion();
        else if (!StringUtils.isNullOrEmpty(result.getAddress().getCounty()))
            region = result.getAddress().getCounty();
        else if (!StringUtils.isNullOrEmpty(result.getAddress().getStateDistrict()))
            region = result.getAddress().getStateDistrict();
        else if (!StringUtils.isNullOrEmpty(result.getAddress().getState()))
            region = result.getAddress().getState();
        else
            region = result.getAddress().getCountry();

        if (!StringUtils.isNullOrEmpty(result.getAddress().getName()) && !(result.getAddress().getName().equals(town)))
            locationName = String.format("%s, %s, %s", result.getAddress().getName(), town, region);
        else
            locationName = String.format("%s, %s", town, region);

        if (!StringUtils.isNullOrWhitespace(result.getAddress().getCountryCode()))
            locationCountry = result.getAddress().getCountryCode().toUpperCase(Locale.ROOT);
        else
            locationCountry = result.getAddress().getCountry();

        locationLat = Double.parseDouble(result.getLat());
        locationLong = Double.parseDouble(result.getLon());

        locationTZLong = null;

        locationSource = WeatherAPI.LOCATIONIQ;
        weatherSource = weatherAPI;

        updateLocationQuery();
    }

    /* Android Geocoder */
    public LocationQueryViewModel(Address result, String weatherAPI) {
        setLocation(result, weatherAPI);
    }

    /* Android Geocoder */
    private void setLocation(Address result, String weatherAPI) {
        if (result == null || !result.hasLatitude() || !result.hasLongitude())
            return;

        String town;
        if (!StringUtils.isNullOrEmpty(result.getLocality())) {
            town = result.getLocality();
        } else/* if (StringUtils.isNullOrEmpty(result.getSubLocality()))*/ {
            town = result.getSubLocality();
        }
        String region = result.getAdminArea();

        if (region != null && !ObjectsCompat.equals(town, region)) {
            locationName = String.format("%s, %s", town, region);
        } else {
            locationName = String.format("%s, %s", town, result.getCountryName());
        }

        locationRegion = result.getSubAdminArea();

        locationLat = result.getLatitude();
        locationLong = result.getLongitude();

        locationCountry = result.getCountryCode();

        locationTZLong = null;

        locationSource = WeatherAPI.GOOGLE;
        weatherSource = weatherAPI;

        updateLocationQuery();
    }

    /* WeatherAPI Search/AutoComplete Query */
    public LocationQueryViewModel(LocationItem result, String weatherAPI) {
        setLocation(result, weatherAPI);
    }

    /* WeatherAPI Search/AutoComplete Query */
    private void setLocation(LocationItem result, String weatherAPI) {
        if (result == null)
            return;

        final boolean isUSorCA = LocationUtils.isUSorCanada(result.getCountry());

        String name = result.getName();
        if (isUSorCA) {
            name = name.replaceFirst(String.format(", %s", result.getCountry()), "");
        } else {
            name = name.replaceFirst(String.format(", %s, %s", result.getRegion(), result.getCountry()), String.format(", %s", result.getCountry()));
            locationRegion = result.getRegion();
        }

        locationName = name;
        locationCountry = result.getCountry();
        locationQuery = Integer.toString(result.getId());

        locationLat = result.getLat();
        locationLong = result.getLon();

        locationTZLong = null;

        locationSource = WeatherAPI.WEATHERAPI;
        weatherSource = weatherAPI;

        updateLocationQuery();
    }

    private void updateLocationQuery() {
        DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(Locale.ROOT);
        df.applyPattern("0.####");

        if (WeatherAPI.HERE.equals(weatherSource)) {
            locationQuery = String.format(Locale.ROOT, "latitude=%s&longitude=%s", df.format(locationLat), df.format(locationLong));
        } else if (WeatherAPI.WEATHERUNLOCKED.equals(weatherSource)) {
            locationQuery = String.format(Locale.ROOT, "%s,%s", df.format(locationLat), df.format(locationLong));
        } else {
            locationQuery = String.format(Locale.ROOT, "lat=%s&lon=%s", df.format(locationLat), df.format(locationLong));
        }
    }

    public void updateWeatherSource(@WeatherAPI.WeatherProviders @NonNull String API) {
        weatherSource = API;
        updateLocationQuery();
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getLocationRegion() {
        return locationRegion;
    }

    public void setLocationRegion(String locationRegion) {
        this.locationRegion = locationRegion;
    }

    public String getLocationCountry() {
        return locationCountry;
    }

    public void setLocationCountry(String locationCountry) {
        this.locationCountry = locationCountry;
    }

    public String getLocationQuery() {
        return locationQuery;
    }

    public void setLocationQuery(String locationQuery) {
        this.locationQuery = locationQuery;
    }

    public double getLocationLat() {
        return locationLat;
    }

    public void setLocationLat(double locationLat) {
        this.locationLat = locationLat;
    }

    public double getLocationLong() {
        return locationLong;
    }

    public void setLocationLong(double locationLong) {
        this.locationLong = locationLong;
    }

    public String getLocationTZLong() {
        return locationTZLong;
    }

    public void setLocationTZLong(String locationTZLong) {
        this.locationTZLong = locationTZLong;
    }

    public String getLocationSource() {
        return locationSource;
    }

    public void setLocationSource(String locationSource) {
        this.locationSource = locationSource;
    }

    public String getWeatherSource() {
        return weatherSource;
    }

    public void setWeatherSource(String weatherSource) {
        this.weatherSource = weatherSource;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LocationQueryViewModel that = (LocationQueryViewModel) o;

        if (locationName != null ? !locationName.equals(that.locationName) : that.locationName != null)
            return false;
        if (locationRegion != null ? !locationRegion.equals(that.locationRegion) : that.locationRegion != null)
            return false;
        return locationCountry != null ? locationCountry.equals(that.locationCountry) : that.locationCountry == null;
    }

    @Override
    public int hashCode() {
        int result = locationName != null ? locationName.hashCode() : 0;
        result = 31 * result + (locationRegion != null ? locationRegion.hashCode() : 0);
        result = 31 * result + (locationCountry != null ? locationCountry.hashCode() : 0);
        return result;
    }
}
