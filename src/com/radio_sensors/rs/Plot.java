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
import java.util.ArrayList;
import java.util.TimeZone;
import java.util.Random;
import java.text.DecimalFormat;

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
import android.widget.ImageView;
import android.view.Display;
import android.util.Log;

// Point class using double (PointF uses floats)
final class Pt{
	public double x;
	public double y;
	
	Pt(){
		set(0.0, 0.0);
	}
	
	Pt(double x, double y){
		set(x, y);
	}
	
	public void set(double x0, double y0){
		x = x0;
		y = y0;		
	}
    
	public Point // scale function: return screen coordinates
    pt2screen(int px, int py, int pw, int ph, // screen rectangle. py is upper left
	      double xlow, double xhigh, 
	      double ylow, double yhigh)
    {
    	Point sp = new Point();   // return value
    	if (xlow == xhigh || ylow == yhigh)
    		return null;
    	try {
    		sp.x = (int)(px + pw*(x - xlow)/(xhigh - xlow));
    		sp.y = (int)(py + ph*(yhigh - y)/(yhigh - ylow));
    	} 
    	catch  (Exception e)  {
    		return (null);
    	}
    	return sp;
    } // pt2screen
}


// A single plot instance. Eg a function.
// Contains info about each individual plot.
final class PlotVector {
    public static final float LINEWIDTH = (float)2.0;
    public String title; // title for plot
    public int where;        // y1(=1) or y2 (=2)-axis or not-active (0)
    public ArrayList <Pt> vec;
    public Set <Integer> style = new TreeSet<Integer>(); // XXX: Why must I use TreeSet?
    public int color;
    public double ymin = Double.POSITIVE_INFINITY;
    public double ymax = Double.NEGATIVE_INFINITY;
    private float linewidth = LINEWIDTH;
    public String name;

    PlotVector(ArrayList <Pt> v0, String t0, int w0, int s0, int c0){
    	vec = v0;
    	title = t0;
    	where = w0;
    	style.clear();
    	style.add(s0);
    	color = c0;
    }
    /* where = 0 dont show; 1=show on y1-axis; 2=show on y2-axis */
    public void setWhere(int w0){
	where = w0;
    }

    public void style_set(int s) {
    	style.clear();
    	style.add(s);	
    }

    public void linewidth_set(float w) {
	linewidth = w;
    }
    public float linewidth_get() {
	return linewidth;
    }

    public void sample(Pt pt){
//	Log.d("RStrace", String.format("sample: x=%f y=%f", pt.x, pt.y));
	if (pt.y < ymin)
	    ymin = pt.y;
	if (pt.y > ymax)
	    ymax = pt.y;
	vec.add(pt);
    }
    public void samplexy(double x, double y){
    	Pt pt = new Pt(x, y);  
	sample(pt);
    }

} // PlotVector

// The complete plot
public final class Plot {
    private static final int TICKLEN = 6; // how long solid ticks
    private static final int CROSSHAIR = 5; // how large point crosshair
    public static final int FONTSIZE = 25; // default font size

    public static final int GRIDNR = 3;    // min nr of grid lines

    public static int XWINDOW = 240; // how many seconds to show default

    public static final int POINTS = 1; // style
    public static final int LINES = 2; // style
    public static final int BARS = 3; // style

    private Canvas canvas;
    private ArrayList <PlotVector> plots; // ArrayList of plots 
    private int x, y, w, h; // bitmap x,y,width height
    private int px, py, pw, ph; // plotarea x,y,width height
    private int leftmargin, rightmargin; 
    private int topmargin, bottommargin; 
    private Bitmap bitmap;
    private double xmin, xmax;
    private double xwin = XWINDOW;       // x window size
	
    public boolean liveUpdate = true; // Scroll x-axis as new values arrive

    private boolean textexternal = true; // Text external vs inside plot-area

    private int textcolor = Color.WHITE;
    private int bgcolor = Color.BLACK;

    private int fontsize = FONTSIZE;
    private int nrPlots = 0;	
    private String xlabel = "";  // text to print on x-axis
    private String y1label = ""; // text to print on left-hand y-axis
    private String y2label = ""; // text to print on right-hand y-axis
    private double xscale = 1.0;  // factor to multiply x-values for x-axis text
    private double y1scale = 1.0; // factor to multiply y-values for left y-axis text
    private double y2scale = 1.0; // factor to multiply y-values for right y-axis text

    private int gridx;            /* Min nr of grid-lines in x-direction */
    private int gridy;            /* Min nr of grid-lines in y-direction */
    private int id;                 // resource id

