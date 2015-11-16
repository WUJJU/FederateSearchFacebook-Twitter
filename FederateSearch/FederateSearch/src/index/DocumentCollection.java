package index;

import java.io.IOException;
import java.util.Map;

/**
 * DocumentCollection is an interface for reading individual document files from 
 * a collection file.
 */
public interface DocumentCollection {
	
	/**
	 * Try to read and return the next document stored in the collection.
	 * If it is the end of the collection file, return null.
	 * Each document should be stored as a Map, of which the key and value are both String type.
	 * Two fields should be stored in the map: DOCNO and CONTENT (case-sensitive),
	 * so that you can get the document's docno and content by calling 
	 * map.get("DOCNO") and map.get("CONTENT").
	 * 
	 * @return The next document stored in the collection; or null if it is the end of the collection file.
	 */
	public abstract Map<String,Object> nextDocument() throws IOException;
	
}
