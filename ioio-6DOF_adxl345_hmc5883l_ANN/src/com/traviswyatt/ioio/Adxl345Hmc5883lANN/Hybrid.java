package com.traviswyatt.ioio.Adxl345Hmc5883lANN;

import com.traviswyatt.ioio.Adxl345Hmc5883lANN.ADXL345.ADXL345Listener;

import ioio.lib.api.IOIO;
import ioio.lib.api.TwiMaster;
import ioio.lib.api.TwiMaster.Rate;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.IOIOLooper;

public class Hybrid implements IOIOLooper {
	private ADXL345 adxl345;
	private ITG3205 itg3205;
	private HMC5883L hmc5883l;
	private TwiMaster i2c;
	private int twiNum;
	private Rate rate;
	
	private byte deviceID;
	
	private float Acc_x;
	private float Acc_y;
	private float Acc_z;
	
	private float Acc_x_offset=(float) 0.65; //0.65 is for the 9DOF calibration
	private float Acc_y_offset=(float) 0.3;
	private float Acc_z_offset=(float) 0.9;
	
	private float Acc_magnitude;
	
	
	
	private float Gyro_x;
	private float Gyro_y;
	private float Gyro_z;
	private float Gyro_x_offset=(float) -0.72;
	private float Gyro_y_offset=(float) 0.57;
	private float Gyro_z_offset=(float) -0.54;
	private float Gyro_temperature;
	
	private float Mag_x;
	private float Mag_y;
	private float Mag_z;
	
	private float Mag_Resolution=(float) 1.22; // 

	
	public interface HybridListener {
		public void onDeviceId(byte deviceId);
		public void onData(
				float  acc_x,
				float  acc_y,
				float  acc_z,
				double acc_magnitude,
//				float gyro_x,
//				float gyro_y,
//				float gyro_z,
//				float gyro_temperature,
				float Mag_x,
			    float Mag_y,
				float Mag_z
				);
	}
	
	private HybridListener listener;

	
	public Hybrid(int twiNum, Rate rate) {
		this.twiNum = twiNum;
		this.rate = rate;
		this.adxl345 = new ADXL345(twiNum, rate);
//		this.itg3205 = new ITG3205(twiNum, rate);
		this.hmc5883l= new HMC5883L(twiNum, rate);
	}
	
	public Hybrid setListener(HybridListener listener) {
		this.listener = listener;
		return this;
	}
	
	@Override
	public void setup(IOIO ioio) throws ConnectionLostException, InterruptedException {
		i2c = ioio.openTwiMaster(twiNum, rate, false /* smbus */);
		adxl345.setI2C(i2c);
//		itg3205.setI2C(i2c);
	    hmc5883l.setI2C(i2c);
		
		final Hybrid hybrid = this;
		adxl345.setListener(new ADXL345.ADXL345Listener() {
			@Override
			public void onDeviceId(byte deviceId) {
				hybrid.deviceID = deviceId;
				if (hybrid.listener != null) {
					hybrid.listener.onDeviceId(deviceId);
				}
			}
									
			@Override
			public void onData(int x, int y, int z) {
				hybrid.Acc_x = x * adxl345.getMultiplier() * 9.8f+Acc_x_offset;
				hybrid.Acc_y = y * adxl345.getMultiplier() * 9.8f+Acc_y_offset;
				hybrid.Acc_z = z * adxl345.getMultiplier() * 9.9f+Acc_z_offset;
				hybrid.Acc_magnitude = (float) Math.sqrt(hybrid.Acc_x * hybrid.Acc_x + hybrid.Acc_y * hybrid.Acc_y + hybrid.Acc_z * hybrid.Acc_z);
			}
			@Override
			public void onError(String message) {
				// TODO Auto-generated method stub
			}
		});
		
//		itg3205.setListener(new ITG3205.ITG3205Listener() {
//			public void onDeviceId(byte deviceId) {
//
//			}
//			
//			
//			@Override
//			public void onData(int x, int y, int z, int temperature) {
//				hybrid.Gyro_x = ((float) x / 14.375f)+Gyro_x_offset;
//				hybrid.Gyro_y = ((float) y / 14.375f)+Gyro_y_offset;
//				hybrid.Gyro_z = ((float) z / 14.375f)+Gyro_z_offset;
//				hybrid.Gyro_temperature = (35f + (float) (temperature + 13200) / 280f);
//			}
//			@Override
//			public void onError(String message) {
//				// TODO Auto-generated method stub
//			}
//		});
//		
		hmc5883l.setListener(new HMC5883L.HMC5883LListener() {
			public void onDeviceId(byte deviceId) {

			}			
			@Override
			public void onData(int Mag_x, int Mag_y, int Mag_z) {
				
				hybrid.Mag_x = (float) Mag_x*Mag_Resolution;
				hybrid.Mag_y = (float) Mag_y*Mag_Resolution;
				hybrid.Mag_z = (float) Mag_z*Mag_Resolution;
		
			}
			@Override
			public void onError(String message) {
				// TODO Auto-generated method stub
			}
		});
		adxl345.setup(ioio);
//		itg3205.setup(ioio);
		hmc5883l.setup(ioio);
	}

	@Override
	public void loop() throws ConnectionLostException, InterruptedException {
	
		adxl345.loop();
			
//		itg3205.loop();
		
		hmc5883l.loop();
				
		if (listener != null) {
			
			listener.onData(
					Acc_x,
					Acc_y,
					Acc_z,
					Acc_magnitude,
//					Gyro_x,
//					Gyro_y,
//					Gyro_z,
//					Gyro_temperature,
					Mag_x,
					Mag_y,
					Mag_z					
					);
		}
	}

	@Override
	public void disconnected() {
		adxl345.disconnected();
//		itg3205.disconnected();
		hmc5883l.disconnected();
		// TODO Auto-generated method stub
	}

	@Override
	public void incompatible() {
		adxl345.incompatible();
//		itg3205.incompatible();
		hmc5883l.incompatible();
		// TODO Auto-generated method stub
	}
}
