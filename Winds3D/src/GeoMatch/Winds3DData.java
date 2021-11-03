package GeoMatch;

import ucar.ma2.Array;
import ucar.ma2.ArrayFloat;
import ucar.ma2.ArrayInt;
import ucar.ma2.ArrayShort;
import ucar.ma2.ArrayString;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

import java.awt.Point;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import com.javadocmd.simplelatlng.LatLng;
import com.javadocmd.simplelatlng.LatLngTool;
import com.javadocmd.simplelatlng.LatLngTool.Bearing;
import com.javadocmd.simplelatlng.util.LengthUnit;

import Util.TempFile;
import Util.colormap;

public class Winds3DData {

	Array x,y,z,u,v,w,deltaT;
	Dimension timeDim,xDim,yDim,zDim;
	float originLat, originLon, originAlt;
	double uValidMin,uValidMax,vValidMin,vValidMax,wValidMin,wValidMax,deltaTValidMin,deltaTValidMax;
	double uMissing, vMissing, wMissing,deltaTMissing;

	public double getuValidMin() {
		return uValidMin;
	}
	public double getuValidMax() {
		return uValidMax;
	}
	public double getvValidMin() {
		return vValidMin;
	}
	public double getvValidMax() {
		return vValidMax;
	}
	public double getwValidMin() {
		return wValidMin;
	}
	public double getwValidMax() {
		return wValidMax;
	}
	public double getdeltaTValidMin() {
		return deltaTValidMin;
	}
	public double getdeltaTValidMax() {
		return deltaTValidMax;
	}
	public double getuMissing() {
		return uMissing;
	}
	public double getvMissing() {
		return vMissing;
	}
	public double getwMissing() {
		return wMissing;
	}
	public double getdeltaTMissing() {
		return deltaTMissing;
	}

	public Array getX() {
		return x;
	}
	public void setX(Array x) {
		this.x = x;
	}
	public Array getY() {
		return y;
	}
	public void setY(Array y) {
		this.y = y;
	}
	public Array getZ() {
		return z;
	}
	public void setZ(Array z) {
		this.z = z;
	}
	public Array getU() {
		return u;
	}
	public void setU(Array u) {
		this.u = u;
	}
	public Array getV() {
		return v;
	}
	public void setV(Array v) {
		this.v = v;
	}
	public Array getW() {
		return w;
	}
	public void setW(Array w) {
		this.w = w;
	}
	public Array getdeltaT() {
		return deltaT;
	}
	public void setdeltaT(Array deltaT) {
		this.deltaT = deltaT;
	}

	public Dimension getTimeDim() {
		return timeDim;
	}
	public void setTimeDim(Dimension timeDim) {
		this.timeDim = timeDim;
	}
	public Dimension getxDim() {
		return xDim;
	}
	public void setxDim(Dimension xDim) {
		this.xDim = xDim;
	}
	public Dimension getyDim() {
		return yDim;
	}
	public void setyDim(Dimension yDim) {
		this.yDim = yDim;
	}
	public Dimension getzDim() {
		return zDim;
	}
	public void setzDim(Dimension zDim) {
		this.zDim = zDim;
	}
	public float getOriginLat() {
		return originLat;
	}
	public void setOriginLat(float originLat) {
		this.originLat = originLat;
	}
	public float getOriginLon() {
		return originLon;
	}
	public void setOriginLon(float originLon) {
		this.originLon = originLon;
	}
	public float getOriginAlt() {
		return originAlt;
	}
	public void setOriginAlt(float originAlt) {
		this.originAlt = originAlt;
	}
	
