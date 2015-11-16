package index;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ExternalMergeSort {
	
	static int MAX_ITEMS_IN_CHUNK = 100000; // max items the memory buffer can hold
	 
	/**
	 * External sort string in a file
	 * @param inputFileName
	 * @param outputFileName
	 */
	public void externalSortTextFile(String inputFileName, String outputFileName, boolean is_int)
	{
		String tfile = "temp-file-";
		ArrayList<String> buffer = new ArrayList<String>();
		
		try
		{
			File	   inputFile = new File(inputFileName);
			FileReader fr = new FileReader(inputFile.getAbsoluteFile());
			BufferedReader br = new BufferedReader(fr);
			boolean is_eof = false;
			int i, j;
			i = j = 0;
			
			// Iterate through the elements in the file
			while(true)
			{
				// Read MAX_ITEMS_IN_CHUNK-element chunk at a time from the file
				for (j = 0; j < MAX_ITEMS_IN_CHUNK; j++)
				{
					String t = br.readLine();
					if (t != null)
					{
						buffer.add(t);
					}
					else
					{
					 	is_eof = true;
					 	break;					 	
					}
				}
				// Sort MAX_ITEMS_IN_CHUNKM elements
				String ar_buffer[];
				if(is_int)
				{
					Integer in_buffer[] = buffer.toArray(new Integer[0]);
					Arrays.sort(in_buffer);
					
					int a_size = buffer.size();
					ar_buffer = new String[a_size];
					for(i = 0; i < a_size; i++)
					{
						ar_buffer[i] = in_buffer[i].toString();
					}
				}
				else
				{
					String str_buffer[] = buffer.toArray(new String[0]);
					Arrays.sort(str_buffer);
					ar_buffer = str_buffer;
				}								
								
				// Write the sorted data to temp file
				FileWriter fw = new FileWriter(inputFile.getParent() + tfile + Integer.toString(i) + ".tmp");
				BufferedWriter bw = new BufferedWriter(fw);
				for (int k = 0; k < j; k++)
				{
				 	bw.write(ar_buffer[k]);
				 	bw.newLine();
				}
				// Increase number of chunk files
				i++;
				
				// Close and clean resource
				bw.close();
				fw.close();
				buffer.clear();
				
				// If reached the EOF, break
				if(is_eof)
					break;
			}
			
			br.close();
			fr.close();
				
			// merge the sorted chunk files
			externalMergeTextFile(inputFile.getParent(), tfile + "*.tmp", outputFileName, is_int);
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Merge the temporary sorted files into a large one
	 * @param _dir
	 * @param pattern
	 * @param outputFileName
	 */
	public void externalMergeTextFile(String _dir, String pattern, String outputFileName, boolean is_int)
	{
		// Get all temporary files needed to be merged according to the pattern
		File dir = new File(_dir);
		final String _pattern = pattern;
		File [] files = dir.listFiles(new FilenameFilter() {
		    @Override
		    public boolean accept(File dir, String name) {
		        return name.matches(_pattern);
		    }
		});

		int chunks_files_num = files.length;
		
		try
		{
			int i, j;
			i = j = 0;
			
			// Open each chunk file and merge them, then write back to disk
			Map<Integer, String> top_str = new HashMap<Integer, String>();
			BufferedReader[] brs = new BufferedReader[chunks_files_num];
			int remaining_files = chunks_files_num;
						
			for (i = 0; i < chunks_files_num; i++)
			{
				 brs[i] = new BufferedReader(new FileReader(files[i].getAbsoluteFile()));
				 String t = brs[i].readLine();
				 if (t != null)
				 	top_str.put(i, t);
				 else
				 {
				 	brs[i].close();
				 	brs[i] = null;
				 	files[i].delete();
				 	remaining_files--;
				 }
			}
			
			FileWriter fw = new FileWriter(outputFileName);
			BufferedWriter bw = new BufferedWriter(fw);
			
			// Keep reading and merge untill all chunk files are handled			
			while(true)
			{
				Integer i_key[] = top_str.keySet().toArray(new Integer[0]);
				
				String min_str = top_str.get(i_key[0]);
				int minFile = i_key[0].intValue();
				int d_size = i_key.length;
				 
				for (j = 0; j < d_size; j++)
				{
					String _val = top_str.get(i_key[j]);
					if(is_int)
					{
						if(Integer.parseInt(min_str.split("\t")[0]) > Integer.parseInt(_val.split("\t")[0]))
						{
							min_str = _val;
							minFile = i_key[j].intValue();
						}
					}
					else
					{
						if(min_str.split("\t")[0].compareTo(_val.split("\t")[0]) > 0)
						{
							min_str = _val;
							minFile = i_key[j].intValue();
						}
					}
				 }
			 
				 bw.write(min_str);
				 bw.newLine();
				 String t = null;
				 if(brs[minFile] != null)
				 {
					 t = brs[minFile].readLine();
				 
					 if(t != null)
					 	top_str.put(minFile, t);
					 else
					 {
					 	top_str.remove(minFile);
					 	brs[minFile].close();
					 	brs[minFile] = null;
					 	files[minFile].delete();
					 	remaining_files--;
					 }
				 }
			 
				 // check if all chunk files are handled
				 if(remaining_files < 1)
				 {
					 break;
				 }
			}
			
			bw.close();
			fw.close();
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Find the doc No. and ID pair from the sorted document NO. & ID map files
	 * matching by doc No. or Id, using a binary search.
	 * 
	 * @param String file_name
	 *            the file to search.
	 * @param String target
	 *            the target to find.
	 * @return String 
	 *         Found document No. and Id pair
	 * @throws IOException
	 */
	public String[] binarySearchTextFile(RandomAccessFile file, String target, boolean is_int)
	        throws IOException 
	{
	    /*
	     * because we read the second line after each seek there is no way the
	     * binary search will find the first line, so check it first.
	     */
		String rv[] = null;
		
		if(file == null) return rv;
		
		file.seek(0);
	    String line = file.readLine();
	    if (line == null) 
	    {
	    	return rv;
	    }
	    else
	    {
		    rv = line.split("\t");
		    
		    if(!is_int && rv[0].compareToIgnoreCase(target) == 0)
		    {
		    	return rv;
		    }
		    
		    if(is_int && Integer.parseInt(rv[0]) == Integer.parseInt(target))
		    {
		    	return rv;
		    }
	    }
	    
	    // Start binary searching
	    rv = null;
	    long beg = 0;
	    long end = file.length();
	    while (beg <= end) {
	        //get the mid point
	        long mid = beg + (end - beg) / 2;
	        file.seek(mid);
	        // Skip current line for we may be not at the beginning of the line
	        file.readLine();
	        line = file.readLine();

	        if(line == null)
	        {
	        	return rv;
	        }
	        
	        rv = line.split("\t");
	        int cmp_r;
	        if(is_int)
	        {
	        	cmp_r = Integer.parseInt(rv[0]) - Integer.parseInt(target);		        
	        }
	        else
	        {
	        	cmp_r = rv[0].compareTo(target);	        	
	        }
	        
	        if(cmp_r > 0) 
	        {
	            // Mid term is greater than the target, so look before it.
	            end = mid - 1;
	        } 
	        else if(cmp_r < 0)
	        {
	            //Mid term is less than the target, look after it.
	            beg = mid + 1;
	        }
	        else
	        {
	        	// Find it
	        	return rv;
	        }
	    }

	    //The search falls through when the range is narrowed to nothing.
	    file.seek(0);
	    rv = null;
	    return rv;
	}
	
	
	/**
	 * Merge the temporary sorted files into a large one
	 * @param _dir
	 * @param pattern
	 * @param outputFileName
	 */
	public void externalMergeBinaryFile(String _dir, String pattern, String outputFileName, String outAlphIndexFile)
	{
		// Get all temporary files needed to be merged according to the pattern
		File dir = new File(_dir);
		final String _pattern = pattern;
		File [] files = dir.listFiles(new FilenameFilter() {
		    @Override
		    public boolean accept(File dir, String name) {
		        return name.matches(_pattern);
		    }
		});

		DataOutputStream bw = null;
		RandomAccessFile   ix_raf = null;
		FileOutputStream   ix_fos = null;
		ObjectOutputStream bw_alpha = null;
		
		int chunks_files_num = files.length;
		long file_pos = 0;
		TermHeader term_header = new TermHeader();
		
		try
		{
			int i, j;
			i = j = 0;
			
			// Open each chunk file and merge them, then write back to disk
			Map<Integer, TermObject> top_to = new HashMap<Integer, TermObject>();
			DataInputStream[] brs = new DataInputStream[chunks_files_num];
			int remaining_files = chunks_files_num;
						
			for (i = 0; i < chunks_files_num; i++)
			{
				 brs[i] = new DataInputStream(new BufferedInputStream(
						 		new FileInputStream(
						 				new RandomAccessFile(files[i].getAbsoluteFile(), "r").getFD())));
				 
				 TermObject t = null;
				 try
				 {
					 t = (TermObject)ExternalMergeSort.read_term_object(brs[i]);
					 top_to.put(i, t);
				 }
				 catch(ClassNotFoundException e)
				 {					 
				 }
				 catch(EOFException ex)	// End of file
				 {
					brs[i].close();
				 	brs[i] = null;
				 	files[i].delete();
				 	remaining_files--;				
				 }
			}
			
			// Index file
			ix_raf = new RandomAccessFile(outputFileName, "rw");
			ix_fos = new FileOutputStream(ix_raf.getFD());
			bw = new DataOutputStream(new BufferedOutputStream(
					ix_fos));
			
			// Alphabet index file
			bw_alpha = new ObjectOutputStream(new BufferedOutputStream(
			          new FileOutputStream(outAlphIndexFile)));
			
			// Keep reading and merge untill all chunk files are handled			
			while(true)
			{
				Integer i_key[] = top_to.keySet().toArray(new Integer[0]);
				
				TermObject min_to = top_to.get(i_key[0]);
				int minFile = i_key[0].intValue();
				int d_size = i_key.length;
				 
				for (j = 1; j < d_size; j++)
				{
					TermObject _val = top_to.get(i_key[j]);
					
					if(min_to.compareTo(_val) > 0)
					{
						min_to = _val;
						minFile = i_key[j].intValue();
					}
					
					if(min_to.compareTo(_val) == 0)
					{
						// Merge the same token
						min_to.addObject(_val);

						TermObject t = null;
						if(brs[i_key[j].intValue()] != null)
						{
							 try
							 {
								 t = (TermObject)ExternalMergeSort.read_term_object(brs[i_key[j].intValue()]);
								 top_to.put(i_key[j], t);
								 j--;
							 }
							 catch(ClassNotFoundException e)
							 {
								 top_to.remove(i_key[j]);
							 }
							 catch(EOFException ex)
							 {
								top_to.remove(i_key[j]);
							 	brs[i_key[j].intValue()].close();
							 	brs[i_key[j].intValue()] = null;
							 	files[i_key[j].intValue()].delete();
							 	remaining_files--;
							 }
						 }
					}
				 }
			 
				 // Record the file position of each alphabet
            	 char f_c = min_to.getTerm().toCharArray()[0];
	             f_c = Character.isAlphabetic(f_c) ? f_c : '-';
	             if(term_header.getPosition(f_c) == -1)
	             {
	            	 bw.flush();
	            	 ix_raf.getFD().sync();
	            	 file_pos = ix_raf.getFilePointer();
	            	 term_header.setPosition(f_c, file_pos);
	             }
	             
	             // Write object to the final inverted index file
            	 //bw.writeObject(min_to);
	             ExternalMergeSort.add_term_object(bw, min_to);
            	 
            	 TermObject t = null;
				 if(brs[minFile] != null)
				 {
					 try
					 {
						 t = (TermObject)ExternalMergeSort.read_term_object(brs[minFile]);
						 top_to.put(minFile, t);
					 }
					 catch(ClassNotFoundException e)
					 {
						 top_to.remove(minFile);
					 }
					 catch(EOFException ex)
					 {
						top_to.remove(minFile);
					 	brs[minFile].close();
					 	brs[minFile] = null;
					 	files[minFile].delete();
					 	remaining_files--;
					 }
				 }
			 
				 // check if all chunk files are handled
				 if(remaining_files < 1)
				 {
					 break;
				 }
			}
			
			// Write out alphabet index file
			bw_alpha.writeObject(term_header);
			bw_alpha.flush();			
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		finally
		{
			// Close files
			try 
			{
				// Flush content
				bw.flush();
				bw.close();
				ix_raf.close();
			} catch (IOException e) 
			{
				e.printStackTrace();
			}			
			
			try 
			{
				bw_alpha.close();
			}
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		}
	}
	
	/** Adds this object to the store 
	 * 
	 */  
	public static void add_term_object(DataOutputStream raf, Object o) throws IOException
	{    
		  ByteArrayOutputStream bos = new ByteArrayOutputStream();  
		  ObjectOutputStream oos = new ObjectOutputStream(bos);  
		  oos.writeObject(o);  
		  byte[] buffer = bos.toByteArray();  
		  
		  raf.writeInt(buffer.length);
		  raf.write(buffer);
	}
	
	// does the actual byte to Object comversion for getObject(int) and getObjects()  
	public static Object read_term_object(DataInputStream raf) throws IOException,ClassNotFoundException
	{  
		  int bytes = raf.readInt();  
		  byte[] buff = new byte[bytes];  
		  raf.readFully(buff);  
		  ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(buff));  
		  return ois.readObject();
	}  

}
