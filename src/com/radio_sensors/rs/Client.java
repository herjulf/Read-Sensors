// GPL CopyRight 2013 by Robert Olsson robert@Radio-Sensors.COM 
// Some code reused from TinyTelnet.java
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.TextView;
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
import java.util.Vector;
import android.util.Log;
import android.os.Message;


public class Client extends Activity {
    private class StateSaver 
    {
	private Socket socket = null;
	private boolean connected = false;
	private boolean started = false;
	private Thread thread = null;
	private String serverAddr = "";
	private String serverPort = "";
	private String sid = "";
	private String tag = "";
    }
    final public static int SENSD = 3;      // Message	to other activity

    public static boolean debug = false;
    public static Client client = null;
    private Socket socket = null;
    private Context context = this;
    private boolean connected = false;
    private boolean started = false;
    private String serverAddr = "";
    private String serverPort = "";
    private String sid = "";
    private String tag = "";
    Handler handler = null;
    Thread thread = null;
    public static Handler ploth = null; // PlotWindow handler
    public static Handler reporth = null;

       // This is code for lower right button menu
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.layout.main_menu, menu);
	    return true;
	}	

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.about:
		AboutBox.Show(this);
		return true;
	    case R.id.prefs:
		toActivity("PrefWindow");
		return true;
	    case R.id.debug:
		Toast.makeText(this, "Debugging enabled", Toast.LENGTH_SHORT).show();
		debug=true;
	        return true;
	    case R.id.plot:
		toActivity("PlotWindow");
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
    public void onCreate(Bundle savedInstanceState)
	{
	    super.onCreate(savedInstanceState);
	    client = this;
	    // To keep states over rotates etc.
	    StateSaver saved = (StateSaver) getLastNonConfigurationInstance();
	    if (saved != null && saved.started) {
		// Restore saved running state
		Log.d("RStrace Stateserver RESTORE", "!null");        

		setContentView(R.layout.main);

	    // Rotate when not connected

		socket = saved.socket;
		connected = saved.connected;
		started = saved.started;
		serverAddr = saved.serverAddr;
		serverPort = saved.serverPort;
		sid = saved.sid;
		tag = saved.tag;
		connect();
	    } 

	    setContentView(R.layout.main);

	    final EditText server_ip = (EditText) findViewById(R.id.server_ip);
	    final EditText port = (EditText) findViewById(R.id.server_port);

	    // Sync Pref and main menu
	    sync_prefs();

	    this.handler = new Handler();

	    Button buttonConnect = (Button) findViewById(R.id.server_connect);
	    buttonConnect.setOnClickListener(new View.OnClickListener() {

		    @Override
		    public void onClick(View view) {

			// We can can change throughout the connection

			if(connected) {
			    Toast.makeText(context, "Disconnecting...", Toast.LENGTH_LONG).show();
			    disconnect();
			    return;
			}
			serverAddr = get_server_ip();
			serverPort = String.valueOf(get_server_port());

			started = true;
//			plot_select();
			connect();
		    }
		});
	}
	    
            @Override
		public Object onRetainNonConfigurationInstance() {
		StateSaver saved = new StateSaver();
		// Place holder for storing variables needed
		// to keep states over rotates etc.

		Log.d("RStrace SAVE", "1");        
		saved.socket = socket;
		saved.connected = connected;
		saved.started = started;
		saved.serverAddr = serverAddr;
		saved.serverPort = serverPort;
		saved.sid = sid;
		saved.tag = tag;

		return saved;
	}

    protected void onDestroy()
	{
	    super.onDestroy();

	    if(connected)
		{
		    connected = false;
		    try
			{
			    if(this.thread != null)
				{
				    Thread threadHelper = this.thread; 
				    this.thread = null;
				    threadHelper.interrupt();
				}
			}
		    catch (Exception e1)
			{
			}
		}

	}

    // Send a message to other activity
    protected void message(Handler h, int what, Object msg){
	Message message = Message.obtain();
	message.what = what;
	message.obj = msg;
	h.sendMessage(message); // To other activity
    }


    public class RunThread implements Runnable 
    {
    	public String filter(String s, String id, String tag) 
	    {
		String s1 = "";
		boolean got = true;
	    
		tag = tag + "=";

		if( id != null) {
		    got = false;
		    for (String t: s.split(" ")) {
			if(t.indexOf(id) > 0) {
			    got = true;
			    Log.d("RStrace 2", String.format("id=%s tag=%s", id, tag));
			    break;
			}
		    } 
		}

		if(got) {
		    Log.d("RStrace 3", String.format("id%s tag=%s", id, tag));
		    for (String t: s.split(" ")) {
			if(t.indexOf(tag) == 0)
			    s1 = t.substring(tag.length());
		    } 
		}
		return  s1;
	    }


    	public void run() 
	    {
		try 
		    {
			Log.d("RStrace", String.format("Socket Server=%s Port=%s", serverAddr, serverPort));
			Socket sock = new Socket(serverAddr, Integer.parseInt(serverPort));
			socket = sock;

			InputStream streamInput = socket.getInputStream();
			connected = true;

			byte[] buf = new byte[2048];
			while (connected)
			    {
				int j = 0;

				Message message = Message.obtain();
				message.what = 1;
				mHandler.sendMessageDelayed(message, 1);

				try
				    {
					int i = buf.length;
					j = streamInput.read(buf, 0, i);
					if (j == -1)
					    {
						throw new Exception("Error while reading socket.");
					    }
				    }
				catch (Exception e0)
				    {
					Handler handlerException = Client.this.handler;
					String strException = e0.getMessage();
					final String strMessage = 
					    "Error while receiving from server:\r\n" + strException;
					Runnable rExceptionThread = new Runnable()
					    {
						// Avoid error message on user disconnect
						public void run()
						{
						    if (connected)
							Toast.makeText(context, strMessage, 3000).show();
						}
					    };

					handlerException.post(rExceptionThread);
		    			
					if(strException.indexOf("reset") != -1 || strException.indexOf("rejected") != -1)
					    {
						connected = false;
						try 
						    {
							socket.close();
						    }
						catch (IOException e1) 
						    {
							e1.printStackTrace();
						    }
						socket = null;
						break;
					    }
				    }

				if (j == 0)
				    continue;

				final String strData = new String(buf, 0, j).replace("\r", "");
				if (ploth != null)
				    message(ploth, SENSD, strData);
				if (reporth != null)
				    message(reporth, SENSD, strData);
				if(true) runOnUiThread(new Runnable() {
					public void run() {
					    String f = filter(strData, sid, tag);
					    String t = filter(strData, null, "UT"); // time

					    Toast.makeText(context, "Filter Miss: " + strData, Toast.LENGTH_LONG).show();

					    if( f != "" && t != "") 
						{
						    Long x = new Long(t);
						    Double res = Double.parseDouble(f);
						    Toast.makeText(context, "Filter Match: " + tag + "=" + String.format("%5.1f", res ), Toast.LENGTH_LONG).show();

/*
  XXX: Use indirect method
						    Pt p = new Pt(x, res, seq);
						    power.sample(p);
						    seq++;
*/
						}
					}
				    });
			    }
			socket.close();
		    }
		catch (Exception e0) 
		    {
			connected = false;
			Handler handlerException = Client.this.handler;
			String strException = e0.getMessage();

			if(strException == null)
			    strException = "Connection closed";
			else
			    strException = "Cannot connect to server:\r\n" + strException;
    			
			final String strMessage = strException;
			Runnable rExceptionThread = new Runnable()
			    {
				public void run()
				    {
					Toast.makeText(context, strMessage, 2000).show();
				    }
			    };
			handlerException.post(rExceptionThread);
		    }
	    }
    }