	public Winds3DData(String fn) throws IOException {
		String filename=fn;
		TempFile temp=null;
		NetcdfFile mFptr = null;
		try {
			if (filename.endsWith(".gz")) {
				temp = new TempFile(fn);
				filename = temp.getTempFilename();
//				System.out.println("temporary file: "+ filename);
				temp.unzip();
			}
			// TODO: add error check here
			mFptr = NetcdfFile.open(filename);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			throw new IOException(e.getMessage());			
		}

		
		
		Variable var = mFptr.findVariable("origin_latitude");
		if (var==null) {
			System.err.println("cannot find variable origin_latitude");
			throw new IOException();			
		}
		originLat = (float)var.readScalarDouble();
		System.out.println("origin_lat: " + originLat);
		
		var = mFptr.findVariable("origin_longitude");
		if (var==null) {
			System.err.println("cannot find variable origin_longitude");
			throw new IOException();			
		}
		originLon = (float)var.readScalarDouble();
		System.out.println("origin_longitude: " + originLon);

		var = mFptr.findVariable("origin_altitude");
		if (var==null) {
			System.err.println("cannot find variable origin_altitude");
			throw new IOException();			
		}
		originAlt = (float)var.readScalarDouble();
		System.out.println("origin_altitude: " + originAlt);

		// get dimensions
		
		// start here with arrays
		timeDim = mFptr.findDimension("time");
		xDim = mFptr.findDimension("x");
		yDim = mFptr.findDimension("y");
		zDim = mFptr.findDimension("z");
		
		var = mFptr.findVariable("x");
		if (var==null) {
			System.err.println("cannot find variable x");
			throw new IOException();			
		}
		x = var.read();
		var = mFptr.findVariable("y");
		if (var==null) {
			System.err.println("cannot find variable y");
			throw new IOException();			
		}
		y = var.read();
		var = mFptr.findVariable("z");
		if (var==null) {
			System.err.println("cannot find variable z");
			throw new IOException();			
		}
		z = var.read();

		var = mFptr.findVariable("u");
		if (var==null) {
			System.err.println("cannot find variable u");
			throw new IOException();			
		}
		u = var.read();
		Attribute attr = var.findAttribute("valid_min");
		uValidMin=attr.getNumericValue().doubleValue();
		attr = var.findAttribute("valid_max");
		uValidMax=attr.getNumericValue().doubleValue();
		attr = var.findAttribute("missing_value");
		uMissing=attr.getNumericValue().doubleValue();
		
		var = mFptr.findVariable("v");
		if (var==null) {
			System.err.println("cannot find variable v");
			throw new IOException();			
		}
		v = var.read();
		attr = var.findAttribute("valid_min");
		vValidMin=attr.getNumericValue().doubleValue();
		attr = var.findAttribute("valid_max");
		vValidMax=attr.getNumericValue().doubleValue();
		attr = var.findAttribute("missing_value");
		vMissing=attr.getNumericValue().doubleValue();
		
		var = mFptr.findVariable("w");
		if (var==null) {
			System.err.println("cannot find variable w");
			throw new IOException();			
		}
		w = var.read();
		attr = var.findAttribute("valid_min");
		wValidMin=attr.getNumericValue().doubleValue();
		attr = var.findAttribute("valid_max");
		wValidMax=attr.getNumericValue().doubleValue();
		attr = var.findAttribute("missing_value");
		wMissing=attr.getNumericValue().doubleValue();
		
		var = mFptr.findVariable("sample_time_difference");
		if (var==null) {
			System.err.println("cannot find variable sample_time_difference");
			throw new IOException();			
		}
		deltaT = var.read();
		deltaTValidMin=-675.0;
		deltaTValidMax=675.0;
		attr = var.findAttribute("_FillValue");
		deltaTMissing=attr.getNumericValue().doubleValue();

	}
	static int xytobin(float val)
	{
		// assume -200000m to 200000m range grid maps to 401 bins (0 to 400 index)
		float indexVal = 400.0f*(val + 200000.0f)/400000.0f;
		return (int)indexVal;
	}
	
