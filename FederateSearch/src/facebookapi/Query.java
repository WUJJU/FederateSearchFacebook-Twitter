package facebookapi;

import index.MyIndexReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import search.MyRetrievalModel;
import search.SearchResult;


/**
 * !!! YOU CANNOT CHANGE ANYTHING IN THIS CLASS !!!
 * 
 * Main class for running your HW3.
 * 
 */
public class Query {
    
    
    public String path_dir;
    public String queries;
    
    public Query(String path_dir,String queries){
        
        this.path_dir=path_dir;
                this.queries=queries;
    }

	
	public  List<SearchResult>  run() {
		
		
	
		
	    List<SearchResult> results=null;
		MyIndexReader ixreader = null;
		try {
			// Initiate the index reader ...
			ixreader = new MyIndexReader(path_dir);
		}catch(Exception e){
			System.out.println("ERROR: cannot initiate index directory.");
			e.printStackTrace();
		}
	
		
		
		
		MyRetrievalModel model = new MyRetrievalModel().setIndex(ixreader);
		
		//List<Topic> topics = Topic.parse( new File(path_topic) );
		//Topic topic=null;
		
		queries.trim();
		try{
		 String[]array=  queries.split(" ");
		        List<String> terms=Arrays.asList(array);
		results = model.search(terms, 100);
				
					if( results!=null ) {
						int rank = 1;
						for( SearchResult result:results ){
							System.out.println(" Q0 "+result.docno()+" "+rank+" "+result.score()+" MYRUN");
							rank++;
						}
					}
				}catch(IOException e){
					System.err.println(" >> cannot read index ");
					e.printStackTrace();
				}//try-catch
				    
		                try{
		                        ixreader.close();
		                }catch(Exception e){}
		                
		                return results;
		                
			}
          
	
		
	}
	
