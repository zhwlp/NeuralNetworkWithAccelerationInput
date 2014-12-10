package com.traviswyatt.ioio.Adxl345Hmc5883lANN;

public class HeadMotionRecognition {
	
    double[] data;
    double size;    

    public HeadMotionRecognition(double[] NeuralNetworkOutput) 
    {
        this.data = NeuralNetworkOutput;
        size = data.length;
    }   

    public void RecognitionResult(){
    	   
    Statistics StatisticsOfANNOut = new Statistics(data);
    
	System.out.println("what is the motion?");
	System.out.println(StatisticsOfANNOut.getMaxIndex());
	
	switch (StatisticsOfANNOut.getMaxIndex()) {
						
		case 0: 
			System.out.println("The head motion is idle");
			break;
		case 1: 
			System.out.println("The head motion is look away");
			break;
		case 2: 
			System.out.println("The head motion is Nodding off");
			break;
/*		case 3: 
			System.out.println("The head motion is No");
			break;
		case 4: 
			System.out.println("The head motion is turn left");
			break;
		case 5: 
			System.out.println("The head motion is turn right");
			break;
		case 6: 
			System.out.println("The head motion is Yes");
			break;
		case 7: 
			System.out.println("The head motion is Nodding off");
			break;	*/					
	 } 
    }
    
}
