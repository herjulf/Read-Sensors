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
	public long seq;
	
	Pt(){
		set(0.0, 0.0, 0);
	}
	
	Pt(double x, double y, long seq){
		set(x, y, seq);
	}
	
	public void set(double x0, double y0, long seq){
		x = x0;
		y = y0;		
		this.seq = seq;
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
    public String title; // title for plot
    public int y;        // y1 or y2-axis
    public Vector <Pt> vec;
    public Set <Integer> style = new TreeSet<Integer>(); // XXX: Why must I use TreeSet?
    public int color;
    public double ymin = Double.POSITIVE_INFINITY;
    public double ymax = Double.NEGATIVE_INFINITY;

    PlotVector(Vector <Pt> v0, String t0, int y0, int s0, int c0){
    	vec = v0;
    	title = t0;
    	y = y0;
    	style.add(s0);
    	color = c0;
    }
    public void sample(Pt pt){
	Log.d("RStrace", String.format("sample: x=%f y=%f seq=%d", pt.x, pt.y, pt.seq));
	if (pt.y < ymin)
	    ymin = pt.y;
	if (pt.y > ymax)
	    ymax = pt.y;
	vec.add(pt);
    }
} // PlotVector

// The complete plot
public final class Plot {
    private static final int TICKLEN = 6; // how long solid ticks
    private static final int CROSSHAIR = 5; // how large point crosshair
    private static final int FONTSIZE = 20; // font size
    private static final int LEFTMARGIN = 1*(FONTSIZE+2); // left 
    private static final int BOTTOMMARGIN = 2*(FONTSIZE+2); // lower
    private static final int RIGHTMARGIN = 1*(FONTSIZE+2); // right
    private static final int TOPMARGIN = 4; // right and upper

    private static int XWINDOW = 10; // how many seconds to show default

    public static final int POINTS = 1; // style
    public static final int LINES = 2; // style
    public static final int BARS = 3; // style

    private Canvas canvas;
    private Vector <PlotVector> plots; // Vector of plots 
    private int x,y,w,h; // bitmap x,y,width height
    private int px,py,pw,ph; // plotarea x,y,width height
    private Bitmap bitmap;
    private double xmin, xmax;
    private double xwin = XWINDOW;       // x window size
	
    public boolean liveUpdate = true; // Scroll x-axis as new values arrive

    private int nrPlots = 0;	
    private String xlabel;  // text to print on x-axis
    private String y1label; // text to print on left y-axis
    private String y2label; // text to print on right y-axis
    private double xscale;  // factor to multiply x-values for x-axis text
    private double y1scale; // factor to multiply y-values for left y-axis text
    private double y2scale; // factor to multiply y-values for right y-axis text

    private int id;                 // resource id

    Plot(int id0, Display display){ 
	canvas = new Canvas();
	id = id0;
	plots = new Vector <PlotVector>();
    }

    public void newDisplay(Display display){
	// bitmap: x,y,w,h
	x = 0;
	y = 0;
	w = display.getWidth();
	h = display.getHeight();
	// plotarea: px,py,pw,ph
	px = x + LEFTMARGIN;
	py = y + TOPMARGIN;
	pw = w - LEFTMARGIN - RIGHTMARGIN;
	ph = h - BOTTOMMARGIN - TOPMARGIN;
	bitmap = Bitmap.createBitmap(w, h, Config.ARGB_8888); 
	canvas.setBitmap(bitmap);
    }

