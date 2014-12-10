package com.traviswyatt.ioio.Adxl345Hmc5883lANN;

import ioio.lib.api.IOIO;
import ioio.lib.api.TwiMaster;
import ioio.lib.api.TwiMaster.Rate;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.IOIOLooper;

public class ADXL345 implements IOIOLooper {
	
	/**
	 * Duration to sleep thread after a register write.
	 */
	public static final long REGISTER_WRITE_DELAY = 200L;
	
	/**
	 * Bit structure:
	 * | Read/Write | Multibyte | A5 | A4 | A3 | A2 | A1 | A0 |
	 */
    //	public static final byte ADXL345_SPI_WRITE  = (byte) 0x00; // 0000 0000
    //	public static final byte ADXL345_SPI_READ   = (byte) 0x80; // 1000 0000
	//	public static final byte ADXL345_MULTI_BYTE = (byte) 0x40; // 0100 0000
	
    public static final byte Acc_ADDRESS = (byte) 0x53; // ALT = 0 (GND),add Acc_
//	public static final byte ADDRESS = (byte) 0x1D; // ALT = 1 (VDD)
	
	/**
	 * Register map per datasheet.
	 * 
	 * http://www.analog.com/static/imported-files/data_sheets/ADXL345.pdf
	 */
	public static final byte DEVID          = (byte) 0x00; // Device ID
	public static final byte THRESH_TAP     = (byte) 0x1D; // Tap threshold
	public static final byte OFFSX          = (byte) 0x1E; // X-axis offset
	public static final byte OFFSY          = (byte) 0x1F; // Y-axis offset
	public static final byte OFFSZ          = (byte) 0x20; // Z-axis offset
	public static final byte DUR            = (byte) 0x21; // Tap duration
	public static final byte Latent         = (byte) 0x22; // Tap latency
	public static final byte Window         = (byte) 0x23; // Tap window
	public static final byte THRESH_ACT     = (byte) 0x24; // Activity threshold
	public static final byte THRESH_INACT   = (byte) 0x25; // Inactivity threshold
	public static final byte TIME_INACT     = (byte) 0x26; // Inactivity time
	public static final byte ACT_INACT_CTL  = (byte) 0x27; // Axis enable control for activity and inactivity detection
	public static final byte THRESH_FF      = (byte) 0x28; // Free-fall threshold
	public static final byte TIME_FF        = (byte) 0x29; // Free-fall time
	public static final byte TAP_AXES       = (byte) 0x2A; // Axis control for single tap/double tap
	public static final byte ACT_TAP_STATUS = (byte) 0x2B; // Source of single tap/double tap
	public static final byte BW_RATE        = (byte) 0x2C; // Data rate and power mode control
	public static final byte POWER_CTL      = (byte) 0x2D; // Power-saving features control
	public static final byte INT_ENABLE     = (byte) 0x2E; // Interrupt enable control
	public static final byte INT_MAP        = (byte) 0x2F; // Interrupt mapping control
	public static final byte INT_SOURCE     = (byte) 0x30; // Source of interrupts
	public static final byte DATA_FORMAT    = (byte) 0x31; // Data format control
	public static final byte DATAX0         = (byte) 0x32; // X-Axis Data 0
	public static final byte DATAX1         = (byte) 0x33; // X-Axis Data 1
	public static final byte DATAY0         = (byte) 0x34; // Y-Axis Data 0
	public static final byte DATAY1         = (byte) 0x35; // Y-Axis Data 1
	public static final byte DATAZ0         = (byte) 0x36; // Z-Axis Data 0
	public static final byte DATAZ1         = (byte) 0x37; // Z-Axis Data 1
	public static final byte FIFO_CTL       = (byte) 0x38; // FIFO control
	public static final byte FIFO_STATUS    = (byte) 0x39; // FIFO status
	
	public static final byte DEFAULT_RESET_VALUE    = (byte) 0x00;
	public static final byte DEVID_RESET_VALUE      = (byte) 0xE5;
	public static final byte BW_RATE_RESET_VALUE    = (byte) 0x0A;
	public static final byte INT_SOURCE_RESET_VALUE = (byte) 0x02;
	
	/**
	 * Register 0x2D-POWER_CTL (Read/Write)
	 * 
	 * | D7 | D6 |  D5  |     D4     |   D3    |   D2  | D1 | D0 |
	 * |  0 |  0 | Link | AUTO_SLEEP | Measure | Sleep |  Wakeup |
	 */
	public static final byte POWER_CTL_Measure = (byte) 0x08;
	
