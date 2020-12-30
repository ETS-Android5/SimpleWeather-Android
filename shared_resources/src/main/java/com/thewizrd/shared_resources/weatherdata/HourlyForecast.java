package com.thewizrd.shared_resources.weatherdata;

import android.util.Log;

import androidx.annotation.RestrictTo;

import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.thewizrd.shared_resources.utils.ConversionMethods;
import com.thewizrd.shared_resources.utils.DateTimeUtils;
import com.thewizrd.shared_resources.utils.LocaleUtils;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.NumberUtils;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherUtils;

import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalTime;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.IOException;
import java.io.StringReader;
import java.util.Locale;

public class HourlyForecast extends BaseForecast {

    @SerializedName("wind_degrees")
    private Integer windDegrees;

    @SerializedName("wind_mph")
    private Float windMph;

    @SerializedName("wind_kph")
    private Float windKph;

    @SerializedName("date")
    private String _date;

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public HourlyForecast() {

    }

    public HourlyForecast(com.thewizrd.shared_resources.weatherdata.openweather.ListItem hr_forecast) {
        setDate(ZonedDateTime.ofInstant(Instant.ofEpochSecond(hr_forecast.getDt()), ZoneOffset.UTC));
        highF = ConversionMethods.KtoF(hr_forecast.getMain().getTemp());
        highC = ConversionMethods.KtoC(hr_forecast.getMain().getTemp());
        condition = StringUtils.toUpperCase(hr_forecast.getWeather().get(0).getDescription());

        // Use icon to determine if day or night
        String ico = hr_forecast.getWeather().get(0).getIcon();
        String dn = Character.toString(ico.charAt(ico.length() == 0 ? 0 : ico.length() - 1));

        try {
            int x = Integer.parseInt(dn);
            dn = "";
        } catch (NumberFormatException ex) {
            // Do nothing
        }

        icon = WeatherManager.getProvider(WeatherAPI.OPENWEATHERMAP)
                .getWeatherIcon(hr_forecast.getWeather().get(0).getId() + dn);

        windDegrees = Math.round(hr_forecast.getWind().getDeg());
        windMph = (float) Math.round(ConversionMethods.msecToMph(hr_forecast.getWind().getSpeed()));
        windKph = (float) Math.round(ConversionMethods.msecToKph(hr_forecast.getWind().getSpeed()));

        // Extras
        extras = new ForecastExtras();
        extras.setHumidity(hr_forecast.getMain().getHumidity());
        extras.setCloudiness(hr_forecast.getClouds().getAll());
        // 1hPA = 1mbar
        extras.setPressureMb(hr_forecast.getMain().getPressure());
        extras.setPressureIn(ConversionMethods.mbToInHg(hr_forecast.getMain().getPressure()));
        extras.setWindDegrees(windDegrees);
        extras.setWindMph(windMph);
        extras.setWindKph(windKph);
        if (highC > 0 && highC < 60 && hr_forecast.getMain().getHumidity() > 1) {
            extras.setDewpointC((float) Math.round(WeatherUtils.calculateDewpointC(highC, hr_forecast.getMain().getHumidity())));
            extras.setDewpointF((float) Math.round(ConversionMethods.CtoF(extras.getDewpointC())));
        }
        if (hr_forecast.getMain().getFeelsLike() != null) {
            extras.setFeelslikeF(ConversionMethods.KtoF(hr_forecast.getMain().getFeelsLike()));
            extras.setFeelslikeC(ConversionMethods.KtoC(hr_forecast.getMain().getFeelsLike()));
        }
        if (hr_forecast.getPop() != null) {
            extras.setPop(Math.round(hr_forecast.getPop() * 100));
        }
        if (hr_forecast.getWind().getGust() != null) {
            extras.setWindGustMph((float) Math.round(ConversionMethods.msecToMph(hr_forecast.getWind().getGust())));
            extras.setWindGustKph((float) Math.round(ConversionMethods.msecToKph(hr_forecast.getWind().getGust())));
        }
        if (hr_forecast.getVisibility() != null) {
            extras.setVisibilityKm(hr_forecast.getVisibility().floatValue() / 1000);
            extras.setVisibilityMi(ConversionMethods.kmToMi(extras.getVisibilityKm()));
        }
        if (hr_forecast.getRain() != null && hr_forecast.getRain().get_3h() != null) {
            extras.setQpfRainMm(hr_forecast.getRain().get_3h());
            extras.setQpfRainIn(ConversionMethods.mmToIn(hr_forecast.getRain().get_3h()));
        }
        if (hr_forecast.getSnow() != null && hr_forecast.getSnow().get_3h() != null) {
            extras.setQpfSnowCm(hr_forecast.getSnow().get_3h() / 10);
            extras.setQpfSnowIn(ConversionMethods.mmToIn(hr_forecast.getSnow().get_3h()));
        }
    }