    private Random rnd = new Random(42); // Init random generator;

    Plot(int id0, Display display){ 
	canvas = new Canvas();
	id = id0;
	plots = new ArrayList <PlotVector>();
    }

    /* find plotvector by name */
    public PlotVector pv_find(String name){
        PlotVector pv;
        for (int i=0 ; i < plots.size(); i++ ){
	    pv = plots.get(i);
	    if (pv.name == name)
		return pv;
	}
	return null;
    }

    public void fontsize_set(int sz){
	fontsize = sz;
    }

    private String printyear(Time t){
	return String.format("%d", t.year);
    }

    private String printdate(Time t){
	return String.format("%d-%02d-%02d", t.year, t.month+1, t.monthDay);
    }

    private String printtime(Time t){
	return String.format("%02d:%02d:%02d", t.hour, t.minute, t.second);
    }
    /*
     * A new/changed display needs to get new x/y/w/h values
     */
    public void newDisplay(Display display){
	// bitmap: x,y,w,h
	x = 0;
	y = 0;
	w = display.getWidth();
	h = display.getHeight();
	// See also compute_margins
	// plotarea: px,py,pw,ph
	bitmap = Bitmap.createBitmap(w, h, Config.ARGB_8888); 
	canvas.setBitmap(bitmap);
	/* Automatically compute number of min grid lines in x & y directions */
	gridx = Math.max(GRIDNR, Math.min(5, Math.round(w/(8*fontsize))));
	gridy = Math.max(GRIDNR, Math.min(5, Math.round(h/(8*fontsize))));
    }

    /*
     * When scales are known, margins and plot area can be computed
     */
    private void computeMargins(double y1min, double y1max, double y2min, double y2max){
	int y1, y2;

	this.topmargin = 4;
	if (this.textexternal){
	    final Paint paint = new Paint();
	    paint.setTextSize(fontsize);
	    DecimalFormat format = new DecimalFormat("0.#######");
	    String s1 = format.format(y1min);
	    String s2 = format.format(y1max);
	    float w1 = paint.measureText(s1);
	    float w2 = paint.measureText(s2);
	    this.leftmargin = Math.max((int)w1, (int)w2);
	    this.leftmargin = Math.max(this.fontsize, this.leftmargin);
	    this.leftmargin += this.fontsize/2;
		
	    this.rightmargin = (int)Math.round(this.fontsize*(1+Math.floor(Math.log10(y2max))));
	    this.bottommargin = (int)Math.round(this.fontsize*2.2);
	}
	else{
	    this.leftmargin = 4;
	    this.rightmargin = 4;
	    this.bottommargin = (int)Math.round(this.fontsize*2.2);
	}
	this.px = this.x + this.leftmargin;
	this.py = this.y + this.topmargin;
	this.pw = this.w - this.leftmargin - this.rightmargin;
	this.ph = this.h - this.bottommargin - this.topmargin;
    }


    private int colornr = 0;
    public int nextColor(){
	float[] hsv = new float[3];
	hsv[0] = rnd.nextInt(360); 
	hsv[1] = 1; 
	hsv[2] = 1;
	return Color.HSVToColor(hsv); 
    }
    // Update style on all plotvectors
    public void 
    style_set(int style) {
	PlotVector pv;

	for (int i=0; i<plots.size(); i++){
	    pv = plots.get(i); 
	    pv.style_set(style);
	}
    }
    // Update linewidth on all plotvectors
    public void 
    linewidth_set(float w) {
	PlotVector pv;

	for (int i=0; i<plots.size(); i++){
	    pv = plots.get(i); 
	    pv.linewidth_set(w);
	}
    }
    // Reset plot area, scaling and axes to default
    public void reset(){
	liveUpdate = true;
	xwin = XWINDOW;
	xscale = 1.0;
    }

    public void 
    init(ImageView image) {

    } // init
	
    public int
    get_nrPlots(){
	return nrPlots;
    }
    public void 
    add(PlotVector pv){
	try {
	    plots.add(pv);
	} catch (Exception e) {
	    e.printStackTrace();
	}
	nrPlots++;
    } // add
	
    public void // XXX
    xaxis(String label, double scale) {
	xlabel = label;
	xscale = scale;
    }
    public void // XXX
    xlabel_set(String label) {
	xlabel = label;
    }

    public void 
    xscale_set(double scale) {
	xscale = scale;
    }

