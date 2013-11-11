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
import android.widget.Toast;
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

class ConnectUSB implements Runnable {
    private Handler mainHandler; 
    private boolean killed = false;

    private SerialInputOutputManager mSerialIoManager;
    private UsbManager manager;
    private UsbSerialDriver driver;

    ConnectUSB(Handler h){ 
	usb_connect();
	mainHandler = h;
    }

    public boolean usb_connect() {

	Log.d("USB", "Connect 00");
	manager = (UsbManager) Client.client.getSystemService(Context.USB_SERVICE);
	driver = UsbSerialProber.acquire(manager);

	Log.d("USB", "Connect 01");

	if (driver != null) {
	    try {
		driver.open();
		driver.setParameters(38400, 8, UsbSerialDriver.STOPBITS_1, UsbSerialDriver.PARITY_NONE);
		Toast.makeText(Client.client, "RS USB opened", Toast.LENGTH_SHORT).show();
	    } catch (IOException e) {
		Toast.makeText(Client.client, "Error setting up device: " + e.getMessage(), Toast.LENGTH_SHORT).show();
		//Log.e("USB", "Error setting up device: " + e.getMessage(), e);
		try {
		    driver.close();
		} catch (IOException e2) {
                                        // Ignore.
		}
		driver = null;
		return false;
	    }
	    // Signal connected
	    //message(mainHandler, Client.STATUS_USB, new Integer(1));
	    Log.d("USB", "Connect 02 OK");
	    return true;
	}
	return false;
    }

    // Send a message to other activity
    private void message(Handler h, int what, Object msg){
	Message message = Message.obtain();
	message.what = what;
	message.obj = msg;
	h.sendMessage(message); // To other activity
    }

    public void run() {
	boolean done = false;

	int j = 0;
	while (! done ){
	    if(driver == null)
		usb_connect();
	    try {
		byte buf[] = new byte[1024];
		j = driver.read(buf, 1000000); // 100000 is Delay i ms
		// Send data to plotter and main thread
		if(j == 0) 
		    continue;
		final String strData = new String(buf, 0, j).replace("\r", "");
		j = 0;
		message(mainHandler, Client.SENSD, strData);
		Log.d("RS USB", "run 5 + strData");
	    } catch (IOException e) {
		Log.d("USB", "Error reading device: " + e.getMessage(), e);
	    } 
	}
    }
    
    // Method that closes the socket if open. This is to interrupt the thread
    // waiting in a blocking read
    public void kill(){
	if (driver != null){
	    try {
		driver.close();
	    }
	    catch (IOException e0) {
		e0.printStackTrace();
	    }
	}
	killed = true;
    }
}