    /* OpenWeather OneCall
    public HourlyForecast(com.thewizrd.shared_resources.weatherdata.openweather.onecall.HourlyItem hr_forecast) {
        setDate(ZonedDateTime.ofInstant(Instant.ofEpochSecond(hr_forecast.getDt()), ZoneOffset.UTC));
        highF = ConversionMethods.KtoF(hr_forecast.getTemp());
        highC = ConversionMethods.KtoC(hr_forecast.getTemp());
        condition = StringUtils.toUpperCase(hr_forecast.getWeather().get(0).getDescription());

        // Use icon to determine if day or night
        String ico = hr_forecast.getWeather().get(0).getIcon();
        String dn = Character.toString(ico.charAt(ico.length() == 0 ? 0 : ico.length() - 1));

        try {
            int x = Integer.parseInt(dn);
            dn = "";
        } catch (NumberFormatException ex) {
            // Do nothing
        }

        icon = WeatherManager.getProvider(WeatherAPI.OPENWEATHERMAP)
                .getWeatherIcon(hr_forecast.getWeather().get(0).getId() + dn);

        windDegrees = hr_forecast.getWindDeg();
        windMph = (float) Math.round(ConversionMethods.msecToMph(hr_forecast.getWindSpeed()));
        windKph = (float) Math.round(ConversionMethods.msecToKph(hr_forecast.getWindSpeed()));

        // Extras
        extras = new ForecastExtras();
        extras.setFeelslikeF(ConversionMethods.KtoF(hr_forecast.getFeelsLike()));
        extras.setFeelslikeC(ConversionMethods.KtoC(hr_forecast.getFeelsLike()));
        extras.setDewpointF(ConversionMethods.KtoF(hr_forecast.getDewPoint()));
        extras.setDewpointC(ConversionMethods.KtoC(hr_forecast.getDewPoint()));
        extras.setHumidity(hr_forecast.getHumidity());
        extras.setCloudiness(hr_forecast.getClouds());
        if (hr_forecast.getPop() != null) {
            extras.setPop(Math.round(hr_forecast.getPop() * 100));
        }
        // 1hPA = 1mbar
        extras.setPressureMb(hr_forecast.getPressure());
        extras.setPressureIn(ConversionMethods.mbToInHg(hr_forecast.getPressure()));
        extras.setWindDegrees(windDegrees);
        extras.setWindMph(windMph);
        extras.setWindKph(windKph);
        if (hr_forecast.getWindGust() != null) {
            extras.setWindGustMph((float) Math.round(ConversionMethods.msecToMph(hr_forecast.getWindGust())));
            extras.setWindGustKph((float) Math.round(ConversionMethods.msecToKph(hr_forecast.getWindGust())));
        }
        if (hr_forecast.getVisibility() != null) {
            extras.setVisibilityKm(hr_forecast.getVisibility().floatValue() / 1000);
            extras.setVisibilityMi(ConversionMethods.kmToMi(extras.getVisibilityKm()));
        }
        if (hr_forecast.getRain() != null) {
            extras.setQpfRainMm(hr_forecast.getRain().get_1h());
            extras.setQpfRainIn(ConversionMethods.mmToIn(hr_forecast.getRain().get_1h()));
        }
        if (hr_forecast.getSnow() != null) {
            extras.setQpfSnowCm(hr_forecast.getSnow().get_1h() / 10);
            extras.setQpfSnowIn(ConversionMethods.mmToIn(hr_forecast.getSnow().get_1h()));
        }
    }
     */

