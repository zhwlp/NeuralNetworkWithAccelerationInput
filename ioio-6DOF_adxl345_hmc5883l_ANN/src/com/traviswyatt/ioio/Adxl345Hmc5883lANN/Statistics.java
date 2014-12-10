package com.traviswyatt.ioio.Adxl345Hmc5883lANN;

import java.util.Arrays;

public class Statistics {
	
    double[] data;
    double size;    

    public Statistics(double[] data) 
    {
        this.data = data;
        size = data.length;
    }   

    double getNormalizedMean()
    {
    	double MaxAbs = getMaxAbsValue();
    	double sum = 0.0;
        for(double a : data)
            sum += a/MaxAbs;
        return sum/size;
    }
    
    
    double getMean()
    {
        double sum = 0.0;
        for(double a : data)
            sum += a;
        return sum/size;
    }

    double getNormalizedVariance()
    {
        double mean = getNormalizedMean();
        double MaxAbs = getMaxAbsValue();
        double temp = 0;
        for(double a :data)
            temp += (mean-a/MaxAbs)*(mean-a/MaxAbs);
        return temp/(size-1);
    }
    
    double getVariance()
    {
        double mean = getMean();
        double temp = 0;
        for(double a :data)
            temp += (mean-a)*(mean-a);
        return temp/(size-1);
    }

    double getStdDev()
    {
        return Math.sqrt(getVariance());
    }
    
    public double[] LPF(){  
        double[] LPFValue = new double[data.length];  
        double alpha = 0.7;
        LPFValue[0]=data[0];
        
        for(int i=1;i < data.length;i++){  
        	LPFValue[i]=alpha*LPFValue[i-1]+(1-alpha)*data[i];
        }  
        return LPFValue;  
     } 
    
    public double getMaxValue(){  
        double maxValue = data[0];  
        for(int i=1;i < data.length;i++){  
        if(data[i] > maxValue){  
             maxValue = data[i];  
           }  
        }  
        return maxValue;  
     } 
    
    public double getMaxAbsValue(){  
        double maxAbsValue = Math.abs(data[0]);  
        for(int i=1;i < data.length;i++){  
        if(Math.abs(data[i]) > maxAbsValue){  
        	maxAbsValue = Math.abs(data[i]);  
           }  
        }  
        return maxAbsValue;  
     } 
    
    public int getMaxIndex(){  
        double maxValue = data[0];
        int index=0;
        for(int i=1;i < data.length;i++){  
        if(data[i] > maxValue){  
             maxValue = data[i];
             index=i;
           }  
        }  
        return index;  
     } 
    
    public double median() 
    {
       double[] b = new double[data.length];
       System.arraycopy(data, 0, b, 0, b.length);
       Arrays.sort(b);

       if (data.length % 2 == 0) 
       {
          return (b[(b.length / 2) - 1] + b[b.length / 2]) / 2.0;
       } 
       else 
       {
          return b[b.length / 2];
       }
    }
}