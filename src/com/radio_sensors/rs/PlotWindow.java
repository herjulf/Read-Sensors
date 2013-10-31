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
import java.util.ArrayList;
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
import android.view.View.OnClickListener;
import android.view.MotionEvent;
import android.view.Menu;
import android.view.SubMenu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.os.Handler;
import android.os.Bundle;
import android.os.Message;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.content.res.Configuration;
import android.content.Intent;
import android.view.Display;
import android.util.Log;

public class PlotWindow extends Activity implements OnTouchListener{
    private static int XWINDOW = 10; // how many seconds to show default

    private static int PLOTINTERVAL = 1000; // interval between plot calls in ms
    private long debugseq = 0;              // timrestamp for debug plot
    private static int SAMPLEINTERVAL = 100; // interval between sample receives in ms
    final public static int PLOT = 5;      // Message
    final public static int SAMPLE = 8;      // Debug Sample

    private static String sid = Client.client.get_pref_sid();
    private static String tag = Client.client.get_pref_tag();

    private long nsoffset = 0; // guaranteed monotonic

    private Plot plot;
    private PlotVector debug; 

    private Random rnd;

    private ArrayList <SensdId> idv = new ArrayList<SensdId>(); 

    private boolean runPlot = false;
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

    final private static int ID_SENSORID   = 4242; // Used for dynamic select sub-menu
    final private static int ID_TAG        = 4217;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);

	debugseq = Timestamp2s(Timestamp());
	setContentView(R.layout.plot);
	Client.ploth = mHandler;
	image = (ImageView) findViewById(R.id.img);
	image.setOnTouchListener(this);

	rnd = new Random(42); // Init random generator

	init_plot();
	plot.fontsize_set(Client.client.get_pref_plot_fontsize());
	// Initialize messages (plot for plotting, samle for test samples)

	Message message = Message.obtain();
	message.what = PLOT;
	mHandler.sendMessageDelayed(message, PLOTINTERVAL);

	message = Message.obtain();
	message.what = SAMPLE; 
	mHandler.sendMessageDelayed(message, SAMPLEINTERVAL);

	/* XXX: move to onStart? */
	Display display = getWindowManager().getDefaultDisplay(); 
	plot.newDisplay(display);
	setActive();
	plot.autodraw(image);

    }

    protected void onStart(){
	runPlot = true;
	super.onStart();
	Log.d("RStrace", "PlotWindow onStart");
    }

    protected void onResume(){
	super.onResume();
	Log.d("RStrace", "PlotWindow onResume");
    }

    protected void onPause(){
	super.onPause();
	Log.d("RStrace", "PlotWindow onPause");
    }

    protected void onStop(){  // This is called when starting another activity
	runPlot = false;
	super.onStop();
	Log.d("RStrace", "PlotWindow onStop");
    }

    protected void onDestroy(){ // This is called on 'Back' button
	super.onDestroy();
	// empty timer messages to mkPlot
	if (mHandler.hasMessages(PLOT))
	    mHandler.removeMessages(PLOT);
	if (mHandler.hasMessages(SAMPLE))
	    mHandler.removeMessages(SAMPLE);
	Client.ploth = null;

	Log.d("RStrace", "PlotWindow onDestroy");
    }

    // Send a message to other activity
    private void message(Handler h, int what, Object msg){
	Message message = Message.obtain();
	message.what = what;
	message.obj = msg;
	h.sendMessage(message); // To other activity
    }

    // Initialize the Plot area
    private void init_plot(){
	// Initialize plotter
	Display display = getWindowManager().getDefaultDisplay(); 
	plot = new Plot(R.id.img, display);
	plot.xwin_set(Client.client.get_pref_plot_window());
	plot.xaxis("Time[s]", 1.0);  // x-axis is current time

	// Special debug plotvector (enabled as tag)
	ArrayList <Pt> vec = new ArrayList<Pt>(); 
	debug = new PlotVector(vec, "Debug", 0, Client.client.get_pref_plot_style(), plot.nextColor());
	plot.add(debug);

	message(Client.client.mHandler, Client.REPLAY, null);
    }

    // second & nanosecond to twamp timestamp 
    protected static long ns2Timestamp(long s, long ns){ 
	// Days between 1900-1970: 25569. #seconds in a day: 86400
	long sec = s; //+ 2208988800L;
	long q = ns << 32;
	long subsec = q/1000000000L;
	return (sec<<32) + subsec;
    }

    private static long Timestamp2s(long ts){ 
	long sec = (ts&0xffffffff00000000L)>>32;
	return sec;
    }

    // Get timestamp in owamp/twamp format. Epoch 1970.
    private long Timestamp(){
	if (nsoffset == 0){
	    long ms0 = System.currentTimeMillis();
	    long ns0 = System.nanoTime();
	    nsoffset = ms0*1000000 - ns0;
	}
	long ns = System.nanoTime() + nsoffset;
	long ts = ns2Timestamp(ns/1000000000L, ns%1000000000L);
	return ts;
    }

    // Go through all plots and set active plotvectors according
    // to global sid/tag setting
    private void setActive(){
	SensdId obj;
	SensdTag t;

	if (tag == null)
	    plot.y1label("Misc");
	for(int i=0; i < idv.size() ; i++){
 	    obj = idv.get(i);
	    if (sid == null || obj.id.equals(sid))
		for(int j=0; j < obj.tagv.size(); j++){ 
		    t = obj.tagv.get(j);
		    if (tag == null || t.tag.equals(tag)){
			if (tag != null && t.label != null){
			    plot.y1label(t.label);
			}
			t.pv.setWhere(1);
		    }
		    else{
			t.pv.setWhere(0);
		    }
	    }
	    else
		for(int j=0; j < obj.tagv.size() ; j++){ 
		    t = obj.tagv.get(j);
		    t.pv.setWhere(0);
		}
	}
	if (tag == "Debug")
	    debug.setWhere(1);
	else
	    debug.setWhere(0);

    }

    // This is code for options, called everytime invoked
    public boolean onPrepareOptionsMenu(Menu menu) {
	SensdId obj;
	SubMenu sm;
	MenuItem item;
	item = menu.findItem(R.id.sid);
	if (item != null && (sm = item.getSubMenu()) != null){
	    sm.clear();
	    sm.setHeaderTitle("SensorId");
	    sm.add(NONE, ID_SENSORID, NONE, "All");
	    for(int i=0; i < idv.size() ; i++){ 
		obj = idv.get(i);
		sm.add(NONE, ID_SENSORID, NONE, obj.id);
	    }
	}

	return true;
    }

    // This is code for options menu, called on activity-create. See also
    // onPrepareOptionsMenu which is called on everytime it is shown
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
	MenuInflater inflater = getMenuInflater();
	inflater.inflate(R.layout.plot_menu, menu);
	MenuItem item;
	SubMenu sm;
	SensdId obj;
	item = menu.findItem(R.id.tag);
	if (item != null && (sm = item.getSubMenu()) != null){
	    sm.setHeaderTitle("TagName");
	    sm.add(NONE, ID_TAG, NONE, "All"); 
	    sm.add(NONE, ID_TAG, NONE, "T");
	    sm.add(NONE, ID_TAG, NONE, "PS");
	    sm.add(NONE, ID_TAG, NONE, "P");
	    sm.add(NONE, ID_TAG, NONE, "V_MCU");
	    sm.add(NONE, ID_TAG, NONE, "RH");
	    sm.add(NONE, ID_TAG, NONE, "RND");
	    sm.add(NONE, ID_TAG, NONE, "V_IN");
	    sm.add(NONE, ID_TAG, NONE, "V_A1");
	    sm.add(NONE, ID_TAG, NONE, "V_A2");
	    sm.add(NONE, ID_TAG, NONE, "V_A3");
	    sm.add(NONE, ID_TAG, NONE, "GWGPS_LON");
	    sm.add(NONE, ID_TAG, NONE, "GWGPS_LAT");
	    sm.add(NONE, ID_TAG, NONE, "P0");
	    sm.add(NONE, ID_TAG, NONE, "P0_T");
	    sm.add(NONE, ID_TAG, NONE, "P1");
	    sm.add(NONE, ID_TAG, NONE, "P1_T");
	    sm.add(NONE, ID_TAG, NONE, "LQI");
	    sm.add(NONE, ID_TAG, NONE, "ADDR");
	    sm.add(NONE, ID_TAG, NONE, "SEQ");
	    sm.add(NONE, ID_TAG, NONE, "RSSI");
	    sm.add(NONE, ID_TAG, NONE, "DRP");
	    sm.add(NONE, ID_TAG, NONE, "PRX");
	    sm.add(NONE, ID_TAG, NONE, "BRX");
	    sm.add(NONE, ID_TAG, NONE, "PPS");
	    sm.add(NONE, ID_TAG, NONE, "MBPS");
	    sm.add(NONE, ID_TAG, NONE, "Debug");
	}
	return true;
    }	

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
	// Handle item selection
	String str;
	switch (item.getItemId()) {
	case ID_SENSORID: /* sensorid */
	    str = (String)item.getTitle();
	    if (str.equals("All")){
		sid = null;
	    }
	    else
		sid = str; // active sid
	    return true;
	case ID_TAG: /* tag */
	    str = (String)item.getTitle();
	    if (str.equals("All"))
		tag = null;
	    else
		tag = str;
	    return true;
	case R.id.sid:
	case R.id.tag:
	    return true;
	case R.id.replot:
	    plot.reset();
	    return true;
	case R.id.prefs:
	    toActivity("PrefWindow");
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
	plot.fontsize_set(Client.client.get_pref_plot_fontsize());
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
	    touch_p.set(event.getX(), event.getY());
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
	    setActive();
	    plot.autodraw(image);
	    break;
	case MotionEvent.ACTION_MOVE:
	    if (touch_mode == DRAG) {
		double delta = (event.getX() - touch_p.x)/(System.currentTimeMillis()-touch_t);
		touch_p.set(event.getX(), event.getY());
		touch_t = System.currentTimeMillis();
//		Log.d("RStrace", "ACTION_MOVE DRAG delta="+delta);
		plot.xmax_add(-7*delta);
	    setActive();
	    plot.autodraw(image);
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

    private SensdId findid(String id){
	SensdId obj;

	for(int i=0; i < idv.size() ; i++){ 
	    obj = idv.get(i);
	    if (obj.id.equals(id)){
		return obj;
	    }
	}	
	return null;
    }

    private void add_sample(String id, String tag, Long x, Double y, String label){
	SensdId sid;
	SensdTag stag;

	// 1. See if id exists in idvector, if no create it
	if ((sid = findid(id)) == null){
	    sid = new SensdId(id);
	    idv.add(sid);
	}
	// 2. See if tag exists in tagvector, if no, create it
	if ((stag = sid.findtag(tag)) == null){
	    stag = new SensdTag(tag, label);
	    sid.tagv.add(stag);
	    ArrayList <Pt> vec = new ArrayList<Pt>(); 
	    stag.pv = new PlotVector(vec, label, 0, Client.client.get_pref_plot_style(), plot.nextColor());
	    plot.add(stag.pv); //?
	}
	// 3. Add to plotvector
	Pt p = new Pt(x, y);
	stag.pv.sample(p);
    }


    /*
     * Process sensd message.
     * Identify ID and tags.
     * Add entries to PlotVectors
     * For tag documentation see README.md of https://github.com/herjulf/sensd
     * Example sensd string: 
     * 2013-10-12 19:41:14 UT=1381599674 &: E64=fcc23d000000511d PS=0 T=22.13  V_MCU=3.08 UP=28DD1 V_IN=4.47  V_A3=0 
     */
    private int sensd_msg(String s){
	String[] sv;
	String[] sv2;
	String   tag, val;
	Long     time=null; /* Time == x-coordinate */
	String   id = null;
	Double   y;

	sv = s.split("[ \n\\[\\]]");
	Log.d("RStrace", "report="+s);
	/* Loop 1 : first identify time and id. Maybe they come in reverse order? */
	for (int i=0; i<sv.length; i++){	
	    if (sv[i].length() == 0)
		continue;
	    sv2 = sv[i].split("=");
	    if (sv2.length != 2)
		continue;
	    tag = sv2[0];
	    val = sv2[1];
	    if (tag.equals("UT")){ // Unix time (Id)
		time = new Long(val);
	    }
	    else if (tag.equals("TZ")){ // Time Zone (String)
	    }
	    else if (tag .equals("ID")){ // Unique 64 bit ID (S)
		id = val;
	    }
	    else if (tag .equals("E64")){ // EUI-64 Unique 64 bit ID (S)
		id = val;
	    }
	}
	if (id == null || time == null){
	    Log.e("RStrace", "Sensd report does not contain id or time");
	    return -1;
	}
	/* Loop 2 : All value tags */
	for (int i=0; i<sv.length; i++){	
	    if (sv[i].length() == 0)
		continue;
	    sv2 = sv[i].split("=");
	    if (sv2.length != 2)
		continue;
	    tag = sv2[0];
	    val = sv2[1];
	    if (tag.equals("T")){ // temp in Celsius (F)
		y = Double.parseDouble(val);
		add_sample(id, tag, time, y, "Temp[C]");
	    }
	    else if (tag.equals("PS")){ // Power Save Indicator (B)
		try{
		    y = Double.parseDouble(val);
		}
		catch (NumberFormatException e){
		    Log.e("RStrace", "Illegal number format:"+tag+"="+val);
		    return 0;
		}

		add_sample(id, tag, time, y, "PS");
	    }
	    else if (tag.equals("P")){ // Pressure (F)
		try{
		    y = Double.parseDouble(val);
		}
		catch (NumberFormatException e){
		    Log.e("RStrace", "Illegal number format:"+tag+"="+val);
		    return 0;
		}

		add_sample(id, tag, time, y, "P");
	    }
	    else if (tag.equals("V_MCU")){ // Microcontorller Voltage (F)
		try{
		    y = Double.parseDouble(val);
		}
		catch (NumberFormatException e){
		    Log.e("RStrace", "Illegal number format:"+tag+"="+val);
		    return 0;
		}

		add_sample(id, tag, time, y, "V_MCU");
	    }
	    else if (tag.equals("UP")){ // Uptime (Ih)
//		y = Double.parseDouble(val);
//		add_sample(id, tag, time, y, "UP");
	    }
	    else if (tag.equals("RH")){ // Relative Humidity in % (F)
		try{
		    y = Double.parseDouble(val);
		}
		catch (NumberFormatException e){
		    Log.e("RStrace", "Illegal number format:"+tag+"="+val);
		    return 0;
		}

		add_sample(id, tag, time, y, "RH");
	    }
	    else if (tag.equals("RND")){ // Random 0-999 
		try{
		    y = Double.parseDouble(val);
		}
		catch (NumberFormatException e){
		    Log.e("RStrace", "Illegal number format:"+tag+"="+val);
		    return 0;
		}

		add_sample(id, tag, time, y, "Random Data Test");
	    }
	    else if (tag.equals("V_IN")){ // Voltage Input (F)
		try{
		    y = Double.parseDouble(val);
		}
		catch (NumberFormatException e){
		    Log.e("RStrace", "Illegal number format:"+tag+"="+val);
		    return 0;
		}

		add_sample(id, tag, time, y, "V_IN");
	    }
	    else if (tag.equals("V_A1")){ // Voltage Analog 1 (A1) (F)
		try{
		    y = Double.parseDouble(val);
		}
		catch (NumberFormatException e){
		    Log.e("RStrace", "Illegal number format:"+tag+"="+val);
		    return 0;
		}

		add_sample(id, tag, time, y, "V_A1");
	    }
	    else if (tag.equals("V_A2")){ // Voltage Analog 1 (A2) (F)
		try{
		    y = Double.parseDouble(val);
		}
		catch (NumberFormatException e){
		    Log.e("RStrace", "Illegal number format:"+tag+"="+val);
		    return 0;
		}

		add_sample(id, tag, time, y, "V_A2");
	    }
	    else if (tag.equals("V_A3")){ // Voltage Analog 1 (A3) (F)
		try{
		    y = Double.parseDouble(val);
		}
		catch (NumberFormatException e){
		    Log.e("RStrace", "Illegal number format:"+tag+"="+val);
		    return 0;
		}

		add_sample(id, tag, time, y, "V_A3");
	    }
	    else if (tag.equals("RSSI")){ // Reeiver Signal Strengh Indicator (Id)
		try{
		    y = Double.parseDouble(val);
		}
		catch (NumberFormatException e){
		    Log.e("RStrace", "Illegal number format:"+tag+"="+val);
		    return 0;
		}

		add_sample(id, tag, time, y, "RSSI");
	    }
	    else if (tag.equals("LQI")){ // Link Quality Indicator (Id)
		try{
		    y = Double.parseDouble(val);
		}
		catch (NumberFormatException e){
		    Log.e("RStrace", "Illegal number format:"+tag+"="+val);
		    return 0;
		}

		add_sample(id, tag, time, y, "LQI");
	    }
	    else if (tag.equals("SEQ")){ // Sequental Number (packet) (Id)
		try{
		    y = Double.parseDouble(val);
		}
		catch (NumberFormatException e){
		    Log.e("RStrace", "Illegal number format:"+tag+"="+val);
		    return 0;
		}

		add_sample(id, tag, time, y, "SEQ");
	    }
	    else if (tag.equals("DRP")){ // Drop Probability (Contiki) (F)
		try{
		    y = Double.parseDouble(val);
		}
		catch (NumberFormatException e){
		    Log.e("RStrace", "Illegal number format:"+tag+"="+val);
		    return 0;
		}
		add_sample(id, tag, time, y, "DRP");
	    }
	    else if (tag.equals("ADDR")){  // (S)
		try{
		    y = Double.parseDouble(val);
		}
		catch (NumberFormatException e){
		    Log.e("RStrace", "NumberFormatException:"+val);
		    return 0;
		}
		add_sample(id, tag, time, y, "ADDR");

	    }
	    else if (tag.equals("GWGPS_LON")){  // (F)?
		try{
		    y = Double.parseDouble(val);
		}
		catch (NumberFormatException e){
		    Log.e("RStrace", "NumberFormatException:"+val);
		    return 0;
		}
		add_sample(id, tag, time, y, "GWGPS_LON");
	    }
	    else if (tag.equals("GWGPS_LAT")){  // (F)?
		try{
		    y = Double.parseDouble(val);
		}
		catch (NumberFormatException e){
		    Log.e("RStrace", "NumberFormatException:"+val);
		    return 0;
		}
		add_sample(id, tag, time, y, "GWGPS_LAT");
	    }else if (tag.equals("PRX")) { // (F)?
			try {
				y = Double.parseDouble(val);
			} catch (NumberFormatException e) {
				Log.e("RStrace", "NumberFormatException:" + val);
				return 0;
			}
			add_sample(id, tag, time, y, "PRX");
		}else if (tag.equals("BRX")) { // (F)?
			try {
				y = Double.parseDouble(val);
			} catch (NumberFormatException e) {
				Log.e("RStrace", "NumberFormatException:" + val);
				return 0;
			}
			add_sample(id, tag, time, y, "BRX");
		}else if (tag.equals("PPS")) { // (F)?
			try {
				y = Double.parseDouble(val);
			} catch (NumberFormatException e) {
				Log.e("RStrace", "NumberFormatException:" + val);
				return 0;
			}
			add_sample(id, tag, time, y, "PPS");
		}else if (tag.equals("MBPS")) { // (F)?
			try {
				y = Double.parseDouble(val);
			} catch (NumberFormatException e) {
				Log.e("RStrace", "NumberFormatException:" + val);
				return 0;
			}
			add_sample(id, tag, time, y, "MBPS");
		}

	}
	return 0;
    }

    private final Handler mHandler = new Handler() {
	    public void handleMessage(Message msg) {
		Message message;
		Pt p;
		switch (msg.what) {
		case Client.SENSD: // New report from sensd
		    sensd_msg((String)msg.obj);
		    break;
		case SAMPLE: // Periodic debug sample
		    message = Message.obtain();
		    if (tag == "Debug"){
			int y = rnd.nextInt(10);
//			long ts = Timestamp2s(Timestamp());
			p = new Pt(debugseq++, y);
			debug.sample(p);
		    }
		    setActive();
		    plot.autodraw(image);
		    message.what = SAMPLE;
		    mHandler.sendMessageDelayed(message, SAMPLEINTERVAL);
		    break;
		case PLOT:
		    if (!runPlot)
			break;
		    message = Message.obtain();
//		    setActive();
//		    plot.autodraw(image);
		    message.what = PLOT;
		    mHandler.sendMessageDelayed(message, PLOTINTERVAL);
		    break;
		default:	
		    Log.e("Plot handler", "Unknown what: " + msg.what + " "+(String) msg.obj);  			
		    break;
		}
	    }
	};

}

final class SensdTag{
    public String tag;
    public String label;
    public PlotVector pv;

    SensdTag(String tag0, String lbl0){
	tag = tag0;
	label = lbl0;
    }

}

final class SensdId{
    public String id;
    public ArrayList <SensdTag> tagv = new ArrayList<SensdTag>();
    SensdId(String id0){
	id = id0;
    }
    public SensdTag findtag(String tag){
	SensdTag obj;
	for(int i=0; i < tagv.size() ; i++){ 
	    obj = tagv.get(i);
	    if (obj.tag.equals(tag))
		return obj;
	}	
	return null;
    }

}