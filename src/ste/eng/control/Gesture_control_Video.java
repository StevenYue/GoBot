package ste.eng.control;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
















import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class Gesture_control_Video 
	extends Activity implements OnTouchListener, SensorEventListener, OnLongClickListener{
	private boolean isBotOn = false;
	private int servoStatus = 0;//1,2,3,4 == up,down,left,right
//	private boolean isServoUp = false;
	private String urlToLoad = "";
	private SoundPool sp;
	private int powerOnID = 0;
	private int powerOffID = 0;
	private Bitmap bmServoPressed,bmServo; 
	
	//different views
	private WebView mWebview;
	private Button buOn,buOFF;
	private ImageView ivSLeft,ivSRight,ivSUp,ivSDown;
	private EditText etURL;
	private TextView tvBotStatus;
	
	//powermanager to keep screen on
	private PowerManager.WakeLock myWakelock;
	
	//globla variables of sensors
	private SensorManager senMan;
	private Sensor senAcc;
	
	//Bluetooth variables
	private String targetBTName = Gobot_constants.BTBOLUTEK;
	private BluetoothAdapter myBTA;
	private BluetoothDevice targetBTD;
	private BluetoothSocket BTSocket;
	private OutputStream BTOutStream;
	private ArrayList<String> myBTDList;
		
	private Runnable sendServoCommand;
	private Handler servoHandler;
	
	//
	private byte lastCommand = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		final PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
		myWakelock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "ON");
		myWakelock.acquire();
		
		setContentView(R.layout.web_display);
		mWebview  = (WebView)findViewById(R.id.webVDisplay);
//		mWebview = new WebView(this);
		
        mWebview.getSettings().setJavaScriptEnabled(true); // enable javascript
        
        final Activity activity = this;

        mWebview.setWebViewClient(new WebViewClient() {
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Toast.makeText(activity, description, Toast.LENGTH_SHORT).show();
            }
        });
        
