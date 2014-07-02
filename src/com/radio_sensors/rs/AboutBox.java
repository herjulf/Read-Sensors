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
// Some code reused from Andriod Cookbook

package com.radio_sensors.rs;

import java.util.Vector;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import android.app.Activity;
import android.app.AlertDialog;
import android.os.Handler;
import android.os.Bundle;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.ImageView;
import android.view.InflateException;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.ViewGroup;    
import android.view.View.OnClickListener;    
import android.os.Message;
import android.graphics.Color;    
import android.graphics.Typeface;    
import android.graphics.drawable.Drawable;    
import android.text.Spannable;    
import android.text.SpannableString;    
import android.text.style.AbsoluteSizeSpan;    
import android.text.style.BackgroundColorSpan;    
import android.text.style.ForegroundColorSpan;    
import android.text.style.ImageSpan;    
import android.text.style.StrikethroughSpan;    
import android.text.style.StyleSpan;    
import android.text.style.URLSpan;    
import android.text.style.UnderlineSpan;    
import android.text.util.Linkify;
import android.util.Log;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.Context;
import android.content.Intent;

public class AboutBox {
    public static void Show(Activity callingActivity) {
	Context context = callingActivity.getApplicationContext(); // or activity.getApplicationContext()
	PackageManager packageManager = context.getPackageManager();
	String packageName = context.getPackageName();

        //Generate views to pass to AlertDialog.Builder and to set the text
        View about;
        TextView tvAbout;
        try {
            //Inflate the custom view
            LayoutInflater inflater = callingActivity.getLayoutInflater();
            about = inflater.inflate(R.layout.aboutbox, (ViewGroup) callingActivity.findViewById(R.id.aboutView));
            tvAbout = (TextView) about.findViewById(R.id.aboutText);
        }
        catch(InflateException e) {
            //Inflater can throw exception, unlikely but default to TextView if it occurs
            about = tvAbout = new TextView(callingActivity);
        }

	String myVersionName = "not available"; // initialize String
	try {
	    myVersionName = packageManager.getPackageInfo(packageName, 0).versionName;
	} catch (PackageManager.NameNotFoundException e) {
	    e.printStackTrace();
	}

        //Use a Spannable to allow for links highlighting
        SpannableString aboutText = new SpannableString("Version " + myVersionName +
	 System.getProperty ("line.separator") +
	 callingActivity.getString(R.string.about));

        //Set the about text 
        tvAbout.setText(aboutText);
        // Now Linkify the text
        Linkify.addLinks(tvAbout, Linkify.ALL);		

        new AlertDialog.Builder(callingActivity)
            .setTitle("About " + callingActivity.getString(R.string.app_name))
            .setCancelable(true)
	    .setIcon(R.drawable.icon)
            .setPositiveButton("OK", null)
            .setView(about)
            .show();
    }	
}