    private int colornr = 0;
    public int nextColor(){
	int c;
	switch (colornr++){
	case 0: c = Color.RED; break;
	case 1: c = Color.GREEN; break;
	case 2: c = Color.BLUE; break;
	case 3: c = Color.CYAN; break;
	case 4: c = Color.YELLOW; break;
	default: c = Color.WHITE; break;
	}
	return c;
    }
    public double get_pw(){ // plot area
	return pw;
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
	
    public void 
    xaxis(String label, double scale) {
	xlabel = label;
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
    y1axis(String label, double scale) {
	y1label = label;
	y1scale = scale;
    }
	
    public void 
    y2axis(String label, double scale) {
	y2label = label;
	y2scale = scale;
    }

    private static int 
    points_in_interval(Vector <Pt>vec, double low, double high){
	int nr = 0;
	for(int i=0; i < vec.size() ; i++){ 
	    double x = vec.elementAt(i).x;
	    if (x < low)
		continue;
	    if (x > high)
		continue;
	    nr++;
	}
	return nr;
    } // points_in_interval

    // Now draw plot and auto-scale axis depending on plot contents
    public void autodraw(ImageView image){
	PlotVector pv;
	boolean y1empty = true;
	double y1min = Double.POSITIVE_INFINITY;
	double y2min = Double.POSITIVE_INFINITY;
	double y1max = Double.NEGATIVE_INFINITY;
	double y2max = Double.NEGATIVE_INFINITY;
	boolean y2empty = true;
	double xmin1 = Double.POSITIVE_INFINITY; // Tentative x-interval min
	double xmax1 = Double.NEGATIVE_INFINITY;

	// Clear bitmap
	image.setImageBitmap(bitmap); 

	// Loop 1a: compute tentative x-interval [xmin1, xmax1] for y1 plots
	for (int i=0; i<plots.size(); i++){
	    pv = plots.elementAt(i);
	    if (pv.vec.size() > 0){
		y1empty = false; // XXX: inverse logic
		xmin1 = Math.min(xmin1, pv.vec.firstElement().x);
		xmax1 = Math.max(xmax1, pv.vec.lastElement().x);
	    }
	}
	// Loop 1b: Same for y2 plots
	// XXX

	if (y1empty && y2empty){
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

	y1empty = true; // XXX: inverse logic
	// Loop 2a:  compute y1 intervals, etc
	for (int i=0; i<plots.size(); i++){
	    pv = plots.elementAt(i); // Only in [xmin,xmax]
	    for (int j = 0 ; j< pv.vec.size(); j++){
		double x = pv.vec.elementAt(j).x;
		double y = pv.vec.elementAt(j).y;
		if (x < xmin)
		    continue;
		if (x > xmax)
		    continue;
		y1empty = false; 
		y1max = Math.max(y1max, y);
		y1min = Math.min(y1min, y);
	    }	    
	}
	if (y1empty){
	    y1min = 0;
	    y1max = 1;
	}

	// Loop 2b: Same for y2 plots
	// XXX: Also y2 plots

	if (y2empty){
	    y2min = 0;
	    y2max = 1;
	}

	Autoscale ax = new Autoscale(xmin, xmax);
	Autoscale ay1 = new Autoscale(y1min, y1max);
	Autoscale ay2 = new Autoscale(y2min, y2max);
	double pix = get_pw()*(xmax1-xmin1)/(ax.high-ax.low);
	double aggF = 0.0; 

	// Loop 3: compute level of aggregation in x-axis
	for (int i=0; i<plots.size(); i++){
	    pv = plots.elementAt(i);
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
	Vector <Pt> vec;
	// assert |xvec| == |yvec|
	if (!draw_grid(ax, ay1, ay2))
	    return;
	for(int j=0; j < plots.size() ; j++)  {
	    PlotVector pi = plots.elementAt(j);
	    vec = pi.vec;
	    draw_plot_label(j, pi.title, pi.color);
	    draw_plot(vec, ax, pi.y==1?ay1:ay2, pi.color, pi.style, agg);
	}
    } // draw



    // plot name and label (key) for each graph including color
    private void
    draw_plot_label(int i, String title, int color){
	final Paint paint = new Paint();
	paint.setStyle(Paint.Style.STROKE);
	paint.setColor(Color.WHITE);
	paint.setTextSize(FONTSIZE);
	float x = (float)(px+0.6*pw);
	float x1 = x + title.length()*FONTSIZE*(float)0.8;
	float y = py+(i+1)*2*FONTSIZE;
	canvas.drawText(title, x, y, paint);
	paint.setColor(color);
	canvas.drawLine(x1,
			y,	
			x1 +  30, 
			y,
			paint);
    } // draw_plot_label
	
    private boolean 
    draw_grid(Autoscale ax, Autoscale ay1, Autoscale ay2)
	{
	    final Paint paint = new Paint();
	    paint.setAntiAlias(true);
	    paint.setTextSize(FONTSIZE);
	    paint.setStyle(Paint.Style.FILL); 
	    paint.setColor(Color.BLACK);  
	    int ax1 = ax.ticks-1;
	    int ay11 = ay1.ticks-1;
	    int ay21 = ay2.ticks-1;

	    canvas.clipRect(x, y, x+w, y+h, Region.Op.REPLACE); // clip it.
	    // Fill the canvas with white
	    canvas.drawRect(x, y, x + w, y + h, paint);

	    // Draw black plot box
	    paint.setColor(Color.WHITE);
	    paint.setStyle(Paint.Style.STROKE); 
	    canvas.drawRect(px, py, px + pw, py + ph, paint);
	    // Write text
	    paint.setTypeface(Typeface.SANS_SERIF ); 
	    paint.setTextSize(FONTSIZE);
	    paint.setColor(Color.WHITE);
	    // axis label text
	    canvas.drawText(xlabel, px+pw/2-FONTSIZE*2, y+h-2, paint);
	    // y1 axis
	    canvas.translate(x+FONTSIZE, y+ph/2);
	    canvas.rotate(-90, 0, 0);
	    canvas.drawText(y1label, 0, 0, paint);
	    canvas.rotate(90, 0, 0);
	    canvas.translate(-x-FONTSIZE, -y-ph/2);
	    // y2 axis
	    canvas.translate(px+pw+FONTSIZE+2, y+ph/2);
	    canvas.rotate(90, 0, 0);
	    canvas.drawText(y2label, 0, 0, paint);
	    canvas.rotate(-90, 0, 0);
	    canvas.translate(-px-pw-FONTSIZE-2, -y-ph/2);
	    // text along x-axis
	    paint.setColor(Color.WHITE);
	    boolean subseconds = (ax.high-ax.low)<4.0;
	    subseconds = false; // XXX
	    if (ax1>0)
		for(int i=0; i < ax.ticks; i++) { 
		    int x1 = px + (i * pw/ax1); //left
		    if (i == ax1) // right
			x1 -= FONTSIZE;
		    else
			if (i != 0) // normal case
			    x1 -= FONTSIZE/2;
		    double x = ax.low+i*ax.spacing*xscale;
		    Time t = new Time(); //ms
		    t.set((long)(x*1000.0));
		    // String s = t.format3339(true); // iso
		    String s;
		    if (i==0){
			if (subseconds)
			    s = String.format("%d:%d:%d.%d", t.hour, t.minute, t.second, (int)(x1%1.0));
			else
			    s = String.format("%d:%d:%d", t.hour, t.minute, t.second);
		    }
		    else{
			if (subseconds)
			    s = String.format("%d.%ds", t.second, (x1%1.0));
			else
			    s = String.format("%d:%d", t.minute, t.second);
		    }
		    canvas.drawText(s, x1, py+ph+FONTSIZE+2, paint);
		}
	    // text along y-axis
	    if (ay11>0)
		for(int i=0; i < ay1.ticks; i++) { 
		    int y1 = py + (i * ph/ay11); //lower
		    if (i == 0) //upper
			y1 += FONTSIZE;
		    else
			if (i != ay11) // normal case
			    y1 += FONTSIZE/2;
		    String s = String.format("%.2f", (ay1.high-i*ay1.spacing)*y1scale);
		    canvas.drawText(s, FONTSIZE+2, y1, paint);
		}
	    // text along y2-axis
	    if (ay21>0)
		for(int i=0; i < ay2.ticks; i++) { 
		    int y2 = py + (i * ph/ay21); //lower
		    if (i == 0) //upper
			y2 += FONTSIZE;
		    else
			if (i != ay21) // normal case
			    y2 += FONTSIZE/2;
		    String s = String.format("%.2f", (ay2.high-i*ay2.spacing)*y2scale);
		    canvas.drawText(s, px+pw+2, y2, paint);
		}
	    // Draw small lines at end
	    paint.setColor(Color.BLACK);
	    for(int i=1; i < ax1 ; i++) { 
		canvas.drawLine(px + (i * pw /ax1), 
				py,
				px+ (i * pw/ax1),
				py + TICKLEN,
				paint);
		canvas.drawLine(px + (i * pw/ax1), 
				py+ph-TICKLEN,
				px+ (i * pw/ax1),
				py + ph,
				paint);
	    }
	    for(int i=1; i < ay11; i++) {
		canvas.drawLine(px, 
				py + (i * ph/ay11), 
				px+TICKLEN, 
				py + (i * ph/ay11), 
				paint);
		canvas.drawLine(px+pw-TICKLEN, 
				py + (i * ph/ay11), 
				px+pw, 
				py + (i * ph/ay11), 
				paint);
	    }
	    // Draw the grid with white dashed lines
	    paint.setColor(Color.WHITE);
	    paint.setPathEffect( new DashPathEffect(new float[] { 2, 8 }, 0));
	    for(int i=1; i < ay11; i++)  
		canvas.drawLine(px, 
				py + (i * ph /ay11), 
				px + pw, 
				py + (i * ph /ay11), 
				paint);
	    for(int i=1; i < ax.ticks-1; i++)  
		canvas.drawLine(px + (i * pw /ax1), 
				py, 
				px + (i * pw /ax1), 
				py + ph, 
				paint);
	    return true;
	}  // draw_grid

    private void 
    draw_plot(Vector <Pt> vec, 
	      Autoscale ax, 
	      Autoscale ay,
	      int color,
	      Set <Integer> style,
	      int agg){ // aggregation
	Pt ptv;
	Point pt;
	boolean skip = false;
	Point pt_prev = new Point(0, 0);   
	Pt pt_avg = new Pt(0, 0, 0);   
	final Paint paint = new Paint();

	paint.setAntiAlias(true);
	paint.setStyle(Paint.Style.STROKE);
	paint.setColor(color);
	try {
	    canvas.clipRect(px, py, px+pw, py+ph, Region.Op.REPLACE); // clip it.
	    for (int i = 0 ; i< vec.size(); i+=agg)	{
		if (agg>1){
		    pt_avg.x = vec.elementAt(i).x;
		    pt_avg.y = 0.0;
		    for (int j=i; j<i+agg; j++){
			double y3 = vec.elementAt(j).y;
			if (Double.isNaN(y3)){
			    pt_avg.y = y3;
			}
			pt_avg.y += y3;

		    }
		    if (!Double.isNaN(pt_avg.y))
			pt_avg.y /= agg;
		    ptv = pt_avg;
		}
		else
		    ptv = vec.elementAt(i);
		if (Double.isNaN(ptv.y)){
		    skip = true; // Make a break in a line.
		    continue;
		}
		pt = ptv.pt2screen(px, py, pw, ph, 
				   ax.low, ax.high, ay.low, ay.high);
		if (style.contains(POINTS)){
		    // canvas.drawRect(pt.x-1,	pt.y-1,	pt.x+1, pt.y +1, paint);
		    canvas.drawLine(pt.x-CROSSHAIR, pt.y, pt.x+CROSSHAIR, pt.y, paint);
		    canvas.drawLine(pt.x, pt.y-CROSSHAIR, pt.x, pt.y+CROSSHAIR, paint);
		}
				
		if (style.contains(BARS))
		    canvas.drawLine(pt.x, py+ph, 
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
 */
final class Autoscale {
	private static double PENALTY = 0.02;
	public double low, high, spacing;
	public int ticks;

	Autoscale(double min, double max){
		double ulow[] = new double[12];
		double uhigh[] = new double[12];
		double uticks[] = new double[12];

		scales(min, max, ulow, uhigh, uticks);

		double udelta = max - min;
		double ufit[] = new double[12];
		double fit[] = new double[12];
		int k = 0;
		for (int i=0; i<=11; i++) {
			ufit[i] = ((uhigh[i]-ulow[i])-udelta)/(uhigh[i]-ulow[i]);
			fit[i] = 2*ufit[i] + PENALTY * Math.pow( ((uticks[i]-6.0)>1.0)?(uticks[i]-6.0):1.0 ,2.0);
			if (i > 0) {
				if (fit[i] < fit[k]) {
					k = i;
				}
			}
		}
		low = ulow[k];
		high = uhigh[k];
		ticks = (int)uticks[k];
		spacing = (high-low)/(ticks-1.0);
	}
	private void 
	scales(double xmin, double xmax, double low[], double high[], double ticks[]){
	    double bestDelta[] = {0.01, 0.02, 0.05, 0.1, 0.2, 0.5, 1.0, 2.0, 5.0, 10.0, 20.0, 50.0};
		int i;
		double xdelta = xmax-xmin;
		double delta[] = new double[12];

		if (xdelta == 0) {
		    for (i=0; i<=11; i++) {
			xdelta=1;
			delta[i] = Math.pow(10,Math.round(Math.log10(xdelta)-1)) * bestDelta[i];
			high[i] = delta[i] * Math.ceil((xmin+0.5)/delta[i]);
			low[i] = delta[i] * Math.floor((xmin-0.5)/delta[i]);
			ticks[i] = Math.round((high[i]-low[i])/delta[i]) + 1;
//			low[i] = xmin-0.5;
//			high[i] = xmax+0.5;
//			ticks[i] = 3;
			}
			return;
		}

		for (i=0; i<=11; i++) {
			delta[i] = Math.pow(10,Math.round(Math.log10(xdelta)-1)) * bestDelta[i];
			high[i] = delta[i] * Math.ceil(xmax/delta[i]);
			low[i] = delta[i] * Math.floor(xmin/delta[i]);
			ticks[i] = Math.round((high[i]-low[i])/delta[i]) + 1;
		}

	}
}
