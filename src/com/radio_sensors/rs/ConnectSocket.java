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


class ConnectSocket implements Runnable {
    private String  server_ip;
    private int     server_port;
    private Handler mainHandler; 
    public static Handler ploth = null; // PlotWindow handler
    private Socket socket;

    ConnectSocket(String srv_ip, int srv_port, Handler h){ 
	server_ip = srv_ip;
	server_port = srv_port;
	mainHandler = h;
    }

    // Send a message to other activity
    protected void message(Handler h, int what, Object msg){
	Message message = Message.obtain();
	message.what = what;
	message.obj = msg;
	h.sendMessage(message); // To other activity
    }

    public void run() {
	InputStream streamInput;

	Log.d("RStrace", String.format("ConnectSocket Server=%s Port=%d", server_ip, server_port));
	try {
	    socket = new Socket(server_ip, server_port);
	}
	catch (Exception e0)  {
	    String str = e0.getMessage();
	    
	    if(str == null)
		str = "Connection closed";
	    else
		str = "Cannot connect to server:\r\n" + str;
	    message(mainHandler, Client.ERROR, str);
	    return;
	}
	try {
	    streamInput = socket.getInputStream();
	}
	catch (Exception e0){
	    String str = e0.getMessage();
	    message(mainHandler, Client.ERROR, str);
	    return;
	}

	// Signal connected
	message(mainHandler, Client.STATUS, new Integer(1));

	Log.d("RStrace", String.format("ConnectSocket Open"));
	byte[] buf = new byte[2048];
	while (true){
	    int j = 0;
	Log.d("RStrace", String.format("ConnectSocket loop 1"));
	    if (Thread.currentThread().interrupted()) 
                break;
	Log.d("RStrace", String.format("ConnectSocket loop 2"));
	    try	{
		int i = buf.length;
		j = streamInput.read(buf, 0, i);
		Log.d("RStrace", String.format("ConnectSocket j="+j));
		if (j == -1) {
		    message(mainHandler, Client.ERROR, "Error while reading socket.");
		    break;
		}
	    }
	    catch (Exception e0){
		String str = e0.getMessage();
		Log.d("RStrace", String.format("ConnectSocket exception %s", str));
		message(mainHandler, Client.ERROR, str);
		break;
	    }
	    if (j == 0)
		continue;
	    if (Thread.currentThread().interrupted()) 
                break;
	    Log.d("RStrace", String.format("ConnectSocket loop 2"));
	    final String strData = new String(buf, 0, j).replace("\r", "");
	    Log.d("RStrace", "strData:"+strData);
	    if (ploth != null)
		message(ploth, Client.SENSD, strData);
	    message(mainHandler, Client.SENSD, strData);
	    Log.d("RStrace", String.format("ConnectSocket loop 3"));
	} // while
	try {
	    socket.close();
	}
	catch (IOException e0) {
	    e0.printStackTrace();
	}
	// Signal disconnected
	message(mainHandler, Client.STATUS, new Integer(0));
	Log.d("RStrace", String.format("ConnectSocket quit thread"));
    }
    public void kill(){
	try {
	    socket.close();
	}
	catch (IOException e0) {
	    e0.printStackTrace();
	}
    }
}
