package uwsimresgen;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;


public class OutputLog {

	private String fileprefix;
	private String filename;
	private String filepath = "";
	private FileOutputStream fos = null;
	private File file;
		
	public OutputLog( String fileprefix )  { 
		this.fileprefix = fileprefix;
		java.util.Date date = new java.util.Date();
		SimpleDateFormat sft = new SimpleDateFormat("MM_dd_yyyy_HH_mm_ss_SSSS");
		String formattedDate = sft.format(date);
		
		File file = new File("logs/");
		
		if( !file.exists() ) {
			file.mkdir();
		} else {
			System.out.println("directory exists");
		}
		
		file = null;
		
		this.filename = "logs/" + this.fileprefix + "_" + formattedDate + ".txt";
				
		this.clearFile();
		this.outputStringAndNewLine("UWSimResGen: Log File");
	}
	
	private void writeString( String str, Boolean append ) {
		try {				
			file = new File( filename );
			
			fos = new FileOutputStream(file, append);
			
			if( !file.exists() ) {				
				file.mkdirs();
				file.createNewFile();
			}
			
			byte[] output = str.getBytes();
			fos.write(output);
			fos.flush();
			fos.close();
			
			file = null;
			fos = null;
			
		} catch ( IOException e ) {
			e.printStackTrace();			
		} finally {
			try {
				if( fos != null ) {
					fos.close();
					fos = null;
				}
				
				if( file != null ) {
					file = null;
				}
			} catch ( IOException e ) {
				e.printStackTrace();
			}
		}
	}
	
	private void clearFile() {
		this.writeString("", false);
	}
	
	/**
	 * Appends given string to log file.
	 * @param str the string to output.
	 */
	public void outputString( String str ) {
		this.writeString(str, true);
	}
	
	/**
	 * Appends given string and new line to log file. 
	 * @param str the string to output.
	 */
	public void outputStringAndNewLine( String str ) {
		outputString(str + newline);
	}
	
	
	public String getFilePath() { return this.filename; }
	
	public static final String newline = System.getProperty("line.separator");
}