package index;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class TrectextCollection implements DocumentCollection {
	
	private FileInputStream trec_is = null;
	private TrecParser trec_parser = null;
	
	
	// YOU SHOULD IMPLEMENT THIS METHOD
	public TrectextCollection( FileInputStream instream ) throws IOException {
		// This constructor should take an inputstream of the collection file as the input parameter.
		this.trec_is = instream;
		this.trec_parser = new TrecParser(instream);
	}
	
	// YOU SHOULD IMPLEMENT THIS METHOD
	public Map<String, Object> nextDocument() throws IOException {
		// Read the definition of this method from edu.pitt.sis.infsci2140.index.DocumentCollection interface 
		// and follow the assignment instructions to implement this method.
		
		// Read char by char. Do not do it by readline for we can merge all the content 
		// into one line
		return this.trec_parser.next_doc();
	}	
}
