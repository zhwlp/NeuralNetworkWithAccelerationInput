package com.traviswyatt.ioio.Adxl345Hmc5883lANN;


import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.TwiMaster;
import ioio.lib.api.Uart;
import ioio.lib.api.Uart.Parity;
import ioio.lib.api.Uart.StopBits;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.encog.neural.networks.BasicNetwork;

import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.traviswyatt.ioio.adxl345_itg3205_DataSaving.R;

public class MainActivity extends IOIOActivity {

	private TextView ioioStatusText;
	private TextView deviceIdText;
	private TextView Acc_xAxisText;
	private TextView Acc_yAxisText;
	private TextView Acc_zAxisText;
	private TextView Acc_magnitudeText;

	private TextView Mag_xAxisText;
	private TextView Mag_yAxisText;
	private TextView Mag_zAxisText;
	
	// Record start
	TextView txtMessage;
	Button btnStart;
	Button btnStop;
	TextRecorder recorder;
	Thread writeThread;
	boolean started;

	//Artifical Neural Network and Xbee
	private DigitalOutput led_;
	Uart uart;
	OutputStream XbeeOut;
	public InputStream Xbeein;
	BasicNetwork network= (BasicNetwork) NeuralNetwork.createNetwork();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		ioioStatusText = (TextView) findViewById(R.id.ioio_status);
		deviceIdText = (TextView) findViewById(R.id.device_id);
		Acc_xAxisText = (TextView) findViewById(R.id.x_axis);
		Acc_yAxisText = (TextView) findViewById(R.id.y_axis);
		Acc_zAxisText = (TextView) findViewById(R.id.z_axis);
		
		Acc_magnitudeText = (TextView) findViewById(R.id.magnitude);

		
		Mag_xAxisText = (TextView) findViewById(R.id.Mag_x_axis);
		Mag_yAxisText = (TextView) findViewById(R.id.Mag_y_axis);
		Mag_zAxisText = (TextView) findViewById(R.id.Mag_z_axis);
		
		// record start
        txtMessage = (TextView)this.findViewById(R.id.textViewMessage);
        btnStart = (Button)this.findViewById(R.id.buttonStart);
        btnStop = (Button)this.findViewById(R.id.buttonStop);
        
        final MainActivity activity = this;
        
        btnStart.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				activity.start();
				
