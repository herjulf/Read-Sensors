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
import android.app.AlertDialog;
import android.os.Handler;
import android.os.Bundle;
import android.os.Environment;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.ScrollView;
import android.widget.ImageView;
import android.view.Display;
import android.view.View;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import android.net.Uri;
import java.util.ArrayList;
import java.lang.Thread;
import android.util.Log;
import android.os.Message;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Bitmap.CompressFormat; 
import android.graphics.Canvas;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;


/*
 * Shared abstract Radio-Sensor that all other activities inherit from
 * This is useful to collect common methods, such as shared preferences, etc.
 */
abstract class RSActivity extends Activity{

    final private static String TAG = "RS-" + Client.class.getName();

    // Default preference values. 
    // Only place where default values should appear.
    final private static String PREF_SERVER_IP = "Radio-Sensors.com"; 
    final private static int    PREF_SERVER_PORT = 1235;
    final private static String PREF_SENSOR_ID  = "Learn"; // Learn is first tag found
    final private static String PREF_SENSOR_TAG  = "T";
    final private static int    PREF_MAX_SAMPLES = 100;
    final private static int    PREF_PLOT_WINDOW = Plot.XWINDOW; // seconds
    final private static int    PREF_PLOT_STYLE = Plot.LINES; 
    final private static int    PREF_PLOT_FONTSIZE = 25; 
    final private static float  PREF_PLOT_LINEWIDTH = PlotVector.LINEWIDTH; 
    final private static String PREF_DOMAIN = "RS-APP.SE"; 
    final private static float  PREF_PLOT_FACTOR = 1.0f; 

    protected RSActivity main; // This is the main activity (Client)
    /* The following variables only have values for main instance.
       The 'main' variable (above) is used to get the values */
    private String server_ip;  
    private int    server_port;  
    private String sensor_id;  
    private String sensor_tag;  
    private String user_tag;  
    private int    max_samples;  
    private int    plot_window;  
    private int    plot_style;  
    private int    plot_fontsize;  
    private Double plot_ymin = null;  
    private Double plot_ymax = null;  
    private float plot_linewidth;  
    private String domain;  
    private float plot_factor = 1.0f;  

    final public static String FILE = "/RS-screen.png";

    protected ArrayList<String> sensor_ids = new ArrayList<String>();
    protected ArrayList<String> sensor_tags = new ArrayList<String>();

    /*
     * Access methods for persistent data
     * setting persisting values only in PrefWindow class
     */
    protected String get_server_ip(){
	return main.server_ip;
    }
    protected void set_server_ip(String ip){
	main.server_ip = ip;
    }
    protected int get_server_port(){
	return main.server_port;
    }
    protected void set_server_port(int port){
	main.server_port = port;
    }
    protected String get_sensor_id(){
	return main.sensor_id;
    }
    protected void set_sensor_id(String id){
	main.sensor_id = id;
    }
    protected String get_sensor_tag(){
	return main.sensor_tag;
    }
    protected void set_sensor_tag(String tag){
	main.sensor_tag = tag;
    }
    protected String get_domain(){
	Log.d(TAG, "get_domain 1 " + main.domain);
	if(main != null) 
	    return main.domain;
	else
	    return "ERR";
    }
    protected void set_domain(String domain){
	if(main != null) 
	    main.domain = domain;
	Log.d(TAG, "set_domain 1 " + main.domain);
    }
/*
    protected String get_user_tag(){
	return main.user_tag;
    }
    protected void set_user_tag(String tag){
	main.user_tag = tag;
    }
*/
    protected int get_max_samples(){
	return main.max_samples;
    }
    protected void set_max_samples(int samples){
	main.max_samples = samples;
    }
    protected int get_plot_window(){
	return main.plot_window;
    }
    protected void set_plot_window(int xwin){
	main.plot_window = xwin;
    }
    protected int get_plot_style(){
	return main.plot_style;
    }
    protected void set_plot_style(int style){
	main.plot_style = style;
    }
    protected int get_plot_fontsize(){
	return main.plot_fontsize;
    }
    protected void set_plot_fontsize(int fontsize){
	main.plot_fontsize = fontsize;
    }
    protected Double get_plot_ymin(){
	return main.plot_ymin;
    }
    protected void set_plot_ymin(Double y){
	main.plot_ymin = y;
    }
    protected Double get_plot_ymax(){
	return main.plot_ymax;
    }
    protected void set_plot_ymax(Double y){
	main.plot_ymax = y;
    }
    protected Float get_plot_linewidth(){
	return main.plot_linewidth;
    }
    protected void set_plot_linewidth(float w){
	main.plot_linewidth = w;
    }
    protected void add_sensor_ids(String id){
	if (!main.sensor_ids.contains(id))
	    main.sensor_ids.add(id);
    }
    protected void add_sensor_tags(String tag){
	if (!main.sensor_tags.contains(tag))
	    main.sensor_tags.add(tag);
    }
    protected float get_plot_factor(){
	return main.plot_factor;
    }
    protected void set_plot_factor(float y){
	main.plot_factor = y;
    }
    /*------------------------------------------------------*/
    private SharedPreferences getpref(){
	return getSharedPreferences("Read-Sensors", 0);
    }
    /*
     * Access methods for persistent data
     * setting persisting values only in PrefWindow class
     */
    protected String get_pref_server_ip(){
	return getpref().getString("server_ip", PREF_SERVER_IP);
    }
    protected int get_pref_server_port(){
	return getpref().getInt("server_port", PREF_SERVER_PORT);
    }
    protected String get_pref_sensor_id(){
	SharedPreferences sPref = getSharedPreferences("Read-Sensors", 0);
	return getpref().getString("sid", PREF_SENSOR_ID);
    }
    protected String get_pref_sensor_tag(){
	return getpref().getString("tag", PREF_SENSOR_TAG);
    }
    protected String get_pref_domain(){
	return getpref().getString("domain", PREF_DOMAIN);
    }
/*
    protected String get_pref_user_tag(){
	return getpref().getString("user_tag", null);
    }
*/
    protected int get_pref_max_samples(){
	return getpref().getInt("max_samples", PREF_MAX_SAMPLES);
    }
    protected int get_pref_plot_window(){
	return getpref().getInt("plot_window", PREF_PLOT_WINDOW);
    }
    protected int get_pref_plot_style(){
	return getpref().getInt("plot_style", PREF_PLOT_STYLE);
    }
    protected int get_pref_plot_fontsize(){
	return getpref().getInt("plot_fontsize", PREF_PLOT_FONTSIZE);
    }
    protected float get_pref_plot_linewidth(){
	return getpref().getFloat("plot_linewidth", PREF_PLOT_LINEWIDTH);
    }
    protected float get_pref_plot_factor(){
     	return getpref().getFloat("plot_factor", PREF_PLOT_FACTOR);
    }

