/**
 * Parse Trec Text or Web file. In order to handle a very large file, 
 * we use BufferedInputStream to read data from file instead of reading all data of the file 
 * into memory
 * File: TrecParser.java
 * Author: ijab(zhancaibao#gmail.com)
 * Date: 2013/01/14
 */
package index;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @class TrecParser
 * @@ A mini parser to parse TrecText and TrecWeb file
 *
 */
public class TrecParser {
	protected FileInputStream trec_fis = null;
	protected BufferedReader buffer_rd = null;
	
	protected Set<String> concerned_tags = new HashSet<String>();
	
	private static final String START_DOC_TAG = "<DOC>";
	private static final String END_DOC_TAG = "</DOC>";
	private static final String START_DOCHDR_TAG = "<DOCHDR>";
	private static final String END_DOCHDR_TAG = "</DOCHDR>";
	private static final String START_DOCNO_TAG = "<DOCNO>";
	private static final String END_DOCNO_TAG = "</DOCNO>";
	private static final String DOC_TAG_NAME = "DOC";
	private static final String DOC_NO_TAG_NAME = "DOCNO";
	private static final String DOC_CONTENT_TAG_NAME = "TEXT";
	private static final String HDR_TAG_NAME = "DOCHDR";
	private static final int BUFFER_SIZE = 8*1024;
	
	private enum STATE {OUTDOC, STARTDOC, STARTEL, INDOC, ENDDOC, ENDEL, STARTHDR, ENDHDR, 
						OUTTOPIC, INTOPIC, STARTTOPIC, ENDTOPIC};
	private STATE state = STATE.OUTDOC;
	private STATE doc_state = STATE.OUTDOC;
	private long docs_count = 0;
	private String tag = "";
	private Map<String, Object> post_ft = null;
	
	///////////////////////////////////////////////////////////////
	// Codes for parsing Trec Topics file
	private STATE state_t = STATE.OUTTOPIC;
	private STATE topic_state = STATE.OUTTOPIC;

	// Debug related params
	private boolean DEBUG = false;
	private long start_time;
	private long end_time;
	
	/**
	 * @constructor: TrecParser
	 * @param : FileInputStream is
	 */
	public TrecParser(FileInputStream is)
	{
		this.trec_fis = is;
		this.buffer_rd = new BufferedReader(new InputStreamReader(this.trec_fis), BUFFER_SIZE);
		
		// concerned tags
		this.concerned_tags.add(DOC_NO_TAG_NAME);
		this.concerned_tags.add(DOC_CONTENT_TAG_NAME);
		
		start_time = System.currentTimeMillis();
		if(DEBUG)
		{
			System.out.println("Start handling trec file at " + current_datetime() );
		}
	}
	
	/**
	 * @constructor: TrecParser : For parsing Trec Topics
	 * @param : FileInputStream is
	 * @param : Set<String> _concerned_tags
	 */
	public TrecParser(FileInputStream is, Set<String> _concerned_tags)
	{
		this.trec_fis = is;
		this.buffer_rd = new BufferedReader(new InputStreamReader(this.trec_fis), BUFFER_SIZE);
		
		// concerned tags
		this.concerned_tags = _concerned_tags;
		
		start_time = System.currentTimeMillis();
		if(DEBUG)
		{
			System.out.println("Start handling trec file at " + current_datetime() );
		}
	}
	
	/**
	 * next_doc: Iterate the trec file to get doc
	 * @param void
	 * @return Map<String, Object> Doc content or null reaching the end of trec file
	 */
	public Map<String, Object> next_doc()
	{
		post_ft = null;
		
		parse();
		
		if(post_ft == null)
		{
			end_time = System.currentTimeMillis();
			if(DEBUG)
			{
				System.out.println("Finished handling " + docs_count + " docs in " 
								+ (end_time-start_time)/1000 + "s at " + current_datetime());
			}			
		}
		return post_ft;
	}
	
