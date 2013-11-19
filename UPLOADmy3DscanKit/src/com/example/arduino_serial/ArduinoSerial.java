package com.example.arduino_serial;

import android.app.Activity;
import android.os.Bundle;

import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.HexDump;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A sample Activity demonstrating USB-Serial support. 
 * modified to take pictures upon receive ascii char over serial
 * from arduino 
 * @author mike wakerly (opensource@hoho.com)
 * @author shin (teos0009@gmail.com)
 */

public class ArduinoSerial extends Activity implements SurfaceHolder.Callback {
	private final String TAG = ArduinoSerial.class.getSimpleName();

	/**
	 * The device currently in use, or {@code null}.
	 */
	private UsbSerialDriver mSerialDevice;

	/**
	 * The system's USB service.
	 */
	private UsbManager mUsbManager;

	private TextView mTitleTextView;
	// private TextView mDumpTextView;//not used
	// private ScrollView mScrollView;//not used

	// ======var====
	static Camera mCamera; // shared var with picHandler
	private int mCameraId = 0;
	private SurfaceView mSurfaceView;
	private SurfaceHolder mSurfaceHolder;
	boolean myPreviewState = false;
	static boolean myTakeState = false;// shared var with picHandler
	private int count = 0;
	// =============

	private final ExecutorService mExecutor = Executors
			.newSingleThreadExecutor();

	private SerialInputOutputManager mSerialIoManager;