	public float getUValue(float xRelCenter, float yRelCenter, float heightMeters)
	// assumes x and y coordinates are distances relative to center of grid
	// must convert to lower left corner relative array indices then 
	// retrieve value
	{
		// compute distance from origin and offset into distance indexed array
		int xIndex=xytobin(xRelCenter);
		int yIndex=xytobin(yRelCenter);
		int zIndex=(int)(heightMeters/1000.0f);
		
		return((float)(u.getDouble(u.getIndex().set(xIndex,yIndex,zIndex))));
	}
	public float getVValue(float xRelCenter, float yRelCenter, float heightMeters)
	// assumes x and y coordinates are distances relative to center of grid
	// must convert to lower left corner relative array indices then 
	// retrieve value
	{
		// compute distance from origin and offset into distance indexed array
		int xIndex=xytobin(xRelCenter);
		int yIndex=xytobin(yRelCenter);
		int zIndex=(int)(heightMeters/1000.0f);
		
		return((float)(v.getDouble(v.getIndex().set(xIndex,yIndex,zIndex))));
	}
	public float getWValue(float xRelCenter, float yRelCenter, float heightMeters)
	// assumes x and y coordinates are distances relative to center of grid
	// must convert to lower left corner relative array indices then 
	// retrieve value
	{
		// compute distance from origin and offset into distance indexed array
		int xIndex=xytobin(xRelCenter);
		int yIndex=xytobin(yRelCenter);
		int zIndex=(int)(heightMeters/1000.0f);
		
		return((float)(w.getDouble(w.getIndex().set(xIndex,yIndex,zIndex))));
	}
	public float getdeltaTValue(float xRelCenter, float yRelCenter, float heightMeters)
	// assumes x and y coordinates are distances relative to center of grid
	// must convert to lower left corner relative array indices then 
	// retrieve value
	{
		// compute distance from origin and offset into distance indexed array
		int xIndex=xytobin(xRelCenter);
		int yIndex=xytobin(yRelCenter);
		int zIndex=(int)(heightMeters/1000.0f);
		
		return((float)(deltaT.getDouble(deltaT.getIndex().set(xIndex,yIndex,zIndex))));
	}
	public DescriptiveStatistics getStats(Array dataValues,float lat, float lon, float radiusM, float zmin, float zmax, double validMin, double validMax, boolean absFlag) 
	{
		// compute the offsets into the specified subset range of the given data array and compute statistics
		
		// need to account for bad/missing values, don't include in stats
		
		// compute min and max lat/lon x,y indices and set up looping through the subset area
		
		BoundingBox bbox = new BoundingBox(lat, lon, radiusM);
		
		LatLng west = bbox.getWest();
		LatLng east = bbox.getEast();
		LatLng north = bbox.getNorth();
		LatLng south = bbox.getSouth();
//		System.out.println("east " + east);
//		System.out.println("west " + west);
//		System.out.println("north " + north);
//		System.out.println("south " + south);
		
		
//		float westLatDiff = (float)west.getLatitude() - originLat;
//		float westLonDiff = (float)west.getLongitude() - originLon;
//		float eastLatDiff = (float)east.getLatitude() - originLat;
//		float eastLonDiff = (float)east.getLongitude() - originLon;
//		float northLatDiff = (float)north.getLatitude() - originLat;
//		float northLonDiff = (float)north.getLongitude() - originLon;
//		float southLatDiff = (float)south.getLatitude() - originLat;
//		float southLonDiff = (float)south.getLongitude() - originLon;
//		
		// compute corner lat/lon from grid center lat lon
		LatLng cornerLatLon = LatLngTool.travel(new LatLng(originLat,originLon), Bearing.SOUTH, 200000.0 ,LengthUnit.METER);
		cornerLatLon = LatLngTool.travel(cornerLatLon, Bearing.WEST, 200000.0 ,LengthUnit.METER);
		
		float cornerLat=(float)cornerLatLon.getLatitude();
		float cornerLon=(float)cornerLatLon.getLongitude();
		
		// compute X offset in m from west longitude
		float westX = (float)LatLngTool.distance(cornerLatLon, new LatLng(cornerLat,west.getLongitude()), LengthUnit.METER);
		// compute X offset in m from east longitude
		float eastX = (float)LatLngTool.distance(cornerLatLon, new LatLng(cornerLat,east.getLongitude()), LengthUnit.METER);
		// compute Y offset in m from north latitude
		float northY = (float)LatLngTool.distance(cornerLatLon, new LatLng(north.getLatitude(),cornerLon), LengthUnit.METER);
		// compute Y offset in m from south latitude
		float southY = (float)LatLngTool.distance(cornerLatLon, new LatLng(south.getLatitude(),cornerLon), LengthUnit.METER);
				
		int xStartBin, yStartBin, zStartBin;
		int xEndBin, yEndBin, zEndBin;
		
		xStartBin = (int)(westX / 1000.0f);
		xEndBin = (int)(eastX / 1000.0f);
		yStartBin = (int)(southY / 1000.0f);
		yEndBin = (int)(northY / 1000.0f);
		zStartBin = (int)(zmin / 1000.0f);
		zEndBin = (int)(zmax / 1000.0f);
		
//		System.out.println("xStartBin " + xStartBin + " xEndBin "+xEndBin);
//		System.out.println("yStartBin " + yStartBin + " yEndBin "+yEndBin);
//		System.out.println("zStartBin " + zStartBin + " zEndBin "+zEndBin);

		int centerYBin = (int)((float)(yEndBin+yStartBin)/2.0f);
		int centerXBin = (int)((float)(xEndBin+xStartBin)/2.0f);

		DescriptiveStatistics stats = new DescriptiveStatistics();
		for (int ind1=zStartBin;ind1<=zEndBin&&ind1<zDim.getLength();ind1++ ) {
			for (int ind2=yStartBin;ind2<=yEndBin&&ind2<yDim.getLength();ind2++) {
				float yDist=Math.abs(centerYBin-ind2);
				for (int ind3=xStartBin;ind3<=xEndBin&&ind3<xDim.getLength();ind3++) {
					float xDist=Math.abs(centerXBin-ind3);
					if (Math.sqrt(xDist*xDist+yDist*yDist)>(radiusM/1000.0f))
						continue;
//					System.out.println("ind1 " + ind1 + " ind2 " + ind2 + " ind3 "+ ind3);
					double value = dataValues.getDouble(dataValues.getIndex().set(0,ind1,ind2,ind3));
					if (absFlag) {
						value = Math.abs(value);
					}
					
//					System.out.println("value " + value);
					
					if (value >= validMin && value <= validMax)
						stats.addValue(value);
					
				}
			}
		}
		if (stats.getN()==0)
			return null;
		else
			return stats;
		
		// Compute some statistics
//		double mean = stats.getMean();
//		double std = stats.getStandardDeviation();
//		double median = stats.getPercentile(50);
//		int count = (int) stats.getN();
		
	}
	public BufferedImage makeVNImage(Array dataValues, Array fpLat, Array fpLon, int elevIndex, float radiusM, float validMin, float validMax,boolean colorFlag) 
	{
		// compute the offsets into the specified subset range of the given data array and compute statistics
		
		// need to account for bad/missing values, don't include in stats
		
		// compute min and max lat/lon x,y indices and set up looping through the subset area
		
		
		
		int numLines =yDim.getLength();
		int numPix = xDim.getLength();

		byte [][] allLines;
		if (colorFlag) {
			allLines = new byte[numLines][numPix*3];
		}
		else {
			allLines = new byte[numLines][numPix];
		}

		int [] shape = fpLon.getShape();
		DescriptiveStatistics stats = new DescriptiveStatistics();
		for (int ind1=0;ind1<shape[1];ind1++){
			float value = dataValues.getFloat(dataValues.getIndex().set(elevIndex, ind1));
			if (value>=validMin && value<=validMax) {
				stats.addValue(value);					
			}
		}
		float minValue = (float)stats.getMin();
		float maxValue = (float)stats.getMax();


		for (int fpInd=0;fpInd<shape[1];fpInd++) {
			float lat=fpLat.getFloat(fpLat.getIndex().set(elevIndex,fpInd));
			float lon=fpLon.getFloat(fpLon.getIndex().set(elevIndex,fpInd));
			
			BoundingBox bbox = new BoundingBox(lat, lon, radiusM);
			
			LatLng west = bbox.getWest();
			LatLng east = bbox.getEast();
			LatLng north = bbox.getNorth();
			LatLng south = bbox.getSouth();
//			System.out.println("east " + east);
//			System.out.println("west " + west);
//			System.out.println("north " + north);
//			System.out.println("south " + south);
			
			// compute corner lat/lon from grid center lat lon
			LatLng cornerLatLon = LatLngTool.travel(new LatLng(originLat,originLon), Bearing.SOUTH, 200000.0 ,LengthUnit.METER);
			cornerLatLon = LatLngTool.travel(cornerLatLon, Bearing.WEST, 200000.0 ,LengthUnit.METER);
			
			float cornerLat=(float)cornerLatLon.getLatitude();
			float cornerLon=(float)cornerLatLon.getLongitude();
			
			// compute X offset in m from west longitude
			float westX = (float)LatLngTool.distance(cornerLatLon, new LatLng(cornerLat,west.getLongitude()), LengthUnit.METER);
			// compute X offset in m from east longitude
			float eastX = (float)LatLngTool.distance(cornerLatLon, new LatLng(cornerLat,east.getLongitude()), LengthUnit.METER);
			// compute Y offset in m from north latitude
			float northY = (float)LatLngTool.distance(cornerLatLon, new LatLng(north.getLatitude(),cornerLon), LengthUnit.METER);
			// compute Y offset in m from south latitude
			float southY = (float)LatLngTool.distance(cornerLatLon, new LatLng(south.getLatitude(),cornerLon), LengthUnit.METER);

			
//			// compute X offset in m from west longitude
//			float westX = (float)LatLngTool.distance(new LatLng(originLat,originLon), new LatLng(originLat,west.getLongitude()), LengthUnit.METER);
//			// compute X offset in m from east longitude
//			float eastX = (float)LatLngTool.distance(new LatLng(originLat,originLon), new LatLng(originLat,east.getLongitude()), LengthUnit.METER);
//			// compute Y offset in m from north latitude
//			float northY = (float)LatLngTool.distance(new LatLng(originLat,originLon), new LatLng(north.getLatitude(),originLon), LengthUnit.METER);
//			// compute Y offset in m from south latitude
//			float southY = (float)LatLngTool.distance(new LatLng(originLat,originLon), new LatLng(south.getLatitude(),originLon), LengthUnit.METER);
					
			int xStartBin, yStartBin, zStartBin;
			int xEndBin, yEndBin, zEndBin;
			
			xStartBin = (int)(westX / 1000.0f);
			xEndBin = (int)(eastX / 1000.0f);
			yStartBin = (int)(southY / 1000.0f);
			yEndBin = (int)(northY / 1000.0f);

//			System.out.println("xStartBin " + xStartBin + " xEndBin "+xEndBin);
//			System.out.println("yStartBin " + yStartBin + " yEndBin "+yEndBin);
	
	
			double value = dataValues.getDouble(dataValues.getIndex().set(elevIndex,fpInd));
//			if (value<validMin || value>validMax) continue;
			
			int centerYBin = (int)((float)(yEndBin+yStartBin)/2.0f);
			int centerXBin = (int)((float)(xEndBin+xStartBin)/2.0f);
			
			for (int ind1=yStartBin;ind1<yEndBin&&ind1<numLines;ind1++) {
				float yDist=Math.abs(centerYBin-ind1);
				for (int ind2=xStartBin;ind2<xEndBin&&ind2<numPix;ind2++) {
					float xDist=Math.abs(centerXBin-ind2);
					if (Math.sqrt(xDist*xDist+yDist*yDist)>(radiusM/1000.0f))
						continue;
					byte byteValue;
					if (value<minValue) byteValue = 0;
					else if (value>maxValue) byteValue = (byte)255;
					else byteValue=(byte) (255.0 * ( value - minValue) / (maxValue - minValue));
					if (colorFlag) {
						int index=0xFF&byteValue;
						allLines[ind1][ind2*3] = colormap.radarScale[index][0];
						allLines[ind1][ind2*3+1] = colormap.radarScale[index][1];
						allLines[ind1][ind2*3+2] = colormap.radarScale[index][2];
					}
					else {
						allLines[ind1][ind2] = byteValue;
					}
						
				}
			}
		}
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		// write image, flip line order for top to bottom image rendering
		for (int lineInd=0,ind1=0;ind1<numLines;ind1++,lineInd++){
			
			// draw one line at a time into bufferedImage
			if (colorFlag)
				bos.write(allLines[numLines - 1 - lineInd], 0, numPix*3);
			else
				bos.write(allLines[numLines - 1 - lineInd], 0, numPix);;
		}
		byte[] pixels = bos.toByteArray();
		
		BufferedImage image;
		if (colorFlag)
			image = createRGBImage(pixels, numPix, numLines);
		else 
			image = createGreyscaleImage(pixels, numPix, numLines);
				
		return image;
	}
	public BufferedImage makeWindsImage(Array windsData, int zIndex, float validMin, float validMax, boolean colorFlag, boolean absFlag)
	/*
	 * creates grey scale image 0-255 between specified max/min values
	 */
	{
		
//		BufferedImage image = new BufferedImage(ncols, nrows, BufferedImage.TYPE_BYTE_GRAY);
		
		int numLines = getyDim().getLength();
		int numPix = getxDim().getLength();

		byte [][] allLines;
		if (colorFlag) {
			allLines = new byte[numLines][numPix*3];
		}
		else {
			allLines = new byte[numLines][numPix];
		}
		
		DescriptiveStatistics stats = new DescriptiveStatistics();
		for (int ind1=0;ind1<numLines;ind1++){
			for (int ind2=0;ind2<numPix;ind2++){
				float value = windsData.getFloat(windsData.getIndex().set(0, zIndex, ind1, ind2));
				if (absFlag) {
					value = Math.abs(value);
				}
				if (value>=validMin && value<=validMax) {
					stats.addValue(value);					
				}
			}
		}
		float minValue = (float)stats.getMin();
		float maxValue = (float)stats.getMax();
		System.out.println("min " + minValue + " max " + maxValue);
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		for (int ind1=0;ind1<numLines;ind1++){
			for (int ind2=0;ind2<numPix;ind2++){
				float value = windsData.getFloat(windsData.getIndex().set(0, zIndex, ind1, ind2));
				byte byteValue;
				if (value<minValue) byteValue = 0;
				else if (value>maxValue) byteValue = (byte)255;
				else byteValue=(byte) (255.0 * ( value - minValue) / (maxValue - minValue));
				if (colorFlag) {
					int index=0xFF&byteValue;
					allLines[ind1][ind2*3] = colormap.radarScale[index][0];
					allLines[ind1][ind2*3+1] = colormap.radarScale[index][1];
					allLines[ind1][ind2*3+2] = colormap.radarScale[index][2];
				}
				else {
					allLines[ind1][ind2] = byteValue;
				}
			}
//			// draw one line at a time into bufferedImage
//			if (colorFlag)
//				bos.write(oneLine, 0, numPix*3);
//			else
//				bos.write(oneLine, 0, numPix);
		}
		// write image, flip line order for top to bottom image rendering
		for (int lineInd=0,ind1=0;ind1<numLines;ind1++,lineInd++){
			
			// draw one line at a time into bufferedImage
			if (colorFlag)
				bos.write(allLines[numLines - 1 - lineInd], 0, numPix*3);
			else
				bos.write(allLines[numLines - 1 - lineInd], 0, numPix);;
		}
		byte[] pixels = bos.toByteArray();
		
		BufferedImage image;
		if (colorFlag)
			image = Winds3DData.createRGBImage(pixels, numPix, numLines);
		else 
			image = Winds3DData.createGreyscaleImage(pixels, numPix, numLines);
				
		return image;
	}
	public class BoundingBox
	{
		LatLng north,south,east,west;
		public LatLng getNorth() {
			return north;
		}

