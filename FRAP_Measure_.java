import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.OvalRoi;
import ij.gui.WaitForUserDialog;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.filter.Analyzer;
import ij.plugin.frame.RoiManager;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;


public class FRAP_Measure_ implements PlugIn{
	String filename;
	ImagePlus FinalFRAP;
	double [] thearea = new double[50];
	double [] themean = new double[50];
	int FinalFRAPID;
	
	//DELETE THIS BEFORE COMPILING
	static{
			System.setProperty("plugins.dir", "C:/Workspace/IJ/plugins"); 
	}
	
	public void run(String arg) {
		
		
		IJ.run("Set Measurements...", "area mean min centroid redirect=None decimal=2");
		
		new WaitForUserDialog("FRAP Image", "Open FRAP Image").show();
		IJ.run("Bio-Formats Importer");
		
		ImagePlus imp = WindowManager.getCurrentImage();
    	filename = imp.getTitle(); 	//Get file name
	
    	IJ.run(imp, "Z Project...", "projection=[Max Intensity] all");		//Z PROJECT, COMMENT OUT IF NO Z
    	FinalFRAP = WindowManager.getCurrentImage();
    	FinalFRAPID = FinalFRAP.getID();
    	imp.changes = false;
    	imp.close();
		
		IJ.run(FinalFRAP, "Set Scale...", "distance=1 known=1 pixel=1 unit=micron");
	 
		MeasureFRAPSpot();
		IJ.run("Close All", "");
		new WaitForUserDialog("Done", "Plugin Finished.").show();
	}
	
