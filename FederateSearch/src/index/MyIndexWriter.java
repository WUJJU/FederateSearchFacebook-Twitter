package index;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.Externalizable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedOutputStream;
import java.io.BufferedInputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static java.nio.file.StandardCopyOption.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import analysis.TextNormalizer;
import analysis.TextTokenizer;



public class MyIndexWriter {
	
	protected File dir;
	
	
	private boolean is_remove_stopwords = false;
	
	private static final String TERM_INDEX_FILE = "trec_index.dat";
	private static final String TERM_ALPHABET_INDEX_FILE = "trec_index.idx";
	private static final String DOCNO_ID_MAP_FILE = "trec_docno_id_mapping.dat";
	private static final String DOCID_NO_MAP_FILE = "trec_docid_no_mapping.dat";
	private static final String DOC_TERM_COUNT_FILE="doc_term_count.dat";
	private static final int MAX_TERM_IN_CHUNK = 500000;
	private static final int MAX_DOC_ID_IN_CHUNK = 500000;
	
	// Store term dictionary in memory as HashMap: 
	//   <term1, TermObject>, TermObject stores term, collection frequency, 
	//   document frequency, postings<docid1, [pos1, pos2,...]>
	protected Map<String, TermObject> term_dict = new HashMap<String, TermObject>();
	protected TermHeader term_header = new TermHeader();
	protected Map<String, Integer> docno_id_dict = new HashMap<String, Integer>();

	protected int term_dict_files_number = -1;
	protected int docno_id_files_number = -1;
	protected int doc_term_count_files_number=-1;
	protected int cur_doc_id = -1;
	
	private static boolean DEBUG = true;
	private long start_time;
	private long end_time;
	
	public MyIndexWriter( File dir ) throws IOException {
		this.dir = dir;
		
		
		start_time = System.currentTimeMillis();		
	}
	
	
	public MyIndexWriter(String path_dir){
	    this.dir=new File(path_dir);
	    if( !this.dir.exists() ) {
                this.dir.mkdir();
        }
	}
	/**
	 * This method build index for each document.
	 * NOTE THAT: in your implementation of the index, you should transform your string docnos into non-negative integer docids !!!
	 * In MyIndexReader, you should be able to request the integer docid for docnos.
	 * 
	 * @param docno Docno
	 * @param tokenizer A tokenizer that iteratively gives out each token in the document.
	 * @throws IOException
	 */
	public void index( String docno, TextTokenizer tokenizer ) throws IOException {
		// you should implement this method to build index for each document
		
		// Create document No. and Id Mapping files
		int doc_id = create_docno_id_mapping(docno);
		
		
		// Create inverted index file
		char word[];
		int word_pos = 0;
		
		if(DEBUG)
		{
			System.out.println("Start indexing doc " + docno + " at " + current_datetime() );
		}
		while( ( word=tokenizer.nextWord() ) != null ) { // iteratively loading each word from the document
			word = TextNormalizer.normalize(word); // normalize each word
			if( !this.is_remove_stopwords  )
			{
				// if the word is not a stopword, cope with it
				create_term_index(new String(word), doc_id, word_pos);
			}
			word_pos++;
		}
		if(DEBUG)
		{
			end_time = System.currentTimeMillis();
			System.out.println("Finished indexing doc " + docno + " at " + current_datetime());
		}
		
	}
	
	/**
	 * set remove stop words or not
	 */
	public void set_is_remove_stopwords(boolean _remove)
	{
		this.is_remove_stopwords = _remove;
	}
	
