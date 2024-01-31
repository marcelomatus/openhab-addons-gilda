/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.openweathermap.internal.dto.onecall;

import java.util.List;

import org.openhab.binding.openweathermap.internal.dto.forecast.daily.FeelsLikeTemp;
import org.openhab.binding.openweathermap.internal.dto.forecast.daily.Temp;

import com.google.gson.annotations.SerializedName;

/**
 * Holds the data from the deserialised JSON response. Created using http://www.jsonschema2pojo.org/.
 * Settings:
 * Annotation Style: GSON
 * Use primitive types
 * Use double numbers
 * allow additional properties
 *
 * @author Wolfgang Klimt - Initial contribution
 */
public class Daily {
    private int dt;
    private int sunrise;
    private int sunset;
    private int moonrise;
    private int moonset;
    private double moon_phase;
    private Temp temp;
    @SerializedName("feels_like")
    private FeelsLikeTemp feelsLikeTemp;
    private int pressure;
    private int humidity;
    @SerializedName("dew_point")
    private double dewPoint;
    @SerializedName("wind_speed")
    private double windSpeed;
    @SerializedName("wind_deg")
    private int windDeg;
    @SerializedName("wind_gust")
    private double windGust;
    private List<Weather> weather = null;
    private int clouds;
    private double pop;
    private int visibility;
    private double rain;
    private double snow;
    private double uvi;

    public int getDt() {
        return dt;
    }

    public void setDt(int dt) {
        this.dt = dt;
    }

    public int getSunrise() {
        return sunrise;
    }

    public void setSunrise(int sunrise) {
        this.sunrise = sunrise;
    }

    public int getMoonrise() {
        return moonrise;
    }

    public void setMoonrise(int moonrise) {
        this.moonrise = moonrise;
    }

    public int getMoonset() {
        return moonset;
    }

    public void setMoonset(int moonset) {
        this.moonset = moonset;
    }

    public int getMoon_phase() {
        return moon_phase;
    }

    public void setMoon_phase(double moon_phase) {
        this.moon_phase = moon_phase;
    }

    public int getSunset() {
        return sunset;
    }

    public void setSunset(int sunset) {
        this.sunset = sunset;
    }

    public Temp getTemp() {
        return temp;
    }

    public void setTemp(Temp temp) {
        this.temp = temp;
    }

    public FeelsLikeTemp getFeelsLike() {
        return feelsLikeTemp;
    }

    public void setFeelsLike(FeelsLikeTemp feelsLikeTemp) {
        this.feelsLikeTemp = feelsLikeTemp;
    }

    public int getPressure() {
        return pressure;
    }

    public void setPressure(int pressure) {
        this.pressure = pressure;
    }

    public int getHumidity() {
        return humidity;
    }

    public void setHumidity(int humidity) {
        this.humidity = humidity;
    }

    public double getDewPoint() {
        return dewPoint;
    }

    public void setDewPoint(double dewPoint) {
        this.dewPoint = dewPoint;
    }

    public double getWindSpeed() {
        return windSpeed;
    }

    public void setWindSpeed(double windSpeed) {
        this.windSpeed = windSpeed;
    }

    public int getWindDeg() {
        return windDeg;
    }

    public void setWindDeg(int windDeg) {
        this.windDeg = windDeg;
    }

    public double getWindGust() {
        return windGust;
    }

    public void setWindGust(double windGust) {
        this.windGust = windGust;
    }

    public List<Weather> getWeather() {
        return weather;
    }

    public void setWeather(List<Weather> weather) {
        this.weather = weather;
    }

    public int getClouds() {
        return clouds;
    }

    public void setClouds(int clouds) {
        this.clouds = clouds;
    }

    public double getPop() {
        return pop;
    }

    public void setPop(double pop) {
        this.pop = pop;
    }

    public double getRain() {
        return rain;
    }

    public void setRain(double rain) {
        this.rain = rain;
    }

    public double getUvi() {
        return uvi;
    }

    public void setUvi(double uvi) {
        this.uvi = uvi;
    }

    public int getVisibility() {
        return visibility;
    }

    public void setVisibility(int visibility) {
        this.visibility = visibility;
    }

    public double getSnow() {
        return snow;
    }

    public void setSnow(double snow) {
        this.snow = snow;
    }
}