    /* Set X window-size. Ie how much data to present in X-direction.
       This is overriden if you rescale the x-window.
     */
    public void 
    xwin_set(double xw_max) {
	xwin = xw_max;
    }

    /* Add to xmax value, ie 'right' x-interval limit */
    public void 
    xmax_add(double xm) {
	xmax += xm;
    }
    public void 
    xmax_set(double xm) {
	xmax = xm;
    }

    /* Multiply xscale value, ie how many x-values per unit */
    public void 
    xscale_mult(double sm) {
	xscale *= sm;
    }

    /* Multiply yscale values (both y1 and y2), ie how many y-values per unit */
    public void 
    yscale_mult(double sm) {
	y1scale *= sm;
	y2scale *= sm;
    }
	
    public void 
    y1label(String label) {
	y1label = label;
    }
	
    public void 
    y2label(String label) {
	y2label = label;
    }

    private static int 
    points_in_interval(ArrayList <Pt>vec, double low, double high){
	int nr = 0;
	for(int i=0; i < vec.size() ; i++){ 
	    double x = vec.get(i).x;
	    if (x < low)
		continue;
	    if (x > high)
		continue;
	    nr++;
	}
	return nr;
    } // points_in_interval

    // Now draw plot and auto-scale axis depending on plot contents
    // XXX: This should be separated into different functions
    public void autodraw(ImageView image){
	PlotVector pv;
	boolean y1vals = false; // There are values in y1 plots
	boolean y2vals = false;  // There are values in y2 plots
	double y1min = Double.POSITIVE_INFINITY;
	double y2min = Double.POSITIVE_INFINITY;
	double y1max = Double.NEGATIVE_INFINITY;
	double y2max = Double.NEGATIVE_INFINITY;

	double xmin1 = Double.POSITIVE_INFINITY; // Tentative x-interval min
	double xmax1 = Double.NEGATIVE_INFINITY;

	// Clear bitmap
	image.setImageBitmap(bitmap); 

	// Loop 1a: compute tentative x-interval [xmin1, xmax1] for y1 plots
	for (int i=0; i<plots.size(); i++){
	    pv = plots.get(i);
	    if (pv.where != 1) /* Keep only y1 plots */
		continue;
	    if (pv.vec.size() > 0){
		y1vals = true; // XXX: inverse logic
		xmin1 = Math.min(xmin1, pv.vec.get(0).x);
		int size = pv.vec.size();
		xmax1 = Math.max(xmax1, pv.vec.get(size-1).x);
	    }
	}
	// Here, [xmin1,xmax1] is complete y1 x-interval of all visible
	// series (assuming monotonically increasing)

	// Loop 1b: Same for y2 plots
	for (int i=0; i<plots.size(); i++){
	    pv = plots.get(i);
	    if (pv.where != 2) /* Keep only y2 plots */
		continue;
	    if (pv.vec.size() > 0){
		y2vals = true; // XXX: inverse logic
	    }
	    
	}

	if (y1vals==false && y2vals==false){
	    xmin1 = 0;
	    xmax1 = 10;
	}

	/* Now compute xwindow: how much to show of x-axis: [xmin, xmax]:
	   If scrolled to old x-values, do not auto-scroll with new values by updating max
	*/

	if (liveUpdate)
	    xmax = xmax1;
	else
	    if (xmax1 < xmax){
		liveUpdate = true;
		xmax = xmax1;
	    }
	/*
                  xmin1                         xmax
	  ----------|------------------------------|------>
                     <-----------xmax-xwin1--------> 
	 */
	double xws = xwin*xscale;
	if (liveUpdate){
	    if (xws > xmax - xmin1){ // window larger than #samples
		xmin = xmin1;         // Start at left of screen
		xmax = xmin + xws;
	    }
	    else
		xmin = xmax - xws;   // window smaller: crop x at window limit
	}
	else{
	    if (xmax < xmin1) // not live and scrolled beyond x-values
		xmax = xmin1 + xws/3; // ensure some x-values are visible.
	    xmin = xmax - xws;
	}
//	Log.d("RStrace", "PLOT xscale="+xscale);
//	Log.d("RStrace", "PLOT xwin="+xwin);
//	Log.d("RStrace", "PLOT xws="+xws);
//	Log.d("RStrace", "PLOT xmax1-xmin1="+(xmax1-xmin1));

	y1vals = false; // XXX: inverse logic
	// Loop 2a:  compute y1 intervals, etc
	for (int i=0; i<plots.size(); i++){
	    pv = plots.get(i); // Only in [xmin,xmax]
	    if (pv.where != 1) /* Keep only y2 plots */
		continue;
	    for (int j = 0 ; j< pv.vec.size(); j++){
		double x = pv.vec.get(j).x;
		double y = pv.vec.get(j).y;
		if (x < xmin)
		    continue;
		if (x > xmax)
		    continue;
		y1vals = true; 
		y1max = Math.max(y1max, y);
		y1min = Math.min(y1min, y);
	    }	    
	}
	if (y1vals==false){
	    y1min = 0;
	    y1max = 1;
	}

	// Loop 2b: Same for y2 plots
	// XXX: Also y2 plots

	if (y2vals==false){
	    y2min = 0;
	    y2max = 1;
	}
	// Compute the x and y axis: where labels should be,...
	Autoscale ax = new Autoscale(xmin, xmax, gridx, "time");
	Autoscale ay1 = new Autoscale(y1min, y1max, gridy, "ten");
	Autoscale ay2 = null;
	if (y2vals)
	    ay2 = new Autoscale(y2min, y2max, gridy, "ten");

	computeMargins(ay1.low, ay1.high, ay2==null?0:ay2.low, ay2==null?0:ay2.high);

	/* pix is the number of pixels used for the displayed points 
	   by computing the number of points in this interval,
	   we can compute the numbers of points/pixel which is aggregation level
        */
	double pix = this.pw*(xmax-xmin)/(ax.high-ax.low);
	double aggF = 0.0; 

	// Loop 3: compute level of aggregation in x-axis
	for (int i=0; i<plots.size(); i++){
	    pv = plots.get(i);
	    if (pv.vec.size() > 0){
		int nr = points_in_interval(pv.vec, xmin, xmax);
		aggF = Math.max(aggF, nr/pix);
	    }
	}
	int agg = Math.max(1, (int)Math.floor(aggF*4));
	
	draw(ax, ay1, ay2, agg); // Finally draw it.	
    }

