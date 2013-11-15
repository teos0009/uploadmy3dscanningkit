package com.example.arduino_serial;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

public class picHandler implements PictureCallback {

	private final Context context;
	private final String TAG = "";

	public picHandler(Context context) {
		this.context = context;
		appendLog("picHandler context");
	}

	@Override
	public void onPictureTaken(byte[] data, Camera camera) {
		
		File pictureFileDir = getDir();

		if (!pictureFileDir.exists() && !pictureFileDir.mkdirs()) {

			Log.e(TAG, "Can't create directory to save image.");
			Toast.makeText(context, "Can't create directory to save image.",
					Toast.LENGTH_LONG).show();
			appendLog("Can't create directory to save image.");
			return;

		}// end if pic dir

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyymmddhhmmss");
		String date = dateFormat.format(new Date());
		String photoFile = "3DScanKit_" + date + ".jpg";

		String filename = pictureFileDir.getPath() + File.separator + photoFile;

		File pictureFile = new File(filename);

		try {
			FileOutputStream fos = new FileOutputStream(pictureFile);
			fos.write(data);
			fos.close();
			Toast.makeText(context, "New Image saved:" + photoFile,
					Toast.LENGTH_SHORT).show();
			appendLog("New Image saved");
			//----shin--------
			ArduinoSerial.myTakeState = false;//finish take pic set to false
			ArduinoSerial.setCameraParam();//set camera param
			ArduinoSerial.mCamera.startPreview();//need to refesh surface view
		} catch (Exception error) {
			Log.e(TAG, "File" + filename + "not saved: " + error.getMessage());
			Toast.makeText(context, "Image could not be saved.",
					Toast.LENGTH_SHORT).show();
			appendLog("Image could not be saved.");
		}
	}// on pic taken

	private File getDir() {
		File sdDir = Environment
				.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
		appendLog("directory pictures");
		return new File(sdDir, "scanKit");
	}// end get dir

	public void appendLog(String text) {
		File path = Environment
				.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
		File logFile = new File(path, "arduino-camera.log");

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
}