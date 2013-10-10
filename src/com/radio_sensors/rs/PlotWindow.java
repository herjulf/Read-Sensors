// Copyright (C) 2013 Olof Hagsand
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
import android.os.Handler;
import android.os.Bundle;
import android.os.Message;
import android.widget.ImageView;
import android.content.res.Configuration;
import android.view.Display;
import android.util.Log;

public class PlotWindow extends Activity implements OnTouchListener{
    private static int XWINDOW = 10; // how many seconds to show default
    public static int THREADKILLTIMEOUT = 3; // interval until kill rcv thread s
    public static long LOSSTIMEOUT = 2; // interval [s] until pkt considered lost
    public static long PURGETIMEOUT = 20; // interval [s] until pkt info purged

    private static int PLOTINTERVAL = 500; // interval between plot calls in ms
    private static int SAMPLEINTERVAL = 2000; // interval between sample receives
    final public static int PLOT = 5;      // Message
    final public static int SAMPLE = 8;      // Message	

    private Plot plot;
    private PlotVector power;
    private PlotVector random; // Test

    private Random rnd;

    private int seq = 0;
    private boolean resetPending = false;


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.plot);
	View v = findViewById(R.id.img);
	v.setOnTouchListener(this);

	rnd = new Random(42); // Init random generator

	plot = Client.plot;
	power = Client.power;
	ImageView image = (ImageView) findViewById(R.id.img);
	plot.autodraw(image);
	// Add one graph (plotvector) for power
//	Vector <Pt> vec = new Vector<Pt>(); 
//	random = new PlotVector(vec, "temp", 1, Plot.LINES, plot.nextColor());
//	plot.add(random);
	
	// Initialize messages (plot for plotting, samle for test samples)
	Message message = Message.obtain();
	message.what = PLOT;
	mHandler.sendMessageDelayed(message, PLOTINTERVAL);
	message = Message.obtain();
	message.what = SAMPLE; 
	mHandler.sendMessageDelayed(message, SAMPLEINTERVAL);
    }

    @Override
	public void onConfigurationChanged(Configuration newConfig) {
	  super.onConfigurationChanged(newConfig);
	  setContentView(R.layout.plot);
//	  Display display = getWindowManager().getDefaultDisplay(); 
//	  plot = new Plot(R.id.img, display);
	  // Copy from old / resize plot
	}

    public boolean onTouch(View v, MotionEvent event) {
	return true; // indicate event was handled
    }


    private final Handler mHandler = new Handler() {
	    public void handleMessage(Message msg) {
		Message message;
		    switch (msg.what) {
		    case SAMPLE:
			message = Message.obtain();
			if (true){
			    int y = rnd.nextInt(10);
			    Pt p = new Pt(seq, y, seq);
//			    random.sample(p);
			    seq++;
			    message.what = SAMPLE;
			    mHandler.sendMessageDelayed(message, SAMPLEINTERVAL);
			}
			break;
		    case PLOT:
			message = Message.obtain();
			ImageView image = (ImageView) findViewById(R.id.img);
			plot.autodraw(image);
			message.what = PLOT;
			mHandler.sendMessageDelayed(message, PLOTINTERVAL);
			break;
		    default:	
			Log.e("Test handler", "Unknown what: " + msg.what + " "+(String) msg.obj);  			
			break;
		    }
	    }
	};

}