//      setContentView(mWebview);
        init();
    }

    private void init(){
    	senMan = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		senAcc = senMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		senMan.registerListener(this, senAcc , SensorManager.SENSOR_DELAY_NORMAL);
		myBTDList = new ArrayList<String>();
		
		
		buOn = (Button)findViewById(R.id.buOn);
		buOFF = (Button)findViewById(R.id.buOff);
		buOn.setOnLongClickListener(this);
		buOFF.setOnLongClickListener(this);
		
		ivSLeft = (ImageView)findViewById(R.id.ivSLEFT);
		ivSRight = (ImageView)findViewById(R.id.ivSRIGHT);
		ivSUp = (ImageView)findViewById(R.id.ivSUP);
		ivSDown = (ImageView)findViewById(R.id.ivSDOWN);
		ivSLeft.setOnTouchListener(this);
		ivSRight.setOnTouchListener(this);
		ivSUp.setOnTouchListener(this);
		ivSDown.setOnTouchListener(this);
		
		bmServo = BitmapFactory.decodeResource(getResources(), R.drawable.servo_move_64high);
		bmServoPressed = BitmapFactory.decodeResource(getResources(), R.drawable.servo_move_64high_pressed);
		
		etURL = (EditText)findViewById(R.id.etServerURL);
		tvBotStatus = (TextView)findViewById(R.id.tvBotStatus);
		
		sp = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
		powerOnID = sp.load(this, R.raw.on,1);
		powerOffID = sp.load(this, R.raw.off,1);
		
		blueSetUp();
		
		sendServoCommand = new Runnable() {
			public void run() {
				// TODO Auto-generated method stub
				switch(servoStatus){
				case Gobot_constants.servoStatusUp:sendCommand(Gobot_constants.servoUpCommand);break;
				case Gobot_constants.servoStatusDown:sendCommand(Gobot_constants.servoDownCommand);break;
				case Gobot_constants.servoStatusLeft:sendCommand(Gobot_constants.servoLeftCommand);break;
				case Gobot_constants.servoStatusRight:sendCommand(Gobot_constants.servoRightCommand);break;
					default:break;
				}
				servoHandler.postDelayed(this, 100);
			}
		};
    }
    
    
    
    @Override
    protected void onPause() {
    	// TODO Auto-generated method stub
    	super.onPause();
    	myWakelock.release();
    	senMan.unregisterListener(this);
    }
    
    protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		myWakelock.acquire(); 
		senMan.registerListener(this, senAcc,SensorManager.SENSOR_DELAY_NORMAL);
	}
    
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		if(isBotOn)
			closeBTConnection();
	}
    
    
	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		if(isBotOn == false)
			return;
		
		Sensor mySensor = event.sensor;
		float x,y,z;
		int shift = -1;
		 
	    if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
	        x = event.values[0];
	        y = event.values[1];
	        z = event.values[2];
	        
	        if(x < -1){ // go forward
				shift =  (int)((-x)/4);
				if(shift>2) shift = 2;
				
				if(y<-1.5)
					sendCommand(Gobot_constants.rightWheelIncrease);
				else if(y>1.5)
					sendCommand(Gobot_constants.leftWheelIncrease);
				else	
					sendCommand(Gobot_constants.forwardCommand[shift]);
	        }else if(x > 2){// go backward
				shift =  (int)((x)/4);
				if(shift>2) shift = 2;
				sendCommand(Gobot_constants.backwardCommand[shift]);
	        }else if(y < -3){ // left turn	
				sendCommand(Gobot_constants.trunLeftCommand);
	        }else if(y > 3){ // right turn
				sendCommand(Gobot_constants.trunRightCommand);
	        }else{// Don't move				
				sendCommand(Gobot_constants.clearCommand);
	        }
	        
	    }
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if(isBotOn == false)
			return true;
		
		
		if(v.getId() == R.id.ivSUP){
			servoStatus = Gobot_constants.servoStatusUp;
			switch(event.getAction()){
			case MotionEvent.ACTION_DOWN: 
				ivSUp.setImageBitmap(bmServoPressed);
				if(servoHandler != null) return true;
					servoHandler = new Handler();
					servoHandler.postDelayed(sendServoCommand, 100);
				break;
			case MotionEvent.ACTION_UP:
				ivSUp.setImageBitmap(bmServo);
				if (servoHandler == null) return true;
				servoHandler.removeCallbacks(sendServoCommand);
				servoHandler = null;
				break;
			default: break;
			}
		}
		
		if(v.getId() == R.id.ivSDOWN){
			servoStatus = Gobot_constants.servoStatusDown;
			switch(event.getAction()){
			case MotionEvent.ACTION_DOWN: 
				ivSDown.setImageBitmap(bmServoPressed);
				if(servoHandler != null) return true;
				servoHandler = new Handler();
				servoHandler.postDelayed(sendServoCommand, 100);
				break;
			case MotionEvent.ACTION_UP:
				ivSDown.setImageBitmap(bmServo);
				if (servoHandler == null) return true;
				servoHandler.removeCallbacks(sendServoCommand);
				servoHandler = null;
				break;
			default: break;
			}
		}
		
		if(v.getId() == R.id.ivSLEFT){
			servoStatus = Gobot_constants.servoStatusLeft;
			switch(event.getAction()){
			case MotionEvent.ACTION_DOWN: 
				ivSLeft.setImageBitmap(bmServoPressed);
				if(servoHandler != null) return true;
					servoHandler = new Handler();
					servoHandler.postDelayed(sendServoCommand, 100);
				break;
			case MotionEvent.ACTION_UP:
				ivSLeft.setImageBitmap(bmServo);
				if (servoHandler == null) return true;
				servoHandler.removeCallbacks(sendServoCommand);
				servoHandler = null;
				break;
			default: break;
			}
		}
		
		if(v.getId() == R.id.ivSRIGHT){
			servoStatus = Gobot_constants.servoStatusRight;
			switch(event.getAction()){
			case MotionEvent.ACTION_DOWN: 
				ivSRight.setImageBitmap(bmServoPressed);
				if(servoHandler != null) return true;
				servoHandler = new Handler();
				servoHandler.postDelayed(sendServoCommand, 100);
				break;
			case MotionEvent.ACTION_UP:
				ivSRight.setImageBitmap(bmServo);
				if (servoHandler == null) return true;
				servoHandler.removeCallbacks(sendServoCommand);
				servoHandler = null;
				break;
			default: break;
			}
		}
		
		return true;
	}
	
	@Override
	public boolean onLongClick(View v) {
		String tempURL = "";
		// TODO Auto-generated method stub
		if(v.getId() == R.id.buOn){
			isBotOn = true;
			if(powerOnID != 0)
				sp.play(powerOnID, 1, 1, 0, 0, 1);
			tempURL = etURL.getText().toString();
			blueConnect();
			if(!isBotOn)
				return true;
			try{
				mWebview.loadUrl(tempURL);
			}catch(Exception e){
				Toast.makeText(getApplicationContext(), "Can't load page", Toast.LENGTH_SHORT);
				closeBTConnection();
				return true;
			}
			tvBotStatus.setText("Bot Status:ON");	
			return true;
		}else{
			isBotOn = false;
			if(powerOffID != 0)
				sp.play(powerOffID, 1, 1, 0, 0, 1);
			tvBotStatus.setText("Bot Status:OFF");
			closeBTConnection();
			mWebview.loadUrl("about:blank");
			return true;
		}
		
	}
	
	
	
	@Override
	public boolean onCreateOptionsMenu(android.view.Menu menu) {
		// TODO Auto-generated method stub
		super.onCreateOptionsMenu(menu);
		MenuInflater blowUp = getMenuInflater();
		blowUp.inflate(R.menu.gcvmenu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		super.onOptionsItemSelected(item);
		switch(item.getItemId()){
		case R.id.gcvMenuBOLUTEK:
			targetBTName = Gobot_constants.BTBOLUTEK;
			break;
		case R.id.gcvMenuHC_05:
			targetBTName = Gobot_constants.BTHC_05;
			break;

			default: break;
		}
		
		return true;
	}
	
	
	/**
	 * ALL the self define functions
	 */
	
	private void blueSetUp(){
		myBTA = BluetoothAdapter.getDefaultAdapter();
		if(myBTA == null){
			Toast.makeText(getApplicationContext(), "No BT Device", Toast.LENGTH_LONG).show();
			return;
		}
		
		if(!myBTA.isEnabled()){
			Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivity(enableBT);
			return;
		}
		
	}
	
	private void blueConnect(){
		Set<BluetoothDevice> pairedDevices = myBTA.getBondedDevices();
		myBTDList.clear();
		// If there are paired devices
		if (pairedDevices.size() > 0) {
		    // Loop through paired devices
		    for (BluetoothDevice device : pairedDevices) {
		        // Add the name and address to an array adapter to show in a ListView
		    	myBTDList.add(device.getName() + "\n" + device.getAddress());
		    	if(device.getName().contains(targetBTName)){
		    		targetBTD = device;
		    	}
		    }
		}
		
		// Get a BluetoothSocket to connect with the given BluetoothDevice
	    try {
	        BTSocket = targetBTD.createRfcommSocketToServiceRecord(Gobot_constants.MY_UUID);
	    } catch (Exception e) { 
	    	Toast.makeText(getApplicationContext(), "Can't create BT socket!", Toast.LENGTH_SHORT).show();
	    	isBotOn = false;
	    	return;
	    }	
	    
		try {
			// Connect the device through the socket. This will block
			// until it succeeds or throws an exception
			
			BTSocket.connect();
			Toast.makeText(getApplicationContext(), "Connection Built!", Toast.LENGTH_SHORT).show();
		}catch (IOException e) {
			/**
			 * for some reason even with a good connection, it still catches error
			 */
			
			// Unable to connect; close the socket and get out
			Toast.makeText(getApplicationContext(), "Can't connect!", Toast.LENGTH_SHORT).show();
			isBotOn = false;
/*			try {
				BTSocket.close();
				Toast.makeText(getApplicationContext(), "Socket Closed", Toast.LENGTH_SHORT).show();
			} catch (Exception ex) {
				Toast.makeText(getApplicationContext(), "Can't Close!", Toast.LENGTH_SHORT).show();
			}*/
		}
	 
		//Get the output stream from the socket
		try {
			BTOutStream = BTSocket.getOutputStream();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void closeBTConnection(){
		try {
			BTSocket.close();
		} catch (IOException e) {}
	}
	
	private void sendCommand(byte b){
//		byte[] bytes = {b};
		try {
			BTOutStream.write(b);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void sendCommands(byte a, byte b){
		byte[] bytes = {a,b};
		try {
			BTOutStream.write(bytes);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

}
