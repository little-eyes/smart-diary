/**
 * 
 * Motion recognize is used to output a series of
 * motion code from sensor raw data. Motion is same
 * as activity defined in the paper, here I use motion
 * only to differentiate the name from Android's
 * Activity.
 * 
 * Basically, we want to recognize four motion activities:
 * sitting/walking/running/driving
 * **/

package com.smartdiary;

public class MotionRecognize {
	
	private double stdx;
	private double stdy;
	private double stdz;
	
	public MotionRecognize(double _stdx, double _stdy, double _stdz){
		stdx = _stdx;
		stdy = _stdy;
		stdz = _stdz;
	}
	
	/**
	 * Decision tree classifier
	 * @param stdx
	 * @param stdy
	 * @param stdz
	 * @return 1(sit)/2(walk)/3(run)/4(drive)
	 */
	public int J48(double stdx, double stdy, double stdz){ //not the real classifier
		int act = 1;
		if (stdz<=0.3){
			if (stdx<0.2){
				act = 1;
				return act;
			}else{
				act = 4;
				return act;
			}
		}else{
			if (stdy<=4.0){
				act = 2;
				return act;
			}else{
				act = 3;
				return act;
			}				
		}
	}	

}
