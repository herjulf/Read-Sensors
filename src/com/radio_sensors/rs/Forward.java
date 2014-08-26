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
import android.widget.Toast;
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
import android.content.SharedPreferences;
import android.content.IntentFilter;
import java.lang.Math;
import android.os.SystemClock;
import android.location.GpsStatus.Listener;


public class Forward extends RSActivity {

    final private static String TAG = "RS-" + Client.class.getName();
    private static int REPORT_INTERVAL = 10; // In sec
    final public static int REPORT = 6;      // Message
    final public static int GPS_TIMER = 7;
    private static int report_interval = REPORT_INTERVAL;
    public static Handler sockh = null; // ConnectSocket handler
    private static Boolean forward = false;
    private static Boolean forward_gps = false;
    private static Boolean local_report = false;
    private static Boolean local_report_gps = false;
    private static Boolean gpsfix = false;
    private float tmp_lon = 0;
    private float tmp_lat = 0;
    private Location mLastLocation;
    private long mLastLocationMillis;
    private float locked_lon = -1;
    private float locked_lat = -1;
    final private static float PREF_LOCKED_LON = -1;
    final private static float PREF_LOCKED_LAT = -1;


    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	Log.d(TAG, "onCreate");
	main = Client.client; // Ugh, use a public static just to pass the instance.
	Resources res = getResources();
	setContentView(R.layout.forward);
	running2gui();
	toActivity("Gps");
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

	case R.id.cbx_pos_lock:
            if (checked) {
		if( read_gps(true) ) {
		    locked_lat = tmp_lat;
		    locked_lon = tmp_lon;
		}
		else {
		    locked_lat = -1;
		    locked_lon = -1;
		}
	    }
	    else {
		locked_lat = -1;
		locked_lon = -1;
	    }
	    break;

	case R.id.cbx_pos_calc:
	    TextView tv;
	    tv = (TextView) findViewById(R.id.dist);
            if (checked) {
		read_gps(true);
		double dist = gps2m(tmp_lat, tmp_lon, locked_lat, locked_lon);
		tv.setText(String.format( "%.0f m", dist ));
	    }
	    else {
		tv.setText("N/A");
	    }
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

    void set_gps_timer(int ms)
    {
	Log.d(TAG, "set_gps_timer");
	Message message = Message.obtain();
	message.what = GPS_TIMER;
	mHandler.sendMessageDelayed(message, ms);
    }

