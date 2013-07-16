/**
 * This class is used to generate diary.
 * 
 * The generator will be called in DiaryService
 * and write sentence to diary. 
 * 
 * All diaries should be written into a html file
 * e.g. 11-20-2011.html
 * 
 * Technically, the generator will listen to a 
 * broadcast intent.
 * 
 **/

package com.smartdiary;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;

public class DiaryGenerator {

	// the dict.
	private List <Dictionary> dict = null;
	private MediaPool mp = null;
	private static Context context = null;
	private FileOutputStream dfos = null;
	private List <ActivityState> state = null;
	private List <ActivityLocation> loc = null;
	private List <NearbyPeople> people = null;
	private List <PreferredLocation> prefloc = null;
	private static String userPreference = null;
	
	private class ActivityState {
		public long time = 0;
		public int id = 0;
	}
	private class ActivityLocation {
		public long time = 0;
		public double lat = 0;
		public double lng = 0;
	}
	private class PreferredLocation {
		public double lat = 0;
		public double lng = 0;
		public String name = "";
	}
	private class NearbyPeople {
		public long time = 0;
		public String name = null;
	}
	
	private String path = null;
	
	/*
	 * Constructor.
	 * */
	public DiaryGenerator(Context ctx) {
		init_dict();
		init_preference();
		context = ctx;
		mp = new MediaPool(context);
		init_files();
		Date d = new Date();
		String [] x = d.toGMTString().split(" ");
		path = CommonData.baseDir + x[1] + " " + x[0] + " " + x[2];
		File f = new File(path);
		try {
			if (!f.exists()) f.createNewFile();
			dfos = new FileOutputStream(f, true);
		}
		catch (Exception e) {}
	}
	
	private void init_files() {
		state = new ArrayList<ActivityState>();
		loc = new ArrayList<ActivityLocation>();
		people = new ArrayList<NearbyPeople>();
		
		File f = new File(CommonData.activityStatePath);
		try {
			BufferedReader br = new BufferedReader(new FileReader(f));
			while (true) {
				String s = br.readLine();
				if (s == null) break;
				String [] x = s.split(":");
				ActivityState as = new ActivityState();
				as.time = Long.parseLong(x[0]);
				as.id = Integer.parseInt(x[1]);
				state.add(as);
			}
		} catch (Exception e) {}
		
		f = new File(CommonData.gpsPath);
		try {
			BufferedReader br = new BufferedReader(new FileReader(f));
			while (true) {
				String s = br.readLine();
				if (s == null) break;
				String [] x = s.split(":");
				if (x.length != 2) continue;
				String [] y = x[1].split(",");
				ActivityLocation al = new ActivityLocation();
				al.time = Long.parseLong(x[0]);
				al.lat = Double.parseDouble(y[0]);
				al.lng = Double.parseDouble(y[1]);
				loc.add(al);
			}
		} catch (Exception e) {}
		
		
		f = new File(CommonData.nearByPeoplePath);
		try {
			BufferedReader br = new BufferedReader(new FileReader(f));
			while (true) {
				String s = br.readLine();
				if (s == null) break;
				String [] x = s.split(":");
				if (x.length != 2) continue;
				NearbyPeople np = new NearbyPeople();
				np.time = Long.parseLong(x[0]);
				np.name = x[1];
				people.add(np);
			}
		} catch (Exception e) {}
		
	}
	
	private boolean init_dict() {
		dict = new ArrayList <Dictionary> ();
		File f = new File(CommonData.dictPath);
		if (!f.exists()) return false;
		try {
			BufferedReader br = new BufferedReader(new FileReader(f));
			String s = null;
			while (true) {
				s = br.readLine();
				if (s == null) break;
				String [] x = s.split("	"); // separated by a tab.
				if (x.length != 2) continue;
				Dictionary d = new Dictionary();
				d.key = x[0];
				d.sentence = x[1];
				dict.add(d);
			}
			return true;
		}
		catch (Exception e) {
			Log.e("dict", "not found.");
			e.printStackTrace();
			return false;
		}
	}
	
	// Initial the user preference, preferred location and life style.
	private void init_preference() {
		prefloc = new ArrayList<PreferredLocation>();
		File f = new File(CommonData.userpreferencePath);
		try {
			BufferedReader br = new BufferedReader(new FileReader(f));
			while (true) {
				String s = br.readLine();
				if (s == null) break;
				String [] x = s.split(":");
				if (x[0].equalsIgnoreCase("location")) {
					String [] y = x[1].split(",");
					PreferredLocation ploc = new PreferredLocation();
					ploc.name = y[2];
					ploc.lat = Double.parseDouble(y[0]);
					ploc.lng = Double.parseDouble(y[1]);
					prefloc.add(ploc);
				}
				else if (x[0].equalsIgnoreCase("preference")) {
					userPreference = x[1];
				}
			}
		}
		catch (Exception e) {}
	}
	
	/*
	 * Call generate(), get a sentence.
	 * */
	
