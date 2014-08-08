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
import android.os.Handler;
import android.widget.TextView;
import android.view.View;
import android.util.Log;
import android.os.Message;
import android.widget.EditText;
import android.content.res.Resources;
import java.util.*;
import java.text.*;
import android.location.*;
import java.lang.String;
import java.text.DecimalFormat;
import android.os.BatteryManager;
import android.content.Intent;
import android.widget.CheckBox;
import android.widget.Button;
import android.content.IntentFilter;

public class Forward extends RSActivity {

    final private static String TAG = "RS-" + Client.class.getName();
    private static int REPORT_INTERVAL = 10; // In sec
    final public static int REPORT = 6;      // Message
    private static int report_interval = REPORT_INTERVAL;
    public static Handler sockh = null; // ConnectSocket handler
    private static Boolean forward = false;
    private static Boolean forward_gps = false;
    private static Boolean local_report = false;
    private static Boolean local_report_gps = false;

    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	Log.d("TAG", "onCreate");
	main = Client.client; // Ugh, use a public static just to pass the instance.
	Resources res = getResources();
	setContentView(R.layout.forward);
	running2gui();
    }
 
    public void onCheckboxClicked(View view) {
	boolean checked = ((CheckBox) view).isChecked();

	switch(view.getId()) {

        case R.id.cbx_forward:
            if (checked) 
		forward = true;
            else 
		forward = false;

	    update_forward();
	    break;

        case R.id.cbx_forward_gps:
            if (checked) 
		forward_gps = true;
            else 
		forward_gps = false;
	    break;

        case R.id.cbx_local_report:
            if (checked) {
		local_report = true;
		stop_report_timer();
		set_report_timer(report_interval*1000);
		String s1 = " TXT=z1" + "V_BAT=" + String.valueOf(batVoltage()/1000);
		compose_report(s1);
		Log.d(TAG, "Report Start");
	    }
            else {
		local_report = false;
	    	stop_report_timer();
		Log.d(TAG, "Report Stop");
	    }
	    update();
	    break;
	default:
	    Log.d(TAG, "CBX Miss");
	}
    }

    void set_report_timer(int ms)
    {
	Log.d(TAG, "set_report_timer");
	Message message = Message.obtain();
	message.what = REPORT;
	mHandler.sendMessageDelayed(message, ms);
    }

    void stop_report_timer()
    {
	if (mHandler.hasMessages(REPORT))
	    mHandler.removeMessages(REPORT);
    }

    void update()
    {

	Button bt;
	EditText et;

	et = (EditText) findViewById(R.id.interval);
	if(local_report == true) 
	    et.setEnabled(true);
	else
	    et.setEnabled(false);

	bt = (Button) findViewById(R.id.cbx_forward_gps);
	if(forward_gps == true) 
	    bt.setEnabled(true);
	else
	    bt.setEnabled(false);

    }

    void update_forward()
    {

	Button bt;
	EditText et;

	bt = (Button) findViewById(R.id.cbx_forward_gps);
	if(forward == true) {
	    bt.setEnabled(true);
	}
	else
	    bt.setEnabled(false);

    }


    // GUI -> running
    private void gui2running(){
	EditText et = (EditText) findViewById(R.id.domain);
	set_domain(et.getText().toString());
	et = (EditText) findViewById(R.id.interval);
	set_interval(Integer.parseInt(et.getText().toString()));
    }

    // running -> GUI
    private void running2gui(){
	setTextVal(R.id.domain, get_domain());
	setIntVal(R.id.interval, get_interval());
    }

    protected void onStart(){
	super.onStart();
	Log.d(TAG, "onStart");
    }

    protected void onResume(){
	super.onResume();
	Log.d(TAG, "onResume");
    }

    protected void onPause(){
	super.onPause();
	Log.d(TAG, "onPause");
	gui2running(); // commit values to running
    }

    protected void onStop(){  // This is called when starting another activity
	super.onStop();
	Log.d(TAG, "onStop");
    }

    protected void onDestroy()	{
	super.onDestroy();
	Log.d(TAG, "onDestroy");
	//stop_report_timer();
    }

   private final Handler mHandler = new Handler() {
	    public void handleMessage(Message msg) {
		Message message;
		Pt p;
		switch (msg.what) {
	
		case REPORT: // Periodic 
		    Log.e(TAG, "Sending report: " + msg.what + " "+(String) msg.obj);  			
		    set_report_timer(report_interval*1000);
		    String s1 = " TXT=z1 " + "V_BAT=" + String.valueOf(batVoltage()/1000);
		    compose_report(s1);
		    break;

		default:	
		    Log.e(TAG, "Handler Unknown what: " + msg.what + " "+(String) msg.obj);  			
		    break;
		}
	    }
	};

    void compose_report(String s1)
    {
	String lon = "";
        String lat = "";

	if(sockh == null)
	    return;

	LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE); 
	Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
	if (location != null) {
	    double longitude = location.getLongitude();
	    double latitude = location.getLatitude();
	    lon = String.valueOf(longitude);
	    lat = String.valueOf(latitude);
	    //String foo = String.format( "Value of a: %.4f",  longitude );

	    Log.d(TAG, "LON " + lon + " LAT " + lat);
	}

	SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss  ");
	SimpleDateFormat ftz = new SimpleDateFormat("z ");
	String msg = ft.format(new Date())+"TZ="+ftz.format(new Date())+"UT="+(int)System.currentTimeMillis()/1000L+" "+ "DOMAIN="+ get_domain() + " " + "GWGPS_LON=" + lon + " " + "GWGPS_LAT=" + lat +" &: " + s1;

	char c = 0x1a;
	msg = msg.replace(c,' ');
	msg = msg.replaceAll("\n", "");
	msg = msg.replaceAll("\r", "");
	//String dump = HexDump.dumpHexString(msg.getBytes());
	//Log.d(TAG, "DUMP " + dump);

	message(sockh, Client.SENSD_SEND, msg + "\n");
    }

    private double batVoltage()
    {
    	IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    	Intent b = this.registerReceiver(null, ifilter);
    	return b.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
    }

    protected void set_interval(int ri){
	    report_interval = ri;
    }

    protected int get_interval(){
	    return report_interval;
    }
    private String getTextVal(int id){
	final EditText et = (EditText) findViewById(id);
	return et.getText().toString();
    }
    private void setTextVal(int id, String s){
	final EditText et = (EditText) findViewById(id);
	et.setText(s);
    }
    private int getIntVal(int id){
	final EditText et = (EditText) findViewById(id);
	return Integer.parseInt(et.getText().toString());
    }
    private void setIntVal(int id, int i){
	final EditText et = (EditText) findViewById(id);
	et.setText(i+"");
    }

}