	/**
	 * Register 0x31-DATA_FORMAT (Read/Write)
	 * 
	 * |     D7    |  D6 |     D5     | D4 |    D3    |    D2   | D1 | D0 |
	 * | SELF_TEST | SPI | INT_INVERT |  0 | FULL_RES | Justify |  Range  |
	 */

	
	public static final byte DATA_FORMAT_Range_2G  = (byte) 0x00; // +/- 2 g
	public static final byte DATA_FORMAT_Range_4G  = (byte) 0x01; // +/- 4 g
	public static final byte DATA_FORMAT_Range_8G  = (byte) 0x02; // +/- 8 g
	public static final byte DATA_FORMAT_Range_16G = (byte) 0x03; // +/- 16 g
	
	private static final int READ_BUFFER_SIZE  = 10; // bytes
	private static final int WRITE_BUFFER_SIZE = 10; // bytes
	
	public interface ADXL345Listener {
		public void onDeviceId(byte deviceId);
		public void onData(int Acc_x, int Acc_y, int Acc_z);
		public void onError(String message);
	}
	
	private ADXL345Listener listener;
	
	private byte deviceId;
	private float multiplier = 2f * 2f / 1024f; // default is for +/- 2G range
	
	private int Acc_x;
	private int Acc_y;
	private int Acc_z;
	
	private int twiNum;
	private TwiMaster i2c;
	private Rate rate;
	
	private byte[] readBuffer  = new byte[READ_BUFFER_SIZE];
	private byte[] writeBuffer = new byte[WRITE_BUFFER_SIZE];

	public ADXL345(int twiNum, Rate rate) {
		this.twiNum = twiNum;
		this.rate = rate;
	}
	
	public ADXL345 setListener(ADXL345Listener listener) {
		this.listener = listener;
		return this;
	}
	
	public void setI2C(TwiMaster i2c) {
		this.i2c = i2c;
	}

	/**
	 * Returns the multiplier that should be used to convert data values to Gs.
	 * 
	 * @return
	 */
	public float getMultiplier() {
		return multiplier;
	}
	
	/**
	 * Sets the G range.
	 * Supported ranges are: +/- 2 G, +/- 4 G, +/- 8 G, +/- 16 G
	 * 
	 * @param range G range of either 2, 4, 8, or 16.
	 * @throws InterruptedException 
	 * @throws ConnectionLostException 
	 */
	public void setRange(int range) throws ConnectionLostException, InterruptedException {
		byte value;
		switch (range) {
		case 2: // +/- 2 G
			value = DATA_FORMAT_Range_2G;
			break;
		case 4: // +/- 4 G
			value = DATA_FORMAT_Range_4G;
			break;
		case 8: // +/- 8 G
			value = DATA_FORMAT_Range_8G;
			break;
		case 16: // +/- 16 G
			value = DATA_FORMAT_Range_16G;
			break;
		default:
			onError("Unsupported G range: " + range);
			return;
		}
		
		write(DATA_FORMAT, value);
		
		// Gs = Measurement Value * (G-range / 2^10)
		multiplier = (float) range * 2f / 1024f;
	}
	
	public byte getDeviceId() throws ConnectionLostException, InterruptedException {
		read(DEVID, 1, readBuffer);
		return readBuffer[0];
	}
	
	private void setupDevice() throws InterruptedException, ConnectionLostException {
		byte id = getDeviceId();
		Thread.sleep(REGISTER_WRITE_DELAY);
		
		if (id == DEVID_RESET_VALUE) {
			deviceId = id;
		} else {
			onError("Invalid device ID, expected " + (DEVID_RESET_VALUE & 0xFF) + " but got " + (id & 0xFF));
		}
		
		if (listener != null) {
			listener.onDeviceId(deviceId);
		}
		
		setRange(4); // +/- 4 G
		write(POWER_CTL, POWER_CTL_Measure);
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
		i2c.writeRead(Acc_ADDRESS, tenBitAddr, writeBuffer, length, readBuffer, readSize);
		
		if (REGISTER_WRITE_DELAY > 0)
			Thread.sleep(REGISTER_WRITE_DELAY);
	}
	
	protected void read(byte register, int length, byte[] values) throws ConnectionLostException, InterruptedException {
		boolean tenBitAddr = false;
		writeBuffer[0] = register;
		
		i2c.writeRead(Acc_ADDRESS, tenBitAddr, writeBuffer, 1, readBuffer, length);
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
	//	i2c = ioio.openTwiMaster(twiNum, rate, false /* smbus */);
		setupDevice();
	}

	@Override
	public void loop() throws ConnectionLostException, InterruptedException {
		if (listener != null) {
								
			read(DATAX0, 6, readBuffer);
			Acc_x = (readBuffer[1] << 8) | readBuffer[0];
			Acc_y = (readBuffer[3] << 8) | readBuffer[2];
			Acc_z = (readBuffer[5] << 8) | readBuffer[4];
			listener.onData(Acc_x, Acc_y, Acc_z);
			
			//System.out.println("ADXL345 read data end");
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