    // draw plot and auto-scale x-axis with fixed y1-scale. Ignore y2
    // XXX should be merged with autodraw (where y1min,y1max i fixed
    public void draw_y1fix(ImageView image, double y1min, double y1max){
	PlotVector pv;
	double xmin1 = Double.POSITIVE_INFINITY; // Tentative x-interval min
	double xmax1 = Double.NEGATIVE_INFINITY;
	boolean y1vals = false;
	boolean y2vals = false;

	// Clear bitmap
	image.setImageBitmap(bitmap); 

	// Loop 1a: compute tentative x-interval [xmin1, xmax1] for y1 plots
	for (int i=0; i<plots.size(); i++){
	    pv = plots.get(i);
	    if (pv.where != 1) /* Keep only y1 plots */
		continue;
	    if (pv.vec.size() > 0){
		y1vals = true; 
		xmin1 = Math.min(xmin1, pv.vec.get(0).x);
		int size = pv.vec.size();
		xmax1 = Math.max(xmax1, pv.vec.get(size-1).x);
	    }
	}
	if (y1vals==false){
	    xmin1 = 0;
	    xmax1 = 10;
	}

	/* Now compute xwindow: how much to show of x-axis: [xmin, xmax]:
	   If scrolled to old x-values, do not auto-scroll with new values by updating max
	*/

	if (liveUpdate)
	    xmax = xmax1;
	else
	    if (xmax1 < xmax){
		liveUpdate = true;
		xmax = xmax1;
	    }
	/*
                  xmin1                         xmax
	  ----------|------------------------------|------>
                     <-----------xmax-xwin1--------> 
	 */
	double xws = xwin*xscale;
	if (liveUpdate){
	    if (xws > xmax - xmin1){ // window larger than #samples
		xmin = xmin1;         // Start at left of screen
		xmax = xmin + xws;
	    }
	    else
		xmin = xmax - xws;   // window smaller: crop x at window limit
	}
	else{
	    if (xmax < xmin1) // not live and scrolled beyond x-values
		xmax = xmin1 + xws/3; // ensure some x-values are visible.
	    xmin = xmax - xws;
	}

	Autoscale ax = new Autoscale(xmin, xmax, gridx, "time");
	Autoscale ay1 = new Autoscale(y1min, y1max, gridy, "ten");
	Autoscale ay2 = null;
	if (y2vals)
	    ay2 = new Autoscale(y1min, y1max, gridy, "ten"); // XXX y2?

	computeMargins(ay1.low, ay1.high, ay2==null?0:ay2.low, ay2==null?0:ay2.high);

	double pix = this.pw*(xmax1-xmin1)/(ax.high-ax.low);
	double aggF = 0.0; 

	// Loop 3: compute level of aggregation in x-axis
	for (int i=0; i<plots.size(); i++){
	    pv = plots.get(i);
	    if (pv.where == 0) 
		continue;
	    if (pv.vec.size() > 0){
		int nr = points_in_interval(pv.vec, xmin, xmax);
		aggF = Math.max(aggF, nr/pix);
	    }
	}
	int agg = Math.max(1, (int)Math.floor(aggF*4));
	
	draw(ax, ay1, ay2, agg); // Finally draw it.	
    }