	private final SerialInputOutputManager.Listener mListener = new SerialInputOutputManager.Listener() {

		@Override
		public void onRunError(Exception e) {
			Log.d(TAG, "Runner stopped.");
		}

		@Override
		public void onNewData(final byte[] data) {
			ArduinoSerial.this.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					ArduinoSerial.this.updateReceivedData(data);
				}
			});
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		mTitleTextView = (TextView) findViewById(R.id.demoTitle);
		// mDumpTextView = (TextView) findViewById(R.id.demoText);
		// mScrollView = (ScrollView) findViewById(R.id.demoScroller);//not used

		// =====shin=========
		getWindow().setFormat(PixelFormat.UNKNOWN);
		mSurfaceView = (SurfaceView) findViewById(R.id.surfaceview);
		mSurfaceHolder = mSurfaceView.getHolder();
		mSurfaceHolder.addCallback(this);

		// mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);//deprecated
		// mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);//deprecated
		mCameraId = findBackCamera();// ret cam found.
		mCamera = Camera.open(mCameraId);
		setCameraParam();
		// ===============

	}

	@Override
	protected void onPause() {
		super.onPause();
		stopIoManager();
		if (mSerialDevice != null) {
			try {
				mSerialDevice.close();
			} catch (IOException e) {
				// Ignore.
			}
			mSerialDevice = null;
		}

		// ==shin==========
		if (mCamera != null && myPreviewState) {
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;

			myPreviewState = false;
		}
		// =============

	}

	@Override
	protected void onResume() {
		super.onResume();

		// ==shin==========
		if (mCamera == null) {// camera was released when pause
			mCamera = Camera.open(mCameraId);
			setCameraParam();// reopen cam
		}
		// ==================
		mSerialDevice = UsbSerialProber.acquire(mUsbManager);
		Log.d(TAG, "Resumed, mSerialDevice=" + mSerialDevice);
		if (mSerialDevice == null) {
			mTitleTextView.setText("No serial device.");
		} else {
			try {
				mSerialDevice.open();
			} catch (IOException e) {
				Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
				mTitleTextView.setText("Error opening device: "
						+ e.getMessage());
				try {
					mSerialDevice.close();
				} catch (IOException e2) {
					// Ignore.
				}
				mSerialDevice = null;
				return;
			}
			mTitleTextView.setText("Serial device: " + mSerialDevice);
		}
		onDeviceStateChange();
	}

	private void stopIoManager() {
		if (mSerialIoManager != null) {
			Log.i(TAG, "Stopping io manager ..");
			mSerialIoManager.stop();
			mSerialIoManager = null;
		}
	}

	private void startIoManager() {
		if (mSerialDevice != null) {
			Log.i(TAG, "Starting io manager ..");
			mSerialIoManager = new SerialInputOutputManager(mSerialDevice,
					mListener);
			mExecutor.submit(mSerialIoManager);
		}
	}

	private void onDeviceStateChange() {
		stopIoManager();
		startIoManager();
	}

	private void updateReceivedData(byte[] data) {
		final String message = "Read " + data.length + " bytes: \n"
				+ HexDump.dumpHexString(data) + "\n\n";
		appendLog(HexDump.dumpHexString(data));
		// mDumpTextView.append(message);
		// scroll view
		// mScrollView.smoothScrollTo(0, mDumpTextView.getBottom());
		// ---shin---
		if (HexDump.dumpHexString(data).contains("A")) {
			autoPic();
			appendLog("mCamera.takePicture");
			Toast.makeText(getBaseContext(), "mCamera.takePicture",
					Toast.LENGTH_SHORT).show();
			//refreshSurfacePreview();//crash here?=> yes
		}// else: do nothing
	}// end update rx data

	
	// -------shin----
	// after take pic refresh preview on surface
	private void refreshSurfacePreview() {
		// start preview after pic taken
		mCamera.startPreview();
		myPreviewState = true;
		myTakeState = false;// guard for take pic
		appendLog("refresh surface preview");
	}

	// After a picture is taken, you must restart the preview before the user
	// can take another picture
	private void autoPic() {

		if (myTakeState == true) {//still taking pic. do nothing
			appendLog("true myTakeState");
			Log.e(TAG, "true myTakeState");
		
		
		} else {
			appendLog("false myTakeState");
			Log.e(TAG, "false myTakeState");
			if (mCamera != null) {
				myTakeState = true;
				count++;
			mCamera.takePicture(null, null, new picHandler(
					getApplicationContext()));
			appendLog("mCamera.autoPic" + " " + count);
			Toast.makeText(getBaseContext(), "mCamera.autoPic",
					Toast.LENGTH_SHORT).show();
			}
		}
	}// end auto pic

	private int findBackCamera() {
		int mCameraId = -1;
		String mCameraState = "";
		// Search for the back mCamera
		int numberOfCameras = Camera.getNumberOfCameras();
		for (int i = 0; i < numberOfCameras; i++) {
			CameraInfo info = new CameraInfo();
			Camera.getCameraInfo(i, info);
			if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
				Log.d(TAG, "Camera found");
				mTitleTextView.setText("Camera Found");
				mCameraId = i;
				mCameraState = "mCamera found=" + " " + mCameraId;
				appendLog(mCameraState);
				Toast.makeText(getBaseContext(), "mCamera found",
						Toast.LENGTH_LONG).show();
				break;
			}
		}
		return mCameraId;
	}// end find mCamera

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub
		setCameraParam();

		//style1
		try {
			mCamera.setPreviewDisplay(holder);
			mCamera.startPreview();
			myPreviewState = true;

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			appendLog("set preview disp holder");
		}
	}// end surface changed

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		// put mCamera preview in the surface
		appendLog("surface created");

		if (mCamera != null) {
			try {
				appendLog("Created set Camera Param");
				// mCamera param
				Camera.Parameters params = mCamera.getParameters();
				// set the focus mode
				params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
				// set Camera parameters
				mCamera.setParameters(params);

				// set preview size
				// Size mSize = mCamera.getParameters().getPreviewSize();
				// mSurfaceHolder.setSizeFromLayout();
				// mSurfaceHolder.setFixedSize(mSize.height, mSize.width);

				// preview param
				mCamera.setPreviewDisplay(holder);
				mCamera.startPreview();
				myPreviewState = true;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				appendLog(e.toString());
			}
		}// end if
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		// ==shin==========
		if (mCamera != null && myPreviewState) {
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
			myPreviewState = false;
		}
		// =============
	}// end surface destroy

	public void appendLog(String text) {
		File path = Environment
				.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
		File logFile = new File(path, "arduino-serial.log");

		if (!logFile.exists()) {
			try {
				logFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			// BufferedWriter for performance, true to set append to file flag
			BufferedWriter buf = new BufferedWriter(new FileWriter(logFile,
					true));
			buf.append(text);
			buf.newLine();
			buf.flush();
			buf.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}// end append log

	public static void setCameraParam() {
		if (mCamera != null) {
			try {
				
				// mCamera param
				Camera.Parameters params = mCamera.getParameters();
				// set the focus mode
				params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
				// set Camera parameters
				mCamera.setParameters(params);
				// mCamera.setDisplay(surfaceHolder);//method unavail
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				
			}
		}// end if
	}// end set mCamera
}