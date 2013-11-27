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

import java.util.*;
import java.text.*;

import android.hardware.usb.UsbManager;

import com.hoho.android.usbserial.driver.FtdiSerialDriver;
import com.hoho.android.usbserial.driver.UsbId;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialRuntimeException;
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver;
import com.hoho.android.usbserial.util.SerialInputOutputManager;
import com.hoho.android.usbserial.util.HexDump;




class ConnectUSB extends RSActivity implements Runnable {
    private Handler mainHandler; 
    private boolean killed = false;

    private SerialInputOutputManager mSerialIoManager;
    private UsbManager manager;
    private UsbSerialDriver driver;

    private int    report_interval;  
    private int    report_mask;  


    ConnectUSB(Handler h){ 
	usb_connect();
	mainHandler = h;
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

    public boolean usb_connect() {

	manager = (UsbManager) Client.client.getSystemService(Context.USB_SERVICE);
	driver = UsbSerialProber.acquire(manager);

	if (driver != null) {
	    try {
		driver.open();
		driver.setParameters(38400, 8, UsbSerialDriver.STOPBITS_1, UsbSerialDriver.PARITY_NONE);
	    } catch (IOException e) {
		Toast.makeText(Client.client, "Error setting up device: " + e.getMessage(), Toast.LENGTH_SHORT).show();
		Log.e("USB", "Error setting up device: " + e.getMessage(), e);
		try {
		    driver.close();
		} catch (IOException e2) {
                      // Ignore.
		}
		driver = null;
		return false;
	    }
	    parse_settings();
	    return true;
	}
	return false;
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

	    try {
		usb_write("ss\r".getBytes());
		l0 = usb_readline(b1);
		l1 = l0.replace("\n", " ");
		l2 = l1.replace("\r", " ");
		l3 = l2.replace("\t", " ");
	    }
	    catch  (IOException e) {
		Log.e("USB", "Error reading device: " + e.getMessage(), e);
	    }
	    set_report_interval(parse_int(l3, "report_interval")); 
	    set_report_mask(parse_int(l3, "report_mask")); 

	    //Toast.makeText(Client.client, Integer.toString(i), Toast.LENGTH_SHORT).show();

	    Toast.makeText(Client.client, String.format("Interval=%d, Mask=0x%x", get_report_interval(), get_report_mask()), Toast.LENGTH_SHORT).show();
    }

    private String usb_readline(byte[] buf) throws IOException
    {
	int j;
	String line = "";

	while( true ) {
	    try {
		j = driver.read(buf, 0); // buf and delay in ms
		// Send data to plotter and main thread
		
	    } catch (IOException e) {
		throw new IOException(e);
	    } 
	    if(j == 0) 
		continue;
	    String s1 = new String(buf, 0, j);
	    line = line + s1;
	    
	    if(buf[j-1] != 0x1A) {
		continue;
	    }
	    return line;
	}
    }

    private int usb_write(byte[] buf) throws IOException
    {
	int j = 0;

	while( true ) {
	    try {
		j = driver.write(buf, 0); // buf and delay in ms
		// Send data to plotter and main thread
		
	    } catch (IOException e) {
		throw new IOException(e);
	    } 
	    return j;
	}
    }

    public void run() {
	boolean done = false;
	String line = "";
	while (! done ){
	    if(driver == null)
		usb_connect();
	    
	    try {
		byte buf[] = new byte[10000];
		line = usb_readline(buf);
		SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss  ");
		SimpleDateFormat ftz = new SimpleDateFormat("z ");
		message(mainHandler, Client.SENSD, ft.format(new Date())+"TZ="+ftz.format(new Date())+"UT="+(int)System.currentTimeMillis()/1000L+" "+line);
	    }
	    catch  (IOException e) {
		Log.e("USB", "Error reading device: " + e.getMessage(), e);
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
	//message(mainHandler, Client.STATUS_USB, new Integer(0));
    }
}