	private String replaceWildcard(String s, String wildcard, String info) {
		String ans = "";
		String [] x = s.split(" ");
		for (String i: x) {
			if (i == wildcard)
				i = info;
			ans += i + " ";
		}
		return ans;
	}
	
	private String [] stateName = {"", "sit", "walk", "run", "drive"};
	
	// generate sentence with activity.
	public String createActivityStateSentence(int state, long begin_time, long change_time) {
		String key = stateName[state];
		List <String> group = new ArrayList <String> ();
		
		// retrieve all potential sentences by key.
		for (Dictionary d: dict)
			if (d.key.equalsIgnoreCase(key)) group.add(d.sentence);
				
		String time = fetchTimeString(begin_time, change_time);
		String location = fetchLocationString(begin_time, change_time);
		String people = fetchPeopleString(begin_time, change_time);
		String duration = fetchDurationString(begin_time, change_time);
		String distance = fetchDistanceString(begin_time, change_time);
		
		// for every potential sentence, check if resource
		// satisfied, then do replacement.
		List <Sentence> ans = new ArrayList <Sentence> ();
		for (String s: group) {
			
			int cnt = 0;
			if (s.indexOf("*") > 0) {
				if (time == null) continue;
				s = s.replace("*", time);
				++cnt;
			}
			if (s.indexOf("@") > 0) {
				if (location == null) continue;
				s = s.replaceAll("@", location);
				++cnt;
			}
			if (s.indexOf("#") > 0) {
				if (people == null) continue;
				s = s.replaceAll("#", people);
				++cnt;
			}
			if (s.indexOf("%") > 0) {
				if (duration == null) continue;
				s = s.replaceAll("%", duration);
				++cnt;
			}
			if (s.indexOf("$") > 0) {
				if (distance == null) continue;
				s = s.replaceAll("$", distance);
				++cnt;
			}
			Sentence ss = new Sentence();
			ss.nWildcards = cnt;
			ss.sentence = s;
			ans.add(ss);
		}
		
		// nothing was grabbed.
		if (ans.size() == 0) {
			System.out.println("No candidate sentences selected.");
			return null;
		}
		
		// sort answer by number of wildcards.
		Collections.sort(ans, new Comparator<Sentence>(){

			@Override
			public int compare(Sentence arg0, Sentence arg1) {
				return arg1.nWildcards - arg0.nWildcards;
			}});
		
		// random walk in sentence with the same amount of
		// wildcards.
		int x = ans.get(0).nWildcards;
		int t = 0;
		for (Sentence p: ans) {
			if (p.nWildcards == x) ++t;
		}
		
		Random r = new Random();
		String ret = ans.get(r.nextInt(t)).sentence;
		System.out.println(stateName[state] + " sentence:" + ret);
		return stateName[state] + ":" + ret + "\n";
	}
	
	// generate sentence of weather.
	public String createWeatherSentence() {
		MediaPool mp = new MediaPool(this.context);
		if (CommonData.lastGpsCheckin == null) return null;
		double lat = CommonData.lastLocCheckin.getLatitude();
		double lng = CommonData.lastLocCheckin.getLongitude();
		String [] key = mp.getCurrentWeather(lat, lng);
		
		List <String> group = new ArrayList <String> ();
		
		// retrieve all potential sentences by key.
		for (Dictionary d: dict) {
			if (d.key.equalsIgnoreCase(key[0])) group.add(d.sentence);
		}
		
		int n = group.size();
		if (n == 0) return null;
		
		Random r = new Random();
		return key[0] + ":" + group.get(r.nextInt(n)) + "\n";
	}
	
	// generate sentence of phone call statistics.
	public String createPhoneCallStatisitcSentence() {
		return null;
	}
	
	/*
	 * Core function.
	 * */
	public void generate() {
		String sentence = null;//this.createWeatherSentence();
		sentence = this.filter(sentence);
		if (sentence != null) {
			try {
				dfos.write(sentence.getBytes());
				dfos.flush();
			} catch (Exception e) {}
		}
		if (state.size() == 0) return;
		ActivityState as = state.get(0);
		int cnt = 0;
		for (ActivityState t: state) {
			if (t.id != as.id) {
				sentence = this.createActivityStateSentence(t.id, as.time, t.time);
				sentence = this.filter(sentence);
				if (sentence != null) {
					try {
						dfos.write(sentence.getBytes());
						dfos.flush();
					} catch (Exception e) {}
				}
				as = t;
				++cnt;
			}
		}
		System.out.println("Sentence #:" + String.valueOf(cnt));
		try {
			dfos.close();
		} catch (Exception e) {}
		
		
		try {
			FileOutputStream lfos = new FileOutputStream(CommonData.diarylistPath, true);
			lfos.write((path.subSequence(path.lastIndexOf("/")+1, path.length())+"\n").getBytes());
			lfos.flush();
			lfos.close();
		} catch (Exception e) {}
		// clear file. It's disabled in debug mode.
		/*
		try {
			File f = new File(CommonData.activityStatePath);
			f.delete();
			f.createNewFile();
			f = new File(CommonData.gpsPath);
			f.delete();
			f.createNewFile();
			f = new File(CommonData.nearByPeoplePath);
			f.delete();
			f.createNewFile();
		} catch (Exception e) {}*/
		
		// broadcast intent to inform diarylist refresh.
		Intent intent = new Intent();
		intent.putExtra("list-refresh", "ok");
		context.sendBroadcast(intent);
	}
	