	public void MeasureFRAPSpot(){
		int BleachSlice = 0;
		IJ.selectWindow(FinalFRAPID);
		FinalFRAP = WindowManager.getCurrentImage();
		IJ.run(FinalFRAP, "Enhance Contrast", "saturated=0.35");
		IJ.run("Set Measurements...", "area mean min centroid feret's redirect=None decimal=2");
	
		IJ.setTool("oval");
		new WaitForUserDialog("Draw", "Draw around FRAP spot region").show();
		
		IJ.setAutoThreshold(FinalFRAP, "Default dark");
		
		double threshminimal = FinalFRAP.getProcessor().getMinThreshold();
		double threshmaximal = FinalFRAP.getProcessor().getMaxThreshold();
		
		RoiManager rm = new RoiManager();
		rm = RoiManager.getInstance();
		int numroi = rm.getCount();
		if (numroi > 0){
			rm.runCommand("Deselect");
			rm.runCommand("Delete");
		}
		rm.addRoi(FinalFRAP.getRoi()); //Initial FRAP region defined by user
		

		//GET INITIAL CENTRE POINT OF ROI AND ITS DIAMETER
		rm.select(0);		
		ResultsTable ct = new ResultsTable();		
		double MaxThresh = 4095;
		double MinThresh = 0;
		IJ.setThreshold(FinalFRAP, MinThresh, MaxThresh);
		IJ.run(FinalFRAP, "Analyze Particles...", "size=25-4500 pixel circularity=0.00-1.00 show=Nothing display clear include slice");
		ct = Analyzer.getResultsTable();

		double yval = ct.getValueAsDouble(7, 0);
		double xval = ct.getValueAsDouble(6, 0);
		double theferet = ct.getValueAsDouble(19, 0);
		int ROIdiameter = (int) Math.abs(theferet);
					
		int frapframes = FinalFRAP.getNFrames();
		ResultsTable rt = new ResultsTable();
		
		double [] totalarea = new double [frapframes];
		double [] totalmean = new double [frapframes];
		double [] totalmax = new double [frapframes];
		double [] wholemean = new double [frapframes];
		
		
		//MEASURE THE BLEACHED SPOT

		for (int y=0; y<frapframes-1; y++){
			int xpos = (int) (Math.abs(xval) - (ROIdiameter/2));
			int ypos = (int) (Math.abs(yval) - (ROIdiameter/2));
			FinalFRAP.setRoi(new OvalRoi(xpos, ypos, ROIdiameter, ROIdiameter));
			FinalFRAP.setSlice(y);
			
			IJ.setThreshold(FinalFRAP, threshminimal, threshmaximal);
			
			IJ.run(FinalFRAP, "Analyze Particles...", "size=25-4500 pixel circularity=0.00-1.00 show=Nothing display clear include slice");
			rt = Analyzer.getResultsTable();
			int thevals = rt.getCounter();
			double testmean = 0;
			double testmax = 0;
			
			//Check its not the bleach frame
			if (thevals>0){
				testmean = rt.getValueAsDouble(1, 0);
				testmax = rt.getValueAsDouble(5, 0);
			}
			
			if(thevals>0){
				if (testmean!=testmax){
					rm.runCommand("Deselect");
					rm.runCommand("Delete");
					FinalFRAP.setRoi(new OvalRoi(xpos, ypos, ROIdiameter, ROIdiameter));
					IJ.run(FinalFRAP, "Analyze Particles...", "size=25-4500 pixel circularity=0.00-1.00 show=Nothing display clear include add slice");
				}
				if (testmean==testmax){
					BleachSlice = FinalFRAP.getCurrentSlice();
				}
			}
			
			boolean leavecoordinates = false;
			if (thevals == 0){
				rm.select(0);
				FinalFRAP.setSlice(y);
				IJ.setThreshold(FinalFRAP, 0, 4095);
				IJ.run(FinalFRAP, "Measure", "");
				rt = Analyzer.getResultsTable();
				thevals = rt.getCounter();
				leavecoordinates = true;
			}
			FinalFRAP.setRoi(new OvalRoi(xpos, ypos, ROIdiameter, ROIdiameter));

			double temparea = 0;
			double tempmean = 0;
			double xvalM = 0 ;
			double yvalM = 0 ;
			int counter = 0;
			double maxval = 0;
			for (int z=0; z < thevals; z++){
				thearea[z] = rt.getValueAsDouble(0, z);
				themean[z] = rt.getValueAsDouble(1, z);
				temparea = temparea+thearea[z];
				tempmean = tempmean+themean[z];
				counter++;
				xvalM = rt.getValueAsDouble(6, 0);
				yvalM = rt.getValueAsDouble(7, 0);
				double tempmaxval = ct.getValueAsDouble(5, 0);
				if (tempmaxval > maxval){
					maxval = tempmaxval;
				}
			}
			totalarea[y] = Math.round(temparea/counter-1);
			totalmean[y] = Math.round(tempmean/counter-1);
			totalmax[y] = maxval;
			
			double wholeMeanIntensity = WholeAreaBleach(xpos, ypos, ROIdiameter);
			wholemean[y] = wholeMeanIntensity;
			if (totalarea[y] > 1 && leavecoordinates==false){
				xval = xvalM;
				yval = yvalM;
			}
		}
		
		//MEASURE THE CELL BLEACH
		IJ.setTool("oval");
		IJ.resetThreshold(FinalFRAP);
		rm.runCommand("Deselect");
		rm.runCommand("Delete");
		new WaitForUserDialog("Draw", "Draw around region of cell for photobleach correction").show();
		ResultsTable bt = new ResultsTable();
		double [] photobleachmean = new double [frapframes];
		double [] photobleacharea = new double [frapframes];
		
		FinalFRAP.setSlice(1);
		for (int y=1; y<frapframes-1; y++){
			if (y!=BleachSlice){			
				FinalFRAP.setSlice(y);	
				IJ.setAutoThreshold(FinalFRAP, "Default dark");
				IJ.run(FinalFRAP, "Analyze Particles...", "size=25-4500 pixel circularity=0.00-1.00 show=Nothing display clear include slice");
				bt = Analyzer.getResultsTable();
				photobleachmean [y] = Math.round(bt.getValueAsDouble(1, 0));
				photobleacharea [y] = Math.round(bt.getValueAsDouble(0, 0));
				xval = rt.getValueAsDouble(6, 0);
				yval = rt.getValueAsDouble(7, 0);
				int xpos = (int) (Math.abs(xval) - (ROIdiameter/2));
				int ypos = (int) (Math.abs(yval) - (ROIdiameter/2));
				FinalFRAP.setRoi(new OvalRoi(xpos, ypos, ROIdiameter, ROIdiameter));
		}

}		

		//MEASURE THE BACKGROUND
		IJ.setTool("oval");
		IJ.resetThreshold(FinalFRAP);
		new WaitForUserDialog("Draw", "Draw around region of background").show();
		rm.addRoi(FinalFRAP.getRoi());
		ResultsTable dt = new ResultsTable();
		double [] backgroundmean = new double [frapframes];
		double [] backgroundarea = new double [frapframes];
		for (int y=1; y<frapframes-1; y++){
			rm.select(0);
			FinalFRAP.setSlice(y);
			MaxThresh = 4095;
			MinThresh = 0;
			IJ.setThreshold(FinalFRAP, MinThresh, MaxThresh);
			IJ.run(FinalFRAP, "Analyze Particles...", "size=25-40500 pixel circularity=0.00-1.00 show=Nothing display clear include slice");
			dt = Analyzer.getResultsTable();
			backgroundmean [y] = dt.getValueAsDouble(1, 0);
			backgroundarea [y] = dt.getValueAsDouble(0, 0);
		}
		
		outputtext(totalarea, totalmean, totalmax, wholemean, photobleachmean, photobleacharea, backgroundmean, backgroundarea, frapframes);
	}
	