    public void 
    draw(Autoscale ax, Autoscale ay1, Autoscale ay2, int agg) {
	ArrayList <Pt> vec;
	// assert |xvec| == |yvec|
	if (!draw_grid(ax, ay1, ay2))
	    return;
	int i=0;
	for(int j=0; j < plots.size() ; j++)  {
	    PlotVector pv = plots.get(j);
	    if (pv.where == 0) /* don't draw */
		continue;
	    vec = pv.vec;
	    draw_plot_label(i, pv.title, pv.color);
	    draw_plot(vec, ax, pv.where==1?ay1:ay2, pv.color, pv.style, agg, 
		      pv.linewidth_get());
	    i++;
	}
    } // draw



    // plot name and label (key) for each graph including color
    private void
    draw_plot_label(int i, String title, int color){
	final Paint paint = new Paint();
	paint.setStyle(Paint.Style.STROKE);
	paint.setColor(this.textcolor);
	paint.setTextSize(fontsize);
	float x = (float)(this.px+0.6*this.pw);
	float x1 = x + title.length()*fontsize*(float)0.8;
	float y = this.py+(i+1)*2*fontsize;
	canvas.drawText(title, x, y, paint);
	paint.setColor(color);
	canvas.drawLine(x1,
			y,	
			x1 +  30, 
			y,
			paint);
    } // draw_plot_label
	
    private void
    draw_grid_x0(Time t, Autoscale ax, String mode, Paint paint){
	String s;
	Time t1;
	if (mode.equals("y"))
	    s = "";
	else
	if (mode.equals("d"))
	    s = "";
	else
	    s = printdate(t);
	canvas.drawText(s, this.x, this.y+this.h-2, paint); // leftalign
	t1 = new Time();
	t1.set((long)(ax.high*1000.0));
	if (t.year != t1.year || t.yearDay != t1.yearDay){
	    // right align
	    paint.setTextAlign(Paint.Align.RIGHT);
	    if (mode.equals("y"))
		s = "";
	    else
		if (mode.equals("d"))
		    s = "";
		else
		    s = printdate(t);
	    canvas.drawText(s, this.x+this.w, this.y+this.h-2, paint); // leftalign
	}
    }

    /*
     * ad-hoc algorithm on which dates to print:
     * |---|---|---..........--|
     * Alt1:
     * dont print first or last
     * if too many grid lines, only print 1,3,5,...
     * Alt2:
     * print first (0) and last (ax1)
     * dont print 1 and ax-1
     * if too many grid lines, only print 2,4,6,...
     * return new pos
     */
    private int
    draw_grid_x(Time t, Autoscale ax, int i, int pos, String mode, Paint paint){
	String s;
	int    x1;
	int    w;
	boolean skip = false;

	if (mode.equals("y"))
	    s = printyear(t);
	else
	    if (mode.equals("d"))
		s = printdate(t);
	    else
		if (mode.equals("s"))
		    s = printtime(t);
		else
		    s = printtime(t);
	w = (int)paint.measureText(s);
	if (i==0){
	    x1 = this.x;
	    paint.setTextAlign(Paint.Align.LEFT);
	    pos = x1 + w;
	}
	else if (i==ax.nr){
	    x1 = this.x+this.w;
	    paint.setTextAlign(Paint.Align.RIGHT);
	    if (x1-w-fontsize < pos)
		skip = true;
	    else
		pos = x1;
	}
	else{
	    x1 = this.px + (i * this.pw/ax.nr);
	    paint.setTextAlign(Paint.Align.CENTER);
	    if (x1-w/2-fontsize < pos)
		skip = true;
	    else
		pos = x1+w/2;
	}
	if (!skip)
	    canvas.drawText(s, x1, this.py+this.ph+this.fontsize+2, paint);
      done:
	return pos;
    }

