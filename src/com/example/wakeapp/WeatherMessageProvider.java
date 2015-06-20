package com.example.wakeapp;

import org.json.JSONArray;
import org.json.JSONObject;

public class WeatherMessageProvider extends messageProvider {
	private double latitude;
	private double longitude;
	public WeatherMessageProvider(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
		
		URL = "http://api.openweathermap.org/data/2.5/weather?lat=" + latitude + "&lon=" + longitude;
		name = "Weather";
	}
	@Override
	public String getMessage(String messageProviderMessage) {
		final String description;
		final double temperatureKelvin;
		final double temperatureCelsius;
		final String city;
		try {
			final JSONObject data = new JSONObject(messageProviderMessage);
			final JSONArray weather = data.getJSONArray("weather");
			description = weather.getJSONObject(0).getString("description");
			final JSONObject main = data.getJSONObject("main");
			temperatureKelvin = main.getDouble("temp");
			temperatureCelsius = (double)Math.round((temperatureKelvin - 273.15) * 100) / 100;
			city = data.getString("name");
		}
		catch(Exception e) {
			return e.getMessage();
		}
		
		return name + ":\n" + description + "\n Temperature" + (city == "Earth" ? "" : " in " + city) + " is: " + temperatureCelsius + "°C\n";
	}

}