    public HourlyForecast(com.thewizrd.shared_resources.weatherdata.metno.TimeseriesItem hr_forecast) {
        // new DateTimeOffset(, TimeSpan.Zero);
        setDate(ZonedDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(hr_forecast.getTime())), ZoneOffset.UTC));
        highF = ConversionMethods.CtoF(hr_forecast.getData().getInstant().getDetails().getAirTemperature());
        highC = hr_forecast.getData().getInstant().getDetails().getAirTemperature();
        windDegrees = Math.round(hr_forecast.getData().getInstant().getDetails().getWindFromDirection());
        windMph = (float) Math.round(ConversionMethods.msecToMph(hr_forecast.getData().getInstant().getDetails().getWindSpeed()));
        windKph = (float) Math.round(ConversionMethods.msecToKph(hr_forecast.getData().getInstant().getDetails().getWindSpeed()));

        if (hr_forecast.getData().getNext12Hours() != null) {
            icon = hr_forecast.getData().getNext12Hours().getSummary().getSymbolCode();
        } else if (hr_forecast.getData().getNext6Hours() != null) {
            icon = hr_forecast.getData().getNext6Hours().getSummary().getSymbolCode();
        } else if (hr_forecast.getData().getNext1Hours() != null) {
            icon = hr_forecast.getData().getNext1Hours().getSummary().getSymbolCode();
        }

        float humidity = hr_forecast.getData().getInstant().getDetails().getRelativeHumidity();
        // Extras
        extras = new ForecastExtras();
        extras.setFeelslikeF(WeatherUtils.getFeelsLikeTemp(highF, windMph, Math.round(humidity)));
        extras.setFeelslikeC(ConversionMethods.FtoC(WeatherUtils.getFeelsLikeTemp(highF, windMph, Math.round(humidity))));
        extras.setHumidity(Math.round(humidity));
        extras.setDewpointF(ConversionMethods.CtoF(hr_forecast.getData().getInstant().getDetails().getDewPointTemperature()));
        extras.setDewpointC(hr_forecast.getData().getInstant().getDetails().getDewPointTemperature());
        if (hr_forecast.getData().getInstant().getDetails().getCloudAreaFraction() != null) {
            extras.setCloudiness(Math.round(hr_forecast.getData().getInstant().getDetails().getCloudAreaFraction()));
        }
        // Precipitation
        if (hr_forecast.getData().getInstant().getDetails().getProbabilityOfPrecipitation() != null) {
            extras.setPop(Math.round(hr_forecast.getData().getInstant().getDetails().getProbabilityOfPrecipitation()));
        } else if (hr_forecast.getData().getNext1Hours() != null && hr_forecast.getData().getNext1Hours().getDetails() != null && hr_forecast.getData().getNext1Hours().getDetails().getProbabilityOfPrecipitation() != null) {
            extras.setPop(Math.round(hr_forecast.getData().getNext1Hours().getDetails().getProbabilityOfPrecipitation()));
        } else if (hr_forecast.getData().getNext6Hours() != null && hr_forecast.getData().getNext6Hours().getDetails() != null && hr_forecast.getData().getNext6Hours().getDetails().getProbabilityOfPrecipitation() != null) {
            extras.setPop(Math.round(hr_forecast.getData().getNext6Hours().getDetails().getProbabilityOfPrecipitation()));
        } else if (hr_forecast.getData().getNext12Hours() != null && hr_forecast.getData().getNext12Hours().getDetails() != null && hr_forecast.getData().getNext12Hours().getDetails().getProbabilityOfPrecipitation() != null) {
            extras.setPop(Math.round(hr_forecast.getData().getNext12Hours().getDetails().getProbabilityOfPrecipitation()));
        }
        extras.setPressureIn(ConversionMethods.mbToInHg(hr_forecast.getData().getInstant().getDetails().getAirPressureAtSeaLevel()));
        extras.setPressureMb(hr_forecast.getData().getInstant().getDetails().getAirPressureAtSeaLevel());
        extras.setWindDegrees(windDegrees);
        extras.setWindMph(windMph);
        extras.setWindKph(windKph);
        if (hr_forecast.getData().getInstant().getDetails().getWindSpeedOfGust() != null) {
            extras.setWindGustMph((float) Math.round(ConversionMethods.msecToMph(hr_forecast.getData().getInstant().getDetails().getWindSpeedOfGust())));
            extras.setWindGustKph((float) Math.round(ConversionMethods.msecToKph(hr_forecast.getData().getInstant().getDetails().getWindSpeedOfGust())));
        }
        if (hr_forecast.getData().getInstant().getDetails().getFogAreaFraction() != null) {
            float visMi = 10.0f;
            extras.setVisibilityMi((visMi - (visMi * hr_forecast.getData().getInstant().getDetails().getFogAreaFraction() / 100)));
            extras.setVisibilityKm(ConversionMethods.miToKm(extras.getVisibilityMi()));
        }
        if (hr_forecast.getData().getInstant().getDetails().getUltravioletIndexClearSky() != null) {
            extras.setUvIndex(hr_forecast.getData().getInstant().getDetails().getUltravioletIndexClearSky());
        }
    }

    public HourlyForecast(com.thewizrd.shared_resources.weatherdata.here.ForecastItem1 hr_forecast) {
        setDate(ZonedDateTime.parse(hr_forecast.getUtcTime()));
        Float high_f = NumberUtils.tryParseFloat(hr_forecast.getTemperature());
        if (high_f != null) {
            highF = high_f;
            highC = ConversionMethods.FtoC(high_f);
        }
        condition = StringUtils.toPascalCase(hr_forecast.getDescription());

        icon = WeatherManager.getProvider(WeatherAPI.HERE)
                .getWeatherIcon(String.format("%s_%s", hr_forecast.getDaylight(), hr_forecast.getIconName()));

        Integer windDeg = NumberUtils.tryParseInt(hr_forecast.getWindDirection());
        if (windDeg != null) {
            windDegrees = windDeg;
        }
        Float windSpeed = NumberUtils.tryParseFloat(hr_forecast.getWindSpeed());
        if (windSpeed != null) {
            windMph = windSpeed;
            windKph = ConversionMethods.mphTokph(windSpeed);
        }

        // Extras
        extras = new ForecastExtras();
        Float comfortTempF = NumberUtils.tryParseFloat(hr_forecast.getComfort());
        if (comfortTempF != null) {
            extras.setFeelslikeF(comfortTempF);
            extras.setFeelslikeC(ConversionMethods.FtoC(comfortTempF));
        }
        Integer humidity = NumberUtils.tryParseInt(hr_forecast.getHumidity());
        if (humidity != null) {
            extras.setHumidity(humidity);
        }
        Float dewpointF = NumberUtils.tryParseFloat(hr_forecast.getDewPoint());
        if (dewpointF != null) {
            extras.setDewpointF(dewpointF);
            extras.setDewpointC(ConversionMethods.FtoC(dewpointF));
        }
        Float visibilityMI = NumberUtils.tryParseFloat(hr_forecast.getVisibility());
        if (visibilityMI != null) {
            extras.setVisibilityMi(visibilityMI);
            extras.setVisibilityKm(ConversionMethods.miToKm(visibilityMI));
        }
        extras.setPop(NumberUtils.tryParseInt(hr_forecast.getPrecipitationProbability()));
        Float rain_in = NumberUtils.tryParseFloat(hr_forecast.getRainFall());
        if (rain_in != null) {
            extras.setQpfRainIn(rain_in);
            extras.setQpfRainMm(ConversionMethods.inToMM(rain_in));
        }
        Float snow_in = NumberUtils.tryParseFloat(hr_forecast.getSnowFall());
        if (snow_in != null) {
            extras.setQpfSnowIn(snow_in);
            extras.setQpfSnowCm(ConversionMethods.inToMM(snow_in) / 10);
        }
        Float pressureIN = NumberUtils.tryParseFloat(hr_forecast.getBarometerPressure());
        if (pressureIN != null) {
            extras.setPressureIn(pressureIN);
            extras.setPressureMb(ConversionMethods.inHgToMB(pressureIN));
        }
        extras.setWindDegrees(windDegrees);
        extras.setWindMph(windMph);
        extras.setWindKph(windKph);
    }

    public HourlyForecast(com.thewizrd.shared_resources.weatherdata.nws.hourly.PeriodItem forecastItem) {
        WeatherProviderImpl provider = WeatherManager.getProvider(WeatherAPI.NWS);
        Locale locale = LocaleUtils.getLocale();

        setDate(Instant.ofEpochSecond(Long.parseLong(forecastItem.getUnixTime())).atZone(ZoneOffset.UTC));

        Float temp = NumberUtils.tryParseFloat(forecastItem.getTemperature());
        if (temp != null) {
            highF = temp;
            highC = ConversionMethods.FtoC(temp);
        }

        if (locale.toString().equals("en") || locale.toString().startsWith("en_") || locale.equals(Locale.ROOT)) {
            condition = forecastItem.getWeather();
        } else {
            condition = provider.getWeatherCondition(forecastItem.getIconLink());
        }
        icon = forecastItem.getIconLink();

        // Extras
        extras = new ForecastExtras();

        if (forecastItem.getWindSpeed() != null && forecastItem.getWindDirection() != null) {
            windDegrees = Integer.parseInt(forecastItem.getWindDirection());
            windMph = Float.parseFloat(forecastItem.getWindSpeed());
            windKph = ConversionMethods.mphTokph(windMph);

            extras.setWindDegrees(this.windDegrees);
            extras.setWindMph(this.windMph);
            extras.setWindKph(this.windKph);
        }

        Float windChill = NumberUtils.tryParseFloat(forecastItem.getWindChill());
        if (windChill != null) {
            extras.setFeelslikeF(windChill);
            extras.setFeelslikeC(ConversionMethods.FtoC(windChill));
        }

        Integer cloudiness = NumberUtils.tryParseInt(forecastItem.getCloudAmount());
        if (cloudiness != null) {
            extras.setCloudiness(cloudiness);
        }

        Integer pop = NumberUtils.tryParseInt(forecastItem.getPop());
        if (pop != null) {
            extras.setPop(pop);
        }

        Integer humidity = NumberUtils.tryParseInt(forecastItem.getRelativeHumidity());
        if (humidity != null) {
            extras.setHumidity(humidity);
        }

        Float windGust = NumberUtils.tryParseFloat(forecastItem.getWindGust());
        if (windGust != null) {
            extras.setWindGustMph(windGust);
            extras.setWindGustKph(ConversionMethods.mphTokph(windGust));
        }
    }

    public HourlyForecast(com.thewizrd.shared_resources.weatherdata.weatherunlocked.TimeframesItem timeframe) {
        final String date = timeframe.getUtcdate();
        final int time = timeframe.getUtctime();
        LocalDate dateObj = LocalDate.parse(date, DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT));
        LocalTime timeObj;
        if (time == 0) {
            timeObj = LocalTime.MIDNIGHT;
        } else {
            timeObj = LocalTime.parse(Integer.toString(time), DateTimeFormatter.ofPattern("Hmm", Locale.ROOT));
        }
        setDate(ZonedDateTime.of(dateObj, timeObj, ZoneOffset.UTC));

        highF = timeframe.getTempF();
        highC = timeframe.getTempC();
        condition = timeframe.getWxDesc();
        icon = Integer.toString(timeframe.getWxCode());

        windDegrees = (int) Math.round(timeframe.getWinddirDeg());
        windMph = (float) Math.round(timeframe.getWindspdMph());
        windKph = (float) Math.round(timeframe.getWindspdKmh());

        // Extras
        extras = new ForecastExtras();
        extras.setHumidity((int) Math.round(timeframe.getHumidPct()));
        extras.setCloudiness((int) Math.round(timeframe.getCloudtotalPct()));
        extras.setPressureMb(timeframe.getSlpMb());
        extras.setPressureIn(timeframe.getSlpIn());
        extras.setWindDegrees(windDegrees);
        extras.setWindMph(windMph);
        extras.setWindKph(windKph);
        extras.setDewpointF((float) Math.round(timeframe.getDewpointF()));
        extras.setDewpointC((float) Math.round(timeframe.getDewpointC()));
        extras.setFeelslikeF((float) Math.round(timeframe.getFeelslikeF()));
        extras.setFeelslikeC((float) Math.round(timeframe.getFeelslikeC()));
        extras.setPop(NumberUtils.tryParseInt(timeframe.getProbPrecipPct(), 0));
        extras.setWindGustMph((float) Math.round(timeframe.getWindgstMph()));
        extras.setWindGustKph((float) Math.round(timeframe.getWindgstKmh()));
        extras.setVisibilityMi(timeframe.getVisMi());
        extras.setVisibilityKm(timeframe.getVisKm());
        extras.setQpfRainMm(timeframe.getRainMm());
        extras.setQpfRainIn(timeframe.getRainIn());
        extras.setQpfSnowCm(timeframe.getSnowMm() / 10);
        extras.setQpfSnowIn(timeframe.getSnowIn());
    }

    public String get_date() {
        return _date;
    }

    public void set_date(String _date) {
        this._date = _date;
    }

    public ZonedDateTime getDate() {
        ZonedDateTime dateTime = null;

        try {
            dateTime = ZonedDateTime.parse(_date, DateTimeUtils.getZonedDateTimeFormatter());
        } catch (Exception ex) {
            dateTime = null;
        }

        if (dateTime == null)
            dateTime = ZonedDateTime.parse(_date);

        return dateTime;
    }

    public void setDate(ZonedDateTime date) {
        _date = date.format(DateTimeUtils.getZonedDateTimeFormatter());
    }

    public Integer getWindDegrees() {
        return windDegrees;
    }

    public void setWindDegrees(Integer windDegrees) {
        this.windDegrees = windDegrees;
    }

    public Float getWindMph() {
        return windMph;
    }

    public void setWindMph(Float windMph) {
        this.windMph = windMph;
    }

    public Float getWindKph() {
        return windKph;
    }

    public void setWindKph(Float windKph) {
        this.windKph = windKph;
    }

    @Override
    public void fromJson(JsonReader extReader) {
        try {
            JsonReader reader;
            String jsonValue;

            if (extReader.peek() == JsonToken.STRING) {
                jsonValue = extReader.nextString();
            } else {
                jsonValue = null;
            }

            if (jsonValue == null)
                reader = extReader;
            else {
                reader = new JsonReader(new StringReader(jsonValue));
                reader.beginObject(); // StartObject
            }

            while (reader.hasNext() && reader.peek() != JsonToken.END_OBJECT) {
                if (reader.peek() == JsonToken.BEGIN_OBJECT)
                    reader.beginObject(); // StartObject

                String property = reader.nextName();

                if (reader.peek() == JsonToken.NULL) {
                    reader.nextNull();
                    continue;
                }

                switch (property) {
                    case "date":
                        this._date = reader.nextString();
                        break;
                    case "high_f":
                        this.highF = NumberUtils.tryParseFloat(reader.nextString());
                        break;
                    case "high_c":
                        this.highC = NumberUtils.tryParseFloat(reader.nextString());
                        break;
                    case "condition":
                        this.condition = reader.nextString();
                        break;
                    case "icon":
                        this.icon = reader.nextString();
                        break;
                    case "wind_degrees":
                        this.windDegrees = NumberUtils.tryParseInt(reader.nextString());
                        break;
                    case "wind_mph":
                        this.windMph = NumberUtils.tryParseFloat(reader.nextString());
                        break;
                    case "wind_kph":
                        this.windKph = NumberUtils.tryParseFloat(reader.nextString());
                        break;
                    case "extras":
                        this.extras = new ForecastExtras();
                        this.extras.fromJson(reader);
                        break;
                    default:
                        reader.skipValue();
                        break;
                }
            }

            if (reader.peek() == JsonToken.END_OBJECT)
                reader.endObject();

        } catch (Exception ignored) {
        }
    }

    @Override
    public void toJson(JsonWriter writer) {
        try {
            // {
            writer.beginObject();

            // "date" : ""
            writer.name("date");
            writer.value(_date);

            // "high_f" : ""
            writer.name("high_f");
            writer.value(highF);

            // "high_c" : ""
            writer.name("high_c");
            writer.value(highC);

            // "condition" : ""
            writer.name("condition");
            writer.value(condition);

            // "icon" : ""
            writer.name("icon");
            writer.value(icon);

            // "wind_degrees" : ""
            writer.name("wind_degrees");
            writer.value(windDegrees);

            // "wind_mph" : ""
            writer.name("wind_mph");
            writer.value(windMph);

            // "wind_kph" : ""
            writer.name("wind_kph");
            writer.value(windKph);

            // "extras" : ""
            if (extras != null) {
                writer.name("extras");
                if (extras == null)
                    writer.nullValue();
                else
                    extras.toJson(writer);
            }

            // }
            writer.endObject();
        } catch (IOException e) {
            Logger.writeLine(Log.ERROR, e, "HourlyForecast: error writing json string");
        }
    }
}