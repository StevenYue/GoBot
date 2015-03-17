package ste.eng.control;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
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
import android.os.SystemClock;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class Gesture_control extends Activity implements OnTouchListener,SensorEventListener{
	private ImageView ivFire, ivLeft,ivRight, ivForward,ivBack,ivStart,ivServoLeft,ivServoRight;
	private Bitmap bmArrow,bmArrowMove,bmFire,bmGun,bmStartPressed,bmStart,bmServo,bmServoPressed;
	private TextView tvDebug,tvOnOff;
	private SoundPool sp;
	private MediaPlayer mp;
	private int shotGunID = 0;
	private int powerOnID = 0;
	private int powerOffID = 0;
	private boolean isBotOn = false;
	private boolean isServoLeft = false;
	
	//globla variables of sensors
	private SensorManager senMan;
	private Sensor senAcc;
	
	//powermanager to keep screen on
	private PowerManager.WakeLock myWakelock;
	
	//Bluetooth variables
	private String targetBTName = Gobot_constants.BTBOLUTEK;
	private BluetoothAdapter myBTA;
	private BluetoothDevice targetBTD;
	private BluetoothSocket BTSocket;
	private OutputStream BTOutStream;
	private ArrayList<String> myBTDList;
	
	private Runnable sendServoCommand;
	private Handler servoHandler;

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		final PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
		myWakelock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "ON");
		myWakelock.acquire();
		
		setContentView(R.layout.gesture_control);
		init();
	} 
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		myWakelock.acquire(); 
		senMan.registerListener(this, senAcc,SensorManager.SENSOR_DELAY_NORMAL);
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		myWakelock.release();
		senMan.unregisterListener(this);
	}
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		if(isBotOn)
			closeBTConnection();
	}
	
	private void init(){
		ivFire = (ImageView)findViewById(R.id.ivFire);
		ivLeft = (ImageView)findViewById(R.id.ivLeft);
		ivRight = (ImageView)findViewById(R.id.ivRight);
		ivForward = (ImageView)findViewById(R.id.ivForward);
		ivBack = (ImageView)findViewById(R.id.ivBack);
		ivStart = (ImageView)findViewById(R.id.ivStart);
		ivServoLeft = (ImageView)findViewById(R.id.ivServoLeft);
		ivServoRight = (ImageView)findViewById(R.id.ivServoRight);
		
		tvDebug = (TextView)findViewById(R.id.tvDebug);
		tvOnOff = (TextView)findViewById(R.id.tvOnOff);
	    Typeface custom_font = Typeface.createFromAsset(getAssets(),"Crackvetica.ttf");
	    tvOnOff.setTypeface(custom_font);
		
		
		senMan = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		senAcc = senMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		senMan.registerListener(this, senAcc , SensorManager.SENSOR_DELAY_NORMAL);
		
		bmFire = BitmapFactory.decodeResource(getResources(), R.drawable.fire);
		bmGun = BitmapFactory.decodeResource(getResources(), R.drawable.gun);
		bmArrow = BitmapFactory.decodeResource(getResources(), R.drawable.arrow);
		bmArrowMove = BitmapFactory.decodeResource(getResources(), R.drawable.arrow_move);
		bmStartPressed = BitmapFactory.decodeResource(getResources(), R.drawable.start_pressed);
		bmStart = BitmapFactory.decodeResource(getResources(), R.drawable.start);
		bmServo = BitmapFactory.decodeResource(getResources(), R.drawable.servo_move);
		bmServoPressed = BitmapFactory.decodeResource(getResources(), R.drawable.servo_move_pressed);
		
		myBTDList = new ArrayList<String>();
		blueSetUp();
		
		sp = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
		shotGunID = sp.load(this, R.raw.shotgun, 1);
		powerOnID = sp.load(this, R.raw.on,1);
		powerOffID = sp.load(this, R.raw.off,1);
		
		
		ivFire.setOnTouchListener(this);
		ivServoLeft.setOnTouchListener(this);
		ivServoRight.setOnTouchListener(this);
		ivStart.setOnTouchListener(this);
		ivStart.setOnLongClickListener(new OnLongClickListener() {
				public boolean onLongClick(View v) {
				// TODO Auto-generated method stub
					if(isBotOn == false){
						isBotOn = true;
						if(powerOnID != 0)
							sp.play(powerOnID, 1, 1, 0, 0, 1);
						blueConnect();
						if(isBotOn)
							tvOnOff.setText("ON");
					}else{
						isBotOn = false;
						if(powerOffID != 0)
							sp.play(powerOffID, 1, 1, 0, 0, 1);
						closeBTConnection();
						tvOnOff.setText("OFF");	
						ivLeft.setImageBitmap(bmArrow);
						ivRight.setImageBitmap(bmArrow);
						ivForward.setImageBitmap(bmArrow);
						ivBack.setImageBitmap(bmArrow);
					}
				return true;
			}
		});
		
		sendServoCommand = new Runnable() {
			public void run() {
				// TODO Auto-generated method stub
				if(isServoLeft)
					sendCommand(Gobot_constants.servoLeftCommand);
				else
					sendCommand(Gobot_constants.servoRightCommand);
				servoHandler.postDelayed(this, 100);
			}
		};	
	} 

	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		// TODO Auto-generated method stub
		if(v.getId() == R.id.ivStart){
			switch(event.getAction()){
			case MotionEvent.ACTION_DOWN: 
				ivStart.setImageBitmap(bmStartPressed);
				break;
				
			case MotionEvent.ACTION_UP:
				ivStart.setImageBitmap(bmStart);
				break;
			default: break;
			}
			return false;
		}
		
		if(isBotOn == false)
			return true;
		
		if(v.getId() == R.id.ivFire){
			switch(event.getAction()){
			case MotionEvent.ACTION_DOWN: 
				ivFire.setImageBitmap(bmFire);
				if(shotGunID != 0)
					sp.play(shotGunID, 1, 1, 0, 0, 1);
				break;
			case MotionEvent.ACTION_UP:
				ivFire.setImageBitmap(bmGun);
//				sendCommand(Gobot_constants.fireCommand);
				break;
			default: break;
			}
		}
		
		
		if(v.getId() == R.id.ivServoLeft){
			isServoLeft = true;
			switch(event.getAction()){
			case MotionEvent.ACTION_DOWN: 
				ivServoLeft.setImageBitmap(bmServoPressed);
				if(servoHandler != null) return true;
					servoHandler = new Handler();
					servoHandler.postDelayed(sendServoCommand, 100);
				break;
			case MotionEvent.ACTION_UP:
				ivServoLeft.setImageBitmap(bmServo);
				if (servoHandler == null) return true;
				servoHandler.removeCallbacks(sendServoCommand);
				servoHandler = null;
				break;
			default: break;
			}
		}
		
		if(v.getId() == R.id.ivServoRight){
			isServoLeft = false;
			switch(event.getAction()){
			case MotionEvent.ACTION_DOWN: 
				ivServoRight.setImageBitmap(bmServoPressed);
				if(servoHandler != null) return true;
				servoHandler = new Handler();
				servoHandler.postDelayed(sendServoCommand, 100);
				break;
			case MotionEvent.ACTION_UP:
				ivServoRight.setImageBitmap(bmServo);
				if (servoHandler == null) return true;
				servoHandler.removeCallbacks(sendServoCommand);
				servoHandler = null;
				break;
			default: break;
			}
		}
		
		
		return true;
	}

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
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		if(resultCode == RESULT_OK){
			Toast.makeText(getApplicationContext(), "BT is On", Toast.LENGTH_SHORT).show();
		}
		if(resultCode == RESULT_CANCELED){
			Toast.makeText(getApplicationContext(), "You have to turn BT on!", Toast.LENGTH_SHORT).show();
		}
	}
	
	
	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		if(isBotOn == false)
			return;
		
		
		Sensor mySensor = event.sensor;
		String sDebug;
		float x,y,z;
		int shift = -1;
		 
	    if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
	        x = event.values[0];
	        y = event.values[1];
	        z = event.values[2];
	        
	        if(y < -3){ // left turn
				ivLeft.setImageBitmap(bmArrowMove);
				
				ivRight.setImageBitmap(bmArrow);
				ivForward.setImageBitmap(bmArrow);
				ivBack.setImageBitmap(bmArrow);
				
				sendCommand(Gobot_constants.trunLeftCommand);
	        }else if(y > 3){ // right turn
				ivRight.setImageBitmap(bmArrowMove);
				
				ivLeft.setImageBitmap(bmArrow);
				ivForward.setImageBitmap(bmArrow);
				ivBack.setImageBitmap(bmArrow);
				sendCommand(Gobot_constants.trunRightCommand);
	        }else if(x < -2){ // go forward
				ivForward.setImageBitmap(bmArrowMove);
				
				ivLeft.setImageBitmap(bmArrow);
				ivRight.setImageBitmap(bmArrow);
				ivBack.setImageBitmap(bmArrow);
				shift =  (int)((-x)/1.7) - 1;
				if(shift<0) shift = 0;
				if(shift>4) shift = 4;
				sendCommand(Gobot_constants.forwardCommand[shift]);
	        }else if(x > 2){// go backward
				ivBack.setImageBitmap(bmArrowMove);
				
				ivLeft.setImageBitmap(bmArrow);
				ivRight.setImageBitmap(bmArrow);
				ivForward.setImageBitmap(bmArrow);
				
				shift =  (int)((x)/1.7) - 1;
				if(shift<0) shift = 0;
				if(shift>4) shift = 4;
				sendCommand(Gobot_constants.backwardCommand[shift]);
	        }else{// Don't move
				ivLeft.setImageBitmap(bmArrow);
				ivRight.setImageBitmap(bmArrow);
				ivForward.setImageBitmap(bmArrow);
				ivBack.setImageBitmap(bmArrow);
				
				sendCommand(Gobot_constants.clearCommand);
	        }
	        
	        
	        //Debug Text to display values of Accelerometer
//	        sDebug = "x:"+ Float.toString(x) + "\ny:"+
//	        		Float.toString(y)+ "\nz:"+Float.toString(z);
//	        tvDebug.setText(sDebug);
	    }
	     
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
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
}
