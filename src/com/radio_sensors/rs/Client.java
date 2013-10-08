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
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Vector;
import java.util.Random;
import android.util.Log;
import android.os.Message;


public class Client extends Activity {
    private Socket socket = null;
    private Context context = this;
    private boolean connected = false;
    private String serverAddr = "";
    private String serverPort = "";
    Handler handler = null;
    Thread thread = null;

    private Plot plot;         // Encompassing plot frame
    private PlotVector power;  // Single function in plot
    private int  seq = 0;
    private static int PLOTINTERVAL = 500; // interval between plot calls in ms
    private static int SAMPLEINTERVAL = 200; // interval between sample receives
    final public static int PLOT = 5;      // Message
    final public static int SAMPLE = 8;      // Message
    private Random rnd;

    @Override
	public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
	Log.d("RStrace", "Client");        
	Display display = getWindowManager().getDefaultDisplay(); 

	// Initialize plotter
	plot = new Plot(R.id.img, display);
	plot.xaxis("Samples", 1.0);  // Customize
	plot.y1axis("Power [kW]", 5.0);
//	plot.y2axis("Loss[%]", 1.0);
	plot.y2axis("", 1.0);
	// Add one graph (plotvector) for power
	Vector <Pt> vec = new Vector<Pt>(); 
	power = new PlotVector(vec, "power", 1, Plot.LINES, plot.nextColor());
	plot.add(power);
	ImageView image = (ImageView) findViewById(R.id.img);
	plot.autodraw(image);

	// Initialize messages (plot for plotting, samle for fake samples)
	Message message = Message.obtain();
	message.what = PLOT;
	mHandler.sendMessageDelayed(message, PLOTINTERVAL);
	message = Message.obtain();
	message.what = SAMPLE;
	mHandler.sendMessageDelayed(message, SAMPLEINTERVAL);

        this.handler = new Handler();
	rnd = new Random(42); // Init random generator
	Button buttonConnect = (Button) findViewById(R.id.server_connect);
	buttonConnect.setOnClickListener(new View.OnClickListener() {
		private EditText server_ip = (EditText) findViewById(R.id.server_ip);
		private EditText port = (EditText) findViewById(R.id.server_port);
		
		@Override
		    public void onClick(View view) {

		    serverAddr = server_ip.getText().toString();
		    serverPort = port.getText().toString();

		    // FIXME
		    SharedPreferences sp  = getSharedPreferences("Read Sensors", context.MODE_PRIVATE);
		    SharedPreferences.Editor ed = sp.edit();
		    ed.putString("server-ip", serverAddr);
		    ed.putString("server-port", serverPort);
		    ed.commit();

		    Connect();
		}
	    });

    }
    

    public void onClick(View view) {

	try {
	    EditText et = (EditText) findViewById(R.id.EditText01);
	    String str = et.getText().toString();
	    PrintWriter out = 
		new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())),
					      true);
	    out.println(str);
	} catch (UnknownHostException e) {
	    e.printStackTrace();
	} catch (IOException e) {
	    e.printStackTrace();
	} catch (Exception e) {
	    e.printStackTrace();
	}
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
	if (mHandler.hasMessages(PLOT))
	    mHandler.removeMessages(PLOT);

    }

    public class RunThread implements Runnable 
    {
    	public String filter(String s, String id, String tag) 
	{
	    String s1 = "";
	    boolean got = true;

	    if( id != null) {

		Log.d("RStrace 1", String.format("ID=%s TG=%s", id, tag));
		got = false;
		for (String t: s.split(" ")) {
		    if(t.indexOf(id) > 0) {
			got = true;
			Log.d("RStrace 2", String.format("ID=%s TAG=%s", id, tag));
			break;
		    }
		} 
	    }

	    if(got) {
		Log.d("RStrace 3", String.format("ID=%s TG=%s", id, tag));
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
					    public void run()
					    {
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

			    if(true) runOnUiThread(new Runnable() {
				    public void run() {
					String f = filter(strData, "0007f", "P0_T=");
					Toast.makeText(context, "Sensor Report " + strData, Toast.LENGTH_LONG).show();

					if( f != "") 
					    {
						Double res = 33.6 * Double.parseDouble(f);					
						Toast.makeText(context, "Power Meter " + String.format("%5.1f", res ), Toast.LENGTH_LONG).show();

						
						Pt p = new Pt(seq, res, seq);
						power.sample(p);
						seq++;
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

    private void Connect()
    {
    	LayoutInflater li = LayoutInflater.from(context);
	View promptsView = li.inflate(R.layout.connect, null);

	Log.d("RStrace", "Connect 1");

	AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
	alertDialogBuilder.setView(promptsView);
	alertDialogBuilder.setCancelable(true);
	alertDialogBuilder.setPositiveButton("Connect", new DialogInterface.OnClickListener() 
	    {
		public void onClick(DialogInterface dialog,int id) 
		{
		    if(!serverAddr.equals("") && !serverPort.equals(""))
			{
			    if(thread != null)
				{
				    thread.stop();
				    thread = null;
				}
			    thread = new Thread(new RunThread());
			    thread.start();
			}
		}
	    });
        
        alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() 
	    {
		public void onClick(DialogInterface dialog,int id) 
		{
		    Log.d("RStrace", "Connect onClick Cancel");
		   
		    dialog.cancel();
		}
	    });

	AlertDialog alertDialog = alertDialogBuilder.create();
	alertDialog.show();
    }
    private final Handler mHandler = new Handler() {
	    public void handleMessage(Message msg) {
		Message message;
		    switch (msg.what) {
		    case SAMPLE:
			message = Message.obtain();
			//int y = rnd.nextInt(10);
			//Pt p = new Pt(seq, y, seq);
			//power.sample(p);
			//seq++;
			//message.what = SAMPLE;
			//mHandler.sendMessageDelayed(message, SAMPLEINTERVAL);
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

