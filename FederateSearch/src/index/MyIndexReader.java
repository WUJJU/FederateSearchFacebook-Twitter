package index;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.RandomAccessFile;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A class for reading your index.
 */
public class MyIndexReader {
	
	protected File dir;
	protected RandomAccessFile file_docno_id_map = null;
	protected RandomAccessFile file_docid_no_map = null;
	
	protected DataInputStream term_index_ois = null;
	protected RandomAccessFile	term_index_raf = null;
	protected FileInputStream term_index_is = null;
	protected ObjectInputStream term_index_alpha_is = null;
	
	protected TermHeader	term_header = null;
	protected Map<String, TermObject> term_dict = new HashMap<String, TermObject>();
	
	private static final String TERM_INDEX_FILE = "trec_index.dat";
	private static final String TERM_ALPHABET_INDEX_FILE = "trec_index.idx";
	private static final String DOCNO_ID_MAP_FILE = "trec_docno_id_mapping.dat";
	private static final String DOCID_NO_MAP_FILE = "trec_docid_no_mapping.dat";
	
	public MyIndexReader( File dir ) throws IOException {
		this.dir = dir;
		
		this.term_index_raf = new RandomAccessFile(this.dir.getAbsolutePath() + File.separator + TERM_INDEX_FILE, "r");
		this.term_index_is = new FileInputStream(this.term_index_raf.getFD());
		
		this.term_index_alpha_is = new ObjectInputStream(
								new BufferedInputStream(
										new FileInputStream(this.dir.getAbsolutePath() + File.separator + TERM_ALPHABET_INDEX_FILE)));
		
		this.file_docno_id_map = new RandomAccessFile(this.dir.getAbsolutePath() + File.separator + DOCNO_ID_MAP_FILE, "r");
		this.file_docid_no_map = new RandomAccessFile(this.dir.getAbsolutePath() + File.separator + DOCID_NO_MAP_FILE, "r");
	}
	
	public MyIndexReader( String path_dir ) throws IOException {
		this( new File(path_dir) );
	}
	
	/**
	 * Get the (non-negative) integer docid for the requested docno.
	 * If -1 returned, it indicates the requested docno does not exist in the index.
	 * 
	 * @param docno
	 * @return
	 */
	public int getDocid( String docno ) throws IOException {
		// you should implement this method.
		
		// Use binary search to get the DocNo from the sorted mapping file
		ExternalMergeSort extBinSearch = new ExternalMergeSort();
		String rv[] = extBinSearch.binarySearchTextFile(this.file_docno_id_map, docno, false);
		
		return rv == null ? -1 : Integer.parseInt(rv[1]);
	}
	
	/**
	 * Retrive the docno for the integer docid.
	 * 
	 * @param docid
	 * @return
	 */
	public String getDocno( int docid ) throws IOException {
		// you should implement this method.
		
		// Use binary search to get the DocNo from the sorted mapping file
		ExternalMergeSort extBinSearch = new ExternalMergeSort();
		String rv[] = extBinSearch.binarySearchTextFile(this.file_docid_no_map, Integer.toString(docid), true);
		
		return rv == null ? null : rv[1];
	}
	
	/**
	 * Get the posting list for the requested token.
	 * 
	 * The posting list records the documents' docids the token appears and corresponding frequencies of the term, such as:
	 *  
	 *  [docid]		[freq]
	 *  1			3
	 *  5			7
	 *  9			1
	 *  13			9
	 * 
	 * ...
	 * 
	 * In the returned 2-dimension array, the first dimension is for each document, and the second dimension records the docid and frequency.
	 * 
	 * For example:
	 * array[0][0] records the docid of the first document the token appears.
	 * array[0][1] records the frequency of the token in the documents with docid = array[0][0]
	 * ...
	 * 
	 * NOTE that the returned posting list array should be ranked by docid from the smallest to the largest. 
	 * 
	 * @param token
	 * @return
	 */
	public int[][] getPostingList( String token ) throws IOException {
		// you should implement this method.
		
		TermObject to = this.findTermObject(token);
		
		if(to == null) return null;
		
		Map<Integer, ArrayList<Integer>> posting = to.getPostings();
		if(posting == null) return null;
				
		int rv[][] = new int[posting.size()][2];
		Iterator it = posting.keySet().iterator();
		
		int i = 0;
		while(it.hasNext())
		{
			Integer _it_k = (Integer)it.next();
			rv[i][0] = _it_k.intValue();
			rv[i][1] = posting.get(_it_k).size();
			i++;
		}
		
		return rv;
	}
	