	/**
	 * Close the index writer, and you should output all the buffered content (if any).
	 * @throws IOException
	 */
	public void close() throws IOException {
		// you should implement this method if necessary
		
		// Flush temporary Doc No. ID mapping files to the merged one
		// If there're more than 1 chunk files, merge them, index base is 0
		flush_docno_id_mapping_buffer();
		
		if(DEBUG)
		{
			end_time = System.currentTimeMillis();
			System.out.println("Finished indexing all docs "  
								+ (end_time-start_time)/1000 + "s at " + current_datetime());
			
			start_time = System.currentTimeMillis();
			System.out.println("Merge indexing file " + "at " + current_datetime());
		}
		
		// Flush term index buffered content to file, if needed, do external merge into 
		// the one index file
		flush_term_dict_buffer();
		
		if(DEBUG)
		{
			end_time = System.currentTimeMillis();
			System.out.println("Finished creating merged indexing file in " 
							+ (end_time-start_time)/1000 + "s at " + current_datetime());
		}
	}
	
	/**
	 * Create term index
	 * @param term
	 * @param doc_id
	 */
	public void create_term_index(String term, int doc_id, int word_pos)
	{
		// First check if reaching the maximum number of term items in memory
		// If greater than MAX_TERM_IN_CHUNK, write to chunk file
		if(this.term_dict.size() >= MAX_TERM_IN_CHUNK)
		{
			String term_keys[] = this.term_dict.keySet().toArray(new String[0]);
			Arrays.sort(term_keys, new Comparator<String>() 
			{
	            @Override
	            public int compare(String t_one, String t_two) 
	            {
	            	// If the first character is not alphabet, assume it greater than any
	            	// alphabet leading term
	            	return new TermObject(t_one).compareTo(new TermObject(t_two));						                
	            }
			});

			// Increase term index file number
			this.term_dict_files_number++;
			
			// Write to term chunk file
			this.write_term_chunk_index(term_keys, false);
			
			// Empty temporary term index container
			this.term_dict.clear();
		}
		
		// Add term to term dictionary for temporary storing
		// If term is not in current term dictionary
		TermObject _to = this.term_dict.get(term);
		if(_to == null)
		{
			_to = new TermObject(term);
		}
		_to.addCollectionFreq();
		_to.addDocFreq(doc_id);
		_to.addPostings(doc_id, word_pos);
		this.term_dict.put(term, _to);		
	}
	
	/**
	 * Flush the buffer content to doc No. and Id mapping file if still data there
	 * @param String docno
	 * @throws IOException 
	 */
	public void flush_term_dict_buffer() throws IOException
	{
		// First flush the buffered data into temporary file
		if(this.term_dict.size() > 0)
		{
			this.term_dict_files_number++;
			String term_keys[] = this.term_dict.keySet().toArray(new String[0]);
			Arrays.sort(term_keys, new Comparator<String>() 
			{
	            @Override
	            public int compare(String t_one, String t_two) 
	            {
	            	// If the first character is not alphabet, assume it greater than any
	            	// alphabet leading term
	            	return new TermObject(t_one).compareTo(new TermObject(t_two));						                
	            }
			});
			
			// Write term index into chunk file
			this.write_term_chunk_index(term_keys, this.term_dict_files_number == 0);
		}
		
		if(this.term_dict_files_number == -1) return;
		
		// If this.term_dict_files_number == 0, means we don't need to call external merge 
		// method to create mapping file		
		if(this.term_dict_files_number == 0)
		{
			// Only one file here
			// Just move it to the correct filename
			File o_index_file = new File(this.dir.getAbsolutePath() + File.separator 
					+ TERM_INDEX_FILE + "." + this.term_dict_files_number);
			File n_index_file = new File(this.dir.getAbsolutePath() + File.separator + TERM_INDEX_FILE);
			MyIndexWriter.mv_file(o_index_file, n_index_file);
		}
		else
		{
			ExternalMergeSort extMerge = new ExternalMergeSort();
			
			// Merge chunk indexfing files into a large one
			extMerge.externalMergeBinaryFile(this.dir.getAbsolutePath(), 
					TERM_INDEX_FILE + ".[0-9]+", this.dir.getAbsolutePath() + File.separator  + TERM_INDEX_FILE,
					this.dir.getAbsolutePath() + File.separator  + TERM_ALPHABET_INDEX_FILE);
		}
		
		// Empty the temporary container
		this.term_dict.clear();
	}
	
