package com.smartdiary;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Bundle;
import android.os.Vibrator;
import android.content.Context;
import android.content.Intent;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;


public class UserPreference extends Activity{

	private Button checkBtn = null;
	private Spinner spinner = null;
	private Button submitBtn = null;
	private int preferenceId = 0;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.userpreference);
        checkBtn = (Button)findViewById(R.id.checkin);
        submitBtn = (Button)findViewById(R.id.submit);
        
        spinner = (Spinner) findViewById(R.id.lifestyle);
        ArrayAdapter<CharSequence> mAdapter = ArrayAdapter.createFromResource(
        		this, R.array.preference_array, android.R.layout.simple_spinner_item);
        mAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(mAdapter);
        
        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				preferenceId = arg2;
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				preferenceId = 0;
			}});
        
        checkBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				//get the latest GPS location

				double lat = CommonData.lastGpsCheckin[0];
				double lng = CommonData.lastGpsCheckin[1];
				
				String location = "LOCATION:"+String.valueOf(lat)+","+String.valueOf(lng);
				showLocationEdit(location);				
			}        	
        });
        
        submitBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (preferenceId==0){
					Toast.makeText(getApplicationContext(), "You should specify your preference!!", Toast.LENGTH_LONG).show();
                    Vibrator vbr = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    vbr.vibrate(500);
                    return;
				}
				String preferenceStr = "PREFERENCE:"+spinner.getItemAtPosition(preferenceId).toString();
				writeIntoFile(preferenceStr);
				startActivity(new Intent(UserPreference.this, DiaryList.class));
			}        	
        });
        
        
	}
	
	private void showLocationEdit(String location){
		AlertDialog.Builder accBuilder;
		final AlertDialog accAlert;
		final String locationGPS = location;
		
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.locationdiag, (ViewGroup)findViewById(R.id.locationedit_root) );
		final EditText locationEdit = (EditText)layout.findViewById(R.id.addressedit);
		Button btOk = (Button) layout.findViewById (R.id.button_ok);
        Button btCancel = (Button) layout.findViewById (R.id.button_cancel);
        accBuilder = new AlertDialog.Builder(this);
        accBuilder.setView(layout);
        accBuilder.setTitle("Speficy address name");
        accAlert = accBuilder.create();
        accAlert.show();
        
        btOk.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				accAlert.dismiss();
				String locationName = locationEdit.getText().toString();
				String locationAll = locationGPS+","+locationName;
				//write the assoiate location information into file
				writeIntoFile(locationAll);
			}        	
        });      
       
        btCancel.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				accAlert.dismiss();
				
			}				
			});
	}
	
	/**
	 * write string into the user preference file
	 * @param str
	 */
	
	private File f = null;
	private BufferedWriter bw = null;
	private void writeIntoFile(String str){
		f = new File (CommonData.userpreferencePath);
		if (!f.exists()) {
            try {
                    f.createNewFile();
            } catch (Exception e) {   }
		}
		try {
			bw = new BufferedWriter(new FileWriter(f, true));
		} catch (Exception e) { }
		try {
			bw.write(str);
			bw.newLine();
	        bw.flush();
	        bw.close();
		} catch (Exception e) {	}		
	}
	
}
