/**
 * The kernel of smart diary.
 * 
 * All other parts should be called in this field.
 **/

package com.smartdiary;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Arrays;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class DiaryService extends Service {
	
	private int activityNum = 4;
	//private int[] activity = {0,1,2,3};
	private String[] activityString = {"sit", "walk", "run", "drive"};
	//first layer activity recognition
	private int shortInterval = 30; // each activity is classified every 30 seconds
	//second layer activity inference
	private int windowSize = 10; 
	private int throwSize = 10;  // throw how many activities
	private LinkedList<Integer> firstActivityQueue = new LinkedList<Integer>();//store activities generated every 30 seconds
	private LinkedList<Integer> secondActivityQueue = new LinkedList<Integer>();// store activities generated every 300 seconds
	//if we use Euclidean distance, following is the centers
	//the standard sequence for different activities
	//Note that the actual lenght of the the center should be decided by the windowSize
	private double[] sitCentroidInit = {0.03309692671394799,0.030732860520094562,0.028368794326241134,0.028368794326241134,0.028368794326241134,0.028368794326241134,0.028368794326241134,0.028368794326241134,0.028368794326241134,0.028368794326241134};
	private double[] driveCentroidInit={0.9871382636655949,0.9871382636655949,0.9871382636655949,0.9839228295819936,0.9807073954983923,0.9807073954983923,0.9807073954983923,0.9807073954983923,0.9807073954983923,0.9807073954983923};
	private double[] walkCentroidInit ={1.937799043062201,1.937799043062201,1.937799043062201,1.937799043062201,1.937799043062201,1.937799043062201,1.937799043062201,1.937799043062201,1.937799043062201,1.937799043062201};
	private double[] runCentroidInit = {2.838709677419355,2.838709677419355,2.838709677419355,2.838709677419355,2.838709677419355,2.838709677419355,2.838709677419355,2.838709677419355,2.838709677419355,2.838709677419355};
    private double[] sitCentroid = new double[windowSize];
    private double[] driveCentroid = new double[windowSize];
    private double[] walkCentroid = new double[windowSize];
    private double[] runCentroid = new double[windowSize];
    
	private boolean timerFired = false; //trigger a timer for first level classification
	
	static public NfcMessenger nfcMsger = null;
	
	@Override
	public void onCreate() {
		super.onCreate();
		init_f();
		mSensorMgr = new CommonSensorManager();
		nfcMsger = new NfcMessenger(this);
		mSensorMgr.startSensor();	
		startFineLocationService();		
		//timer for firse level classification
		Timer activityTimer = new Timer();
		activityTimer.scheduleAtFixedRate(new changeFile(), shortInterval*1000, shortInterval*1000);
		//get the centoids of four activities
		sitCentroid = getCentroid(sitCentroidInit, windowSize);
		driveCentroid = getCentroid(driveCentroidInit, windowSize);
		walkCentroid = getCentroid(walkCentroidInit, windowSize);
		runCentroid = getCentroid(runCentroidInit, windowSize);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		stopFineLocationService();
		mSensorMgr.stopSensor();
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	
	// initiate some files.
	private void init_f() {
		try {
			File f = new File(CommonData.baseDir);
			if (!f.exists()) f.mkdirs();
			f = new File(CommonData.accPath);
			if (!f.exists()) f.createNewFile();
			f = new File(CommonData.diarylistPath);
			if (!f.exists()) f.createNewFile();
			f = new File(CommonData.gpsPath);
			if (!f.exists()) f.createNewFile();
			//f = new File(CommonData.gyroPath);
			//if (!f.exists()) f.createNewFile();
			//f = new File(CommonData.lightPath);
			//if (!f.exists()) f.createNewFile();
			f = new File(CommonData.userpreferencePath);
			if (!f.exists()) f.createNewFile();
			f = new File(CommonData.activityStatePath);
			if (!f.exists()) f.createNewFile();
		}
		catch (Exception e) {
			Log.e("init files", "failed.");
			e.printStackTrace();
		}
	}
	
	/*
	 * --------------------------------------------------------
	 * This part is to do location service.
	 * */
	private LocationManager mLocationMgr = null;
	static private FileOutputStream gpsfos = null;
    
	private LocationListener coarseListener = new LocationListener() {

		public void onLocationChanged(Location loc) {
			CommonData.lastLocCheckin = loc;
			//CommonData.lastAddrCheckin = getAddressFromLocation(loc);
			CommonData.lastGpsCheckin[0] = loc.getLatitude();
			CommonData.lastGpsCheckin[1] = loc.getLongitude();
			CommonData.lastGpsCheckin[2] = loc.getSpeed();
			CommonData.lastGpsCheckin[3] = loc.getAccuracy();
		}

		public void onProviderDisabled(String provider) {}

		public void onProviderEnabled(String provider) {}

		public void onStatusChanged(String arg0, int arg1, Bundle arg2) {}
		
	};
	
	private LocationListener fineListener = new LocationListener() {

		public void onLocationChanged(Location loc) {
			CommonData.lastLocCheckin = loc;
			//CommonData.lastAddrCheckin = getAddressFromLocation(loc);
			CommonData.lastGpsCheckin[0] = loc.getLatitude();
			CommonData.lastGpsCheckin[1] = loc.getLongitude();
			CommonData.lastGpsCheckin[2] = loc.getSpeed();
			CommonData.lastGpsCheckin[3] = loc.getAccuracy();
			
			/*try {
				Date d = new Date();
				String s = String.valueOf(d.getTime()) + "," 
						+ String.valueOf(loc.getLatitude()) + ","
						+ String.valueOf(loc.getLongitude()) + "\n";
				gpsfos.write(s.getBytes());
			}
			catch (Exception e) {
				Log.e("loc", "logging failed.");
				e.printStackTrace();
			}*/
			//String [] x = mp.getCurrentWeather(CommonData.lastAddrCheckin);
			//Toast.makeText(getApplicationContext(), x[0], Toast.LENGTH_LONG).show();
		}

		public void onProviderDisabled(String provider) {}

		public void onProviderEnabled(String provider) {}

		public void onStatusChanged(String arg0, int arg1, Bundle arg2) {}
		
	};
	
	private Criteria createCoarseCriteria() {
		Criteria c = new Criteria();
		c.setAccuracy(Criteria.ACCURACY_COARSE);
		c.setAltitudeRequired(false);
		c.setSpeedRequired(false);
		c.setCostAllowed(true);
		c.setPowerRequirement(Criteria.POWER_LOW);
		return c;
	}
	
	private Criteria createFineCriteria() {
		Criteria c = new Criteria();
		c.setAccuracy(Criteria.ACCURACY_FINE);
		c.setAltitudeRequired(false);
		c.setSpeedRequired(false);
		c.setCostAllowed(true);
		c.setPowerRequirement(Criteria.POWER_HIGH);
		return c;
	}
	
	private boolean startCoarseLocationService() {
		mLocationMgr = (LocationManager) getSystemService(LOCATION_SERVICE);
		LocationProvider locp = mLocationMgr.getProvider(mLocationMgr.getBestProvider(createCoarseCriteria(), true));
		if (locp != null) {
			mLocationMgr.requestLocationUpdates(locp.getName(), 0, 0, coarseListener);
			return true;
		}
		else return false;
	}
	
	private void stopCoarseLocationService() {
		mLocationMgr = (LocationManager) getSystemService(LOCATION_SERVICE);
		mLocationMgr.removeUpdates(coarseListener);
	}
	
	private boolean startFineLocationService() {
		try {
			gpsfos = new FileOutputStream(CommonData.gpsPath, true);
		}
		catch (Exception e) {
			Log.e("loc", "start location failed.");
			e.printStackTrace();
		}
		mLocationMgr = (LocationManager) getSystemService(LOCATION_SERVICE);
		LocationProvider locp = mLocationMgr.getProvider(mLocationMgr.getBestProvider(createFineCriteria(), true));
		if (locp != null) {
			mLocationMgr.requestLocationUpdates(locp.getName(), 0, 0, fineListener);
			return true;
		}
		else return false;
	}
	
	private void stopFineLocationService() {
		try {
			gpsfos.close();
		}
		catch (Exception e) {
			Log.e("loc", "start location failed.");
			e.printStackTrace();
		}
		mLocationMgr = (LocationManager) getSystemService(LOCATION_SERVICE);
		mLocationMgr.removeUpdates(fineListener);
	}
	
	// logging location.
	private void logLocation(Location loc) {
		if (loc == null) return;
		try {
			Date d = new Date();
			String s = String.valueOf(d.getTime()) + ":" 
					+ String.valueOf(loc.getLatitude()) + ","
					+ String.valueOf(loc.getLongitude()) + "\n";
			gpsfos.write(s.getBytes());
			gpsfos.flush();
		}
		catch (Exception e) {
			Log.e("loc", "location output file error.");
			e.printStackTrace();
		}
	}
	
	/*
	 * -----------------------------------------------------------------
	 * This part is for common sensor.
	 * */
	private CommonSensorManager mSensorMgr = null;
	
    private class CommonSensorManager implements SensorEventListener {
    	
    	private SensorManager mSensorManager;
    	
    	private Sensor Accelerometer;    	

    	private final String accelerometerFile = CommonData.accPath;    	
    	
    	private FileOutputStream accelerometerFos = null;
    	
    	private FileOutputStream asfos = null;
    	    	
    	public CommonSensorManager()
    	{
    		this.mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
    		this.Accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
    		
    		File f = new File(CommonData.baseDir);
    		if (!f.exists()) f.mkdirs();
    		try {
	    		f = new File(this.accelerometerFile);
	    		if (!f.exists()) f.createNewFile();
    		} catch (Exception e) {}
    	}
    	
    	// Start sensing
    	public void startSensor() {
    		mSensorManager.registerListener(this, this.Accelerometer, SensorManager.SENSOR_DELAY_NORMAL);    		
    		try {
	    		this.accelerometerFos = new FileOutputStream(this.accelerometerFile, true);	
	    		this.asfos = new FileOutputStream(CommonData.activityStatePath, true);
    		} catch (Exception e) {}
    	}
    	
    	// Stop sensing
    	public void stopSensor() {
    		mSensorManager.unregisterListener(this, this.Accelerometer);    		
    		try {
	    		this.accelerometerFos.close();
	    		this.asfos.close();
    		} catch (Exception e) {}
    	}
    	
    	private int delay = 0;
    	
    	public void onSensorChanged(SensorEvent event) {
    		
    		if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
    			
    			++delay;
    			// log location.
    			if (delay == 5) {
    				logLocation(CommonData.lastLocCheckin);
    				delay = 0;
    			}
    			
    			Date d = new Date();
				String str = String.valueOf(d.getTime())+":"+String.valueOf(event.values[0]) + "," 
						+ String.valueOf(event.values[1]) + "," + String.valueOf(event.values[2]) + "\n";
				byte [] buf = str.getBytes();
				try {
	    			this.accelerometerFos.write(buf);
	    			this.accelerometerFos.flush();
				} catch (Exception e) {
					Log.e("acc", "output file error.");
					e.printStackTrace();
				}
				
				//perform activity recognition
				if (timerFired == true){
					timerFired = false;
					try {
						accelerometerFos.close(); //close existing file
					} catch (Exception e) {	}
					
					File accfile = new File(accelerometerFile);					
					//first level activity recognition for current file (every 30 seconds)
    				int shortActivity = activityClassify(accfile);
    				//create another new file for saving acclerometer readings
					try {
    					accfile.createNewFile();
    					accelerometerFos = new FileOutputStream(accfile, true);
    				} catch (Exception e) {}
    				
    				firstActivityQueue.add(shortActivity);
    				System.out.println("1st layer: " + String.valueOf(shortActivity));
    				//second level activity recognition (when the sequence length is about windowSize)
    				if (firstActivityQueue.size()==windowSize){
        				int[] queueCopy = new int[windowSize]; 
    					// copy the 
    					for (int i=0; i<throwSize; i++){
    						queueCopy[i] = firstActivityQueue.pollFirst();
    					}
    					for (int i=throwSize; i<windowSize; i++){
    						queueCopy[i] = firstActivityQueue.get(i-throwSize);
    					}
    					//Euclidean method to do the second layer activity inference
	    				int secondActivity = activityStateAssign(queueCopy); // this is the activity classified at the second level
	    				secondActivityQueue.add(secondActivity); //add it into a new queque
	    				
	    				// write the activity state into file.
	    				String s = String.valueOf(d.getTime()) + ":" + String.valueOf(secondActivity) + "\n";
	    				try {
							asfos.write(s.getBytes());
							asfos.flush();
						} catch (IOException e) {
							Log.e("state", "write activity state failed.");
							e.printStackTrace();
						}
	    				System.out.println("2nd layer: " + String.valueOf(secondActivity));
    				}
    			}					
			}
    	}
    	
    	public void onAccuracyChanged(Sensor sensor, int accuracy) {
        
    	}
    }
    
    /*
	 * -----------------------------------------------------------------
	 * This part is for activity classification
	 * */
    /** 
	 * mean of samples
	 */
	public double getMean(double[] val){		
		double mean= 0.0;		
		int len = val.length;
		if (len>0){
			for (int i=0; i<len; i++){
				mean += val[i];
			}
			mean = mean/len;
		}else{
			mean = 0.0;
		}
		return mean;
	}
	
	/**
	 *  standard deviation of samples
	 */
	
	public double getStd(double[] val){
		double std = 0.0;
		double sum = 0.0;
		double mean = 0.0;
		int len = val.length;
		if (len<=1){
			std = 0.0;
		}else{
			mean = this.getMean(val);
			for (int i=0; i<len; i++){
				sum  += (val[i]-mean)*(val[i]-mean);
			}
			std = Math.sqrt(sum/(len-1));
		}		
		return std;
	}
	

	/**
	 * set a timer
	 */
	public class changeFile extends TimerTask{
		@Override
		public void run() {
			timerFired = true;
		}		
	}
	
	/**
	 * Euclidean distance between two arrays
	 * @param x
	 * @param y
	 * @return Euclidean distance
	 */
	public double getArrayDistance(int[] x, double[] y){
		double sum = 0.0;
		double dist = 0.0;
		int xsize = x.length;
		int ysize = y.length;		
		for (int i=0; i<xsize; i++)
			sum += (x[i]-y[i])*(x[i]-y[i]);
		dist = Math.sqrt(sum);
		return dist;
	}
    
	/**
	 * Decision tree classifier
	 * @param stdx
	 * @param stdy
	 * @param stdz
	 * @return 1(sit)/2(walk)/3(run)/4(drive)
	 */
	public static int J48(double stdx, double stdy, double stdz){ //not the real classifier
		int act = 0;
		if (stdz<=0.098115){
			if (stdz<=0.041492){
				act = 0;
				return act;
			}else{
				if (stdx<=0.12888){
					if (stdy<=0.047515){
						if (stdx<=0.036547){
							act = 0;
							return act;
						}
						else{
							if (stdy<=0.03895){
								if (stdx<=0.04713){
									act = 2;
									return act;
								}else{
									act = 1;
									return act;
								}
							}else{
								if (stdz<=0.054171){
									act = 0;
									return act;
								}else{
									if (stdx<=0.054778){
										act = 1;
										return act;
									}else{
										act = 2;
										return act;
									}
								}
							}
						}
					}else{
						if (stdx<=0.046669){
							if (stdy<=0.069072){
								act = 0;
								return act;
							}else{
								act = 1;
								return act;
							}
						}else{
							if (stdz<=0.080164){
								if (stdy<=0.11609){
									act = 0;
									return act;
								}else{
									if (stdx<=0.067627){
										if (stdx<=0.05437){
											if (stdy<=0.14921){
												act = 2;
												return act;
											}else{
												act = 1;
												return act;
											}
										}else{
											act = 1;
											return act;
										}
									}else{
										act = 0;
										return act;
									}
								}
							}else{
								if (stdx<=0.078426){
									act = 1;
									return act;
								}else{
									if (stdy<=0.078774){
										act = 1;
										return act;
									}else{
										act = 0;
										return act;
									}
								}
							}
						}						
					}
				}else{
					if (stdy<=0.095444){
						act = 1;
						return act;
					}else{
						act = 2;
						return act;
					}
				}				
			}
		}else{
			if (stdx<=0.4155){
				if (stdz<=0.17787){
					if (stdx<=0.2974){
						if (stdy<=0.2449){
							act = 1;
							return act;
						}else{
							if (stdx<=0.2098){
								act = 0;
								return act;
							}else{
								act = 1;
								return act;
							}
						}
					}else{
						if (stdx<=0.32364){
							act = 1;
							return act;
						}else{
							act = 2;
							return act;
						}
					}
				}else{
					if (stdy<=0.28381){
						act = 1;
						return act;
					}else{
						if (stdz<=0.42672){
							if (stdz<=0.29842){
								if (stdx<=0.36763){
									act = 1;
									return act;
								}else{
									if (stdx<=0.37765){
										act = 0;
										return act;
									}else{
										act = 2;
										return act;
									}
								}
							}else{
								act = 1;
								return act;
							}
						}else{
							if (stdz<=0.52775){
								if (stdx<=0.36346){
									if (stdz<=0.48435){
										if (stdy<=0.51535){
											if (stdx<=0.3097){
												act = 2;
												return act;
											}else{
												if (stdz<=0.46729){
													act = 1;
													return act;
												}else{
													act = 2;
													return act;
												}
											}
										}else{
											act = 1;
											return act;
										}
									}else{
										act = 1;
										return act;
									}
								}else{
									act = 2;
									return act;
								}
							}else{
								act = 1;
								return act;
							}
						}
					}
				}
			}else{
				if (stdx<=5.9759){
					if (stdy<=4.8691){
						if (stdz<=1.2358){
							if (stdz<=0.50991){
								if (stdx<=0.41906){
									if (stdx<=0.41744){
										act = 2;
										return act;
									}else{
										act = 0;
										return act;
									}
								}else{
									act = 1;
									return act;
								}
							}else{
								act = 2;
								return act;
							}
						}else{
							if (stdx<=3.6991){
								act = 2;
								return act;
							}else{
								if (stdz<=3.0212){
									act = 3;
									return act;
								}else{
									act = 2;
									return act;
								}
							}
						}
					}else{
						if (stdy<=5.5904){
							if (stdx<=3.1009){
								act = 2;
								return act;
							}else{
								act = 3;
								return act;
							}
						}else{
							act = 3;
							return act;
						}
					}
				}else{
					act = 3;
					return act;
				}
			}
		}
	}
	
	/**
	 * process samples in a file and classify its activity
	 */
	public int activityClassify(File f){
		int codeActivity = 0;
		BufferedReader br = null;
		ArrayList<String> accxList = new ArrayList<String>(); //list for saving stdx
		ArrayList<String> accyList = new ArrayList<String>(); //list for saving stdy
		ArrayList<String> acczList = new ArrayList<String>(); //list for saving stdz
		try {
			br = new BufferedReader(new FileReader(f));
		} catch (Exception e) {	}
		String readString = null;
		try {
			while ((readString = br.readLine()) != null) {
				String accxyz = readString.split(":")[1];
				accxList.add(accxyz.split(",")[0]);
				accyList.add(accxyz.split(",")[1]);
				acczList.add(accxyz.split(",")[2]);
			}
			br.close();
			f.delete(); //delete the file
			//put the values in list to array for processing 
			//double[] timeArray = new double[timeList.size()];
			double[] accxArray = new double[accxList.size()];
			double[] accyArray = new double[accyList.size()];
			double[] acczArray = new double[acczList.size()];
			for (int i=0; i<accxList.size(); i++){
				accxArray[i] = Double.parseDouble(accxList.get(i));
			}						
			for (int i=0; i<accyList.size(); i++){
				accyArray[i] = Double.parseDouble(accyList.get(i));
			}
			for (int i=0; i<acczList.size(); i++){
				acczArray[i] = Double.parseDouble(acczList.get(i));
			}
			double accx = getStd(accxArray);
			double accy = getStd(accyArray);
			double accz = getStd(acczArray);
			int currentActivity = J48(accx, accy, accz);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return codeActivity;
	}
	
	
	
	/**
	 * second layer activity assignment
	 * assign an activity sequence into one activity
	 */
	public int activityStateAssign(int[] sequence){ 
		double dist0 = getArrayDistance(sequence, sitCentroid);
		double dist1 = getArrayDistance(sequence, driveCentroid);
		double dist2 = getArrayDistance(sequence, walkCentroid);
		double dist3 = getArrayDistance(sequence, runCentroid);
		double[] distInit = {dist0, dist1, dist2, dist3};
		double[] distSort = {dist0, dist1, dist2, dist3};
		//just use existing sort algorithm
		Arrays.sort(distSort);
		int act = 4;
		for (int i=0; i<4; i++){
			act = i;
			if (distSort[0]==distInit[i]){
				break;				
			}			
		}
		return act;
	}	
    
	/**
	 * get the centroid of four activities
	 * these centroids will be used at the second layer activity inference
	 */
	public double[] getCentroid(double[] centroidInit, int windowSize){
		double[] centroid = new double[windowSize];
		for (int i=0; i<windowSize; i++){
			centroid[i] = centroidInit[i];
		}
		return centroid;
	}
	
	
	
}