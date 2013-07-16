/**
 * 
 * This is an view of smart diary, user can view any diary
 * in this page. In order to involve multi-media like photos
 * and voice, this page is in HTML format. In other words, it's
 * the same as a web page though it is a local one.
 * 
 * **/

package com.smartdiary;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class DiaryView extends Activity {

	private TextView tv = null;
	@Override
	public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.diaryview);
        tv = (TextView)findViewById(R.id.diarytext);
        Intent intent = getIntent();
        String localpath = intent.getStringExtra("diary_path");
        String content = "";
        try {
        	BufferedReader br = new BufferedReader(new FileReader(localpath));
        	while (true) {
        		String s = br.readLine();
        		if (s == null) break;
        		content += s.split(":")[1];
        	}
        } catch (Exception e) {}
        tv.setText(content);
	}
}
