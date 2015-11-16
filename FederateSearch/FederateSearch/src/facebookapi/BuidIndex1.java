package facebookapi;

import index.DocumentCollection;
import index.MyIndexWriter;
import index.TrectextCollection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import analysis.TextTokenizer;





/**
 * !!! YOU CANNOT CHANGE ANYTHING IN THIS CLASS !!!
 * 
 * Main class for running your HW2.
 * 
 */
public class BuidIndex1 {
  
    
	public static void main(String[] args) {
		
		if( args==null || args.length<2 ) {
			System.out.println("Usage:");
	
			System.out.println("  args[0]: path of the collection file.");
		
			System.out.println("  args[1]: path of the output index's directory.");
			System.out.println("arg[2]:path of the output post_ft_bk file");
			System.exit(0);
		}
		
		
		String path_input = args[0];
		String path_output = args[1];
		String path_output_post=args[2];
		
		FileInputStream instream_collection = null;
		DocumentCollection collection = null;
		BufferedReader ea_post=null;
		  Map  post_ft = new HashMap<String, Object>();
		  Map post_ft_bk=new HashMap<String,Object>();
		try{
			// Loading the collection file and initiate the DocumentCollection class
			instream_collection = new FileInputStream(path_input);
			 ea_post=new BufferedReader(new InputStreamReader(instream_collection));
			collection = new TrectextCollection(instream_collection);
		}catch(IOException e){
			System.out.println("ERROR: cannot load collection file.");
			e.printStackTrace();
		}
		
		FileInputStream instream_stopwords = null;
	
		MyIndexWriter output = null;
		
		try {
			// Initiate the output index writer ...
			output = new MyIndexWriter(path_output);
		}catch(Exception e){
			System.out.println("ERROR: cannot initiate index directory.");
			e.printStackTrace();
		}
		
		if(output!=null ) {
			
			try{
				
				Map<String,Object> pos = null;
				String post_st;
			
		                    
		            
		               
		        
				while( ( post_st=ea_post.readLine()) != null ) { // iteratively reading each document from the collection
					
				    if(post_st.startsWith("FB")||post_st.startsWith("TW"))
	                                {
	                                  
	                                     post_ft.put("post_id",post_st);
	                                }
	                                    System.out.println(post_st);
	                                    post_st=ea_post.readLine();
	                                    post_ft.put("post_content",post_st);
	                                
					String postId = (String) post_ft.get("post_id"); // load docno of the document and output
					char[] content =  post_ft.get("post_content").toString().toCharArray(); // document content
					TextTokenizer tokenizer = new TextTokenizer(content);
					String t=content.toString();
					post_ft_bk.put(postId, content);
					output.index(postId, tokenizer); // index the tokenized documents
					
					post_ft.clear();
					
				}
				Set<String> keys = post_ft_bk.keySet();  //get all keys
				for(String i: keys)
				{
				    System.out.println(post_ft_bk.get(i));
				}
				
			
			}catch(IOException e){
				System.out.println(e.getMessage());
				e.printStackTrace();
			}
			
		}
		
		if( instream_collection!=null ) {
			try{
				instream_collection.close();
			}catch(IOException e){ }
		}
		
		if( instream_stopwords!=null ) {
			try{
				instream_stopwords.close();
			}catch(IOException e){ }
		}
		
		if( output!=null ) {
			try{
				output.close();
			}catch(IOException e){ }
		}
		
	}
	
}
