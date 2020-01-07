package GeoMatch;
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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import com.google.common.io.Files;
import com.javadocmd.simplelatlng.LatLng;
import com.javadocmd.simplelatlng.LatLngTool;
import com.javadocmd.simplelatlng.util.LengthUnit;

import Util.TempFile;
import Util.colormap;
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

public class Winds3DGeoMatch {

	float DPR_FOOTPRINT = 5200.0f;
	String vnInputFile;
	String vnOutputFile;
	String windsInputFile;
	String swath="";

	public Winds3DGeoMatch(String vnInputFile, String windsInputFile, String vnOutputFile, String swath)
	{
		this.vnInputFile = vnInputFile;
		this.windsInputFile = windsInputFile;
		this.vnOutputFile = vnOutputFile;
		this.swath=swath;
	}
	
	void processFiles()
	{
		NetcdfFileWriter mFptr=null;
		String gpmTime=null;
		float siteLat=0.0f;
		float siteLon=0.0f;
		String filename;
		TempFile temp=null;
		
		Array dprLatitude=null, dprLongitude=null;
		Array xCorners=null, yCorners=null;
		Array topHeight=null, bottomHeight=null;
		Array latitude=null, longitude=null;
		Array GR_Z=null;
		
		Dimension fpdim=null;
		Dimension elevationAngle=null;
		Winds3DData winds=null;
		try {
			temp = new TempFile(vnInputFile);
			filename = temp.getTempFilename();
			if (vnInputFile.endsWith(".gz")) {
//				System.out.println("temporary file: "+ filename);
				temp.unzip();
			}
			else {
				temp.copy();
			}
			
			mFptr = NetcdfFileWriter.openExisting(filename);
			Variable var = mFptr.findVariable("atimeNearestApproach");
			if (var==null) {
				System.err.println("cannot find variable atimeNearestApproach");
				throw new IOException();			
			}
			int[] shape = var.getShape(); // array dimensions
			gpmTime = var.readScalarString();
			System.out.println("gpmTime: " + gpmTime);
//			Array arr = var.read();
			//ByteBuffer buff = arr.getDataAsByteBuffer();

			fpdim = mFptr.getNetcdfFile().findDimension("fpdim"+swath);
			elevationAngle = mFptr.getNetcdfFile().findDimension("elevationAngle");
			var = mFptr.findVariable("site_lat");
			if (var==null) {
				System.err.println("cannot find variable site_lat");
				throw new IOException();			
			}
			siteLat = var.readScalarFloat();
			System.out.println("site_lat: " + siteLat);
			var = mFptr.findVariable("site_lon");
			if (var==null) {
				System.err.println("cannot find variable site_lon");
				throw new IOException();			
			}
			siteLon = var.readScalarFloat();
			System.out.println("site_lat: " + siteLon);

			var = mFptr.findVariable("DPRlatitude"+swath);
			if (var==null) {
				System.err.println("cannot find variable DPRlatitude"+swath);
				throw new IOException();			
			}
			dprLatitude = var.read();
			var = mFptr.findVariable("DPRlongitude"+swath);
			if (var==null) {
				System.err.println("cannot find variable DPRlongitude"+swath);
				throw new IOException();			
			}
			dprLongitude = var.read();

			var = mFptr.findVariable("xCorners"+swath);
			if (var==null) {
				System.err.println("cannot find variable xCorners"+swath);
				throw new IOException();			
			}
			xCorners = var.read();
			var = mFptr.findVariable("yCorners"+swath);
			if (var==null) {
				System.err.println("cannot find variable yCorners"+swath);
				throw new IOException();			
			}
			yCorners = var.read();
			var = mFptr.findVariable("topHeight"+swath);
			if (var==null) {
				System.err.println("cannot find variable topHeight"+swath);
				throw new IOException();			
			}
			topHeight = var.read();
			var = mFptr.findVariable("bottomHeight"+swath);
			if (var==null) {
				System.err.println("cannot find variable bottomHeight"+swath);
				throw new IOException();			
			}
			bottomHeight = var.read();
			
			var = mFptr.findVariable("latitude"+swath);
			if (var==null) {
				System.err.println("cannot find variable latitude"+swath);
				throw new IOException();			
			}
			latitude = var.read();
			var = mFptr.findVariable("longitude"+swath);
			if (var==null) {
				System.err.println("cannot find variable longitude"+swath);
				throw new IOException();			
			}
			longitude = var.read();
			
			// DPR precip rate (elev,fpdim)
			var = mFptr.findVariable("GR_Z"+swath);
			if (var==null) {
				System.err.println("cannot find variable GR_Z"+swath);
				throw new IOException();			
			}
			GR_Z = var.read();

			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			// read in 3D wind data 
			winds = new Winds3DData(windsInputFile);
			
			// output test winds image
			BufferedImage windsImage = winds.makeWindsImage(winds.getU(), 1, (float)winds.getuValidMin(), (float)winds.getuValidMax(),true);
			FileOutputStream windsfout = new FileOutputStream(vnOutputFile + ".winds_U.col" + ".png" );
		    ImageIO.write(windsImage, "png", windsfout);
			windsfout.close();
			
			windsImage = winds.makeWindsImage(winds.getV(), 1, (float)winds.getvValidMin(), (float)winds.getvValidMax(),true);
			windsfout = new FileOutputStream(vnOutputFile + ".winds_V.col" + ".png" );
		    ImageIO.write(windsImage, "png", windsfout);
			windsfout.close();
			
			windsImage = winds.makeWindsImage(winds.getW(), 1, (float)winds.getwValidMin(), (float)winds.getwValidMax(),true);
			windsfout = new FileOutputStream(vnOutputFile + ".winds_W.col" + ".png" );
		    ImageIO.write(windsImage, "png", windsfout);
			windsfout.close();
			
			BufferedImage vnImage = winds.makeVNImage(GR_Z, latitude, longitude, 0, (float)DPR_FOOTPRINT/2.0f, 0.0f, 150.0f, true);
			FileOutputStream dbzfout = new FileOutputStream(vnOutputFile + ".VN_GR_dbz.col" + ".png" );
		    ImageIO.write(vnImage, "png", dbzfout);
		    dbzfout.close();
	
	    	// enter define mode 
			mFptr.setRedefineMode(true);
			mFptr.flush();

			// add new variables to the temporary file
		
			List<Dimension> dims = new ArrayList<Dimension>();
		    dims.add(elevationAngle);
		    dims.add(fpdim);
		    
		    // add scalar variable flag have_winds for presence of winds data, use empty string for dimensions
		    Variable windsFlagVar = mFptr.addVariable(null, "have_winds"+swath, DataType.SHORT, "");
		    windsFlagVar.addAttribute(new Attribute("long_name", "Data exists for 3D winds"));
		    windsFlagVar.addAttribute(new Attribute("_FillValue", (short)0));

		    Variable windsUVar = mFptr.addVariable(null, "GR_U"+swath, DataType.FLOAT, dims);
		    windsUVar.addAttribute(new Attribute("units", "m/s"));
		    windsUVar.addAttribute(new Attribute("long_name", "meridional component of wind velocity mean"));
		    windsUVar.addAttribute(new Attribute("_FillValue", (float)-888.0));
		    windsUVar.addAttribute(new Attribute("valid_min", (float)-106.64999771118164));
		    windsUVar.addAttribute(new Attribute("valid_max", (float)106.64999771118164));
		    Variable windsUVarStdDev = mFptr.addVariable(null, "GR_U_StdDev"+swath, DataType.FLOAT, dims);
		    windsUVarStdDev.addAttribute(new Attribute("units", "m/s"));
		    windsUVarStdDev.addAttribute(new Attribute("long_name", "meridional component of wind velocity Std Dev"));
		    windsUVarStdDev.addAttribute(new Attribute("_FillValue", (float)-888.0));
		    windsUVarStdDev.addAttribute(new Attribute("valid_min", (float)-106.64999771118164));
		    windsUVarStdDev.addAttribute(new Attribute("valid_max", (float)106.64999771118164));
		    Variable windsUVarMax = mFptr.addVariable(null, "GR_U_Max"+swath, DataType.FLOAT, dims);					// loop through DPR footprints
		    windsUVarMax.addAttribute(new Attribute("units", "m/s"));
		    windsUVarMax.addAttribute(new Attribute("long_name", "meridional component of wind velocity max"));
		    windsUVarMax.addAttribute(new Attribute("_FillValue", (float)-888.0));
		    windsUVarMax.addAttribute(new Attribute("valid_min", (float)-106.64999771118164));
		    windsUVarMax.addAttribute(new Attribute("valid_max", (float)106.64999771118164));
				
		    Variable windsVVar = mFptr.addVariable(null, "GR_V"+swath, DataType.FLOAT, dims);
		    windsVVar.addAttribute(new Attribute("units", "m/s"));
		    windsVVar.addAttribute(new Attribute("long_name", "zonal component of wind velocity mean"));
		    windsVVar.addAttribute(new Attribute("_FillValue", (float)-888.0));
		    windsVVar.addAttribute(new Attribute("valid_min", (float)-106.64999771118164));
		    windsVVar.addAttribute(new Attribute("valid_max", (float)106.64999771118164));
		    Variable windsVVarStdDev = mFptr.addVariable(null, "GR_V_StdDev"+swath, DataType.FLOAT, dims);
		    windsVVarStdDev.addAttribute(new Attribute("units", "m/s"));
		    windsVVarStdDev.addAttribute(new Attribute("long_name", "zonal component of wind velocity Std Dev"));
		    windsVVarStdDev.addAttribute(new Attribute("_FillValue", (float)-888.0));
		    windsVVarStdDev.addAttribute(new Attribute("valid_min", (float)-106.64999771118164));
		    windsVVarStdDev.addAttribute(new Attribute("valid_max", (float)106.64999771118164));
		    Variable windsVVarMax = mFptr.addVariable(null, "GR_V_Max"+swath, DataType.FLOAT, dims);
		    windsVVarMax.addAttribute(new Attribute("units", "m/s"));
		    windsVVarMax.addAttribute(new Attribute("long_name", "zonal component of wind velocity max"));
		    windsVVarMax.addAttribute(new Attribute("_FillValue", (float)-888.0));
		    windsVVarMax.addAttribute(new Attribute("valid_min", (float)-106.64999771118164));
		    windsVVarMax.addAttribute(new Attribute("valid_max", (float)106.64999771118164));
		    
		    Variable windsWVar = mFptr.addVariable(null, "GR_W"+swath, DataType.FLOAT, dims);
		    windsWVar.addAttribute(new Attribute("units", "m/s"));
		    windsWVar.addAttribute(new Attribute("long_name", "vertical component of wind velocity mean"));
		    windsWVar.addAttribute(new Attribute("_FillValue", (float)-888.0));
		    windsWVar.addAttribute(new Attribute("valid_min", (float)-106.64999771118164));
		    windsWVar.addAttribute(new Attribute("valid_max", (float)106.64999771118164));
		    Variable windsWVarStdDev = mFptr.addVariable(null, "GR_W_StdDev"+swath, DataType.FLOAT, dims);
		    windsWVarStdDev.addAttribute(new Attribute("units", "m/s"));
		    windsWVarStdDev.addAttribute(new Attribute("long_name", "vertical component of wind velocity Std Dev"));
		    windsWVarStdDev.addAttribute(new Attribute("_FillValue", (float)-888.0));
		    windsWVarStdDev.addAttribute(new Attribute("valid_min", (float)-106.64999771118164));
		    windsWVarStdDev.addAttribute(new Attribute("valid_max", (float)106.64999771118164));
		    Variable windsWVarMax = mFptr.addVariable(null, "GR_W_Max"+swath, DataType.FLOAT, dims);
		    windsWVarMax.addAttribute(new Attribute("units", "m/s"));
		    windsWVarMax.addAttribute(new Attribute("long_name", "vertical component of wind velocity max"));
		    windsWVarMax.addAttribute(new Attribute("_FillValue", (float)-888.0));
		    windsWVarMax.addAttribute(new Attribute("valid_min", (float)-106.64999771118164));
		    windsWVarMax.addAttribute(new Attribute("valid_max", (float)106.64999771118164));
		    
	    	// leave define mode 
			mFptr.flush();
			mFptr.setRedefineMode(false);
			mFptr.flush();
		
			// allocate arrays for variables
			
			int [] shape = windsUVar.getShape();
			ArrayFloat windsUArr = new ArrayFloat.D2(shape[0], shape[1]);
			ArrayFloat windsUArrStdDev = new ArrayFloat.D2(shape[0], shape[1]);
			ArrayFloat windsUArrMax = new ArrayFloat.D2(shape[0], shape[1]);

			ArrayFloat windsVArr = new ArrayFloat.D2(shape[0], shape[1]);
			ArrayFloat windsVArrStdDev = new ArrayFloat.D2(shape[0], shape[1]);
			ArrayFloat windsVArrMax = new ArrayFloat.D2(shape[0], shape[1]);

			ArrayFloat windsWArr = new ArrayFloat.D2(shape[0], shape[1]);
			ArrayFloat windsWArrStdDev = new ArrayFloat.D2(shape[0], shape[1]);
			ArrayFloat windsWArrMax = new ArrayFloat.D2(shape[0], shape[1]);

			// loop through elevations and DPR footprints
			// accumulate wind statistics for GR to GPM volume matches using winds 
			
			for (int elev=0;elev<elevationAngle.getLength();elev++) {
				for (int ind1=0;ind1<fpdim.getLength();ind1++) {
					
//					int indFact[] = new int[2];
//					indFact[0]=elev;
//					indFact[1]=ind1;
//					Index index =Index.factory(indFact);
					float elevMin=bottomHeight.getFloat(bottomHeight.getIndex().set(elev, ind1))*1000.0f;
					float elevMax=topHeight.getFloat(topHeight.getIndex().set(elev, ind1))*1000.0f;
					
//					System.out.println("elev min "+ elevMin + " max " + elevMax);
					
					// skip 
					
					if (elevMin==elevMax) {
						windsUArr.setFloat(windsUArr.getIndex().set(elev, ind1), -888.0f);
						windsUArrStdDev.setFloat(windsUArr.getIndex().set(elev, ind1), -888.0f);
						windsUArrMax.setFloat(windsUArr.getIndex().set(elev, ind1), -888.0f);
						windsVArr.setFloat(windsVArr.getIndex().set(elev, ind1), -888.0f);
						windsVArrStdDev.setFloat(windsVArr.getIndex().set(elev, ind1), -888.0f);
						windsVArrMax.setFloat(windsVArr.getIndex().set(elev, ind1), -888.0f);
						windsWArr.setFloat(windsWArr.getIndex().set(elev, ind1), -888.0f);
						windsWArrStdDev.setFloat(windsWArr.getIndex().set(elev, ind1), -888.0f);
						windsWArrMax.setFloat(windsWArr.getIndex().set(elev, ind1), -888.0f);
						continue;
					}

					// find min and max of x and y bounding coordinates for original GPM bounds (in Km)
//					indFact = new int[3];
//					indFact[0]=elev;
//					indFact[1]=ind1;
//					indFact[2]=0;
//					index =Index.factory(indFact);
					
					float xmin=xCorners.getFloat(xCorners.getIndex().set(elev,ind1,0))*1000.0f;
					float ymin=yCorners.getFloat(xCorners.getIndex().set(elev,ind1,0))*1000.0f;
					float xmax=xCorners.getFloat(yCorners.getIndex().set(elev,ind1,0))*1000.0f;
					float ymax=yCorners.getFloat(yCorners.getIndex().set(elev,ind1,0))*1000.0f;
					for (int ind2=1;ind2<4;ind2++) {
//						indFact[2]=ind2;
//						index =Index.factory(indFact);
						float xc=xCorners.getFloat(xCorners.getIndex().set(elev,ind1,ind2))*1000.0f;
						xmin=(xc<xmin)?xc:xmin;
						xmax=(xc>xmax)?xc:xmax;
						float yc=yCorners.getFloat(yCorners.getIndex().set(elev,ind1,ind2))*1000.0f;
						ymin=(yc<ymin)?yc:ymin;
						ymax=(yc>ymax)?yc:ymax;
					}
					
//					System.out.println("xmin " + xmin + " xmax "+xmax);
//					System.out.println("ymin " + ymin + " ymax "+ymax);
//					System.out.println("zmin " + elevMin + " zmax "+elevMax);
//					System.out.println("valid min " + winds.getuValidMin() + " max "+winds.getuValidMax());
					
//					DescriptiveStatistics uStats = winds.getStats(winds.getU(), xmin, xmax, ymin, ymax, elevMin, elevMax, winds.getuValidMin(), winds.getuValidMax());
					DescriptiveStatistics uStats = winds.getStats(winds.getU(), latitude.getFloat(latitude.getIndex().set(elev,ind1)), longitude.getFloat(longitude.getIndex().set(elev,ind1)), DPR_FOOTPRINT/2.0f, elevMin, elevMax, winds.getuValidMin(), winds.getuValidMax());
					if (uStats!=null) {
						windsUArr.setFloat(windsUArr.getIndex().set(elev, ind1), (float)uStats.getMean());
						windsUArrStdDev.setFloat(windsUArr.getIndex().set(elev, ind1), (float)uStats.getStandardDeviation());
						windsUArrMax.setFloat(windsUArr.getIndex().set(elev, ind1), (float)uStats.getMax());
//						System.out.println("num pts " +uStats.getN());
//						System.out.println("U mean " + uStats.getMean());
//						System.out.println("U max " + uStats.getMax());
//						System.out.println("U std dev " +uStats.getStandardDeviation());
					}
					else {
						windsUArr.setFloat(windsUArr.getIndex().set(elev, ind1), -888.0f);
						windsUArrStdDev.setFloat(windsUArr.getIndex().set(elev, ind1), -888.0f);
						windsUArrMax.setFloat(windsUArr.getIndex().set(elev, ind1), -888.0f);
//						System.out.println("U values missing ");
					}
					
					DescriptiveStatistics vStats = winds.getStats(winds.getV(), latitude.getFloat(latitude.getIndex().set(elev,ind1)), longitude.getFloat(longitude.getIndex().set(elev,ind1)), DPR_FOOTPRINT/2.0f, elevMin, elevMax, winds.getvValidMin(), winds.getvValidMax());
					if (vStats!=null) {
						windsVArr.setFloat(windsVArr.getIndex().set(elev, ind1), (float)vStats.getMean());
						windsVArrStdDev.setFloat(windsVArr.getIndex().set(elev, ind1), (float)vStats.getStandardDeviation());
						windsVArrMax.setFloat(windsVArr.getIndex().set(elev, ind1), (float)vStats.getMax());
					}
					else {
						windsVArr.setFloat(windsVArr.getIndex().set(elev, ind1), -888.0f);
						windsVArrStdDev.setFloat(windsVArr.getIndex().set(elev, ind1), -888.0f);
						windsVArrMax.setFloat(windsVArr.getIndex().set(elev, ind1), -888.0f);
					}
					
					DescriptiveStatistics wStats = winds.getStats(winds.getW(), latitude.getFloat(latitude.getIndex().set(elev,ind1)), longitude.getFloat(longitude.getIndex().set(elev,ind1)), DPR_FOOTPRINT/2.0f, elevMin, elevMax, winds.getwValidMin(), winds.getwValidMax());
					if (wStats!=null) {
						windsWArr.setFloat(windsWArr.getIndex().set(elev, ind1), (float)wStats.getMean());
						windsWArrStdDev.setFloat(windsWArr.getIndex().set(elev, ind1), (float)wStats.getStandardDeviation());
						windsWArrMax.setFloat(windsWArr.getIndex().set(elev, ind1), (float)wStats.getMax());
					}
					else {
						windsWArr.setFloat(windsWArr.getIndex().set(elev, ind1), -888.0f);
						windsWArrStdDev.setFloat(windsWArr.getIndex().set(elev, ind1), -888.0f);
						windsWArrMax.setFloat(windsWArr.getIndex().set(elev, ind1), -888.0f);
					}			
					//System.out.println("elev "+elev+" fpdim " + ind1);
				}
			}
			// fake an array for writing out scalar data
			short windsPresent = 1;
			ArrayShort.D0 scalarData = new ArrayShort.D0();
			scalarData.set(windsPresent);
			// write vars
			mFptr.write(windsFlagVar, scalarData);

			mFptr.write(windsUVar, windsUArr);
			mFptr.write(windsUVarStdDev, windsUArrStdDev);
			mFptr.write(windsUVarMax, windsUArrMax);
			
			BufferedImage vnWindsImage = winds.makeVNImage(windsUArr, latitude, longitude, 0, (float)DPR_FOOTPRINT/2.0f,(float)winds.getuValidMin(), (float)winds.getuValidMax(), true);
			FileOutputStream vnWindsfout = new FileOutputStream(vnOutputFile + ".VN_winds_U.col" + ".png" );
		    ImageIO.write(vnWindsImage, "png", vnWindsfout);
		    vnWindsfout.close();
			
			mFptr.write(windsVVar, windsVArr);
			mFptr.write(windsVVarStdDev, windsVArrStdDev);
			mFptr.write(windsVVarMax, windsVArrMax);
			
			vnWindsImage = winds.makeVNImage(windsVArr, latitude, longitude, 0, (float)DPR_FOOTPRINT/2.0f,(float)winds.getvValidMin(), (float)winds.getvValidMax(), true);
			vnWindsfout = new FileOutputStream(vnOutputFile + ".VN_winds_V.col" + ".png" );
		    ImageIO.write(vnWindsImage, "png", vnWindsfout);
		    vnWindsfout.close();
			
			mFptr.write(windsWVar, windsWArr);
			mFptr.write(windsWVarStdDev, windsWArrStdDev);
			mFptr.write(windsWVarMax, windsWArrMax);
			
			vnWindsImage = winds.makeVNImage(windsWArr, latitude, longitude, 0, (float)DPR_FOOTPRINT/2.0f,(float)winds.getwValidMin(), (float)winds.getwValidMax(), true);
			vnWindsfout = new FileOutputStream(vnOutputFile + ".VN_winds_W.col" + ".png" );
		    ImageIO.write(vnWindsImage, "png", vnWindsfout);
		    vnWindsfout.close();
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			if (winds==null) {
				System.out.println("No matching winds data, skipping file " + vnInputFile);
			}
			else
				e.printStackTrace();
		} finally {
			try {
				mFptr.close();
				
				if (winds!=null) {
					// need to save modified temp files to output path 
		//			java.nio.file.Files.copy(java.nio.file.Paths.get(temp.getTempFilename()), java.nio.file.Paths.get(vnOutputFilename),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
					//or compress..
					//temp.zip(temp.getTempFilename(),vnOutputFilename);
					// remove temp files
		//			temp.deleteTemp();
	
					if (vnOutputFile.endsWith(".gz")) {
						temp.zip(temp.getTempFilename(),vnOutputFile);
					}
					else {
						java.nio.file.Files.copy(java.nio.file.Paths.get(temp.getTempFilename()), java.nio.file.Paths.get(vnOutputFile),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
					}
					System.out.println("Finished file " + vnInputFile);
					
				}
				
				temp.deleteTemp();				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}		
		
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
	
		String vnInputFile;
		String vnOutputFile;
		String windsInputFile;
		String swath="";
		
		if (args.length<3) {
			System.out.println("Usage:  java -jar WindsGeoMatch input_VN_input_file 3Dwinds_input_file output_directory");
			System.out.println("  Matches 3D winds data with GPM VN matchup files and creates a new VN output file with addition of 3D winds");
			System.exit(-1);
		}
		vnInputFile = args[0];
		windsInputFile = args[1];
		File inFile = new File (vnInputFile);
		String inputFileName = inFile.getName();
		vnOutputFile = args[2]+ File.separator +inputFileName;
		if (args.length==4) {
			swath="_"+args[3];
		}
		
		Winds3DGeoMatch windsGeo = new Winds3DGeoMatch(vnInputFile,windsInputFile,vnOutputFile, swath);
		windsGeo.processFiles();
		
	}
}