	/**
	 * Return the number of documents that contains the token.
	 * 
	 * @param token
	 * @return
	 */
	public int DocFreq( String token ) throws IOException {
		// you should implement this method.
		int freq = 0;
		
		TermObject to = this.findTermObject(token);
		freq = to != null ? to.getDocFreq() : 0;
		
		return freq;
	}
	
	/**
	 * Return the total number of times the token appears in the collection.
	 * 
	 * @param token
	 * @return
	 */
	public long CollectionFreq( String token ) throws IOException {
		// you should implement this method.
		int ctf = 0;
		
		TermObject to = this.findTermObject(token);
		ctf = to != null ? to.getCollectionFreq() : 0;
		
		return ctf;
	}
	
	public void close() throws IOException {
		// you should implement this method when necessary
		
		// Release resource
		this.file_docno_id_map.close();
		this.file_docid_no_map.close();
		
		if(this.term_index_ois != null) this.term_index_ois.close();
		this.term_index_alpha_is.close();		
	}
	
	public void reset_index_buffered_reader() throws IOException
	{
		this.term_index_is.getChannel().position(0);
		this.term_index_raf.seek(0);
		
		this.term_index_ois = new DataInputStream( new BufferedInputStream(
				this.term_index_is));
		
	}
	
	/**
	 * get_doc_count from Doc No. & ID mapping file
	 * @return
	 * @throws IOException
	 */
	public int get_doc_count() throws IOException {
		int doc_count = 0;
        InputStream is = new BufferedInputStream(new FileInputStream(this.dir.getAbsolutePath() + File.separator + DOCID_NO_MAP_FILE));
	    
        try {
	        byte[] c = new byte[1024];
	        int readed_chars = 0;
	        boolean is_empty_file = true;
	        while ((readed_chars = is.read(c)) != -1) {
	        	is_empty_file = false;
	            for (int i = 0; i < readed_chars; ++i) {
	                if (c[i] == '\n')
	                    ++doc_count;
	            }
	        }
	        return (doc_count == 0 && !is_empty_file) ? 1 : doc_count;
	    } finally {
	        is.close();	        
	    }

	}
	/**
	 *  Try to locate the TermObject in inverted idnex file by token
	 * @param token
	 * @return
	 * @throws IOException
	 */
	public TermObject findTermObject(String token) throws IOException
	{
		TermObject _to = null;
		
		if(token == null || token.isEmpty())
			return _to;
		
		// Normalize the token
		token = token.toLowerCase();
		
		// Pre-check if has cached in memory
		_to = this.term_dict.get(token);
		if( _to != null)
		{
			return _to;
		}
		
		// First read alphabet based index
		try
		{
			if(this.term_header == null && this.term_index_alpha_is != null)
				this.term_header = (TermHeader)this.term_index_alpha_is.readObject();
		}
		catch(ClassNotFoundException ex)
		{}
		catch(EOFException ex)
		{}
		
		if(this.term_header != null)
		{
			// Get the first letter in token and find the offset in index file of this letter
			char f_c = token.toCharArray()[0];
			f_c = Character.isAlphabetic(f_c) ? f_c : '-';
			
			long offset = this.term_header.getPosition(f_c);
			if(offset > -1) // This letter is indexed in inverted index file
			{
				// Reopen index file to release buffer from last query
				this.reset_index_buffered_reader();
				
				// Try to search this token in inverted index file
				this.term_index_raf.seek(0);
				this.term_index_raf.seek(offset);
				
				while(true)
				{
					try 
					{
						_to = (TermObject)ExternalMergeSort.read_term_object(this.term_index_ois);
						
						String find_term = _to.getTerm();
						if(find_term == null || find_term.isEmpty())
							continue;
						
						// Is it match?
						if(find_term.compareTo(token) == 0)
						{
							this.term_dict.put(token, _to);
							return _to;
						}
						else
						{
							// Go through to another alphabet and we can't find the token
							if(find_term.toCharArray()[0] != f_c)
							{
								break;
							}
						}
					} 
					catch(StreamCorruptedException e)
					{
						e.printStackTrace();
					}
					catch (ClassNotFoundException e) 
					{
						e.printStackTrace();
					}
					catch(EOFException e)
					{
						break;
					}
				}
			}
		}
		
		return null;
	}
	
}
