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
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.os.Handler;
import android.os.Bundle;
import android.os.Message;
import android.widget.ImageView;
import android.widget.Toast;
import android.content.res.Configuration;
import android.content.Intent;
import android.view.Display;
import android.util.Log;

public class PlotWindow extends Activity implements OnTouchListener{
    private static int XWINDOW = 10; // how many seconds to show default
    public static int THREADKILLTIMEOUT = 3; // interval until kill rcv thread s
    public static long LOSSTIMEOUT = 2; // interval [s] until pkt considered lost
    public static long PURGETIMEOUT = 20; // interval [s] until pkt info purged

    private static int PLOTINTERVAL = 1000; // interval between plot calls in ms
    private static int SAMPLEINTERVAL = 1000; // interval between sample receives
    final public static int PLOT = 5;      // Message
    final public static int SAMPLE = 8;      // Message	

    private Plot plot;
    private PlotVector power;
    private PlotVector random; // Test

    private Random rnd;

    private int seq = 0;
    private boolean resetPending = false;

    private Pt touch_p = new Pt();    // Last mouse pointer touch point
    private long touch_t;             // Last mouse pointer time in ms
    private double zoomDist_x;        // How much to zoom in x-axis die to mouse pointer move
    private double zoomDist_y;        // How much to zoom in y-axis die to mouse pointer move

    private static int NONE = 0;
    private static int DRAG = 1;
    private static int ZOOM = 3;
    private int touch_mode = NONE;
    private ImageView image;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.plot);
	image = (ImageView) findViewById(R.id.img);
	image.setOnTouchListener(this);

	rnd = new Random(42); // Init random generator


	plot = Client.plot;
	power = Client.power;

	if (false){
	    Vector <Pt> vec = new Vector<Pt>(); 
	    random = new PlotVector(vec, "Random", 1, Plot.LINES, plot.nextColor());
	    plot.add(random);
	}
	Display display = getWindowManager().getDefaultDisplay(); 
	plot.newDisplay(display);
	plot.autodraw(image);

	// Initialize messages (plot for plotting, samle for test samples)

	Message message = Message.obtain();
	message.what = PLOT;
	mHandler.sendMessageDelayed(message, PLOTINTERVAL);

	if (false){
	    message = Message.obtain();
	    message.what = SAMPLE; 
	    mHandler.sendMessageDelayed(message, SAMPLEINTERVAL);
	}
    }

    protected void onDestroy(){
	    super.onDestroy();
    }

    // This is code for lower right button menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
	MenuInflater inflater = getMenuInflater();
	inflater.inflate(R.layout.plot_menu, menu);
	return true;
    }	

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
	// Handle item selection
	switch (item.getItemId()) {
	case R.id.about:
	    AboutBox.Show(this);
	    return true;
	case R.id.connect:
	    return true;
	case R.id.text:
	    toActivity("TextWindow");
	    return true;
	case R.id.sensors:
	    return true;
	case R.id.tags:
	    return true;
	case R.id.prefs:
	    toActivity("Prefs");
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

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
	super.onConfigurationChanged(newConfig);
	setContentView(R.layout.plot);

	Display display = getWindowManager().getDefaultDisplay(); 
	plot.newDisplay(display);

	image = (ImageView)findViewById(R.id.img);
	image.setOnTouchListener(this);
    }

    private double spacing(MotionEvent event) {
	double x = event.getX(0) - event.getX(1);
	double y = event.getY(0) - event.getY(1);
	return Math.sqrt(x * x + y * y);
    }	
    @Override
    public boolean onTouch(View v, MotionEvent event) {
	switch (event.getAction() & MotionEvent.ACTION_MASK) {
	case MotionEvent.ACTION_DOWN:
	    touch_p.set(event.getX(), event.getY(), 0);
	    touch_t = System.currentTimeMillis();
	    touch_mode = DRAG;
	    plot.liveUpdate = false;
//	    Log.d("RStrace", "touch_mode=DRAG");
	    break;
	case MotionEvent.ACTION_POINTER_DOWN:
	    double zoomDist= spacing(event);
	    if (zoomDist > 10f) {
		zoomDist_x = Math.abs(event.getX(0) - event.getX(1));
		zoomDist_y = Math.abs(event.getY(0) - event.getY(1));
		touch_mode = ZOOM;
//		Log.d("RStrace", "touch_mode=ZOOM" );
	    }
	    break;
	case MotionEvent.ACTION_UP:
	case MotionEvent.ACTION_POINTER_UP:
	    touch_mode = NONE;
//	    Log.d("RStrace", "touch_mode=NONE");
	    plot.autodraw(image);
	    break;
	case MotionEvent.ACTION_MOVE:
	    if (touch_mode == DRAG) {
		double delta = (event.getX() - touch_p.x)/(System.currentTimeMillis()-touch_t);
		touch_p.set(event.getX(), event.getY(), 0);
		touch_t = System.currentTimeMillis();
//		Log.d("RStrace", "ACTION_MOVE DRAG delta="+delta);
		plot.xmax_add(-7*delta);
	    }
	    else{
//	    Log.d("RStrace", "ACTION_MOVE OTHER");
		if (touch_mode == ZOOM){
		    double dist_x = Math.abs(event.getX(0) - event.getX(1));
		    double dist_y = Math.abs(event.getX(0) - event.getX(1));
		    if (dist_x > 10f) {
			if (zoomDist_x > 0)
			    plot.xscale_mult(zoomDist_x / dist_x);
			zoomDist_x = dist_x;
		    }
		    if (dist_y > 10f) {
			if (zoomDist_y > 0)
			    plot.yscale_mult(zoomDist_y / dist_y);
			zoomDist_y = dist_y;
		    }
		}
	    }
	    break;
	}	
	return true; // indicate event was handled
    }



    private final Handler mHandler = new Handler() {
	    public void handleMessage(Message msg) {
		Message message;
		switch (msg.what) {
		case SAMPLE:
		    message = Message.obtain();
		    if (true){ // debug
			int y = rnd.nextInt(10);
			Pt p = new Pt(1381599669+seq, y, seq);
			random.sample(p);
			seq++;
		    }
		    message.what = SAMPLE;
		    mHandler.sendMessageDelayed(message, SAMPLEINTERVAL);
		    break;
		case PLOT:
		    message = Message.obtain();
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