	/**
	 * Write term index that have been filled the buffer into temporary file
	 * @param sorted_terms
	 * @throws FileNotFoundException 
	 */
	public void write_term_chunk_index(String sorted_terms[], boolean write_alpha_index)
	{
		String filename = this.dir.getAbsolutePath() + File.separator + TERM_INDEX_FILE;
		long	file_pos = 0;
		
		if(this.term_dict_files_number > -1)
		{
			// File is so huge, we need temporary files
			filename += "." + this.term_dict_files_number;
		}
		
		DataOutputStream bufferedWriter = null;
		RandomAccessFile index_raf = null;
		FileOutputStream index_fos = null;
		
		ObjectOutputStream alphaWriter = null;
		
		try 
		{
            //Construct the BufferedWriter object
			index_raf = new RandomAccessFile(filename, "rw");
			index_fos = new FileOutputStream(index_raf.getFD());
			bufferedWriter = new DataOutputStream(new BufferedOutputStream(index_fos
  		          ));
			            
            //Start writing Doc No. and ID mapping to file
            int ct_keys = sorted_terms.length;
            for(int i = 0; i < ct_keys; ++i)
            {
            	if(sorted_terms[i] == null || sorted_terms[i].isEmpty()) continue;
            	
            	TermObject _to = this.term_dict.get(sorted_terms[i]);
            	
            	// Should construct alphabet index flie
	            if(write_alpha_index)
	            {
	            	char f_c = sorted_terms[i].toCharArray()[0];
	            	f_c = Character.isAlphabetic(f_c) ? f_c : '-';
	            	if(this.term_header.getPosition(f_c) == -1)
	            	{
	            		bufferedWriter.flush();
	            		index_raf.getFD().sync();
	            		file_pos = index_raf.getFilePointer();
	            		this.term_header.setPosition(f_c, file_pos);
	            	}
	            }
	       
	            //bufferedWriter.writeObject(_to);
	            ExternalMergeSort.add_term_object(bufferedWriter, _to);
            }
        } 
		catch (FileNotFoundException ex) 
		{
            ex.printStackTrace();
        } 
		catch (IOException ex) 
        {
            ex.printStackTrace();
        } 
		finally 
		{
            //Close the BufferedWriter
            try 
            {
            	if (bufferedWriter != null) 
                {
                    bufferedWriter.flush();
                    bufferedWriter.close();
                }
            	
            	if(index_raf != null) index_raf.close();
            	
                // Write alphabetic index file
                if(write_alpha_index)
                {
                	alphaWriter = new ObjectOutputStream(new BufferedOutputStream(
            		          new FileOutputStream(this.dir.getAbsolutePath() + File.separator + TERM_ALPHABET_INDEX_FILE)));
                	alphaWriter.writeObject(this.term_header);
                	alphaWriter.flush();
                	alphaWriter.close();
                }
            } 
            catch (IOException ex) 
            {
                ex.printStackTrace();
            }
        }
	}
	
		
	/**
	 * Generate the reflection between document No. and document ID
	 * @param String docno
	 */
	public int create_docno_id_mapping(String docno)
	{
		// First check if reaching the maximum number of doc, 
		// if greater than MAX_DOC_ID_IN_CHUNK, write to file
		if(this.docno_id_dict.size() >= MAX_DOC_ID_IN_CHUNK)
		{
			String docno_keys[] = this.docno_id_dict.keySet().toArray(new String[0]);
			Arrays.sort(docno_keys);
			
			// Increase docno id files number
			this.docno_id_files_number++;
			
			// Write doc No. and ID mapping chunk file
			this.write_docno_id_mapping_chunk(docno_keys);
			
			// write doc id and No. mapping chunk file
			this.write_docid_no_mapping_chunk();
			
			// Empty the temporary container
			this.docno_id_dict.clear();
		}
		
		this.cur_doc_id++;
		this.docno_id_dict.put(docno, this.cur_doc_id);
		
		return this.cur_doc_id;
	}
	
