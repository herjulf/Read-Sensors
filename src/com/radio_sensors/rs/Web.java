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
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.util.Log;
import android.os.Message;

public class Web extends Activity {
    private WebView webView;
    final private static String TAG = "RS-" + Client.class.getName();

 
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);

	Log.d("TAG", "Web onCreate");
	setContentView(R.layout.web);
	WebView web = (WebView) findViewById(R.id.webview);
 
	web = (WebView) findViewById(R.id.webview);
	web.getSettings().setJavaScriptEnabled(true);
	web.getSettings().setLoadsImagesAutomatically(true);
	web.getSettings().setJavaScriptEnabled(true);
	web.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
	web.loadUrl("http://www.radio-sensors.com/app/Read-Sensors/");
 
	//String html = "<html><body><h1>Read Sensors App</h1></body></html>";
	//web.loadData(html, "text/html", "UTF-8");
 
    }
 
    protected void onStart(){
	super.onStart();
	Log.d("TAG", "Web onStart");
    }

    protected void onResume(){
	super.onResume();
	Log.d("TAG", "Web onResume");
    }

    protected void onPause(){
	super.onPause();
	Log.d("TAG", "Web onPause");
    }

    protected void onStop(){  // This is called when starting another activity
	super.onStop();
	Log.d("TAG", "Web onStop");
    }

    protected void onDestroy()	{
	super.onDestroy();
	Log.d("TAG", "Web onDestroy");
    }
}