    /*
     * draw_grid
     * Draw the surrounding rectangle, the labels of the ticks and the dashed lines
     */
    private boolean 
    draw_grid(Autoscale ax, Autoscale ay1, Autoscale ay2){
	    final Paint paint = new Paint();
	    Time t;
	    String s;
	    int ax1  = ax.nr;
	    int ay11 = ay1.nr;
	    String mode = ax.getmode();
	    int pos;

	    paint.setAntiAlias(true);
	    paint.setTextSize(fontsize);
	    paint.setStyle(Paint.Style.FILL); 
	    paint.setColor(this.bgcolor);  
	    canvas.clipRect(x, y, x+w, y+h, Region.Op.REPLACE); // clip it.
	    // Fill the canvas with a color
	    canvas.drawRect(x, y, x + w, y + h, paint);

	    // Draw white plot rectangle
	    paint.setColor(this.textcolor);
	    paint.setStyle(Paint.Style.STROKE); 
	    canvas.drawRect(this.px, this.py, this.px + this.pw, this.py + this.ph, paint);
	    // Write x axis text and dates
	    paint.setTypeface(Typeface.SANS_SERIF ); 
	    paint.setTextSize(fontsize);
	    paint.setColor(this.textcolor);
	    // axis label text
	    canvas.drawText(xlabel, this.px+this.pw/2-fontsize*2, y+h-2, paint);
	    t = new Time();
	    t.set((long)(ax.low*1000.0));  // milliseconds
	    draw_grid_x0(t, ax, mode, paint);

	    // y1 axis
	    canvas.translate(x+fontsize, y+this.ph/2);
	    canvas.rotate(-90, 0, 0);
	    canvas.drawText(y1label, 0, 0, paint);
	    canvas.rotate(90, 0, 0);
	    canvas.translate(-x-fontsize, -y-this.ph/2);
	    // y2 axis
	    canvas.translate(this.px+this.pw+fontsize+2, y+this.ph/2);
	    canvas.rotate(90, 0, 0);
	    canvas.drawText(y2label, 0, 0, paint);
	    canvas.rotate(-90, 0, 0);
	    canvas.translate(-this.px-this.pw-fontsize-2, -y-this.ph/2);
	    // text along x-axis
	    paint.setColor(this.textcolor);
	    if (ax1>0){
		pos = x; /* track position to avoid overwriting */
		for(int i=0; i < ax1+1; i++) { 
		    double x = ax.low+i*ax.spacing;

		    t = new Time(); //ms
		    t.set((long)(x*1000.0));
		    // String s = t.format3339(true); // iso
		    pos = draw_grid_x(t, ax, i, pos, mode, paint);
		}
	    }
	    // text along y-axis
	    if (textexternal) // outside plot area
		paint.setTextAlign(Paint.Align.RIGHT);
	    else              // inside plot area
		paint.setTextAlign(Paint.Align.LEFT);
	    if (ay11>0)
		for(int i=0; i < ay1.nr+1; i++) { 
		    int y1 = this.py + (i * this.ph/ay11); //lower
		    if (i == 0) //upper
			y1 += fontsize;
		    else
			if (i != ay11) // normal case
			    y1 += fontsize/2;
		    DecimalFormat format = new DecimalFormat("0.#######");
		    s = format.format((ay1.high-i*ay1.spacing)*y1scale);
		    if (textexternal)
			canvas.drawText(s, px-4, y1, paint);
		    else
			canvas.drawText(s, x+4, y1, paint);
		}
	    // text along y2-axis
	    if (ay2 != null){
		int ay21 = ay2.nr;
		for(int i=0; i < ay2.nr+1; i++) { 
		    int y2 = this.py + (i * this.ph/ay21); //lower
		    if (i == 0) //upper
			y2 += fontsize;
		    else
			if (i != ay21) // normal case
			    y2 += fontsize/2;
		    s = String.format("%.2f", (ay2.high-i*ay2.spacing)*y2scale);
		    canvas.drawText(s, this.px+this.pw+2, y2, paint);
		}
	    }
	    // Draw small lines at end
	    paint.setColor(this.textcolor);
	    for(int i=1; i < ax1 ; i++) { 
		canvas.drawLine(this.px + (i * this.pw /ax1), 
				this.py,
				this.px+ (i * this.pw/ax1),
				this.py + TICKLEN,
				paint);
		canvas.drawLine(this.px + (i * this.pw/ax1), 
				this.py+this.ph-TICKLEN,
				this.px+ (i * this.pw/ax1),
				this.py + this.ph,
				paint);
	    }
	    for(int i=1; i < ay11; i++) {
		canvas.drawLine(this.px, 
				this.py + (i * this.ph/ay11), 
				this.px+TICKLEN, 
				this.py + (i * this.ph/ay11), 
				paint);
		canvas.drawLine(this.px+this.pw-TICKLEN, 
				this.py + (i * this.ph/ay11), 
				this.px+this.pw, 
				this.py + (i * this.ph/ay11), 
				paint);
	    }
	    // Draw the grid with white dashed lines
	    paint.setColor(this.textcolor);
	    paint.setPathEffect( new DashPathEffect(new float[] { 2, 8 }, 0));
	    for(int i=1; i < ay11; i++)  
		canvas.drawLine(this.px, 
				this.py + (i * this.ph /ay11), 
				this.px + this.pw, 
				this.py + (i * this.ph /ay11), 
				paint);
	    for(int i=1; i < ax.nr; i++)  
		canvas.drawLine(this.px + (i * this.pw /ax1), 
				this.py, 
				this.px + (i * this.pw /ax1), 
				this.py + this.ph, 
				paint);
	    return true;
	}  // draw_grid