    void stop_gps_timer()
    {
	if (mHandler.hasMessages(GPS_TIMER))
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

    // Read values from layout and into file
    private void locked_lon2pref() {
	Button bt;
	SharedPreferences sPref = getSharedPreferences("Read-Sensors", 0);
	SharedPreferences.Editor ed = sPref.edit();
	ed.putFloat("locked_lon", locked_lon);
	ed.commit();
    }

    private void locked_lat2pref() {
	Button bt;
	SharedPreferences sPref = getSharedPreferences("Read-Sensors", 0);
	SharedPreferences.Editor ed = sPref.edit();
	ed.putFloat("locked_lat", locked_lat);
	ed.commit();
    }

    // GUI -> running
    private void gui2running(){
	EditText et = (EditText) findViewById(R.id.domain);
	set_domain(et.getText().toString());
	et = (EditText) findViewById(R.id.interval);
	set_interval(Integer.parseInt(et.getText().toString()));
	locked_lon2pref();
	locked_lat2pref();
    }

    // running -> GUI
    private void running2gui(){
	set_locked_lat(get_pref_locked_lat());
	set_locked_lon(get_pref_locked_lon());
	if (locked_lon != -1) {
	    CheckBox checkBox = (CheckBox) findViewById(R.id.cbx_pos_lock);
	    checkBox.setChecked(true);
	}
	setTextVal(R.id.domain, get_domain());
	setIntVal(R.id.interval, get_interval());
	//setFloatVal(R.id.pos_lon, get_locked_lon);
	//setFloatVal(R.id.pos_lat, get_locked_lat());
	set_gps_timer(1*1000);
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
	//stop_gps_timer();
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
		   
	       case GPS_TIMER: // Periodic 
		   Log.e(TAG, "Sending report: " + msg.what + " "+(String) msg.obj);  			
		   read_gps(true);
		   set_gps_timer(2*1000);
		   break;
		   
	       default:	
		   Log.e(TAG, "Handler Unknown what: " + msg.what + " "+(String) msg.obj);  			
		   break;
		}
	   }
       };

    public void onGpsStatusChanged(int event) {
	TextView tv;
	tv = (TextView) findViewById(R.id.dist);

	Log.d(TAG, "GPS changed ");

        switch (event) {

	case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
	    if (mLastLocation != null)
		gpsfix = (SystemClock.elapsedRealtime() - mLastLocationMillis) < 3000;

	    if (gpsfix) { // A fix has been acquired.
		// Do something.
		//read_gps(true);
		double dist = gps2m(tmp_lat, tmp_lon, locked_lat, locked_lon);
		tv.setText(String.format( "%.0f m", dist ));
	    } else { // The fix has been lost.
		// Do something.
		tv.setText(" No fix");
	    }
	    break;

	case GpsStatus.GPS_EVENT_FIRST_FIX:
	    // Do something.
	    tv.setText("First fix");
	    gpsfix = true;
	    break;
        }
    }

 private Boolean  read_gps(Boolean update) {
	String lon = "";
        String lat = "";

	// Get the location manager
	LocationManager locationManager = (LocationManager) 
            getSystemService(LOCATION_SERVICE);
	//locationManager.addGpsStatusListener(this);

	Criteria criteria = new Criteria();
	String bestProvider = locationManager.getBestProvider(criteria, false);
	Location location = locationManager.getLastKnownLocation(bestProvider);
	LocationListener loc_listener = new LocationListener() {
		
		public void onLocationChanged(Location location) 
		{
		    if (location == null) 
			return;
		    mLastLocationMillis = SystemClock.elapsedRealtime();
		    // Do something.
		    mLastLocation = location;
		}
		
		public void onProviderEnabled(String p) {}
		
		public void onProviderDisabled(String p) {}
		
		public void onStatusChanged(String p, int status, Bundle extras) {
}
	    };

	locationManager.requestLocationUpdates(bestProvider, 0, 0, loc_listener);
	location = locationManager.getLastKnownLocation(bestProvider);
	try {
	    tmp_lat = (float) location.getLatitude();
	    tmp_lon = (float) location.getLongitude();
	} catch (NullPointerException e) {
	    tmp_lat = -1.0f;
	    tmp_lon = -1.0f;
	    return false;
	}
	//String foo = String.format( "Value of a: %.4f",  longitude );
	Log.d(TAG, "LON " + lon + " LAT " + lat);
	TextView tv;
	lon = String.valueOf(tmp_lon);
	lat = String.valueOf(tmp_lat);
	tv = (TextView) findViewById(R.id.cur_pos_lon);
	tv.setText(lon);
	tv = (TextView) findViewById(R.id.cur_pos_lat);
	tv.setText(lat);

	if (update) {
	    if(locked_lon == -1) {
		lon = "";
		lat = "";
	    }
	    else {
		lon = String.valueOf(locked_lon);
		lat = String.valueOf(locked_lat);
	    }
	    tv = (TextView) findViewById(R.id.pos_lon);
	    tv.setText(lon);
	    tv = (TextView) findViewById(R.id.pos_lat);
	    tv.setText(lat);
	}
	return true;
    }

    void compose_report(String s1)
    {
	String lon = "";
        String lat = "";

	if(sockh == null)
	    return;

	if( read_gps(false)) {
	    lon = String.valueOf(tmp_lon);
	    lat = String.valueOf(tmp_lat);
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

    private double gps2m(double lat_a, double lon_a, double lat_b, double lon_b) 
    {
	double pk = (double) (180/3.14169);
	double a1 = lat_a / pk;
	double a2 = lon_a / pk;
	double b1 = lat_b / pk;
	double b2 = lon_b / pk;
	
	double t1 = Math.cos(a1) * Math.cos(a2) * 
	    Math.cos(b1) * Math.cos(b2);
	double t2 = Math.cos(a1) * Math.sin(a2) * 
	    Math.cos(b1) * Math.sin(b2);
	double t3 = Math.sin(a1) * Math.sin(b1);

	double tt = Math.acos(t1 + t2 + t3);
	return 6366000*tt;
    }

    private double batVoltage()
    {
    	IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    	Intent b = this.registerReceiver(null, ifilter);
    	return b.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
    }

    /*------------------------------------------------------*/
    private SharedPreferences getpref(){
    	return getSharedPreferences("Read-Sensors", 0);
    }

    protected float get_pref_locked_lon(){
       	return (float) getpref().getFloat("locked_lon", PREF_LOCKED_LON);
    }

    protected float get_pref_locked_lat(){
      	return (float) getpref().getFloat("locked_lat", PREF_LOCKED_LAT);
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

    private void setFloatVal(int id, Float d){
	final EditText et = (EditText) findViewById(id);

	if (d==null)
	    et.setText(null);
	else
	    et.setText(d.floatValue()+"");
    }

    private Double getDoubleVal(int id){
	final EditText et = (EditText) findViewById(id);
	if (et.getText()==null || et.getText().toString().equals(""))
	    return null;
	else{
	    try {
		Double d = new Double(et.getText().toString());
		return d;
	    }
	    catch (Exception e1) {
		String str = e1.getMessage();
		Toast.makeText(this, "Error when parsing floar:"+str, Toast.LENGTH_SHORT).show();
		return null;
	    }
	}
    }

    private void setDoubleVal(int id, Double d){
	final EditText et = (EditText) findViewById(id);

	if (d==null)
	    et.setText(null);
	else
	    et.setText(d.doubleValue()+"");
    }
    protected float get_locked_lon(){
	return locked_lon;
    }
    protected void set_locked_lon(float y){
	locked_lon = y;
    }
    protected float get_locked_lat(){
	return locked_lat;
    }
    protected void set_locked_lat(float y){
	locked_lat = y;
    }
}
