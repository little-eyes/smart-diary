/**
 * 
 * This is the main view of the whole smart diary.
 * No registration is needed until a user wants to
 * back up his diray in the cloud. This prototype,
 * however, does not contain cloud part, no such feature
 * is needed. 
 * 
 * This view gives a list of diary that has been written,
 * the user can click any of them and jump to DiaryView.
 * 
 * Also, the user can choose to delete and revise.
 * 
 * The diary list is read from the "diary.list" file!
 * 
 * SQLite database is not involved in the current version,
 * because I need to spend time to revise my previous
 * interface. So, all data are stored in the file and all
 * operations are based on file.
 * **/

package com.smartdiary;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener; 
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class DiaryList extends ListActivity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.diarylist);
        // start the diary service.
        startService(new Intent(DiaryList.this, DiaryService.class));
        
        if (DiaryService.nfcMsger != null)
        	DiaryService.nfcMsger.setMessage(this, "Jilong");
        // set the list adapter.
        diaryOutline = getDiaryOutline();
        adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, diaryOutline);
        setListAdapter(adapter);
        
        registerReceiver(refresh, new IntentFilter());
	}
	
	@Override
    public void onNewIntent(Intent intent)
    {
    	// Get the NFC Message and display it
		if (DiaryService.nfcMsger == null) {
			DiaryService.nfcMsger = new NfcMessenger(getApplicationContext());
			DiaryService.nfcMsger.setMessage(this, "Jilong");
		}
    	String text = DiaryService.nfcMsger.checkNfcMessage(intent);
        if (text != null)
        	Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
    }
	
	private BroadcastReceiver refresh = new BroadcastReceiver() {

		@Override
		public void onReceive(Context arg0, Intent arg1) {
			if (arg1.hasExtra("list-refresh")) {
				diaryOutline = getDiaryOutline();
				adapter.notifyDataSetChanged();
			}
			
		}};
	
	@Override
	public void onDestroy() {
		unregisterReceiver(refresh);
	}
	/*
	 * Handle options on the diary is clicked.
	 * Three options:
	 * 		- read/view
	 * 		- revise
	 * 		- delete
	 * 		- cloud backup (not included in current level)
	 * */
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		
		// On click, present the four options here.
		final String [] options = {"read", "revise", "delete", "cloud backup"};
		final String kw = adapter.getItem(position);
		AlertDialog.Builder optBuilder = new AlertDialog.Builder(this);
		optBuilder.setTitle("Options");
		optBuilder.setSingleChoiceItems(options, -1, new DialogInterface.OnClickListener() {
			
			public void onClick(DialogInterface arg0, int arg1) {
				arg0.dismiss();
				if (arg1 == 0) readDiary(kw);
				else if (arg1 == 1) reviseDiary(kw);
				else if (arg1 == 2) deleteDiary(kw);
				else backupDiary(kw);
			}
		});
		optBuilder.setNegativeButton("Cancel", new OnClickListener() {

			public void onClick(DialogInterface arg0, int arg1) {
				arg0.dismiss();
				
			}});
		optBuilder.create().show();
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.menu, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case R.id.preference: 	Intent i = new Intent(DiaryList.this, UserPreference.class);
	        						startActivity(i);
	        						break;
	        case R.id.diarygen:		DiaryGenerator dg = new DiaryGenerator(this);
	        						dg.generate();
	        case R.id.quit:     	quit();
	                            	break;
	        case R.id.refresh:		diaryOutline = getDiaryOutline();
									adapter.notifyDataSetChanged();
									break;
	    }
	    return true;
	}
	
	private void quit() {
		
	}
	// This string array stores the outline of diaries.
	private static ArrayList <String> diaryOutline = null;
	
	private ArrayList <String> getDiaryOutline() {
		ArrayList <String> ans = new ArrayList();
		File f = new File(CommonData.diarylistPath);
		try {
			BufferedReader br = new BufferedReader(new FileReader(f));
			String str = null;
			while ((str = br.readLine()) != null) {
				if (ans.contains(str)) continue;
				ans.add(str);
			}
		} catch (Exception e) {
			Log.e("dir", "cannot open diary.list file.");
			e.printStackTrace();
		}
		return ans;
	}
	
	private ArrayAdapter <String> adapter = null;
	
	/*
	 * -----------------------------------------------------------
	 * The following is to handle the four options.
	 * */
	private void readDiary(String key) {
		Intent intent = new Intent(DiaryList.this, DiaryView.class);
		Bundle bundle = new Bundle();
		String path = CommonData.baseDir + key;
		bundle.putString("diary_path", path);
		intent.putExtras(bundle);
		startActivity(intent);
	}
	
	private void reviseDiary(String key) {
		Intent intent = new Intent(DiaryList.this, DiaryRevise.class);
		Bundle bundle = new Bundle();
		String path = CommonData.baseDir + key;
		bundle.putString("diary_path", path);
		intent.putExtras(bundle);
		startActivity(intent);
	}
	
	private void deleteDiary(String key) {
		File f = new File(CommonData.baseDir + key);
		f.delete();
		
		ArrayList <String> t = new ArrayList();
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(CommonData.diarylistPath));
			String s = null;
			while ( (s = br.readLine()) != null)
				t.add(s);
			br.close();
			
			f = new File(CommonData.diarylistPath);
			f.delete();
			f.createNewFile();
			FileOutputStream fos = new FileOutputStream(f);
			for (String ss:t) {
				fos.write((ss + "\n").getBytes());
				fos.flush();
			}
			fos.close();
		} catch (Exception e) {
			Log.e("dir", "delete diary failed.");
			e.printStackTrace();
		}
	}
	
	private void backupDiary(String key) {
		
	}
}