	/**
	 * Flush the buffer content to doc No. and Id mapping file if still data there
	 * @param String docno
	 * @throws IOException 
	 */
	public void flush_docno_id_mapping_buffer() throws IOException
	{
		// First flush the buffered data into temporary file
		if(this.docno_id_dict.size() > 0)
		{
			this.docno_id_files_number++;
			String docno_keys[] = this.docno_id_dict.keySet().toArray(new String[0]);
			Arrays.sort(docno_keys);
			
			// Write doc No. and ID mapping chunk file
			this.write_docno_id_mapping_chunk(docno_keys);
			
			// write doc id and No. mapping chunk file
			this.write_docid_no_mapping_chunk();
			
			// Empty the temporary container
			this.docno_id_dict.clear();
		}
		
		if(this.docno_id_files_number == -1) return;
		
		// If this.docno_id_files_number == 0, means we don't need to call external merge 
		// m`		
		if(this.docno_id_files_number == 0)
		{
			// Only one file here
			// Just move it to the correct filename
			File o_noid_map_file = new File(this.dir.getAbsolutePath() + File.separator 
					+ DOCNO_ID_MAP_FILE + "." + this.docno_id_files_number);
			File n_noid_map_file = new File(this.dir.getAbsolutePath() + File.separator + DOCNO_ID_MAP_FILE);
			MyIndexWriter.mv_file(o_noid_map_file, n_noid_map_file);			
			
			// Just move it to the correct filename
			File o_idno_map_file = new File(this.dir.getAbsolutePath() + File.separator 
					+ DOCID_NO_MAP_FILE + "." + this.docno_id_files_number);
			File n_idno_map_file = new File(this.dir.getAbsolutePath() + File.separator + DOCID_NO_MAP_FILE);
			MyIndexWriter.mv_file(o_idno_map_file, n_idno_map_file);
		}
		else
		{
			ExternalMergeSort extMerge = new ExternalMergeSort();
			
			// Merge Document No. and Id mapping file
			extMerge.externalMergeTextFile(this.dir.getAbsolutePath(), 
								DOCNO_ID_MAP_FILE + ".[0-9]+", this.dir.getAbsolutePath() + File.separator  + DOCNO_ID_MAP_FILE,
								false);
			
			// Merge Document Id and No. mapping file
			extMerge.externalMergeTextFile(this.dir.getAbsolutePath(), 
					DOCID_NO_MAP_FILE + ".[0-9]+", this.dir.getAbsolutePath() + File.separator  + DOCID_NO_MAP_FILE,
					true);
			
		}
	}
	
	/**
	 * Write sorted document No. and ID mapping to chunk file
	 * @param String[] sorted document No. keys
	 * 
	 */
	public void write_docno_id_mapping_chunk(final String docno_keys[])
	{
		BufferedWriter bufferedWriter = null;
		String filename = this.dir.getAbsolutePath() + File.separator + DOCNO_ID_MAP_FILE;
		
		if(this.docno_id_files_number > -1)
		{
			// File is so huge, we need temporary files
			filename += "." + this.docno_id_files_number;
		}
		
		try 
		{
            //Construct the BufferedWriter object
            bufferedWriter = new BufferedWriter(new FileWriter(filename));
            
            //Start writing Doc No. and ID mapping to file
            int ct_keys = docno_keys.length;
            for(int i = 0; i < ct_keys; ++i)
            {
	            bufferedWriter.write(docno_keys[i] + "\t" + this.docno_id_dict.get(docno_keys[i]));
	            bufferedWriter.newLine();
            }
        } 
		catch (FileNotFoundException ex) 
		{
            ex.printStackTrace();
        } 
		catch (IOException ex) 
        {
            ex.printStackTrace();
        } 
		finally 
		{
            //Close the BufferedWriter
            try 
            {
                if (bufferedWriter != null) 
                {
                    bufferedWriter.flush();
                    bufferedWriter.close();
                }
            } 
            catch (IOException ex) 
            {
                ex.printStackTrace();
            }
        }
	}
	