    /*
     * draw_plot
     */
    private void 
    draw_plot(ArrayList <Pt> vec, 
	      Autoscale ax, 
	      Autoscale ay,
	      int color,
	      Set <Integer> style,
	      int agg, // aggregation
	      float width
	){ 
	Pt ptv;
	Point pt;
	boolean skip = false;
	Point pt_prev = new Point(0, 0);   
	Pt pt_avg = new Pt(0, 0);   
	final Paint paint = new Paint();

	paint.setAntiAlias(true);
	paint.setStyle(Paint.Style.STROKE);
	paint.setColor(color);
	paint.setStrokeWidth(width);
	try {
	    canvas.clipRect(this.px, this.py, this.px+this.pw, this.py+this.ph, Region.Op.REPLACE); // clip it.
	    for (int i = 0 ; i< vec.size()-agg+1; i+=agg)	{
		if (agg>1){	// some odd agg points at end may be skipped
		    pt_avg.x = vec.get(i).x;
		    pt_avg.y = 0.0;
		    for (int j=i; j<i+agg; j++){
			double y3 = vec.get(j).y;
//			if (Double.isNaN(y3)){
//			    pt_avg.y = y3;
//			}
			pt_avg.y += y3;

		    }
		    if (!Double.isNaN(pt_avg.y))
			pt_avg.y /= agg;
		    ptv = pt_avg;
		}
		else
		    ptv = vec.get(i);
		if (Double.isNaN(ptv.y)){
		    skip = true; // Make a break in a line.
		    continue;
		}
		pt = ptv.pt2screen(this.px, this.py, this.pw, this.ph, 
				   ax.low, ax.high, ay.low, ay.high);
		if (style.contains(POINTS)){
		    // canvas.drawRect(pt.x-1,	pt.y-1,	pt.x+1, pt.y +1, paint);
		    canvas.drawLine(pt.x-CROSSHAIR, pt.y, pt.x+CROSSHAIR, pt.y, paint);
		    canvas.drawLine(pt.x, pt.y-CROSSHAIR, pt.x, pt.y+CROSSHAIR, paint);
		}
				
		if (style.contains(BARS))
		    canvas.drawLine(pt.x, this.py+this.ph, 
				    pt.x, pt.y, 
				    paint);
		if (i > 0){
		    if (style.contains(LINES) && !skip)
			canvas.drawLine(pt_prev.x, pt_prev.y, 
					pt.x, pt.y, 
					paint);
		}
		pt_prev = pt;
		skip = false;
	    } 
	}
	catch (Exception e)
	    {
		//
	    }
    } // draw_plot
} //	

/*
 * Autoscale
 *
 * Given an original interval [minval, maxval] and a delta value(d), a least number
 * of sub-intervals (nr_min) and an algorithm option (time/ten), compute
 * a new interval [low,high], composed of 'nr' sub-intervals, each sub-interval being
 * 'spacing' wide.
 * 
 *    minval                               maxval
 *     |------------------------------------|
 *   |-----|-----|-----|-----|-----|-----|-----|
 * low s     s     s     s     s     s     s  high
 *      'nr' is the number of sub-intervals: (s)
 */
final class Autoscale {
    public double low; 
    public double high;
    public int    nr;
    public double spacing;

    Autoscale(double minval, double maxval, int nr_min, String option){
	if (minval > maxval)
	    return;
	if (minval == maxval) { /* this means xmin = xmax: stipulate that 1 is the scale */
	    minval -= 0.5;
	    maxval += 0.5;
	}
	if (option == "time")
	    this.autotime(minval, maxval, nr_min);
	else
	    this.autoten(minval, maxval, nr_min);
	this.spacing = (this.high-this.low)/this.nr;
    }

