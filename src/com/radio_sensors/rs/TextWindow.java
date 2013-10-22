// Copyright (C) 2013 Olof Hagsand and Robert Olsson
//
// This file is part of Read-Sensors.
//
// Read-Sensors is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// Read-Sensors is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with Read-Sensors; see the file COPYING.


package com.radio_sensors.rs;

import java.util.Set;
import java.util.TreeSet;
import java.util.Random;
import java.util.ArrayList;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;

import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Region;
import android.graphics.Typeface;
import android.text.format.Time;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.MotionEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.os.Handler;
import android.os.Bundle;
import android.os.Message;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ScrollView;
import android.widget.ImageView;
import android.widget.Toast;
import android.content.res.Configuration;
import android.content.Intent;
import android.view.Display;
import android.util.Log;
import android.content.SharedPreferences;

public class TextWindow extends Activity {

    private ArrayList<String> report = new ArrayList<String>();
    private int    max_lines = 20;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.main);
	EditText et = (EditText) findViewById(R.id.server_ip);
        et.setText(get_server_ip());
        et = (EditText) findViewById(R.id.server_port);
        et.setText(""+get_server_port());

	Client.reporth = mHandler;
    }

    protected void onDestroy(){
	    super.onDestroy();
	}

    // This is code for lower right button menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
	MenuInflater inflater = getMenuInflater();
	inflater.inflate(R.layout.text_menu, menu);
	return true;
    }	

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
	// Handle item selection
	switch (item.getItemId()) {
	case R.id.plot:
	    toActivity("PlotWindow");
	    return true;
	default:
	    return super.onOptionsItemSelected(item);
	}
    }

    private void toActivity(String name){
	Intent i = new Intent();
	i.setClassName("com.radio_sensors.rs", "com.radio_sensors.rs."+name);
	try {
	    startActivity(i); 
	}
	catch (Exception e1){
	    e1.printStackTrace();
	}
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
	super.onConfigurationChanged(newConfig);
	setContentView(R.layout.main);
	EditText et = (EditText) findViewById(R.id.server_ip);
        et.setText(get_server_ip());
        et = (EditText) findViewById(R.id.server_port);
        et.setText(""+get_server_port());

	update();
    }

    private void update(){
	TextView tv = (TextView) findViewById(R.id.text);
	ScrollView sv = (ScrollView) findViewById(R.id.scroll);

	String text = "";
	for (String str:report){
	    text += str + System.getProperty("line.separator");
	}
	tv.setText(text);
	sv.smoothScrollTo(0, tv.getBottom());
    }

    private final Handler mHandler = new Handler() {
	    public void handleMessage(Message msg) {
		Message message;
		Pt p;
		switch (msg.what) {
		case Client.SENSD: // New report from sensd
		    String s = (String)msg.obj;

		    if (s.length()>0)
			report.add(s) ;
		    if (report.size() >= max_lines)
			report.remove(0); //remove first line if max
		    update();

		    break;
		default:	
		    Log.e("Report handler", "Unknown what: " + msg.what + " "+(String) msg.obj);  			
		    break;
		}
	    }
	};

  // access methods for prefs (would have them in PrefWindow, but cant make it work)
    public String get_server_ip(){
	SharedPreferences sPref = getSharedPreferences("Read-Sensors", 0);
	return sPref.getString("server_ip", PrefWindow.SERVER_IP);
    }
    public int get_server_port(){
	SharedPreferences sPref = getSharedPreferences("Read-Sensors", 0);
	return sPref.getInt("server_port", PrefWindow.SERVER_PORT);
    }
    public String get_sid(){
	SharedPreferences sPref = getSharedPreferences("Read-Sensors", 0);
	return sPref.getString("sid", PrefWindow.SID);
    }
    public String get_tag(){
	SharedPreferences sPref = getSharedPreferences("Read-Sensors", 0);
	return sPref.getString("tag", PrefWindow.TAG);
    }
}