		public LatLng getSouth() {
			return south;
		}

		public LatLng getEast() {
			return east;
		}

		public LatLng getWest() {
			return west;
		}
		
		public BoundingBox(float centerLat, float centerLon, float radiusM) 
		{
			LatLng centerLoc=new LatLng(centerLat, centerLon);
			// compute bounding box for a radius around a lat/lon location
			north = LatLngTool.travel(centerLoc, Bearing.NORTH, radiusM,LengthUnit.METER);
			south = LatLngTool.travel(centerLoc, Bearing.SOUTH, radiusM,LengthUnit.METER);
			east = LatLngTool.travel(centerLoc, Bearing.EAST, radiusM,LengthUnit.METER);
			west = LatLngTool.travel(centerLoc, Bearing.WEST, radiusM,LengthUnit.METER);
		}
	}
	public static BufferedImage createRGBImage(byte[] bytes, int width, int height) {
	    DataBufferByte buffer = new DataBufferByte(bytes, bytes.length);
	    ColorModel cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), new int[]{8, 8, 8}, false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
	    return new BufferedImage(cm, Raster.createInterleavedRaster(buffer, width, height, width * 3, 3, new int[]{0, 1, 2}, null), false, null);
	}
	public static BufferedImage createGreyscaleImage(byte[] bytes, int width, int height) {
	    DataBufferByte buffer = new DataBufferByte(bytes, bytes.length);
	    ColorModel cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), new int[]{8}, false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
	    return new BufferedImage(cm, Raster.createInterleavedRaster(buffer, width, height, width, 1,new int[]{0}, null), false, null);
	}
