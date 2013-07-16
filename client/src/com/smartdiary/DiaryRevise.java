/**
 * 
 * This gives user an option to revise the diary to 
 * make it more human readable. Any revision should
 * be recorded and send back to dict server so that
 * the dictionary can be updated from users' revision.
 * 
 * Of course, this should be under a privacy control.
 * It's better to be anonymous.
 * 
 * **/

package com.smartdiary;

import android.app.Activity;
import android.os.Bundle;

public class DiaryRevise extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.diaryrevise);
	}
}
