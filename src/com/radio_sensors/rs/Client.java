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

public class Client extends Activity {
    // Messages
    final public static int ERROR  = -1;         // Something went wrong
    final public static int STATUS = 2;          // Status change
    final public static int SENSD  = 3;          // Report arrived from sensd server
    final public static int TIMER  = 4;       // Interval timer every 1s (debug)

    private static int TIMERINTERVAL = 1000; // interval between sample receives

    private String server_ip = "";
    private int server_port = 0;
    private Handler handler = null;
    private Thread connectthread = null;
    public static Client client = null;

    ConnectSocket connect_cs;

    // Debug 
    final static int DEBUG_NONE        = 0;
    final static int DEBUG_REPORT      = 1;
    final static int DEBUG_PLOT        = 2;
    final static int DEBUG_FILTER      = 3;

    public static int debug       = DEBUG_NONE;

    // Textwindow
    private ArrayList<String> report = new ArrayList<String>();
    private int max_lines = 20;

    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	client = this;
	setContentView(R.layout.main);
	this.handler = new Handler();
	// Set-up default values from prefs
	EditText et = (EditText) findViewById(R.id.server_ip);
	et.setText(get_server_ip());
	et = (EditText) findViewById(R.id.server_port);
	et.setText(""+get_server_port());
	Message message = Message.obtain();
	message.what = Client.TIMER;
	mHandler.sendMessageDelayed(message, TIMERINTERVAL);
    }

    public void onClick(View view) {
	// We can can change throughout the connection
	EditText et_srv = (EditText) findViewById(R.id.server_ip);
	EditText et_port = (EditText) findViewById(R.id.server_port);

	if(connectthread != null) {
	    Toast.makeText(this, "Disconnecting...", Toast.LENGTH_LONG).show();
	    disconnect();
	    return;
	}
	server_ip = et_srv.getText().toString();
	server_port = Integer.parseInt(et_port.getText().toString());
	
	connect(server_ip, server_port);
    }

    protected void onStart(){
	super.onStart();
    }

    protected void onResume(){
	super.onResume();
    }

    protected void onPause(){
	super.onPause();
    }

    protected void onStop(){  // This is called when starting another activity
	super.onStop();
    }

    protected void onDestroy()	{
	super.onDestroy();
	if(connectthread != null) {
	    try		    {
		connectthread.interrupt();
//		connectthread = null;
	    }
	    catch (Exception e0) {
		e0.printStackTrace();
	    }
	}

    }

    // This is called on resize
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
	super.onConfigurationChanged(newConfig);
	setContentView(R.layout.main);
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

    // This is code for lower right button menu
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

    private void connect(String srv_ip, int srv_port) {
	if(connectthread != null){
	    connectthread.interrupt();
//	    connectthread = null;
	}
	connect_cs = new ConnectSocket(srv_ip, srv_port, mHandler); // Here add parameters
	connectthread = new Thread(connect_cs, "Connect Socket");
	connectthread.start();
    }

    private void disconnect() {
	if(connectthread != null) {
	    try{
		connectthread.interrupt();
		connect_cs.kill();
//		connectthread = null;
	    }
	    catch (Exception e1)  {
		e1.printStackTrace();
	    }
	}

	// Tell activity status has changed
	Message message = Message.obtain();
	message.what = STATUS;
	mHandler.sendMessage(message);
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

    // Messages comes in from socket-handler due to sensd input or error
    private final Handler mHandler = new Handler() {
	    public void handleMessage(Message msg ) {
		Message message;
		switch (msg.what) {
		case Client.SENSD: // New report from sensd			
		    String s = (String)msg.obj;
		    if (s.length()>0)
			report.add(s) ;
		    if (report.size() >= max_lines)
			report.remove(0); //remove first line if max
		    textupdate();
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

			}
			if (stat.equals(1))
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
		}
	    }
	};

}

