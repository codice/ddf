/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.metrics.reporting.internal.rrd4j;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;
import org.rrd4j.core.FetchData;
import org.rrd4j.core.FetchRequest;
import org.rrd4j.core.RrdDb;
import org.rrd4j.graph.RrdGraph;
import org.rrd4j.graph.RrdGraphDef;

import ddf.metrics.reporting.internal.MetricsGraphException;

public class RrdDumper {
	
	private static final String months[] = { 
        "Jan", "Feb", "Mar", "Apr", 
        "May", "Jun", "Jul", "Aug", 
        "Sep", "Oct", "Nov", "Dec"};
	
	private static final double METRICS_MAX_THRESHOLD = 4000000000.0; 
	
	
	public static void main(String[] args) throws Exception {
		
		//String rrdFilename = args[0];
		String[] rrdFilenames = new String[]
		{
			//"C:/DDF/rrd_SAVE/spikes/catalogExceptions.rrd",
			"C:/DDF/rrd_SAVE/spikes/sourceDib30rhel58Queries.rrd"
		};
		//String rrdFilename = "C:/workspaces/dib_git/distribution/dib/target/dib-4.0.2.RC7-SNAPSHOT/data/metrics/sourceDib30rhel58Queries.rrd";

		for (String rrdFilename : rrdFilenames) {
	        RrdDb rrdDb = new RrdDb(rrdFilename, true);
//	        long endTime = System.currentTimeMillis()/1000;
//	        long duration = TimeUnit.SECONDS.convert(24L, TimeUnit.DAYS);
//	        long startTime = endTime - duration;
	        
	        Calendar cal = Calendar.getInstance();
	        cal.set(2013, 7, 21, 15, 40);
	        long startTime = cal.getTimeInMillis()/1000;
	        cal.set(2013, 7, 22, 8, 0);
	        long endTime = cal.getTimeInMillis()/1000;
	        
	        System.out.println("\n\n>>>>>>>>>>>>>>>>>>>  RRD File:  " + rrdFilename + "  <<<<<<<<<<<<<<<<<<<<<<<<<<<<<\n\n");
	        dumpData(ConsolFun.TOTAL, "TOTAL", rrdDb, "COUNTER", startTime, endTime);
	        
	        displayGraph("Metric Name", rrdFilename, startTime, endTime, "Y-Axis Label", "Graph Title");
	        
//	        MetricsRetriever metricsRetriever = new RrdMetricsRetriever();
//	        OutputStream os = metricsRetriever.createXlsData("Metric Name", rrdFilename, startTime, endTime);
//	        FileOutputStream fos = new FileOutputStream("C:/DDF/rrd_SAVE/spikes/")
		}
	}
	 
	 static private void dumpData(ConsolFun consolFun, String dataType, RrdDb rrdDb, String dsType, 
	     long startTime, long endTime) throws Exception
	 {
		 FetchRequest fetchRequest = rrdDb.createFetchRequest(consolFun, startTime, endTime);
         FetchData fetchData = fetchRequest.fetchData();
         System.out.println("************  " + dsType + ": " + dataType + "  **************");
//		        System.out.println(fetchData.dump());
         
         int rrdStep = 60;  // in seconds
         long[] timestamps = fetchData.getTimestamps();
         double[] values = fetchData.getValues(0);
         double[] adjustedValues = new double[values.length];
         for (int i=0; i < values.length; i++)
         {
        	 double adjustedValue = values[i] * rrdStep;
        	 adjustedValues[i] = adjustedValue;

         	 System.out.println(getCalendarTime(timestamps[i]) + ":  " + values[i] + 
         			 "   (adjusted value = " + adjustedValue + 
         			 ",   floor = " + Math.floor(adjustedValue) +
         			 ",   round = " + Math.round(adjustedValue) +
         			 ")");
         }
         
         System.out.println("adjustedValues.length = " + adjustedValues.length);

         for (int i=0; i < adjustedValues.length; i++)
         {
        	 //System.out.println("adjustedValue[" + i + "] = " + adjustedValues[i]);
        	 if (adjustedValues[i] > METRICS_MAX_THRESHOLD) {
        		 System.out.println("Value [" + adjustedValues[i] + "] is an OUTLIER");
        	 }
         }
	 }
    
    static private String getCalendarTime(long timestamp)
    {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp * 1000);
        
        String calTime = months[calendar.get(Calendar.MONTH)] +
            " " + calendar.get(Calendar.DATE) + " " + 
            calendar.get(Calendar.YEAR) + " ";
        
        calTime += addLeadingZero(calendar.get(Calendar.HOUR_OF_DAY)) + ":";
        calTime += addLeadingZero(calendar.get(Calendar.MINUTE)) + ":";
        calTime += addLeadingZero(calendar.get(Calendar.SECOND)); 
        
