package com.traviswyatt.ioio.Adxl345Hmc5883lANN;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class TextRecorder {
	
	OutputStreamWriter writer;
	boolean started;
	Context context;
	String basePath = "/mnt/sdcard/SSH-ANN/"; // for sumsang, the path is "/mnt/sdcard"
	
	public TextRecorder(Context context, String basePath) {
		this.started = false;
		this.context = context;
		this.basePath = basePath;
	}
	
	public void start() {
		if (started) {
			return;
		}
		
		Date date = new Date();
		String filename = "6DOF-ADXL345_HMC5883L-ANN" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(date) + ".log";
		String filepath = basePath + File.separator + filename;
		
		try {
			File file = new File(filepath);
			writer = new OutputStreamWriter(new FileOutputStream(file));
		} catch (Exception e) {
			started = false;
			Log.e("Error", e.getMessage());
			e.printStackTrace();
			return;
		}
		
		started = true;
	}
	
	public void stop() {
		if (!started) {
			return;
		}
		
		try {
			writer.close();
		} catch (Exception e) {
			Log.e("Error", e.getMessage());
			e.printStackTrace();
		}
		started = false;
	}
	
	public void writeLine(String line) {
		if (!started) {
			return;
		}
		
		try {
			writer.write(line + "\r\n");
			writer.flush();
		} catch (Exception e) {
			Log.e("Error", e.getMessage());
			this.stop();
			e.printStackTrace();
		}
	}
}
