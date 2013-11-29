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
import java.util.Vector;
import java.util.Random;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
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
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.Button;
import android.content.res.Configuration;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.view.Display;
import android.util.Log;
import java.io.IOException;

public class ConfWindow extends RSActivity {

    ConnectUSB USB;  

    private int report_interval;  
    private int report_mask;  
    private String pan;
    private int chan;
    private int tx_power;
    private String firmware;
    private String id;
    private String i2c;
    private String eui64;
    private String sn;
    private String uptime;
    private int neighbor_table_size;
    private String RIME_addr;

    private double v_in;
    private double v_mcu;
    private boolean usb_pwr;
    private int intr_total_P0;
    private double intr_sec_P0;
    private int intr_total_P1;
    private double intr_sec_P1;
    private String log_mode;
    private boolean hum_pwr;
    private int debug;
    private String pwrs;


    private String[] sensor_items;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	Log.d("RStrace", "ConfWindow onCreate");
	main = Client.client; // Ugh, use a public static just to pass the instance.
	setContentView(R.layout.conf);
	setTitle("Sensor Configuration");
    }

    private int get_report_interval(){
	return report_interval;
    }
    private void set_report_interval(int i){
	report_interval = i;
    }
    private int get_report_mask(){
	return report_mask;
    }
    private void set_report_mask(int i){
	report_mask = i;
    }

    protected void onStart(){
	super.onStart();
	Log.d("RStrace", "ConfWindow onStart");
    }

    protected void onResume(){
	super.onResume();
	Log.d("RStrace", "ConfWindow onResume");
    }

    protected void onPause(){
	super.onPause();
	Log.d("RStrace", "ConfWindow onPause");
	gui2running(); // commit values to running
    }

    protected void onStop(){  
	super.onStop();
	Log.d("RStrace", "ConfWindow onStop");
    }

    protected void onDestroy(){
	super.onDestroy();
    }

    // This is code for lower right button menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
	MenuInflater inflater = getMenuInflater();
	inflater.inflate(R.layout.prefs_menu, menu);
	return true;
    }	

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
	// Handle item selection
	switch (item.getItemId()) {
	case R.id.load:
	if(Client.client.usbthread != null)
	    sensor2gui();
	    return true;
	default:
	    return super.onOptionsItemSelected(item);
	}
    }

    public String parse_string(String ss, String tag) 
    {
	String[] s = ss.split(" ");
	String s1 = "";

	for (int i = 0; i < s.length; i++)  
	    {  
		s1 = s[i];  
		if(s1.indexOf(tag) == 0) 
		    return s[i+1];
	    }
	return "";
    }

    public int parse_int(String ss, String tag) 
    {
	String[] s = ss.split(" ");
	String s1 = "";

	for (int i = 0; i < s.length; i++)  
	    {  
		s1 = s[i];  
		if(s1.indexOf(tag) == 0) {
		    //Toast.makeText(Client.client, "Kalle" + HexDump.dumpHexString(s[i+1].getBytes()), Toast.LENGTH_SHORT).show();
		    return Integer.decode(s[i+1]);
		}
	    }
	return 0;
    }

    public void parse_settings() 
    {
	byte b1[] = new byte[1000];
	String l0 ="";
	String l1 ="";
	String l2 ="";
	String l3 ="";

	    if(USB.driver == null)
		USB.connect();

	    try {
		USB.write("ss\r".getBytes());
		l0 = USB.readline(b1);
		l1 = l0.replace("\n", " ");
		l2 = l1.replace("\r", " ");
		l3 = l2.replace("=", " ");
	    }
	    catch  (IOException e) {
		Log.e("USB", "Error reading device: " + e.getMessage(), e);
	    }
	    set_report_interval(parse_int(l3, "report_interval")); 
	    set_report_mask(parse_int(l3, "report_mask")); 

	    //Toast.makeText(Client.client, Integer.toString(i), Toast.LENGTH_SHORT).show();

	    Toast.makeText(Client.client, String.format("Interval=%d, Mask=0x%x", get_report_interval(), get_report_mask()), Toast.LENGTH_SHORT).show();
    }

    // running -> GUI
    private void running2gui(){
	Button bt;
    }
    // GUI -> running
    private void gui2running(){
	Button bt;
    }

    // Read values from running -> GUI
    private void sensor2gui(){
	Button bt;

	parse_settings();
    }
}
