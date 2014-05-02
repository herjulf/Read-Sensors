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


public class Client extends RSActivity {
    // Messages
    final public static int ERROR  = -1;         // Something went wrong
    final public static int STATUS = 2;          // Status change
    final public static int STATUS_USB = 3;          // Status change
    final public static int SENSD  = 4;          // Report from sensd server
    final public static int TIMER  = 5;       // Interval timer every 1s (debug)
    final public static int REPLAY = 6;       // Replay all stored sensd data
    final public static int SENSD_CMD  = 10;          // Report arrived from sensd server

    private static int TIMERINTERVAL = 2000; // interval between sample receives


    private Thread connectthread = null;
    public  Thread usbthread = null;
    public static Client client = null;
    private boolean active = false;           // Activity is active
    
    ConnectSocket connect_cs;                 // Object containing connect-socket
    public static ConnectUSB USB = null; 

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
	Log.d("RStrace", "Main onCreate");
	client = this; 
	main = this;

	setContentView(R.layout.main);

	pref2running(); 	// Set global values from persistent storage

	Button buttonConnect = (Button) findViewById(R.id.server_connect);
	buttonConnect.setText("Connect");

	// Start periodic timer
	Message message = Message.obtain();
	message.what = Client.TIMER;
	mHandler.sendMessageDelayed(message, TIMERINTERVAL);
    }

    // This is called on resize
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
	super.onConfigurationChanged(newConfig);

	setContentView(R.layout.main);
	Log.d("RStrace", "Main onConfigurationChanged");

	// Set edit text fields from prefs
	EditText et = (EditText) findViewById(R.id.server_ip);
	et.setText(get_server_ip());
	et = (EditText) findViewById(R.id.server_port);
	et.setText(""+get_server_port());

	textupdate(); // Update text-sensd reports in window

	// Set connected/disconnected button text
	Button buttonConnect = (Button) findViewById(R.id.server_connect);
	if(connectthread != null)
	    buttonConnect.setText("Disconnect");
	else
	    buttonConnect.setText("Connect");

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

	if(usbthread != null) {
	  disconnect();
	  return;
	}
	
	/* USBConnect introduced in SDK version 12 */
	Log.d("RStrace", "SDK version="+android.os.Build.VERSION.SDK_INT);
	if (android.os.Build.VERSION.SDK_INT >= 12) {
	    // only for gingerbread and newer versions
	    if(connect_usb() == true) {
		return;
	    }
	}
	set_server_ip(et_srv.getText().toString());
	set_server_port(Integer.parseInt(et_port.getText().toString()));
	connect(get_server_ip(), get_server_port());
    }

    protected void onStart(){
	super.onStart();

	// Set-up default values from running
	EditText et = (EditText) findViewById(R.id.server_ip);
	et.setText(get_server_ip());
	et = (EditText) findViewById(R.id.server_port);
	et.setText(""+get_server_port());

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


    // This is code for options menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
	MenuInflater inflater = getMenuInflater();
	inflater.inflate(R.layout.main_menu, menu);
	return true;
    }	

    /*
      Called every time right before the option menu is shown.
      use this method to efficiently enable/disable items or 
      otherwise dynamically modify the contents. 
    */

    @Override
	public boolean onPrepareOptionsMenu(Menu menu) {

	MenuItem item = menu.findItem(R.id.conf);

	if(usbthread != null) 
	    item.setEnabled(true);
	else 
	    item.setEnabled(false);

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
	case R.id.conf:
	    toActivity("ConfWindow");
	    return true;
	default:
	    return super.onOptionsItemSelected(item);
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

    private boolean connect_usb() {
	if(usbthread != null){
	    // Should not happen
	    return true;
	}

	USB = new ConnectUSB(mHandler);
	if( USB.connect() == false )
	    return false;

	usbthread = new Thread(USB, "USB connect");
	usbthread.start();
	return true;
    }

    // Post an interrupt to the connect thread and call its kill method
    private void disconnect() {

	if(usbthread != null) {
	    try{
		USB.kill();
		usbthread.interrupt();
		// usbthread is set to null only when detected by mHandler
	    }
	    catch (Exception e1) {
		e1.printStackTrace();
	    }
	}

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




    // Messages comes in from socket-handler due to sensd input or error
    public final Handler mHandler = new Handler() {
	    public void handleMessage(Message msg ) {
		Message message;
		switch (msg.what) {
		case Client.SENSD: // New report from sensd			
		    String s = (String)msg.obj;
		    if (s.length()>0)
			report.add(s) ;
		    if (report.size() >= get_max_samples())
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
		case Client.STATUS_USB: // Connect status changed
		    stat = (Integer)msg.obj;
		    buttonConnect = (Button) findViewById(R.id.server_connect);

		    Log.d("RStrace", "Client.Status="+stat);
		    if (stat != null){
			if (stat.equals(0)){
			    if (usbthread!= null &&
				usbthread.getState().equals(Thread.State.TERMINATED)){
				Log.d("RStrace", "STATUS: USB Thread TERMINATED");
				usbthread = null;
			    }
			    usbthread = null;
			    buttonConnect.setText("Connect USB");			    
			    Toast.makeText(Client.client, "USB Disconnected", Toast.LENGTH_SHORT).show();
			}
			else if (stat.equals(1)) {
			    buttonConnect.setText("Disconnect USB");
			    Toast.makeText(Client.client, "USB Connected", Toast.LENGTH_SHORT).show();
			}
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

