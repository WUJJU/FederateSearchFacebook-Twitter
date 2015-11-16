package index;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;


/**
 * class TermHeader to store each header info into an inverted index file
 * This header has the info of a to z term's start postion in the file
 *  
 */
public class TermHeader implements Externalizable 
{
	   // Store the start position of each alphabet prefix term in inverted index file
	   // all term started with non-alphabet's postition is stored in the last element
	   private long alpha_index[] = new long[27];
	   
	   private final static String TOKENS = "abcdefghijklmnopqrstuvwxyz-"; 
	
	   /**
		* No-argument constructor required by {@link Externalizable}.
		*/
	   public TermHeader() 
	   {
		   // Init all postions to -1
		   for(int i = 0; i < 27; ++i)
		   {
			   this.alpha_index[i] = -1;
		   }
	   }
	
	   public long getPositionByIdex(final int index)
	   {
		   if(index < 0 || index > 26) return this.alpha_index[0];
		   
		   return this.alpha_index[index];
	   }
	   
	   public long getPosition(final char c) 
	   {
		   char l_c = Character.toLowerCase(c);
		   int ix = TOKENS.indexOf(l_c);
		   
		   if(ix == -1)  // Not found
		   {
			   return -1;
		   }
		   return this.alpha_index[ix];
	   }
	
	   public void setPosition(final char c, long pos) 
	   {
		   char l_c = Character.toLowerCase(c);
		   int ix = TOKENS.indexOf(l_c);
		   
		   if(ix > -1 && this.alpha_index[ix] == -1)  // Good token and not set
		   {
			   this.alpha_index[ix] = pos;
		   }	   
	   }
	
	   public void writeExternal(final ObjectOutput out) throws IOException {
		   for(int i = 0; i < 27; ++i)
		   {
			   out.writeLong(this.alpha_index[i]);
		   }
	   }
	
	   public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
		   for(int i = 0; i < 27; ++i)
		   {
			   this.alpha_index[i] = in.readLong();
		   }
	   }
	
	   public boolean equals(final Object o) {
	      if (this == o) return true;
	      if (o == null || getClass() != o.getClass()) return false;
	      
	      final TermHeader _termO = (TermHeader) o;
	      for(int i = 0; i < 27; ++i)
		  {
	    	  if(this.alpha_index[i] != _termO.getPositionByIdex(i))
	    		  return false;
		  }
	      
	      return true;
	   }
}