    /* 
     * ac_try
     * Given an original interval [min, max] and a delta value(d), compute an
     * new adjusted interval [lo, hi] as follows:
     * lo - the closest lower value vs the orig interval which aligns to delta
     * hi - the closest higher value vs the orig interval which aligns to delta
     * nr - the number of such deltas that fit into this new interval:
     * See figure for explanation of lo, hi, and nr:
     *    min                                  max
     *     |------------------------------------|
     *  |-----|-----|-----|-----|-----|-----|-----|
     * lo  d     d     d     d     d     d     d  hi
     *       'nr' is the number of sub-intervals: (d)
     */
    private void
    ac_try(double  d,           /* IN delta/step/increment */
	   double  min,         /* IN interval */
	   double  max,         /* IN interval */
	   int     align){      /* IN align time : may be be timezone */
	this.low  = d*(Math.floor((min-align)/d)) + align; /* low */
	this.high = d*(Math.ceil((max-align)/d))  + align; /* high */
	this.nr   = (int)Math.round((this.high-this.low)/d);          /* nr of intervals */
    }
    /*
     * autoten
     * We assume a 10-centric world and that we chop up an interval [min, max] in 'at least' nr_min
     * subintervals that look 'nice' in a 10-based system.
     * 'at least nr_min' means as large number as possible but not exceeding nr_min
     * Looking 'nice' means that the sub-intervals start on the form 1, 2 or 5.
     * For example, an interval [5,95] with at least 5 sub-intervals would give:
     *    0-20, 20-40, 40-60, 60-80, 80-100
     */
    private void
    autoten(double min, double max, int nr_min){ 
	double s;             /* spacing - 1st approximation*/
	double si;            /* spacing - iterative */
	double delta; 
	double fv[] = new double[] {10, 5, 2, 1}; /* multiplication factors */
	boolean found = false;

	delta = max - min;    
	s = delta/nr_min;
	/* find highest spacing that is lower than d */
	si = Math.pow(10, Math.floor(Math.log10(s))); /* si is 1, 10, 100, 1000,... */
	for (int i=0; i < fv.length; i++){
	    ac_try(fv[i]*si, min, max, 0); /* this.nr as side-effect */
	    if (this.nr >= nr_min){ /* good enough */
		found = true;
		break;
	    }
	}
	if (found)
	    return;
	/* No solution, shouldnt happen */
	return;
    }

    /*
     * gettimemode
     * This is an 'ad-hoc' function that returns a 'mode' depending on how the x-axis
     * steps in seconds are. This can be used by the plotter to show more or less
     * information about time. For example, if every tick is 10 years, the mode is 'y' and
     * only years may be shown, there seems little use to show seconds, for example.
     * There modes are:
     * y: yyyy
     * d: yyyy-mm-dd
     * s: yyyy-mm-dd hh:mm:ss
     * f: yyyy-mm-dd hh:mm:ss.ff
     * Only applicable when initiated with option == "time"
     */
    public String
    getmode(){
	double s = this.spacing;
	if (s < 1)
	    return "f";
	if (s < 24*60*60)
	    return "s";
	if (s < 364*24*60*60)
	    return "d";
	return "y";
    }

    /*
     * autotime
     * We assume a world of unix-time where the unit is seconds (eg from 1970-01-01).
     * We chop up an interval [min, max] 
     * (as large number as possible but not exceeding nr_min) subintervals 
     */
    private void
    autotime(double min, double max, int nr_min){
	int      nr;      /* the computed nr of sub-intervals */
	double   tv[] = new double[] {10*365*24*3600, 365*24*3600, 3*30*24*3600, 30*24*3600, 7*24*3600, 24*3600, 6*3600, 60*60, 20*60, 5*60, 60, 20, 5, 1};
	boolean found = false;
	TimeZone tz = TimeZone.getDefault();
	int offset = -tz.getOffset((int)(min*1000))/1000;
	
	for (int i=0; i < tv.length; i++){
	    ac_try(tv[i], min, max, offset); // XXX: timezone /* this.nr as side-effect */
	    if (this.nr >= nr_min){ /* good enough */
		found = true;
		break;
	    }
	}
	if (found)
	    return;
	/* for sub-seconds, use power of ten */
	autoten(min, max, nr_min);
    }
} /* Autoscale */
