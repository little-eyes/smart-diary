/**
 * This class is used to store some global variable,
 * like dictionary server's address, diary buffer,
 * sensor data and all other variables is globally used.
 * 
 * All area should be defined as "static public".
 * 
 **/

package com.smartdiary;

import java.util.HashMap;
import java.util.Map;

import android.location.Address;
import android.location.Location;
import android.os.Environment;

public class CommonData {

	// remote dict server address.
	static final public String dictServerAddr = "www.todaymeal.com";
	
	// app base directory.
	static final public String baseDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/smart-diary/";
		
	// important files.
	static final public String diarylistPath = baseDir + "diary.list";
	static final public String dictPath = baseDir + "dict.dat";
	static final public String gpsPath = baseDir + "gps-trace.dat";
	static final public String accPath = baseDir + "accelerometer.dat";
	static final public String gyroPath = baseDir + "gyroscope.dat";
	static final public String lightPath = baseDir + "light.dat";
	
	// last check-in location.
	static public double [] lastGpsCheckin = new double[4];
	static public Location lastLocCheckin = null;
	
	// gps check frequence. default: 1s.
	static final public int gpsCheckFrequence = 1000;
	
	// query url to google weather.
	static final public String weatherQuery = "http://www.google.com/ig/api?weather=%s,%s,%s,%s,%s";
	static public String [] currentWeather = null;
	
	// query url to google geocoder.
	static final public String geocodeQuery = "http://maps.googleapis.com/maps/api/geocode/xml?latlng=%s,%s&sensor=true";
	
	//static public Map <String, String> nearbyPeople = new HashMap <String, String> ();
	static public String nearbyPeople = null;
	static public String activityStateBeginTime = null;
	static public String activityStateChangeTime = null;
	
	// activity state machine.
	static public String previousState = null;
	
	// user preference path.
	static final public String userpreferencePath = baseDir + "user-preference.dat";
	
	// activity state path.
	static final public String activityStatePath = baseDir + "activity-state.dat";
	
	// nearby people path.
	static final public String nearByPeoplePath = baseDir + "nearby-people.dat";
}