    /*
     * Access methods for persistent data
     * setting persisting values only in USBSettings class
     */

    protected int get_pref_serial_speed(){
	return getpref().getInt("serial_speed", USBSettings.PREF_SERIAL_SPEED);
    }
    protected int get_serial_speed(){
	return USBSettings.serial_speed;
    }
    protected void set_serial_speed(int speed){
	USBSettings.serial_speed = speed;
    }

    // Load preferences -> running values 
    protected void pref2running(){
	set_server_ip(get_pref_server_ip());
	set_server_port(get_pref_server_port());
	set_sensor_id(get_pref_sensor_id());
	set_sensor_tag(get_pref_sensor_tag());
//	set_user_tag(get_pref_user_tag());
	set_max_samples(get_pref_max_samples());
	set_plot_window(get_pref_plot_window());
	set_plot_style(get_pref_plot_style());
	set_plot_fontsize(get_pref_plot_fontsize());
	set_plot_linewidth(get_pref_plot_linewidth());
	set_domain(get_pref_domain());
	set_plot_factor(get_pref_plot_factor());
	set_serial_speed(get_pref_serial_speed());
	Log.d(TAG, "pref domain 1 " + main.domain);
    }

    public void shareScreen(View v1)
    {
	v1.setDrawingCacheEnabled(true);
	Bitmap bitmap = v1.getDrawingCache();
	File file = new File( Environment.getExternalStorageDirectory() + FILE);
	try 
	    {
		file.createNewFile();
		FileOutputStream ostream = new FileOutputStream(file);
		bitmap.compress(CompressFormat.PNG, 100, ostream);
		ostream.close();
		v1.setDrawingCacheEnabled(false);
	    } 

	catch (Exception e) 
	    {
		Log.e("RStrace", "Exeception:" + Log.getStackTraceString(e));
		e.printStackTrace();
	    }

	Uri uri = Uri.fromFile(file);
	Intent shareIntent = new Intent();
	shareIntent.setAction(Intent.ACTION_SEND);
	shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Screen from Read Sensors App.");
	//shareIntent.putExtra(Intent.EXTRA_TEXT, "Add text");
	shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
	shareIntent.setType("image/png");
	startActivity(Intent.createChooser(shareIntent, "Share screen dump.."));
    }

    // Send a message to other activity
    protected void message(Handler h, int what, Object msg){
	Message message = Message.obtain();
	message.what = what;
	message.obj = msg;
	h.sendMessage(message); // To other activity
    }

    // Start another activity
    protected void toActivity(String name){
	Intent i = new Intent();
	i.setClassName("com.radio_sensors.rs", "com.radio_sensors.rs."+name);
	try {
	    startActivity(i); 
	}
	catch (Exception e1){
	    e1.printStackTrace();
	}
    }
}
