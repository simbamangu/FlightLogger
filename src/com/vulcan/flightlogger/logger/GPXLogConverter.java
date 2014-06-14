package com.vulcan.flightlogger.logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import android.location.Location;

public class GPXLogConverter {
	
	public final String GPX_HEADER = new StringBuilder()
	.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?> ")
	.append("<gpx version=\"1.0\" ")
	.append("creator=\"Vulcan FlightLogger\" ")
	.append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ")
	.append("xmlns=\"http://www.topografix.com/GPX/1/0\" ")
	.append("xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd\"> ")
	.toString();

	public final String GPX_FOOTER = "</gpx>";
	
	void writeGPXFile(InputStream is, OutputStream os) throws IOException
	{
	    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
	    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));
	    writer.write(GPX_HEADER);
	    try {
	        String line;
	        while ((line = reader.readLine()) != null) {
	             String[] csvData = line.split(",");
	             writer.write(writeGPSTrackRecord(csvData));
	        }
	    }
	    finally {
	        try {
	        	writer.write(GPX_FOOTER);
		    	writer.flush();
		    	writer.close();
		    	reader.close();
	            is.close();
	            os.close();
	        }
	        catch (IOException e) {
	            // handle exception
	        }
	    }		
	}
	
	public String writeGPSTrackRecord(String[] csvVal)
	{
		StringBuilder builder = new StringBuilder();
		builder.append("<trkpt lat=").append(csvVal[1]).append(" lon=").append(csvVal[2]).append(">");
		builder.append("<ele>").append(csvVal[3]).append("</ele>");
		builder.append("<speed>").append(csvVal[4]).append("</speed>");
		builder.append("<time>").append(csvVal[0]).append("</time>");
		builder.append("</trkpt>");
		return builder.toString();
	}

}