	/**
	 * write doc term count to index
	 *
	 */
	public void write_doc_term_count(Map<String,Integer> mp)
        {
                BufferedWriter bufferedWriter = null;
                String filename = this.dir.getAbsolutePath() + File.separator +  DOC_TERM_COUNT_FILE;
                
                if(this.doc_term_count_files_number > -1)
                {
                        // File is so huge, we need temporary files
                        filename += "." + this.doc_term_count_files_number;
                }
                
                try 
                {
            //Construct the BufferedWriter object
            bufferedWriter = new BufferedWriter(new FileWriter(filename));
            
            //write mp into index
            Iterator it = mp.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                bufferedWriter.write(pair.getKey() + "\t" + pair.getValue());
                bufferedWriter.newLine();
           
               
            }
            
      
        } 
                catch (FileNotFoundException ex) 
                {
            ex.printStackTrace();
        } 
                catch (IOException ex) 
        {
            ex.printStackTrace();
        } 
                finally 
                {
            //Close the BufferedWriter
            try 
            {
                if (bufferedWriter != null) 
                {
                    bufferedWriter.flush();
                    bufferedWriter.close();
                }
            } 
            catch (IOException ex) 
            {
                ex.printStackTrace();
            }
        }
        }
	
	
	
	/**
	 * Write sorted document ID and No. mapping to chunk file
	 * @param void
	 * 
	 */
	public void write_docid_no_mapping_chunk()
	{
		BufferedWriter bufferedWriter = null;
		String filename = this.dir.getAbsolutePath() + File.separator + DOCID_NO_MAP_FILE;
		
		if(this.docno_id_files_number > -1)
		{
			// File is so huge, we need temporary files
			filename += "." + this.docno_id_files_number;
		}
		
		// Create Map of id to No.
		int doc_ids[] = new int[this.docno_id_dict.keySet().size()];
		Map<Integer, String> id_no_map = new HashMap<Integer, String>();
		Iterator it = this.docno_id_dict.keySet().iterator();
		
		int ix = 0;
		while(it.hasNext())
		{
			String it_k = (String)it.next();
			Integer it_v = this.docno_id_dict.get(it_k);
			id_no_map.put(it_v, it_k);
			doc_ids[ix++] = it_v.intValue();
		}
		
		// Sort the result by document ID
		Arrays.sort(doc_ids);
		
		try 
		{
            //Construct the BufferedWriter object
            bufferedWriter = new BufferedWriter(new FileWriter(filename));
            
            //Start writing Doc No. and ID mapping to file
            int ct_keys = doc_ids.length;
            for(int i = 0; i < ct_keys; ++i)
            {
	            bufferedWriter.write(doc_ids[i] + "\t" + id_no_map.get(doc_ids[i]));
	            bufferedWriter.newLine();
            }
        } 
		catch (FileNotFoundException ex) 
		{
            ex.printStackTrace();
        } 
		catch (IOException ex) 
        {
            ex.printStackTrace();
        } 
		finally 
		{
            //Close the BufferedWriter
            try 
            {
                if (bufferedWriter != null) 
                {
                    bufferedWriter.flush();
                    bufferedWriter.close();
                }
            } 
            catch (IOException ex) 
            {
                ex.printStackTrace();
            }
        }
	}
	
	///////////////////////////////////////////////////////////////////////////////////
	// Helper functions
	//////////////////////////////////////////////////////////////////////////////////
	/**
	* current_datetime : get current data time as a String
	* @param void
	* @return String
	*/
	private String current_datetime()
	{
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		return sdf.format(cal.getTime());
	}
	
	/**
	 * Move file to another place
	 * @param sourceFile
	 * @param destFile
	 * @throws IOException
	 */
	public static void mv_file(File sourceFile, File destFile) throws IOException 
	{
		if(destFile.exists()) 
		 {
			 destFile.delete();
		 }
		 sourceFile.renameTo(destFile);		
	}
}
