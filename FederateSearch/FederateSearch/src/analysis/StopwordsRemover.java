package analysis;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

public class StopwordsRemover {
	
	private Set<String> stop_words = new HashSet<String>();
	private FileInputStream stopwd_fis = null;
	private BufferedReader stopwd_buf_rd = null;
	
	
	// YOU MUST IMPLEMENT THIS METHOD
	public StopwordsRemover( FileInputStream instream ) {
		// load and store the stop words from the fileinputstream with appropriate data structure
		// that you believe is suitable for matching stop words.
		this.stopwd_fis = instream;
		DataInputStream data_is = new DataInputStream(instream);
		this.stopwd_buf_rd = new BufferedReader(new InputStreamReader(data_is));
		
		init_stop_words();
	}
	
	// YOU MUST IMPLEMENT THIS METHOD
	public boolean isStopword( char[] word ) {
		// return true if the input word is a stopword, or false if not		
		return stop_words.contains(new String(word));
	}
	
	/**
	 * @Init stop words set from stop words file
	 * @method: init_stop_words
	 * @param: None
	 * @return: void 
	 */
	private void init_stop_words()
	{
		try
		{
			String _wd;
			
			while((_wd = stopwd_buf_rd.readLine()) != null)
			{
				if(!_wd.isEmpty())
				{
					stop_words.add(_wd);
				}				
			}
		}
		catch(IOException e)
		{}
	}
}
