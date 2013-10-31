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
import java.util.ArrayList;
import java.lang.Thread;
import android.util.Log;
import android.os.Message;

import android.hardware.usb.UsbManager;

import com.hoho.android.usbserial.driver.FtdiSerialDriver;
import com.hoho.android.usbserial.driver.UsbId;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialRuntimeException;
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver;
import com.hoho.android.usbserial.util.SerialInputOutputManager;
import com.hoho.android.usbserial.util.HexDump;


public class Client extends Activity {
    // Messages
    final public static int ERROR  = -1;         // Something went wrong
    final public static int STATUS = 2;          // Status change
    final public static int SENSD  = 3;          // Report arrived from sensd server
    final public static int TIMER  = 4;       // Interval timer every 1s (debug)
    final public static int REPLAY = 5;       // Replay all stored sensd data

    private static int TIMERINTERVAL = 2000; // interval between sample receives

    private String server_ip = "";
    private int server_port = 0;
    private Thread connectthread = null;
    public static Client client = null;
    private boolean active = false;           // Activity is active

    ConnectSocket connect_cs;                 // Object containing connect-socket

    // Debug 
    final static int DEBUG_NONE        = 0;
    final static int DEBUG_REPORT      = 1;
    final static int DEBUG_PLOT        = 2;
    final static int DEBUG_FILTER      = 3;

    public static int debug       = DEBUG_NONE;

    public static Handler ploth = null; // PlotWindow handler

    // Textwindow
    private ArrayList<String> report = new ArrayList<String>();
    private int max_samples;

    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	client = this;
	setContentView(R.layout.main);