//	public DescriptiveStatistics getStats(Array dataValues, float xmin, float xmax, float ymin, float ymax, float zmin, float zmax, double validMin, double validMax) 
//	{
//		// compute the offsets into the specified subset range of the given data array and compute statistics
//		
//		// need to account for bad/missing values, don't include in stats
//		
//		// compute min and max lat/lon x,y indices and set up looping through the subset area
//		
//		int xStartBin, yStartBin, zStartBin;
//		int xEndBin, yEndBin, zEndBin;
//		
//		xStartBin = xytobin(xmin);
//		yStartBin = xytobin(ymin);
//		xEndBin = xytobin(xmax);
//		yEndBin = xytobin(ymax);
//		zStartBin = (int)(zmin/1000.0f);
//		zEndBin = (int)(zmax/1000.0f);
//		
//		DescriptiveStatistics stats = new DescriptiveStatistics();
//		for (int ind1=zStartBin;ind1<=zEndBin;ind1++ ) {
//			for (int ind2=yStartBin;ind2<=yEndBin;ind2++) {
//				for (int ind3=xStartBin;ind3<=xEndBin;ind3++) {
////					System.out.println("ind1 " + ind1 + " ind2 " + ind2 + " ind3 "+ ind3);
//					double value = dataValues.getDouble(dataValues.getIndex().set(0,ind1,ind2,ind3));
//					
//					if (value >= validMin && value <= validMax)
//						stats.addValue(value);
//					
//				}
//			}
//		}
//		if (stats.getN()==0)
//			return null;
//		else
//			return stats;
//		
//		// Compute some statistics
////		double mean = stats.getMean();
////		double std = stats.getStandardDeviation();
////		double median = stats.getPercentile(50);
////		int count = (int) stats.getN();
//		
//	}

}
