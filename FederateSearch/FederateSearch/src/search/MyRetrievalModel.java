package search;

import index.MyIndexReader;
import index.TermObject;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import analysis.StopwordsRemover;



public class MyRetrievalModel {
	
	private int doc_count = 0;
	private boolean is_remove_stop_words = false;
	private int max_result = 100;
	
	// Store each term's search result
	private Map<String, TermObject> s_results = new HashMap<String, TermObject>();
	private Map<String, TermObject> s_inter_result = null;
	private Set<Integer>query_doc_set = new HashSet<Integer>();
	List<SearchResult> lsr = new ArrayList<SearchResult>();
	
	protected MyIndexReader ixreader;
	protected StopwordsRemover stoprmv;
	protected List<String> terms = null;
	
	
	final private boolean DEBUG = true;
	
	public MyRetrievalModel() {
		// you should implement this method
	}
	
	public MyRetrievalModel setIndex( MyIndexReader ixreader ) {
		this.ixreader = ixreader;
		
		// Get Doc Number in the collection
                try {
                        this.doc_count = this.ixreader.get_doc_count();
                } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                }
		
		return this;
	}
	
	public MyRetrievalModel setStopwordsRemover(StopwordsRemover _stoprmv)
	{
		this.stoprmv = _stoprmv;
		return this;
	}
	
	/**
	 * Search for the topic information. 
	 * The returned results should be ranked by the score (from the most relevant to the least).
	 * max_return specifies the maximum number of results to be returned.
	 * 
	 * @param topic The topic information to be searched for.
	 * @param max_return The maximum number of returned document
	 * @return
	 */
	public List<SearchResult> search( List<String>terms, int max_return ) throws IOException {
		// you should implement this method
		
		if(DEBUG)
		{
			System.out.println("Start searching "  + " at " + current_datetime() );
		}
		
		this.max_result = max_return;
		this.terms = terms;
		
		// Clear previous search result
		this.s_results.clear();
		this.query_doc_set.clear();
		this.lsr.clear();
		
		// Split topic title to terms
	
		
		// Search by each terms
		for(int i = 0; i < terms.size(); ++i)
		{
			this.search_by_term(terms.get(i));
		}
		System.out.println(s_results.keySet());
		// Do intersection on the results
		this.s_inter_result = this.intersect_search_result(terms, this.s_results);
		System.out.println(s_inter_result.keySet());
		// Do Ranking on the search results and store the result into List<SearchResult>
		this.ranking_results();	
		
		if(DEBUG)
		{
			System.out.println("End Searching " 
					+ " with " + this.lsr.size() + " returned records at " + current_datetime() );
		}
		
		return this.lsr;
	}
	
	/**
	 * 
	 * @param _remove
	 */
	public void set_is_remove_stop_words(boolean _remove)
	{
		this.is_remove_stop_words = _remove;
	}
	
	/**
	 * ranking search result and store it into List<SearchResult>
	 * @return List<SearchResult>
	 */
	private List<SearchResult> ranking_results()
	{
		// Calculate length of Document
		Set<String> terms = this.s_results.keySet();
		
		// Length of document
		Map<Integer, Double> docs_length = new HashMap<Integer, Double>();
		
		// Doc score
		Map<Integer, Double> docs_score = new HashMap<Integer, Double>();
		
		// Get term frequency weight for each term in each document
		Map<String, Map<Integer, Double>> tf = new HashMap<String, Map<Integer, Double>>();
		// IDF for each term
		Map<String, Double> idf = new HashMap<String, Double>();
		// Wij weight
		Map<String, Map<Integer, Double>> wij = new HashMap<String, Map<Integer, Double>>();
		
		// Calculate W(i,j)
		Iterator it = terms.iterator();
		while(it.hasNext())
		{
			String term = (String)it.next();
			Map<Integer, Integer> tf_of_doc = new HashMap<Integer, Integer>();
			
			TermObject to = this.s_results.get(term);
			Map<Integer, ArrayList<Integer>> postings = to == null ? null : to.getPostings();
			Set<Integer> docs_set = postings == null ? null : postings.keySet();
			
			// Calculate IDF
			double doc_freq = to == null ? 0.0 : (double)to.getDocFreq();
			double _idf_v = doc_freq == 0 ? 0 : Math.log(this.doc_count/doc_freq)/Math.log(10);
			idf.put(term, _idf_v);
			
			// Calculate tf weight & wij weight
			if(docs_set == null) continue;	// No document returned for this term query
			
			Iterator doc_it = docs_set.iterator();
			while(doc_it.hasNext())
			{
				Integer doc_id = (Integer)doc_it.next();
				Map<Integer, Double> _tf_v = new HashMap<Integer, Double>();
				int term_freq = postings == null ? 0 : postings.get(doc_id).size();
				double term_freq_w = term_freq == 0 ? 0 : 1 + Math.log(term_freq)/Math.log(10);
				_tf_v.put(doc_id, term_freq_w);
				
				// TF weight
				tf.put(term, _tf_v);
				
				// Wij weight
				Map<Integer, Double> _wij_v = new HashMap<Integer, Double>();
				double _wij_w = term_freq_w * _idf_v;
				_wij_v.put(doc_id, _wij_w);
				wij.put(term, _wij_v);
				
				// Sum Length
				double _len = docs_length.get(doc_id) == null ? 0 : docs_length.get(doc_id).doubleValue();
				_len += _wij_w*_wij_w;
				docs_length.put(doc_id, _len);
				
				// Sum to get document score
				double _score = docs_score.get(doc_id) == null ? 0 : docs_score.get(doc_id).doubleValue();
				_score += _wij_w;
				docs_score.put(doc_id, _score);
			}
			
		}
		
		// Normalized the score of each document
		Set<Integer> docid_set = docs_score.keySet();
		Iterator score_it = docid_set.iterator();
		while(score_it.hasNext())
		{
			Integer _d_id = (Integer)score_it.next();
			double _n_score = docs_score.get(_d_id);
			
			_n_score = docs_length.get(_d_id) == 0 ? 0 : _n_score / (Math.sqrt(docs_length.get(_d_id)));
			docs_score.put(_d_id, _n_score);
		}
		
		// Construct search result
		this.construct_search_result(docs_score);
		
		return this.lsr;
	}
	
	/**
	 * Construct search result based on the score
	 * @param score
	 * @return
	 */
	private void construct_search_result(final Map<Integer, Double> scores)
	{
		if(scores == null) return;
		
		Integer dummy[] = {}, doc_ids[] = scores.keySet().toArray(dummy);
		
		// Sorted search result by its score
		Arrays.sort(doc_ids, new Comparator<Integer>() 
		{
            @Override
            public int compare(Integer d_one, Integer d_two) 
            {
            	// Compare the score of each document
            	double _v1 = scores.get(d_one);
            	double _v2 = scores.get(d_two);
            	
            	if(_v2 > _v1)
            		return 1;
            	else if(_v2 == _v1)
            		return 0;
            	else
            		return -1;
            }
		});
		
		// Construct search result
		int _size = doc_ids.length;
		for(int i = 0; i < _size; ++i)
		{
			if(i > this.max_result) break;
			
			try
			{
				String doc_no = this.ixreader.getDocno(doc_ids[i]);
				double doc_score = scores.get(doc_ids[i]);
				
				SearchResult sr = new SearchResult(doc_ids[i], doc_no, doc_score);
				
				this.lsr.add(sr);
			}
			catch(IOException e)
			{
				e.printStackTrace();
				continue;
			}
		}
		
		// Write search result to the file
		this.write_search_result_to_file();
	}
	
	/**
	 * write search result to file with topic id as its file name
	 */
	private void write_search_result_to_file()
	{
		BufferedWriter bufferedWriter = null;
		
		try {
            
            //Construct the BufferedWriter object
            bufferedWriter = new BufferedWriter(new FileWriter("/Users/wuhao/Desktop/Infromation Retrieval&Storage/Final Project/search_result/queries.rst"));
            
            //Start writing to the output stream
            for(String e:terms){
            bufferedWriter.write(e);
            bufferedWriter.write(" ");
            }
            bufferedWriter.newLine();
            
            bufferedWriter.write("Search Results:");
            bufferedWriter.newLine();
            
            // Output the document number of each term's query
            bufferedWriter.write("\n===============Term's Result===============");
            bufferedWriter.newLine();
            Set<String> terms = this.s_results.keySet();
            Iterator it = terms.iterator();
            while(it.hasNext())
            {
            	String term = (String)it.next();
            	bufferedWriter.write(term);
            	bufferedWriter.write(":");
            	int num_docs = this.s_results.get(term) == null ? 0 : this.s_results.get(term).getPostings().size();
            	bufferedWriter.write(String.valueOf(num_docs));
            	bufferedWriter.write(" documents returned.\t");
            	bufferedWriter.newLine();
            }
            bufferedWriter.write("===================================================\n");
            bufferedWriter.newLine();
            
            // Output search result of Boolean Model
            bufferedWriter.write("============Boolean Model Result================");
            bufferedWriter.newLine();
            
            it = terms.iterator();
            if(it.hasNext())
            {
            	TermObject _t_obj = this.s_inter_result.get((String)it.next());
            	if(_t_obj != null)
            	{
            		Set<Integer> docs_id = _t_obj.getPostings().keySet();
            	
	            	Iterator doc_it = docs_id.iterator();
	            	while(doc_it.hasNext())
	            	{
	            		Integer doc_id = (Integer)doc_it.next();
	            		bufferedWriter.write(String.valueOf(doc_id));
	                	bufferedWriter.write('\t');
	                	if(this.ixreader.getDocno(doc_id)==null){
	                	    bufferedWriter.write("null");
	                	}else{
	                	    bufferedWriter.write(this.ixreader.getDocno(doc_id));
	                	}
	                	
	                	
	                	// new line
	                	bufferedWriter.newLine();
	            	}
            	}
            }	
            
            // Output search result of Vector Model
            bufferedWriter.write("\n==============Vector Model Result================");
            bufferedWriter.newLine();
            
            for(int i = 0; i < this.lsr.size(); ++i)
            {
            	SearchResult sr = this.lsr.get(i);
            	
            	bufferedWriter.write(String.valueOf(sr.docid()));
            	bufferedWriter.write('\t');
            	if(sr.docno()==null){
            	bufferedWriter.write("null");
            	}else{
            	bufferedWriter.write(sr.docno());
            	}
            	bufferedWriter.write('\t');
            	bufferedWriter.write(String.valueOf(sr.score()));
            	
            	// new line
            	bufferedWriter.newLine();
            }           
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            //Close the BufferedWriter
            try {
                if (bufferedWriter != null) {
                    bufferedWriter.flush();
                    bufferedWriter.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
	}
	
	 /**
	     * search_by_term
	     * 
	     * @param : String term
	     */
	    private void search_by_term(String term) {
	        TermObject _to = null;
	        try {
	            if (!this.is_phrase(term)) {
	                _to = this.ixreader.findTermObject(term);
	                this.s_results.put(term, _to);

	                // Add result doc id into query_doc_set
	                if (_to != null)
	                    this.query_doc_set.addAll(_to.getPostings().keySet());
	            } else {
	                this.search_by_phrase(term);
	            }
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    }

	/**
	 * search by phrase
	 * @param phrase
	 */
	private Map<String, TermObject> search_by_phrase(String phrase)
	{
		TermObject _to = null;
		List<String> terms = this.split_phrase_into_terms(phrase);
		Map<String, TermObject> _result = new HashMap<String, TermObject>();
		
		int _num_t = terms.size();
		
		if(_num_t < 1) return null;
		
		try 
		{
			for(int i = 0; i < _num_t; ++i)
			{
				String t = terms.get(i);
				_to = this.ixreader.findTermObject(t);
				_result.put(t, _to);
			}
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
			return null;
		}
		
		// Try to find all terms in the successive positions in the docs
		// First, merge search results of all terms in the phrase
		Map<String, TermObject> s_intersect_result = this.intersect_search_result(terms, _result);
		
		TermObject to = new TermObject(phrase);
			
		TermObject f_to = s_intersect_result.get(terms.get(0));
		if(f_to == null) return null;
		
		Integer dummy[] = {}, docs_first_term[] = f_to.getPostings().keySet().toArray(dummy);
		// Check each doc
		for(int i = 0; i < docs_first_term.length; ++i)
		{
			// Check each position of the first term emerges
			int doc_id = docs_first_term[i];
			ArrayList<Integer> pos_list = s_intersect_result.get(terms.get(0)).getPostings().get(doc_id);
			
			for(int j = 0; j < pos_list.size(); ++j)
			{
				int pos = pos_list.get(j);
				// Try to check all other terms in the successive positions or not
				boolean is_successive = true;
				int next_pos = pos + 1;
				for(int k = 1; k < _num_t; ++k)
				{
					ArrayList<Integer> other_pos_list = s_intersect_result.get(terms.get(k)).getPostings().get(doc_id);
					if(!other_pos_list.contains(new Integer(next_pos)))
					{
						is_successive = false;
						break;
					}
					next_pos++;
				}
				
				// If all terms appear together, add this doc into it and record its position
				if(is_successive)
				{
					// Then check if they are successive in the document or not
					to.addPostings(doc_id, pos);
					to.addDocFreq(doc_id);
					to.addCollectionFreq();
				}
			}
		}
		
		this.s_results.put(phrase, to);
		
		// Add result doc id into query_doc_set
		this.query_doc_set.addAll(to.getPostings().keySet());
		
		Map<String, TermObject> rv = new HashMap<String, TermObject>();
		rv.put(phrase, to);
		
		return rv;
	}
	
	/**
	 * Merge search result through query terms
	 * @param _s_result
	 * @return Map<String, TermObject>
	 */
	private Map<String, TermObject> intersect_search_result(List<String> terms, Map<String, TermObject> _s_result)
	{
		Map<String, TermObject> i_result = new HashMap<String, TermObject>();
		System.out.println(_s_result);
		if(_s_result == null || _s_result.size() < 1) return i_result;
		
		int _t_num = terms.size();
		int _min_t = 0;	// Find which one has the least number of docs
		
		// If one term return null result, just return null
		TermObject _each_term_obj_ = _s_result.get(terms.get(0));
		if(_each_term_obj_ == null)
			return i_result;
		
		int _doc_size = _each_term_obj_.getPostings().size();
		for(int i = 1; i < _t_num; ++i)
		{
			_each_term_obj_ = _s_result.get(terms.get(i));
			if(_each_term_obj_ == null)
				return i_result;
			
			int _tmp_doc_size = _each_term_obj_.getPostings().size();
			if(_doc_size > _tmp_doc_size)
			{
				_doc_size = _tmp_doc_size;
				_min_t = i;
			}
		}
		
		// Merge search results
		Integer i_dummy[] = {}, doc_list[] = _s_result.get(terms.get(_min_t)).getPostings().keySet().toArray(i_dummy);
		for(int j = 0; j < _doc_size; ++j)
		{
		   
			boolean is_intersecton = true;
			// check if a doc is included by antoher term's query result
			for(int i = 0; i < _t_num; ++i)
			{
				if(i == _min_t) continue;	// Ignore self
				
				// Some terms search result is empty
				_each_term_obj_ = _s_result.get(terms.get(i));
				if(_each_term_obj_ == null)
					return i_result;
				
				Set<Integer> _other_doc_list = _each_term_obj_.getPostings().keySet();
				
				if(!_other_doc_list.contains(doc_list[j]))
				{
					is_intersecton = false;
					break;
				}
			}
			
			// The doc is the intersection of all query terms
			if(is_intersecton)
			{
				for(int i = 0; i < _t_num; ++i)
				{
					TermObject p = i_result.get(terms.get(i));
					if(p == null) p = new TermObject(terms.get(i));
					
					ArrayList<Integer> _positions = _s_result.get(terms.get(i)).getPostings().get(doc_list[j]);
					
					for(int k = 0; k < _positions.size(); ++k)
					{
						p.addPostings(doc_list[j], _positions.get(k));
					}				
					i_result.put(terms.get(i), p);
				}
			}
		}
		
		return i_result;
	}
	
	/**
	 * get_terms_from_topic
	 * @param topic
	 * @return String[]
	
	public List<String> get_terms_from_topic(Topic topic)
	{
		List<String> terms = new ArrayList<String>();
		
		String topic_title = topic.getValueByTag("TITLE");
		
		// Split terms
		String a_term = "";
		boolean is_start_phrase = false;
		for(int i = 0; i < topic_title.length(); ++i)
		{
			char ch = topic_title.charAt(i);	
			
			if(ch == '"')
			{
				a_term += ch;
				if(is_start_phrase)
				{
					terms.add(a_term);
					a_term = "";
					is_start_phrase = false;
				}
				else
				{
					is_start_phrase = true;
				}
			}
			else if(Character.isAlphabetic(ch) || ch == '-')
			{
				a_term += ch;
				
				if(i == (topic_title.length() - 1))
				{
					if(a_term.length() > 0)
					{
						// Check we should remove stopwords from query terms or not
						if(!this.is_remove_stop_words || !this.stoprmv.isStopword(a_term.toCharArray()))
							terms.add(a_term);
						a_term = "";
					}
				}
			}
			else
			{
				if(is_start_phrase)
				{
					a_term += ch;
				}
				else
				{
					if(a_term.length() > 0)
					{
						// Check we should remove stopwords from query terms or not
						if(!this.is_remove_stop_words || !this.stoprmv.isStopword(a_term.toCharArray()))
							terms.add(a_term);
						a_term = "";
					}
				}
			}
				
		}	
		
		return terms;
	}
	 */
	/**
	 * is_phrase : check if a term is a phrase or not
	 * @param term
	 * @return boolean
	 */
	public boolean is_phrase(String term)
	{
		if(term == null || term.isEmpty() || term.length() < 3)
			return false;
		
		if(term.charAt(0) == '"' && term.charAt(term.length()-1) == '"')
			return true;
		
		return false;
	}
	
	/**
	 * split phrase into terms
	 * @param topic
	 * @return String[]
	 */
	public List<String> split_phrase_into_terms(String phrase)
	{
		List<String> terms = null;
		String _phrase = phrase.substring(1, phrase.length() - 1);
		
		String tmp_terms[] = _phrase.split("\\s+");
		
		if(tmp_terms != null && tmp_terms.length > 0)
		{
			for(int i = 0; i < tmp_terms.length; ++i)
			{
				// Check we should remove stopwords from query terms or not
				if(!this.is_remove_stop_words || !this.stoprmv.isStopword(tmp_terms[i].toCharArray()))
				{
					if(terms == null) terms = new ArrayList<String>();
					
					terms.add(tmp_terms[i]);
				}
			}
		}
		
		return terms;
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
}
