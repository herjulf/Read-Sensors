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
import android.view.View;
import android.view.LayoutInflater;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import android.util.Log;

public class Client extends Activity {
    private Socket socket = null;
    private Context context = this;
    private boolean connected = false;
    private String serverAddr = "";
    private String serverPort = "";
    Handler handler = null;
    Thread thread = null;

    @Override
	public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        this.handler = new Handler();

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
    }

    public class RunThread implements Runnable 
    {
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
					Toast.makeText(context, strData, Toast.LENGTH_LONG).show();
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
}