				try {
					led_.write(false);
					XbeeControl Send = new XbeeControl(XbeeOut);
					//Send.SendHighD1();		// Stop the SSR	 
					Send.SendLowD2();
					
				} catch (ConnectionLostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		});
        
        btnStop.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				activity.stop();
				try {
					led_.write(true);
					XbeeControl Send = new XbeeControl(XbeeOut);
					//Send.SendLowD1();     // Start the SSR	
					Send.SendHighD2();
				} catch (ConnectionLostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
        
        recorder = new TextRecorder(this.getApplicationContext(), "/storage/sdcard0/SSH"); //"/storage/sdcard"
	}  // record end
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	/**
	 * A method to create our IOIO thread.
	 * 
	 * @see ioio.lib.util.AbstractIOIOActivity#createIOIOThread()
	 */
	

	
	@Override
	protected IOIOLooper createIOIOLooper() {
		int twiNum = 1; // IOIO pin 1 = SDA, pin 2 = SCL
		final Hybrid hybrid = new Hybrid(twiNum, TwiMaster.Rate.RATE_400KHz);
		final int Sample=40;
		final double[] XRaw=new double[Sample];
		final double[] YRaw=new double[Sample];
		final double[] ZRaw=new double[Sample];
		final double[] MagXRaw=new double[Sample];
		final double[] MagYRaw=new double[Sample];
		final double[] MagZRaw=new double[Sample];
		
		hybrid.setListener(new Hybrid.HybridListener() {
			
			int Ndata=0;
			@Override
			public void onDeviceId(byte deviceId) {
				updateTextView(deviceIdText, "Device ID: " + (int) (deviceId & 0xFF));
			}
			
			@Override
			public void onData(float acc_x, float acc_y, float acc_z, double acc_magnitude, float Mag_x, float Mag_y, float Mag_z) {
								//float gyro_x, float gyro_y, float gyro_z,	float gyro_temperature,
				updateTextView(Acc_xAxisText, "Acc_X = " + acc_x);
				updateTextView(Acc_yAxisText, "Acc_Y = " + acc_y);
				updateTextView(Acc_zAxisText, "Acc_Z = " + acc_z);
				updateTextView(Acc_magnitudeText, "Magnitude = " + acc_magnitude);
				
/*				updateTextView(Gyro_xAxisText, "Gyro_X = " + gyro_x + " deg/s");
				updateTextView(Gyro_yAxisText, "Gyro_Y = " + gyro_y + " deg/s");
				updateTextView(Gyro_zAxisText, "Gyro_Z = " + gyro_z + " deg/s");
				updateTextView(Gyro_temperatureText, "Temperature = " + gyro_temperature + " C");*/
				
				updateTextView(Mag_xAxisText, "Mag_X = " + Mag_x+" mG");
				updateTextView(Mag_yAxisText, "Mag_Y = " + Mag_y+" mG");
				updateTextView(Mag_zAxisText, "Mag_Z = " + Mag_z+" mG");
				
				//Artificial Neural Network Computation
				
				//Store data to Array
				if (Ndata<Sample){
					XRaw[Ndata]=acc_x;
					YRaw[Ndata]=acc_y;
					ZRaw[Ndata]=acc_z;
					MagXRaw[Ndata]=Mag_x;
					MagYRaw[Ndata]=Mag_y;
					MagZRaw[Ndata]=Mag_z;
					Ndata+=1;
					
				} else if (Ndata==Sample){
		            double[] NeuralNetworkInput=new double[6];
		            double[] NeuralNetworkOutput = new double[3];
		            
					Statistics AccXtemp = new Statistics(XRaw);
					Statistics AccYtemp = new Statistics(YRaw);
					Statistics AccZtemp = new Statistics(ZRaw);
					Statistics MagXtemp = new Statistics(MagXRaw);
					Statistics MagYtemp = new Statistics(MagYRaw);
					Statistics MagZtemp = new Statistics(MagZRaw);
					
					
					AccXtemp.LPF();
					AccYtemp.LPF();
					AccZtemp.LPF();
					MagXtemp.LPF();
					MagYtemp.LPF();
					MagZtemp.LPF();
					
					//for Mag mean + acceleration variance input 6
					NeuralNetworkInput[0]=MagXtemp.getMean();
					NeuralNetworkInput[1]=MagYtemp.getMean();
					NeuralNetworkInput[2]=MagZtemp.getMean();
					NeuralNetworkInput[3]=AccXtemp.getVariance();
					NeuralNetworkInput[4]=AccYtemp.getVariance();
					NeuralNetworkInput[5]=AccZtemp.getVariance();
					
					
/*			          // For Acc+Mag input
					NeuralNetworkInput[0]=AccXtemp.getMean();
					NeuralNetworkInput[1]=AccYtemp.getMean();
					NeuralNetworkInput[2]=AccZtemp.getMean();
					NeuralNetworkInput[3]=MagXtemp.getMean();
					NeuralNetworkInput[4]=MagYtemp.getMean();
					NeuralNetworkInput[5]=MagZtemp.getMean();				
					NeuralNetworkInput[6]=AccXtemp.getVariance();
					NeuralNetworkInput[7]=AccYtemp.getVariance();
					NeuralNetworkInput[8]=AccZtemp.getVariance();*/
					
					
/*					//	For Variance Input only  
					NeuralNetworkInput[0]=AccXtemp.getVariance();
		            NeuralNetworkInput[1]=AccYtemp.getVariance();
		            NeuralNetworkInput[2]=AccZtemp.getVariance();*/

		            //	For non-normalized weights        
		          /*input[0]=AccXtemp.getMean();
		            input[1]=AccYtemp.getMean();
		            input[2]=AccZtemp.getMean();
		            input[3]=AccXtemp.getVariance();
		            input[4]=AccYtemp.getVariance();
		            input[5]=AccZtemp.getVariance();*/
		            
		            // For normalized weights		
		          /*input[0]=AccXtemp.getNormalizedMean();
		            input[1]=AccYtemp.getNormalizedMean();
		            input[2]=AccZtemp.getNormalizedMean();
		            input[3]=AccXtemp.getNormalizedVariance();
		            input[4]=AccYtemp.getNormalizedVariance();
		            input[5]=AccZtemp.getNormalizedVariance();*/
		            
					network.compute(NeuralNetworkInput, NeuralNetworkOutput);	
					
					
					HeadMotionRecognition result= new HeadMotionRecognition(NeuralNetworkOutput);
					result.RecognitionResult();				
		            
		            Ndata=0;
				}	
					
				Date date = new Date();
				String now = new SimpleDateFormat("HHmmss.SSS").format(date);
				
				//System.out.println("data save start");
				
				StringBuilder builder = new StringBuilder();
				builder.append(now)
					.append(",")
					.append(acc_x)
					.append(",")
					.append(acc_y)
					.append(",")
					.append(acc_z)
					.append(",")
					.append(acc_magnitude)
/*					.append(",")
					.append(gyro_x)
					.append(",")
					.append(gyro_y)
					.append(",")
					.append(gyro_z)
					.append(",")
					.append(gyro_temperature)*/
				    .append(",")
				    .append(Mag_x)
			    	.append(",")
			     	.append(Mag_y)
				    .append(",")
				    .append(Mag_z);
				
				recorder.writeLine(builder.toString());
				
				//System.out.println("data save end");
			}
		});
				
		return new DeviceLooper(hybrid);
	}
	

	
	public MainActivity() {
		this.started = false;

	}

    
    private void start() {
    	if (started) {
    		return;
    	}
    	
    	txtMessage.setText("Started");
    	recorder.start();
    	started = true;
    	recorder.writeLine("Time,Acc_X,Acc_Y,Acc_Z,Magnitude,Mag_x, Mag_y,Mag_z"); //Gyro_X,Gyro_Y,Gyro_Z,Temperature,
    }
    
    private void stop() {
    	if (!started) {
    		return;
    	}
    	
    	started = false;
		recorder.stop();
    	txtMessage.setText("Stopped");
    }
	
    // record data end
	

	private void updateTextView(final TextView textView, final String text) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				textView.setText(text);
			}
		});
	}
	
	/**
	 * This is the thread on which all the IOIO activity happens. It will be run
	 * every time the application is resumed and aborted when it is paused. The
	 * method setup() will be called right after a connection with the IOIO has
	 * been established (which might happen several times!). Then, loop() will
	 * be called repetitively until the IOIO gets disconnected.
	 */
	class DeviceLooper implements IOIOLooper {
		
		/**
		 * Duration to sleep after each loop.
		 */
		private static final long THREAD_SLEEP = 1L; // milliseconds
		
		private IOIOLooper device;

		public DeviceLooper(IOIOLooper device) {
			this.device = device;
		}
		
		@Override
		public void setup(IOIO ioio) throws ConnectionLostException, InterruptedException {
			device.setup(ioio);
			led_ = ioio.openDigitalOutput(0, true);
			uart = ioio.openUart(4, 3, 9600, Parity.NONE, StopBits.ONE);
			XbeeOut= uart.getOutputStream();
			Xbeein=uart.getInputStream();
			updateTextView(ioioStatusText, "IOIO Connected");
		}

		/**
		 * Called repetitively while the IOIO is connected.
		 * 
		 * @throws ConnectionLostException
		 *             When IOIO connection is lost.
		 * @throws InterruptedException 
		 * 
		 * @see ioio.lib.util.AbstractIOIOActivity.IOIOThread#loop()
		 */
		@Override
		public void loop() throws ConnectionLostException, InterruptedException {
			device.loop();
			Thread.sleep(THREAD_SLEEP);
		}

		@Override
		public void disconnected() {
			device.disconnected();
			updateTextView(ioioStatusText, "IOIO Disconnected");
		}

		@Override
		public void incompatible() {
			device.incompatible();
			updateTextView(ioioStatusText, "IOIO Incompatible");
		}
	}
  
	
}
