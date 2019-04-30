# FRAPMeasure
Small ImageJ plugin to automate the measurement of fluorescence recovery after photobleaching data

INSTALL

    Ensure that the ImageJ version is at least 1.5 and the installation has Java 1.8.0_60 (64bit) installed. If not download the latest version of ImageJ bundled with Java and install it.

    The versions can be checked by opening ImageJ and clicking Help then About ImageJ.

    Download the latest copy of Bio-Formats into the ImageJ plugin directory

    Create a directory in the C: drive called Temp (case sensitive)

    Using notepad save a blank .txt file called Results.txt into the Temp directory you previously created (also case sensitive).

    Place FRAP_Measurements_.jar into the plugins directory of your ImageJ installation.

    If everything has worked FRAP_Measure should be in the Plugins menu.

    FRAP_Measure.java is the editable code for the plugin should improvements or changes be required.

USAGE

    You will be prompted to Open FRAP Images. The plugin was written for 1 channel FRAP images with Z, the channel colour shouldn't matter.

    When the Bio-Formats dialogue opens make sure that nothing is ticked.

    Once the images have opened you will be prompted to select the bleached spot by drawing a circular ROI around it, make it a bit larger than the spot.
    
    The plugin will track movements of the spot between frames as long as the shift isn't too large
    
    You will then be prompted to draw a region somewhere in the cell for cell photobleach measurements, make sure the region has something to measure otherwise the plugin will fail.

    Repeat the ROI selection in an area of background with no cells in it for background correction.

    Results are saved to the text file you should have created in C:\Temp
    
    If your images do not have any Z elements you can comment it out of the .java file and recompile.