        return calTime;
    }
    
    
    static private String addLeadingZero(int value)
    {
        if (value < 10)
        {
            return "0" + String.valueOf(value);
        }       
        
        return String.valueOf(value);
    }
    
    static public void displayGraph(String metricName, String rrdFilename, long startTime, long endTime, String verticalAxisLabel, String title) 
            throws IOException, MetricsGraphException
    {
        // Create RRD DB in read-only mode for the specified RRD file
        RrdDb rrdDb = new RrdDb(rrdFilename, true);
        
        // Extract the data source (should always only be one data source per RRD file - otherwise we have a problem)
        if (rrdDb.getDsCount() != 1)
        {
            throw new MetricsGraphException("Only one data source per RRD file is supported - RRD file " + 
                rrdFilename + " has " + rrdDb.getDsCount() + " data sources.");
        }

        // Define attributes of the graph to be created for this metric
        RrdGraphDef graphDef = new RrdGraphDef();
        graphDef.setTimeSpan(startTime, endTime);
        graphDef.setImageFormat("PNG");
        graphDef.setShowSignature(false);
        graphDef.setStep(60);
        graphDef.setVerticalLabel(verticalAxisLabel);
        graphDef.setHeight(500);
        graphDef.setWidth(1000);
        graphDef.setTitle(title);

        DsType dataSourceType = rrdDb.getDatasource(0).getType();
        
        // Determine if the Data Source for this RRD file is a COUNTER or GAUGE
        // (Need to know this because COUNTER data is averaged across samples and the vertical axis of the
        // generated graph by default will show data per rrdStep interval)
        if (dataSourceType == DsType.COUNTER)
        {    
            long rrdStep = rrdDb.getRrdDef().getStep();
          
            // Multiplied by the rrdStep to "undo" the automatic averaging that RRD does
            // when it collects TOTAL data - we want the actual totals for the step, not
            // the average of the totals.
            graphDef.datasource("myTotal", rrdFilename, "data", ConsolFun.TOTAL);
            graphDef.datasource("realTotal", "myTotal," + rrdStep + ",*");
            graphDef.datasource("validTotal", "realTotal," + METRICS_MAX_THRESHOLD + ",GT,UNKN,realTotal,IF");
            graphDef.line("validTotal", Color.BLUE, convertCamelCase(metricName), 2);

            // Add some spacing between the graph and the summary stats shown beneath the graph
            graphDef.comment("\\s");
            graphDef.comment("\\s");
            graphDef.comment("\\c");
            
            // Average, Min, and Max over all of the TOTAL data - displayed at bottom of the graph
            graphDef.gprint("validTotal", ConsolFun.AVERAGE, "Average = %.3f%s");  
            graphDef.gprint("validTotal", ConsolFun.MIN, "Min = %.3f%s");
            graphDef.gprint("validTotal", ConsolFun.MAX, "Max = %.3f%s");
        }
        else if (dataSourceType == DsType.GAUGE)
        {
            graphDef.datasource("myAverage", rrdFilename, "data", ConsolFun.AVERAGE);
            graphDef.line("myAverage", Color.RED, convertCamelCase(metricName), 2);
            
            // Add some spacing between the graph and the summary stats shown beneath the graph
            graphDef.comment("\\s");
            graphDef.comment("\\s");
            graphDef.comment("\\c");
            
            // Average, Min, and Max over all of the AVERAGE data - displayed at bottom of the graph
            graphDef.gprint("myAverage", ConsolFun.AVERAGE, "Average = %.3f%s");
            graphDef.gprint("myAverage", ConsolFun.MIN, "Min = %.3f%s");
            graphDef.gprint("myAverage", ConsolFun.MAX, "Max = %.3f%s");
        }
        else
        {
            rrdDb.close();
            throw new MetricsGraphException("Unsupported data source type " + dataSourceType.name() + " in RRD file " + 
                rrdFilename + ", only COUNTER and GAUGE data source types supported.");
        }
        
        rrdDb.close();
                
        // Use "-" as filename so that RRD creates the graph only in memory (no file is
        // created, hence no file locking problems due to race conditions between multiple clients)
        graphDef.setFilename("graph.gif");
        RrdGraph graph = new RrdGraph(graphDef); 
        BufferedImage bi = new BufferedImage(100,100,BufferedImage.TYPE_INT_RGB);
        graph.render(bi.getGraphics());
        //return graph.getRrdGraphInfo().getBytes();
    }
    
    static public String convertCamelCase(String input)
    { 
        String[] parts = StringUtils.splitByCharacterTypeCamelCase(input);
        String convertedStr = StringUtils.join(parts, " ");
        convertedStr = WordUtils.capitalizeFully(convertedStr).trim();
        
        return convertedStr;
    }
}