	// try to fill *.
	private String fetchTimeString(long begin_time, long change_time) {
		long t = (begin_time + change_time) / 2;
		Calendar c = new GregorianCalendar();
		c.setTimeInMillis(t);
		int h = c.get(Calendar.HOUR_OF_DAY);
		h -= 5; // change time zone.
		if (h < 2) return "mid-night";
		else if (h <= 7) return "early morning";
		else if (h < 12) return "morning";
		else if (h < 14) return "noon";
		else if (h < 19) return "afternoon";
		else if (h < 21) return "evening";
		else if (h < 24) return "late night";
		return null;
	}
	
	// try to fill @.
	private String fetchLocationString(long begin_time, long change_time) {
		
		// First check user preference.
		double dist = 1e6;
		String name = "";
		for (int i = loc.size()-1; i >= 0; --i) {
			if (loc.get(i).time < begin_time) break;
			if (loc.get(i).time < change_time) {
				for (PreferredLocation pl: prefloc) {
					double t = getGeoDistance(loc.get(i).lat, loc.get(i).lng, pl.lat, pl.lng);
					if (t < dist) {
						dist = t;
						name = pl.name;
					}
				}
			}
		}
		/*if (dist < 5.0/1000) {
			System.out.println(dist*1000);
			return name;
		}*/
		
		// Not available, then goes to geocoder.
		for (int i = loc.size()-1; i >= 0; --i) {
			if (loc.get(i).time < begin_time) break;
			if (loc.get(i).time < change_time) {
				Address addr = mp.getAddressFromLocation(String.valueOf(loc.get(i).lat), String.valueOf(loc.get(i).lng));
				if (addr != null) {
					String str = "<";
					if (addr.getFeatureName() != null) str += addr.getFeatureName() + ",";
					if (addr.getLocality() != null) str += addr.getLocality() + ",";
					if (addr.getThoroughfare() != null) str += addr.getThoroughfare() + ",";
					if (addr.getAddressLine(0) != null) str += addr.getAddressLine(0) + ",";
					if (addr.getSubThoroughfare() != null) str += addr.getSubThoroughfare() + ">";
					return str;
				}
			}
		}
		return null;
	}
	
	// try to fill #.
	private String fetchPeopleString(long begin_time, long change_time) {
		
		return null;
	}
	
	// try to fill %.
	private String fetchDurationString(long begin_time, long change_time) {
		long t = change_time - begin_time;
		t /= 1000;
		if (t < 60) return "one minute";
		else if (t < 3600) return String.valueOf(t/60) + " minutes";
		else if (t < 3600 * 2) return "one hour";
		else return String.valueOf(t/3600) + " hours";
	}
	
	// try to fill $.
	private String fetchDistanceString(long begin_time, long change_time) {
		double distance = 0;
		double lat = 0;
		double lng = 0;
		for (int i = loc.size()-1; i >= 0; --i) {
			if (loc.get(i).time < begin_time) break;
			if (loc.get(i).time < change_time) {
				if (lat == 0 || lng == 0) {
					lat = loc.get(i).lat;
					lng = loc.get(i).lng;
				}
				else {
					distance += getGeoDistance(lat, lng, loc.get(i).lat, loc.get(i).lng);
				}
			}
		}
		distance *= 0.62; // change to mile.
		if (distance < 0.25) return "a quater mile";
		else if (distance < 0.5) return "half a mile";
		else if (distance < 1) return "a mile";
		else return String.valueOf((int)distance) + " miles";
	}
	
	private double getGeoDistance(double lat1, double lng1, double lat2, double lng2) {
		double R = 6371;
		lat1 = Math.toRadians(lat1);
		lng1 = Math.toRadians(lng1);
		lat2 = Math.toRadians(lat2);
		lng2 = Math.toRadians(lng2);
		double a = (lng2 - lng1) * Math.cos((lat1 + lat2) / 2);
		double b = (lat2 - lat1);
		return Math.sqrt(a*a + b*b)*R;
	}
	
	/*
	 * If passed, return sentence, otherwise null
	 * will be returned.
	 * 
	 * By default, no filter needed.
	 * */
	public String filter(String sentence) {
		
		return sentence;
	}
	
	private class Dictionary {
		public String key = null;
		public String sentence = null;
	}
	
	private class Sentence {
		public int nWildcards = 0;
		public String sentence = null;
	}
}