	/**
	 * parse: parse Trec text or web file
	 * @param void
	 * @return void
	 */
	private void parse()
	{
		String post_st;
		
		try
		{
			while((post_st=buffer_rd.readLine()) != null)
			{
			
				if(post_st.startsWith("FB")||post_st.startsWith("TW"))
				{
				  
				     set_value("post_id",post_st);
				}else{
				    System.out.println(post_st);
				    set_value("post_content",post_st);
				}
				
					
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * parse_web_value: When the state is ENDHDR, try to get the value
	 *  until reaching the end of tag or doc
	 * @param void
	 * @return void
	 */
	
	
	/**
	 * set_value: set doc's value of specified tag
	 * @param String key: tag name as key;
	 * @param Object v: value of the tag
	 * @return void
	 */
	private void set_value(String key, Object v)
	{
		if(post_ft == null)
		{
			post_ft = new HashMap<String, Object>();
		}
		post_ft.put(key, v);
	
	}
	
	///////////////////////////////////////////////////////////////////////////////////
	// The below codes are for parsing Trec Topics
	//////////////////////////////////////////////////////////////////////////////////
	/**
	 * next_topic: Iterate the trec topics file to get topic
	 * @param void
	 * @return Map<String, Object> topics content or null reaching the end of trec topics file
	 */
	public Map<String, Object> next_topic()
	{
		post_ft = null;
		
		parse_topic();
		
		
		if(post_ft == null)
		{
			end_time = System.currentTimeMillis();
		if(DEBUG)
			{
				System.out.println("Finished handling " + docs_count + " docs in " 
								+ (end_time-start_time)/1000 + "s at " + current_datetime());
			}			
		}
		return post_ft;
	}
	
	/**
	 * parse: parse Trec topics file
	 * @param void
	 * @return void
	 */
	private void parse_topic()
	{
		int post_st;
		ArrayList<Character> l_char = new ArrayList<Character>();
		
		try
		{
			while((post_st=buffer_rd.read()) != -1)
			{
				char ch = (char)post_st;
				STATE last_state = state_t;
				String last_tag = tag;
				if(ch == '<')
				{
					parse_topic_tag();
					
					if(topic_state != STATE.INTOPIC)
					{
						// If we don't handle the tag in a doc, it should be malformted
						state_t = STATE.OUTTOPIC;
						l_char.clear();
						continue;
					}
					else
					{
						if(state_t == STATE.ENDTOPIC)
						{						
							// If after parsing tag, reaching </top>
							// just break and return the doc
							state_t = STATE.OUTTOPIC;
							
							// Check last_state and last_tag to set correct value
							if(last_state == STATE.STARTEL)
							{
								if(concerned_tags.contains(last_tag))
								{      
								  
                                                
									String _v_of_tag = new String(list_to_char_array(l_char));
									
									
									set_value(last_tag, _v_of_tag);
								}
								l_char.clear();
							}
							break;
						}
						else if(last_state == STATE.STARTEL && state_t == STATE.STARTEL)
						{
							if(concerned_tags.contains(last_tag))
							{
								String _v_of_tag = new String(list_to_char_array(l_char));
								set_value(last_tag, _v_of_tag);
							}
							l_char.clear();
						}
					}
				}				
				else
				{
					if(state_t == STATE.OUTTOPIC || state_t == STATE.ENDTOPIC)
					{
						// If current state is OUTTOPIC and not encounter a < char,
						// just skip it
						continue;
					}
					else
					{
						l_char.add(ch);
					}
				}
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * parse_topic_tag(): While encountering a < and try to figure out what tag it is
	 * @param void
	 * @return void
	 */
	private void parse_topic_tag() throws IOException
	{
		int post_st = 0;
		String _tag = "";
		
		// set default state to STARTEL while encountering character <
		state_t = STATE.STARTEL;
		
		while((post_st = buffer_rd.read()) != -1)
		{
			char ch = (char)post_st;

			if(ch == '/')
			{
				state_t = STATE.ENDEL;
			}
			else if(ch == '>')
			{
				tag = _tag.toUpperCase();
				
				if(_tag.compareToIgnoreCase("TOP") == 0)
				{
					if(state_t == STATE.ENDEL)
					{
						state_t = STATE.ENDTOPIC;
					}
					else
					{
						state_t = STATE.STARTTOPIC;
						topic_state = STATE.INTOPIC;
						
						docs_count++;
						if(DEBUG == true)
						{
							System.out.println(current_datetime() + " Handle topic " + docs_count);							
						}						
					}
				}				
				break;
			}
			else
			{
				_tag += ch;
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
	 * list_to_char_array: Convert from ArrayList<Character> to char[] array
	 * @param ArrayList<Character> list_char
	 * @return char[]
	 */
	private char[] list_to_char_array(ArrayList<Character> list_char)
	{
		int _size = list_char.size();
		Character _v[] = new Character[_size];
		char _ret_v[] = new char[_size];
	
		list_char.toArray(_v);
		
		for(int i=0; i<_size; i++)
		{
			_ret_v[i] = _v[i].charValue();
		}
		return _ret_v;
	}
}
