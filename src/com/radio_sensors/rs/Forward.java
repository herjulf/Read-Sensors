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
 
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.util.Log;
import android.os.Message;
import android.widget.EditText;
import android.content.res.Resources;

public class Forward extends RSActivity {

    final private static String TAG = "RS-" + Client.class.getName();

    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	Log.d("TAG", "onCreate");
	main = Client.client; // Ugh, use a public static just to pass the instance.
	Resources res = getResources();
	setContentView(R.layout.forward);
	running2gui();
    }
 
    // GUI -> running
    private void gui2running(){
	EditText et = (EditText) findViewById(R.id.domain);
	set_domain(et.getText().toString());
    }

    // running -> GUI
    private void running2gui(){
	setTextVal(R.id.domain, get_domain());
    }

    protected void onStart(){
	super.onStart();
	Log.d("TAG", "onStart");
    }

    protected void onResume(){
	super.onResume();
	Log.d("TAG", "onResume");
    }

    protected void onPause(){
	super.onPause();
	Log.d("TAG", "onPause");
	gui2running(); // commit values to running
    }

    protected void onStop(){  // This is called when starting another activity
	super.onStop();
	Log.d("TAG", "onStop");
    }

    protected void onDestroy()	{
	super.onDestroy();
	Log.d("TAG", "onDestroy");
    }

    private String getTextVal(int id){
	final EditText et = (EditText) findViewById(id);
	return et.getText().toString();
    }

    private void setTextVal(int id, String s){
	final EditText et = (EditText) findViewById(id);
	et.setText(s);
    }
}