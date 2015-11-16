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
public class PostContent {
   public    String path_input;

  
    public PostContent(String input){
         path_input =input;
       
    }
	
		
	public Map <String,String> run(){	
		
		
		FileInputStream instream_collection = null;
		DocumentCollection collection = null;
		BufferedReader ea_post=null;
		  Map  post_ft = new HashMap<String, Object>();
		  Map post_ft_bk = new HashMap<String,String>();
		  
		try{
			// Loading the collection file and initiate the DocumentCollection class
			instream_collection = new FileInputStream(path_input);
			 ea_post=new BufferedReader(new InputStreamReader(instream_collection));
			
		}catch(IOException e){
			System.out.println("ERROR: cannot load collection file.");
			e.printStackTrace();
		}
		
		
		
		
			
			try{
				
				Map<String,Object> pos = null;
				String post_st;
			
		                    
		            
		               
		        
				while( ( post_st=ea_post.readLine()) != null ) { // iteratively reading each document from the collection
					
				    if(post_st.startsWith("FB")||post_st.startsWith("TW"))
	                                {
	                                  
	                                     post_ft.put("post_id",post_st);
	                                }
	                                   // System.out.println(post_st);
	                                    post_st=ea_post.readLine();
	                                    post_st.trim();
	                                    post_ft.put("post_content",post_st);
	                                
					String postId = (String) post_ft.get("post_id"); // load docno of the document and output
					char[] content =  post_ft.get("post_content").toString().toCharArray(); // document content
				String postcon=new String(content);
					post_ft_bk.put(postId, postcon);
					
					post_ft.clear();
					
				}
				
				
			
			}catch(IOException e){
				System.out.println(e.getMessage());
				e.printStackTrace();
			}
			if( instream_collection!=null ) {
	                        try{
	                                instream_collection.close();
	                        }catch(IOException e){ }
	                }
	                
			return post_ft_bk;
		}
		
		
	
	
		
	}
	

