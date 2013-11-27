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

public class PrefWindow extends RSActivity {
    private String[] sensor_items;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	Log.d("RStrace", "PrefWindow onCreate");
	main = Client.client; // Ugh, use a public static just to pass the instance.
	setContentView(R.layout.pref);
	setTitle("Preferences");
	running2gui();
    }

    /*
     * onClickTag
     * Called when 'tag' button is clicked in pref.xml
     * Build a dialogue menu of sensor ids and select one
     */
    public void onClickTag(View view) {
	AlertDialog.Builder dia = new AlertDialog.Builder(this);
	dia.setTitle("Select Sensor Tag");
	dia.setItems(R.array.tags_array, new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int which) {
		    Resources res = getResources();
		    String[] items = res.getStringArray(R.array.tags_array);
		    final Button bt = (Button) findViewById(R.id.tag);
		    bt.setText(items[which]);
		}
	    });
	dia.setInverseBackgroundForced(true);
	dia.create();
	dia.show();
    }

    /*
     * onClickSid
     * Called when 'sid' button is clicked in pref.xml
     * Build a dialogue menu of sensor ids and select one
     */
    public void onClickSid(View view) {
	AlertDialog.Builder dia = new AlertDialog.Builder(this);
	dia.setTitle("Select Sensor id");
	sensor_items = new String[2+main.sensor_ids.size()];
	int i = 0;
	sensor_items[i++] = "All";
	sensor_items[i++] = "Learn";
	for (String s : main.sensor_ids)
	    sensor_items[i++] = s;
	dia.setItems(sensor_items, new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int which) {
		    Resources res = getResources();
		    final Button bt = (Button) findViewById(R.id.sid);
		    bt.setText(sensor_items[which]);
		}
	    });
	dia.setInverseBackgroundForced(true);
	dia.create();
	dia.show();
    }

    protected void onStart(){
	super.onStart();
	Log.d("RStrace", "PrefWindow onStart");
    }

    protected void onResume(){
	super.onResume();
	Log.d("RStrace", "PrefWindow onResume");
    }

    protected void onPause(){
	super.onPause();
	Log.d("RStrace", "PrefWindow onPause");
	gui2running(); // commit values to running
    }

    protected void onStop(){  
	super.onStop();
	Log.d("RStrace", "PrefWindow onStop");
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
	    pref2gui();
	    return true;
	case R.id.save:
	    gui2pref();
	    return true;
	default:
	    return super.onOptionsItemSelected(item);
	}
    }

    // running -> GUI
    private void running2gui(){
	Button bt;
	setTextVal(R.id.server_ip, get_server_ip());
	setIntVal(R.id.server_port, get_server_port());
	bt = (Button) findViewById(R.id.sid);
	bt.setText(get_sensor_id());
	bt = (Button) findViewById(R.id.tag);
	bt.setText(get_sensor_tag());
	setTextVal(R.id.user_tag, get_user_tag());
	setIntVal(R.id.max_samples, get_max_samples());
	setIntVal(R.id.plot_window, get_plot_window());
	setIntVal(R.id.plot_style, get_plot_style());
	setIntVal(R.id.plot_fontsize, get_plot_fontsize());
	setDoubleVal(R.id.plot_ymin, get_plot_ymin());
	setDoubleVal(R.id.plot_ymax, get_plot_ymax());
	setFloatVal(R.id.plot_linewidth, get_plot_linewidth());
    }
    // GUI -> running
    private void gui2running(){
	Button bt;
	set_server_ip((String)getTextVal(R.id.server_ip));
	set_server_port(getIntVal(R.id.server_port));
	bt = (Button) findViewById(R.id.sid);
	set_sensor_id((String)bt.getText());
	bt = (Button) findViewById(R.id.tag);
	set_sensor_tag((String)bt.getText());
	set_user_tag((String)getTextVal(R.id.user_tag));
	set_max_samples(getIntVal(R.id.max_samples));
	set_plot_window(getIntVal(R.id.plot_window));
	set_plot_style(getIntVal(R.id.plot_style));
	set_plot_fontsize(getIntVal(R.id.plot_fontsize));
	set_plot_ymin(getDoubleVal(R.id.plot_ymin));
	set_plot_ymax(getDoubleVal(R.id.plot_ymax));
	set_plot_linewidth(getFloatVal(R.id.plot_linewidth));
    }

    // Read values from running -> GUI
    private void pref2gui(){
	Button bt;
	setTextVal(R.id.server_ip, get_pref_server_ip());
	setIntVal(R.id.server_port, get_pref_server_port());
	bt = (Button) findViewById(R.id.sid);
	bt.setText(get_pref_sensor_id());
	bt = (Button) findViewById(R.id.tag);
	bt.setText(get_pref_sensor_tag());
	setTextVal(R.id.user_tag, get_pref_user_tag());
	setIntVal(R.id.max_samples, get_pref_max_samples());
	setIntVal(R.id.plot_window, get_pref_plot_window());
	setIntVal(R.id.plot_style, get_pref_plot_style());
	setIntVal(R.id.plot_fontsize, get_pref_plot_fontsize());
	setFloatVal(R.id.plot_linewidth, get_pref_plot_linewidth());
    }

    // Read values from layout and into file
    private void gui2pref() {
	Button bt;
	SharedPreferences sPref = getSharedPreferences("Read-Sensors", 0);
	SharedPreferences.Editor ed = sPref.edit();
	ed.putString("server_ip", (String)getTextVal(R.id.server_ip));
	ed.putInt("server_port", getIntVal(R.id.server_port));
	bt = (Button) findViewById(R.id.sid);
	ed.putString("sid", (String)bt.getText());
	bt = (Button) findViewById(R.id.tag);
	ed.putString("tag", (String)bt.getText());
	ed.putString("user_tag", (String)getTextVal(R.id.user_tag));
	ed.putInt("max_samples", getIntVal(R.id.max_samples));
	ed.putInt("plot_window", getIntVal(R.id.plot_window));
	ed.putInt("plot_style", getIntVal(R.id.plot_style));
	ed.putInt("plot_fontsize", getIntVal(R.id.plot_fontsize));
	ed.putFloat("plot_linewidth", getFloatVal(R.id.plot_linewidth));
	ed.commit();
    }

    // get and set methods for GUI
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
    private Float getFloatVal(int id){
	final EditText et = (EditText) findViewById(id);
	if (et.getText()==null || et.getText().toString().equals(""))
	    return null;
	else{
	    try {
		Float d = new Float(et.getText().toString());
		return d;
	    }
	    catch (Exception e1) {
		String str = e1.getMessage();
		Toast.makeText(this, "Error when parsing floar:"+str, Toast.LENGTH_SHORT).show();
		return null;
	    }
	}
    }

    private void setFloatVal(int id, Float d){
	final EditText et = (EditText) findViewById(id);

	if (d==null)
	    et.setText(null);
	else
	    et.setText(d.floatValue()+"");
    }

}