	setDefaultKeyMode(DEFAULT_KEYS_DISABLE);
	// Set-up default values from prefs
	EditText et = (EditText) findViewById(R.id.server_ip);
	et.setText(get_pref_server_ip());
	et = (EditText) findViewById(R.id.server_port);
	et.setText(""+get_pref_server_port());
	max_samples = get_pref_max_samples(); 
	Button buttonConnect = (Button) findViewById(R.id.server_connect);
	buttonConnect.setText("Connect");
	// Start periodic timer
	Message message = Message.obtain();
	message.what = Client.TIMER;
	mHandler.sendMessageDelayed(message, TIMERINTERVAL);

    }

    // Called when 'connect/disconnect' button is clicked
    public void onClickConnect(View view) {
	EditText et_srv = (EditText) findViewById(R.id.server_ip);
	EditText et_port = (EditText) findViewById(R.id.server_port);

	if(connectthread != null) {
	    Toast.makeText(this, "Disconnecting...", Toast.LENGTH_SHORT).show();
	    disconnect();
	    return;
	}
	server_ip = et_srv.getText().toString();
	server_port = Integer.parseInt(et_port.getText().toString());
	
	connect(server_ip, server_port);
    }

    protected void onStart(){
	super.onStart();
	Log.d("RStrace", "Main onStart");
    }

    protected void onResume(){
	super.onResume();
	active = true;
	textupdate(); // Update text-sensd reports in window
	Log.d("RStrace", "Main onResume");
    }

    protected void onPause(){
	super.onPause();
	active = false;
	Log.d("RStrace", "Main onPause");
    }

    protected void onStop(){  // This is called when starting another activity
	super.onStop();

	Log.d("RStrace", "Main onStop");
    }

    protected void onDestroy()	{
	super.onDestroy();
	disconnect(); // Kills connect thread
	Log.d("RStrace", "Main onDestroy");
    }

    // This is called on resize
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
	super.onConfigurationChanged(newConfig);
	setContentView(R.layout.main);
	// Set edit text fields from prefs
	EditText et = (EditText) findViewById(R.id.server_ip);
	et.setText(get_pref_server_ip());
	et = (EditText) findViewById(R.id.server_port);
	et.setText(""+get_pref_server_port());

	textupdate(); // Update text-sensd reports in window

	// Set connected/disconnected button text
	Button buttonConnect = (Button) findViewById(R.id.server_connect);
	if(connectthread != null)
	    buttonConnect.setText("Disconnect");
	else
	    buttonConnect.setText("Connect");

    }

    // This is code for options menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
	MenuInflater inflater = getMenuInflater();
	inflater.inflate(R.layout.main_menu, menu);
	return true;
    }	

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
	// Handle item selection
	switch (item.getItemId()) {
	case R.id.about:
	    AboutBox.Show(this);
	    return true;
	case R.id.prefs:
	    toActivity("PrefWindow");
	    return true;
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

    // Connect to a server: create connectsocket object, a thread and start it.
    private void connect(String srv_ip, int srv_port) {
	if(connectthread != null){
	    // Should not happen
	    return;
	}
	connect_cs = new ConnectSocket(srv_ip, srv_port, mHandler); // Here add parameters
	connectthread = new Thread(connect_cs, "Connect Socket");
	connectthread.start();
    }

  // Connect to USB device
    private void connect_usb() {

	// Get UsbManager from Android.
	UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);

	// Find the first available driver.
	UsbSerialDriver driver = UsbSerialProber.acquire(manager);

	if (driver != null) {
	    try {
		driver.open();
		driver.setParameters(38400, 8, UsbSerialDriver.STOPBITS_1, UsbSerialDriver.PARITY_NONE);
		Toast.makeText(this, "RS USB opened", Toast.LENGTH_SHORT).show();
	    } catch (IOException e) {
		Log.e("USB", "Error setting up device: " + e.getMessage(), e);
		try {
		    driver.close();
		} catch (IOException e2) {
                                        // Ignore.
		}
		driver = null;
		return;
	    }

	    try {
		byte buffer[] = new byte[16];
		int numBytesRead = driver.read(buffer, 1000);
		Toast.makeText(this, "RS USB" + numBytesRead, Toast.LENGTH_SHORT).show();
		Toast.makeText(this, "RS USB" + new String(buffer), Toast.LENGTH_SHORT).show();
		Log.e("RS USB", "Read " + numBytesRead + " bytes.");
	    } catch (IOException e) {
		// Handle error.
	    } finally {
		try {
		    driver.close();
		} catch (IOException e2) {
		    // Ignore.
		}
		driver = null;
	    } 
	}
    }

    // Post an interrupt to the connect thread and call its kill method
    private void disconnect() {
	if(connectthread != null) {
	    try{
		connect_cs.kill();
		connectthread.interrupt();
		// connectthread is set to null only when detected by mHandler
	    }
	    catch (Exception e1) {
		e1.printStackTrace();
	    }
	}
    }

    // Draw reports in text window. Scroll to bottom of texts.
    private void textupdate(){
        TextView tv = (TextView) findViewById(R.id.text);
        ScrollView sv = (ScrollView) findViewById(R.id.scroll);

        String text = "";
        for (String str:report){
	    text += str + System.getProperty("line.separator");
        }
        tv.setText(text);
        sv.smoothScrollTo(0, tv.getBottom());
    }

    // access methods for prefs (would have them in PrefWindow, but cant make it work)
    public String get_pref_server_ip(){
	SharedPreferences sPref = getSharedPreferences("Read-Sensors", 0);
	return sPref.getString("server_ip", PrefWindow.PREF_SERVER_IP);
    }
    public int get_pref_server_port(){
	SharedPreferences sPref = getSharedPreferences("Read-Sensors", 0);
	return sPref.getInt("server_port", PrefWindow.PREF_SERVER_PORT);
    }
    public String get_pref_sid(){
	SharedPreferences sPref = getSharedPreferences("Read-Sensors", 0);
	return sPref.getString("sid", PrefWindow.PREF_SID);
    }
    public String get_pref_tag(){
	SharedPreferences sPref = getSharedPreferences("Read-Sensors", 0);
	return sPref.getString("tag", PrefWindow.PREF_TAG);
    }
    public String get_pref_user_tag(){
	SharedPreferences sPref = getSharedPreferences("Read-Sensors", 0);
	return sPref.getString("user_tag", null);
    }
    public int get_pref_max_samples(){
	SharedPreferences sPref = getSharedPreferences("Read-Sensors", 0);
	return sPref.getInt("max_samples", PrefWindow.PREF_MAX_SAMPLES);
    }
    public int get_pref_plot_window(){
	SharedPreferences sPref = getSharedPreferences("Read-Sensors", 0);
	return sPref.getInt("plot_window", PrefWindow.PREF_PLOT_WINDOW);
    }
    public int get_pref_plot_style(){
	SharedPreferences sPref = getSharedPreferences("Read-Sensors", 0);
	return sPref.getInt("plot_style", PrefWindow.PREF_PLOT_STYLE);
    }
    public int get_pref_plot_fontsize(){
	SharedPreferences sPref = getSharedPreferences("Read-Sensors", 0);
	return sPref.getInt("plot_fontsize", PrefWindow.PREF_PLOT_FONTSIZE);
    }
    // Send a message to other activity
    private void message(Handler h, int what, Object msg){
	Message message = Message.obtain();
	message.what = what;
	message.obj = msg;
	h.sendMessage(message); // To other activity
    }

    // Messages comes in from socket-handler due to sensd input or error
    public final Handler mHandler = new Handler() {
	    public void handleMessage(Message msg ) {
		Message message;
		switch (msg.what) {
		case Client.SENSD: // New report from sensd			
		    String s = (String)msg.obj;
		    if (s.length()>0)
			report.add(s) ;
		    if (report.size() >= max_samples)
			report.remove(0); //remove first line if max
		    if (ploth != null)
			message(ploth, Client.SENSD, s);
		    if (active)
			textupdate();         // update text if active
		    break;
		case Client.ERROR: // Something went wrong
		    Log.d("RStrace", "Error"+(String)msg.obj);
		    Toast.makeText(Client.client, "Error: "+(String)msg.obj, Toast.LENGTH_LONG).show();		    
		    break;

		case Client.STATUS: // Connect status changed
		    Integer stat = (Integer)msg.obj;
		    Button buttonConnect = (Button) findViewById(R.id.server_connect);

		    Log.d("RStrace", "Client.Status="+stat);
		    if (stat != null){
			if (stat.equals(0)){
			    if (connectthread!= null &&
				connectthread.getState().equals(Thread.State.TERMINATED)){
				Log.d("RStrace", "STATUS: Thread TERMINATED");
				connectthread = null;
			    }
			    connectthread = null;
			    buttonConnect.setText("Connect");			    
			    Toast.makeText(Client.client, "Disconnected", Toast.LENGTH_SHORT).show();		    
			}
			else if (stat.equals(1))
			    buttonConnect.setText("Disconnect");			    
		    }
		    break;
		case Client.TIMER: // Periodic timer 

		    // Sanity check: close terminated thread if not by other means
		    if (connectthread != null){
			if (connectthread.getState().equals(Thread.State.TERMINATED)){
			    Log.d("RStrace", "TIME: Thread TERMINATED");
			    connectthread = null;
			}
		    }

		    message = Message.obtain();
		    message.what = Client.TIMER;
		    mHandler.sendMessageDelayed(message, TIMERINTERVAL);
		    break;
		case Client.REPLAY: // Replay all sensd data
		    if (ploth != null)
			for (String str:report)
			    message(ploth, Client.SENSD, str);
		    break;
		}
	    }
	};

}

