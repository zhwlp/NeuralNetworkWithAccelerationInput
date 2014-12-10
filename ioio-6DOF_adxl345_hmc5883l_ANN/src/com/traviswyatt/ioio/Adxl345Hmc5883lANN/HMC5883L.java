package com.traviswyatt.ioio.Adxl345Hmc5883lANN;

import ioio.lib.api.IOIO;
import ioio.lib.api.TwiMaster;
import ioio.lib.api.TwiMaster.Rate;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.IOIOLooper;

public class HMC5883L implements IOIOLooper {
	
	/**
	 * Duration to sleep thread after a register write.
	 */
	public static final long REGISTER_WRITE_DELAY = 200L;
		
	/**
	 * Register map per datasheet.
	 */
	
	public static final byte CRA  = (byte) 0x00; // Configuration Register A
	public static final byte CRB  = (byte) 0x01; // Configuration Register B
	public static final byte Mode_Register =   (byte) 0x02; // Mode Register
	public static final byte Data_XOUT_H = (byte) 0x03;
	public static final byte Data_XOUT_L = (byte) 0x04;
	public static final byte Data_YOUT_H = (byte) 0x05;
	public static final byte Data_YOUT_L = (byte) 0x06;
	public static final byte Data_ZOUT_H = (byte) 0x07;
	public static final byte Data_ZOUT_L = (byte) 0x08;
	public static final byte Status_Register =   (byte) 0x09; // Status Register
	public static final byte IRA = (byte) 0x0A;   // Identification Register A
	public static final byte IRB = (byte) 0x0B;   // Identification Register B	 
	public static final byte IRC = (byte) 0x0C;   // Identification Register C
	
	public static final byte Continue_Measure = (byte) 0x00;
	public static final byte IRA_default = (byte) 0x48;   // Identification Register A default value
	
    public static final byte ADDRESS_R = (byte) 0x1E; // Address for read
    public static final byte ADDRESS_W = (byte) 0x1E; // Address for write by MCU
		
	
	private static final int READ_BUFFER_SIZE  = 10; // bytes
	private static final int WRITE_BUFFER_SIZE = 10; // bytes
	
	public interface HMC5883LListener {
		public void onDeviceId(byte deviceId);
		public void onData(int Mag_x, int Mag_y, int Mag_z);
		public void onError(String message);
	}
	
	private HMC5883LListener listener;
	
	private byte deviceId;
	
	private int Mag_x;
	private int Mag_y;
	private int Mag_z;
	
	private int twiNum;
	private TwiMaster i2c;
	private Rate rate;
	
	private byte[] readBuffer  = new byte[READ_BUFFER_SIZE];
	private byte[] writeBuffer = new byte[WRITE_BUFFER_SIZE];

	public HMC5883L(int twiNum, Rate rate) {
		this.twiNum = twiNum;
		this.rate = rate;
	}
	
	public void setI2C(TwiMaster i2c) {
		this.i2c = i2c;
	}
	public HMC5883L setListener(HMC5883LListener listener) {
		this.listener = listener;
		return this;
	}
	
	public byte readDeviceId() throws ConnectionLostException, InterruptedException {
		read(IRA, 1, readBuffer);
		return readBuffer[0];
	}
	
	private void setupDevice() throws InterruptedException, ConnectionLostException {
		
		byte id = readDeviceId();

		Thread.sleep(REGISTER_WRITE_DELAY);
		
		if (id == IRA_default) {
			deviceId = id;
		} else {
			onError("Invalid device ID, expected " + (IRA_default & 0xFF) + " but got " + (id & 0xFF));
		}
		
		if (listener != null) {
			listener.onDeviceId(deviceId);
		}

		write(CRA, (byte) 0x70); // 8-average, 15 Hz default, normal measurement)
		write(CRB, (byte) 0x40); // Gain=3, Sensor Field Range=±1.9 Ga

		write(Mode_Register, Continue_Measure);// Continuous-measurement mode
	}
	
	protected void write(byte register, byte value) throws ConnectionLostException, InterruptedException {
		writeBuffer[0] = register;
		writeBuffer[1] = value;
		flush(2);
	}
	
	protected void write(byte register, byte[] values) throws ConnectionLostException, InterruptedException {
		writeBuffer[0] = register;
		System.arraycopy(values, 0, writeBuffer, 1, values.length);
		flush(1 + values.length);
	}
	
	/**
	 * Writes the write buffer to the SPI.
	 * 
	 * @param length Number of bytes of the buffer to write.
	 * @throws ConnectionLostException
	 * @throws InterruptedException
	 */
	protected void flush(int length) throws ConnectionLostException, InterruptedException {
		boolean tenBitAddr = false;
		int readSize = 0;
		i2c.writeRead(ADDRESS_W, tenBitAddr, writeBuffer, length, readBuffer, readSize);
		
		if (REGISTER_WRITE_DELAY > 0)
			Thread.sleep(REGISTER_WRITE_DELAY);
	}
	
	protected void read(byte register, int length, byte[] values) throws ConnectionLostException, InterruptedException {
		boolean tenBitAddr = false;
		writeBuffer[0] = register;
		
		i2c.writeRead(ADDRESS_R, tenBitAddr, writeBuffer, 1, readBuffer, length);
	}
	
	private void onError(String message) {
		if (listener != null) {
			listener.onError(message);
		}
	}
	
	/*
	 * IOIOLooper interface methods.
	 */

	@Override
	public void setup(IOIO ioio) throws ConnectionLostException, InterruptedException {
		//i2c = ioio.openTwiMaster(twiNum, rate, false /* smbus */);
		setupDevice();
	}

	@Override
	public void loop() throws ConnectionLostException, InterruptedException {
		if (listener != null) {
								
			read(Data_XOUT_H, 6, readBuffer);
			
			Mag_x = (readBuffer[0] << 8) | readBuffer[1];
			Mag_y = (readBuffer[2] << 8) | readBuffer[3];
			Mag_z = (readBuffer[4] << 8) | readBuffer[5];
	
			listener.onData(Mag_x, Mag_y, Mag_z);
			
			//System.out.println("HMC5883L read data  end");
		}
	}

	@Override
	public void disconnected() {
		// TODO Auto-generated method stub
	}

	@Override
	public void incompatible() {
		// TODO Auto-generated method stub
	}
	
}