	public double WholeAreaBleach(int xpos, int ypos, int ROIdiameter){
		double wholeMeanBleach = 0;
		FinalFRAP.setRoi(new OvalRoi(xpos, ypos, ROIdiameter, ROIdiameter));
		IJ.setThreshold(FinalFRAP, 0, 4095);
		IJ.run(FinalFRAP, "Analyze Particles...", "size=3-4500 pixel circularity=0.00-1.00 show=Nothing display clear include slice");
		ResultsTable wholet = new ResultsTable();
		wholet = Analyzer.getResultsTable();
		wholeMeanBleach = Math.round(wholet.getValueAsDouble(1, 0));
		
		return wholeMeanBleach;
	}
	
	
	public void outputtext(double [] totalarea, double [] totalmean, double [] totalmax, double [] wholemean, double [] photobleachmean, double [] photobleacharea, double [] backgroundmean, double [] backgroundarea, int frapframes){
	String CreateName = "C:\\Temp\\Results.txt";
	String FILE_NAME = CreateName;

	try{
		FileWriter fileWriter = new FileWriter(FILE_NAME,true);
		BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
		bufferedWriter.newLine();
		bufferedWriter.write("File= " + filename);
		bufferedWriter.newLine();
		for (int x=0; x<frapframes; x++){
			bufferedWriter.write( "Bleach Spot Area= " + totalarea[x] + " Bleach Spot Mean Intensity= " + totalmean[x] + " Bleach Max  Intensity = " + totalmax[x] + " Whole Mean Intensity= " + wholemean[x]);
			bufferedWriter.newLine();
		}
		for (int x=0; x<frapframes; x++){
			bufferedWriter.write( "Cell Photobleach Spot Area= " + photobleacharea[x] + " Cell Photobleach Mean Intensity= " + photobleachmean[x]);
			bufferedWriter.newLine();
		}
		for (int x=0; x<frapframes; x++){
			bufferedWriter.write( "Cell Background Spot Area= " + backgroundarea[x] + " Cell Background Mean Intensity= " + backgroundmean[x]);
			bufferedWriter.newLine();
		}
		
		
		bufferedWriter.close();
	}
	catch(IOException ex) {
        System.out.println(
            "Error writing to file '"
            + FILE_NAME + "'");
    }
	}
}
