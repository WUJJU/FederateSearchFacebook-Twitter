package facebookapi;


import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import analysis.TextNormalizer;
import analysis.TextTokenizer;




/**
 *PARSE COLLECTION: NORMALIZE AND TOKENIZE
 * 
 */
public class Tokenize2 {
 
    private Map<String, Object> post_f_t = null;
        
        public static void main(String[] args) throws FileNotFoundException {
                
                if( args==null || args.length<2 ) {
                        System.out.println("Usage:");
             
                        System.out.println("  args[0]: path of the collection file.");
                        System.out.println("  args[1]: path of the output file.");
                        System.exit(0);
                }
                

                String path_input = args[0];
                String path_output = args[1];
                
            
                
                FileInputStream instream_collection = null;
             //   DocumentCollection collection = null;
                Map<String,Integer> doc_term_count  =new HashMap<String, Integer>();    
                
           
    
                instream_collection = new FileInputStream(path_input);
                BufferedReader input=null;
                BufferedWriter output = null;
                
                try {
                        // Initiate the output writer ...
                        input=new BufferedReader(new InputStreamReader(instream_collection));
                        output = new BufferedWriter( new OutputStreamWriter( new FileOutputStream(path_output), "UTF-8" ) );
                       
                }catch(IOException e){
                        System.out.println("ERROR: cannot initiate output file.");
                        e.printStackTrace();
                }
                
                if( output!=null ) {
                        
                        Map<String,Object> doc = null;
                        String post=null;
                        int ch_n;
                        ArrayList<Character> list_ch = new ArrayList<Character>();
                        String post_id="";
                        String post_c="";
                        char[] carray;
                        try {
                            post=input.readLine();
                        while(post!=null){
                            post=post.trim();
                            
                            if(!post.equalsIgnoreCase("")){
                                if(post.startsWith("FB")||post.startsWith("TW")){
                                    StringTokenizer st=new StringTokenizer(post);
                                    String st1="";
                                    while (st.hasMoreTokens()) {
                                       st1+= st.nextToken()+" ";
                                    
                                    }
                                    String[]array=st1.split(" ");
                                 
                                  
                                  post_id=array[0];
                                    output.write(array[0]+"\n");
                                    for(int i=1;i<array.length-2;i++){
                                  
                                        output.write(array[i]+" ");
                                    }
                                    output.write(array[array.length-1]+"\n");
                                   
                                   
                                  
                         
                                    post=input.readLine();
                                }
                   
                            }
                        }//end of while
                          System.out.println("Finshed");
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        
                }
                
             
                if( instream_collection!=null ) {
                        try{
                                instream_collection.close();
                        }catch(IOException e){ }
                }
                
        
                if( output!=null ) {
                        try{
                                output.close();
                        }catch(IOException e){ }
                }
                
        }
       
        
}
