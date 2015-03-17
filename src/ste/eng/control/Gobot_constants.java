package ste.eng.control;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

import android.os.Handler;

public class Gobot_constants {
	public static final String BTBOLUTEK = "BOLUTEK";
	public static final String BTHC_05 = "HC-05";

	public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
		
	//these are the protocal details
	public static final byte clearCommand = 'n';
	public static final byte fireCommand = 'F';
	public static final byte trunLeftCommand = 'l';
	public static final byte trunRightCommand = 'r';
	
	//Among 9 straight commands, there are 3 different speed control
	//they are 2,5,8, the index are 1,4,7. they mean just going straight
	//and 1,4,7 with index 0,3,7 actually mean going straight but left wheel increase
	//3,6,9 with index 2,5,8 mean the opposite
	public static final byte[] forwardCommand = {'0','1','2'};
	public static final byte[] backwardCommand = {'5','6','7'};
	public static final byte leftWheelIncrease = 'L';
	public static final byte rightWheelIncrease = 'R';
	public static final byte leftWheelDecrease = 'q';
	public static final byte rightWheelDecrease = 'w';
	public static final byte servoLeftCommand = '<';
	public static final byte servoRightCommand = '>';
	public static final byte servoUpCommand = '+';
	public static final byte servoDownCommand = '-';
	public static final int servoStatusUp = 1;
	public static final int servoStatusDown = 2;
	public static final int servoStatusLeft = 3;
	public static final int servoStatusRight = 4;
}
