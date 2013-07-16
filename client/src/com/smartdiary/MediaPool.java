/**
 * 
 * Media pool is the place to pull interesting news
 * from out source information. For example, weather
 * data, significant news, plans and emails. All of
 * these information is from surroundings.
 * 
 * **/

package com.smartdiary;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.util.Log;


public class MediaPool {

	private static Context ctx = null;
	
	public MediaPool(Context c) {
		this.ctx = c;
	}
	
	/*
     * This method is to extract weather information from media pool.
     * 
     * The return string array means the following:
     * 		- [0] condition
     * 		- [1] temp_f
     * 		- [2] temp_c
     * 		- [3] humidity
     * 		- [4] wind
  	 *
     */
 	public String [] getCurrentWeather(double lat, double lng) {
     	final String q = String.format(CommonData.weatherQuery, "", "", "", 
     			String.valueOf(lat).replace(".", "").subSequence(0, 8), 
     			String.valueOf(lng).replace(".", "").subSequence(0, 9));
     	URL url = null;
 		try {
 			url = new URL(q);
 		} catch (MalformedURLException e) {
 			Log.e("url", "weather request failed.");
 			e.printStackTrace();
 		}
 		SAXParserFactory spf = SAXParserFactory.newInstance();
 		SAXParser sp = null;
 		try {
 			sp = spf.newSAXParser();
 		} catch (Exception e) {
 			Log.e("xml", "create parser failed.");
 			e.printStackTrace();
 		}
 		XMLReader xr = null;
 		try {
 			xr = sp.getXMLReader();
 		} catch (Exception e) {
 			Log.e("xml", "create reader failed.");
 			e.printStackTrace();
 		}
 		WeatherHandler wh = new WeatherHandler();
 		try {
 			xr.setContentHandler(wh);
 		} catch (Exception e) {
 			Log.e("xml", "set parser failed.");
 			e.printStackTrace();
 		}
 		try {
 			xr.parse(new InputSource(url.openStream()));
 		} catch (Exception e) {
 			Log.e("xml", "open weather stream");
 			e.printStackTrace();
 		}
 		return wh.getWeatherDescription();
     }
 	
 	/*
 	 * 
 	 * Weather XML parser.
 	 * */
 	
 	private class WeatherHandler extends DefaultHandler {
 		private String [] curWeatherDescription = null;
 		private boolean inCurrentCondition = false;
 		
 		public String [] getWeatherDescription() {
 			return this.curWeatherDescription;
 		}
 		
 		public WeatherHandler() {
 			this.curWeatherDescription = new String[5];
 		}
 		
 		@Override
 		public void startDocument() throws SAXException {
 			super.startDocument();
 			curWeatherDescription = new String[5];
 		}
 		
 		@Override
 		public void startElement(String uri, String localname, 
 				String name, Attributes attr) throws SAXException {
 			super.startElement(uri, localname, name, attr);
 			if (localname.equalsIgnoreCase("current_conditions"))
 				this.inCurrentCondition = true;
 			else if (localname.equalsIgnoreCase("weather") || 
 					localname.equalsIgnoreCase("forecast_information") ||
 					localname.equalsIgnoreCase("forecast_conditions"))
 				this.inCurrentCondition = false;
 			else if (localname.equalsIgnoreCase("condition") && 
 					this.inCurrentCondition)
 				this.curWeatherDescription[0] = attr.getValue("data");
 			else if (localname.equalsIgnoreCase("temp_f") &&
 					this.inCurrentCondition)
 				this.curWeatherDescription[1] = attr.getValue("data");
 			else if (localname.equalsIgnoreCase("temp_c") && 
 					this.inCurrentCondition)
 				this.curWeatherDescription[2] = attr.getValue("data");
 			else if (localname.equalsIgnoreCase("humidity") && 
 					this.inCurrentCondition)
 				this.curWeatherDescription[3] = attr.getValue("data");
 			else if (localname.equalsIgnoreCase("wind_condition") &&
 					this.inCurrentCondition)
 				this.curWeatherDescription[4] = attr.getValue("data");
 		}
 		
 		@Override
 	    public void endElement(String uri, String localname, String name)
 	            throws SAXException {
 	        super.endElement(uri, localname, name);
 		}
 	}
 	
 	// this reversely get address information from the current location.
 	public Address getAddressFromLocation(String lat, String lng) {
 		if (Geocoder.isPresent() == false) {
 			//Toast.makeText(getApplicationContext(), "Phone does not support Geocoding", Toast.LENGTH_LONG).show();
 			return null;
 		}
 		try {
 			Geocoder geocoder = new Geocoder(ctx.getApplicationContext(), Locale.getDefault());
 			List <Address> list = geocoder.getFromLocation(Double.parseDouble(lat), Double.parseDouble(lng), 1);
 			if (list != null && list.size() > 0) return list.get(0);
 		}
 		catch (Exception e)	{
 			Log.e("loc2addr", "failed.");
 			e.printStackTrace();
 		}
 		return null;
 	}
}