/*
  XXX: move to plot
    private void plot_select()
	{

	    if(tag.equals("T"))
		{
		    plot.y1axis("Temp [C]", 1.0);
		}

	    if(tag.equals("RH"))
		{
		    plot.y1axis("RH [%]", 1.0);
		}

	    if(tag.equals("SEQ"))
		{
		    plot.y1axis("SEQ no]", 1.0);
		}
	}
*/
    private void connect()
    {
	if(thread != null)
	    {
		thread.stop();
		thread = null;
	    }
	thread = new Thread(new RunThread());
	thread.start();
    }

    private void disconnect()
    {

    if(connected)
		{
		    connected = false;
		    try
			{
			    if(this.thread != null)
				{
				    Thread threadHelper = this.thread; 
				    this.thread = null;
				    threadHelper.interrupt();
				}
			}
		    catch (Exception e1)
			{
			}
		}

	connected = false;

	try 
	    {
		socket.close();
	    }
	catch (IOException e1) 
	    {
		e1.printStackTrace();
	    }
	socket = null;

	Message message = Message.obtain();
	message.what = 1;
	mHandler.sendMessageDelayed(message, 1);
    }

    private final Handler mHandler = new Handler() {
		public void handleMessage(Message msg ) 
	    {
		Message message;
		Button buttonConnect = (Button) findViewById(R.id.server_connect);

		if(connected)
		    buttonConnect.setText("Disconnect");
		else
		    buttonConnect.setText("Connect");
	    }
	};

    // access methods for prefs (dont access them other way from external)
    public String get_server_ip(){
	SharedPreferences sPref = getSharedPreferences("Read-Sensors", 0);
	return sPref.getString("server_ip", "Radio-Sensors.com");
    }
    public int get_server_port(){
	SharedPreferences sPref = getSharedPreferences("Read-Sensors", 0);
	return sPref.getInt("server_port", 1234);
    }
    public String get_sid(){
	SharedPreferences sPref = getSharedPreferences("Read-Sensors", 0);
	return sPref.getString("sid", "fcc23d000000511d");
    }
    public String get_tag(){
	SharedPreferences sPref = getSharedPreferences("Read-Sensors", 0);
	return sPref.getString("tag", "T");
    }
    public void sync_prefs(){
	final EditText server_ip = (EditText) findViewById(R.id.server_ip);
	final EditText port = (EditText) findViewById(R.id.server_port);
	server_ip.setText(get_server_ip());
	port.setText(String.valueOf(get_server_port()));
    }
}
