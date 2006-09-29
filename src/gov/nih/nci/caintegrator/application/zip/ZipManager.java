/*
 * Created on Aug 23, 2005
 *
 *
 *
 * $Revision: 1.2 $
 * $Date: 2006-09-29 18:49:10 $
 * $Author: mholck $
 * $Name: not supported by cvs2svn $
 * $Id: ZipManager.java,v 1.2 2006-09-29 18:49:10 mholck Exp $
 */
package gov.nih.nci.caintegrator.application.zip;

import java.io.File;
import java.util.List;

import org.apache.log4j.Logger;


/**
 * @author Prashant Shah - NCICB/SAIC
 *
 * A wrapper class around the ZipFiles Class.  Also provides the zip status value for the AJAX code
 */
@SuppressWarnings("unused")
public class ZipManager extends Thread {
    
	// Total number of bytes to zip for all series
    private long totalBytesToZip = 0;
    
    // Total number of bytes zipped up to this point
    private long bytesZippedSoFar = 0;
    
    // Bytes zipped in the current zip file (there can be multiple zip files for large data sets)
    private long bytesZippedInCurrentZipFile = 0;
    
    // Name of first zip file created for the data
    private String destinationFile;
    
    // For a large data set, this number keeps track of which zip file is 
    // being zipped.  0 is the first file
    private int sequenceNumber = 0;
    
    // Set to true if resulting zip files should be broken up
    // so that they don't become too large
    private boolean breakIntoMultipleFileIfLarge = true;
    
    // Maximum size for a single zip file (4GB) 
    // Only applies if breakIntoMultipleFileIfLarge is true
    private static final long MAX_ZIP_FILE_SIZE = 4000000000L;

  

    // List of items to be zipped
    private List<ZipItem> items;

    private static Logger logger = Logger.getLogger(ZipManager.class);
    
    private static boolean finished = true;
       
    
    /**
     * Compute the percentage of series that have
     * been processed.  Used by the progress bar
     * 
     * @return
     */
    public Integer getPercentProcessed()
     {
        if (totalBytesToZip != 0) 
         {       
        	return new Double(bytesZippedSoFar * 100 /totalBytesToZip).intValue();
         }
        else
        	return 0;
    }
    
    /**
     * 
     * @param items
     */
    public void setItems(List<ZipItem>  items)
     {
        this.items = items;
     }
    
    /**
     * 
     * @param target
     */
    public void setTarget(String target)
     {
        this.destinationFile = target;
     }
          
    /**
     * Actually does the zipping of the files
     *
     */
    public List<String> zip() throws Exception
    {
		logger.info("Starting to zip: " + destinationFile);  
    	
        long startTime = System.currentTimeMillis();
        
       AbstractFileZipper zipit = FileZipperFactory.getInstance();         
        
        // Initialize zipper
        try
         {
	        zipit.startNewFile(destinationFile, sequenceNumber);
	        
	        // Loop through zip items
	        for(ZipItem zipItem : items)
	        {
	        	  zipit.zip(zipItem.getDirectoryInZip(), zipItem.getFilePath());
	              
	        }      
	        zipit.closeFile();   
         }
        catch (Exception e)
         {
            logger.error("Destination file "+destinationFile+" cannot be created ",e);
            throw e;
         }
        
        long endTime = System.currentTimeMillis();
        
        logger.info(" Total zipping time for file "+destinationFile+" ( "+totalBytesToZip+"bytes) was "+(endTime-startTime)+" ms."); 
        
        return zipit.getListOfZipFiles();
    }
    
    /**
     * 
     */
    public void run()
     {
    	finished = false;
    	
    	try
    	 {
          zip();
    	 }
    	catch(Exception e)
    	{
    		logger.error("Unable to complete zipping of file "+destinationFile, e);
    	}
    	
    	finished = true;
     }
       


    

    
	/**
	 * Process a file for zipping.   
	 * 
	 */
	private void zipFile(AbstractFileZipper zipit, String projectName,  String taskName, String fileName,String filePath, Long fileSize) throws Exception
	{
        // Build the path inside of the zip file based on values passed in		
		String path = projectName + File.separator +
		              taskName + File.separator +
		              fileName;
		       
	   // Possibly start a new zip file instead of 
	   // adding to the current one
	   if(breakIntoMultipleFileIfLarge) {
           // Determine the file size
		   // Only do this if there has been a sufficient amount
		   // of data added to the zip file.   
		   long zipFileSize = 0;
		   if(bytesZippedInCurrentZipFile > MAX_ZIP_FILE_SIZE) {
			   zipFileSize = zipit.getFileSize();
			   
			   long sizeWithThisFileIncluded = zipFileSize + fileSize;			   
			   
			   // See if the zip file would go over the limit if the 
			   // current file is added
			   if(sizeWithThisFileIncluded > MAX_ZIP_FILE_SIZE)
			   {
				   // Close the streams and finalize the zip file 
				   zipit.closeFile();
				   
				   // Start a new zip file, increasing the sequence number
				   zipit.startNewFile(destinationFile, ++sequenceNumber);
				   
				   // Reset the counter
				   bytesZippedInCurrentZipFile = 0;
			   }			   
			   
		   }
	   }

		zipit.zip(path, filePath);
		bytesZippedSoFar += fileSize;
		bytesZippedInCurrentZipFile  += fileSize;
	}
	
	/**
	 * Setter for breakIntoMultipleFileIfLarge
	 * 
	 * @param flag
	 */
	public void setBreakIntoMultipleFileIfLarge(boolean flag) {
		breakIntoMultipleFileIfLarge = flag;
	}

	public static boolean isFinished() {
		return finished;
	}
	
	 
	
}
