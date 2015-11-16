package index;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 * class TermObject to store each term for serialize and deserialize to disk
 *  
 */
public class TermObject implements Externalizable 
{

   private String term;
   private int coll_freq;
   private int doc_freq;
   private Map<Integer, ArrayList<Integer>> postings = null;	// <doc_id, [term_pos1, term_pos2,..]>

   public TermObject(final String _term) 
   {
      this.term = _term;
      this.coll_freq = 0;
      this.doc_freq = 0;
      this.postings = new HashMap<Integer, ArrayList<Integer>>();
   }

   /**
    * No-argument constructor required by {@link Externalizable}.
    */
   public TermObject() 
   {}

   public String getTerm() 
   {
      return term;
   }

   public int getCollectionFreq() 
   {
      return coll_freq;
   }

   public void setCollectionFreq(int freq)
   {
	   this.coll_freq = freq;
   }
   
   public void addCollectionFreq() 
   {
	   this.coll_freq++;
   }
   
   public int getDocFreq() 
   {
	   return doc_freq;
   }

   public void setDocFreq(int freq)
   {
	   this.doc_freq = freq;
   }
   public void addDocFreq(final int doc_id) 
   {
	   // If this doc_id is already in current postings, do nothing
	   if(this.postings.get(new Integer(doc_id)) == null)
	   {
		   this.doc_freq++;
	   }
   }
   
   public Map<Integer, ArrayList<Integer>> getPostings()
   {
	  return this.postings; 
   }
   
   public void setPostings(Map<Integer, ArrayList<Integer>> _postings)
   {
	   this.postings = _postings;
   }
   
   public void addPostings(final int doc_id, final int pos)
   {
	   ArrayList<Integer> _term_pos = this.postings.get(new Integer(doc_id));
	   if(_term_pos == null)
	   {
		   _term_pos = new ArrayList<Integer>();
		   _term_pos.add(new Integer(pos));		   
	   }
	   else
	   {
		   // If pos is not in the postions list
		   if(!_term_pos.contains(new Integer(pos)))
		   {
			   _term_pos.add(new Integer(pos));			   
		   }
	   }
	   this.postings.put(new Integer(doc_id), _term_pos);
   }
   
   public void addObject(final Object o)
   {
	   if(this == o) return;
	   if(o == null || getClass() != o.getClass()) return;
	   
	   TermObject _to = (TermObject)o;
	   if(this.getTerm().compareTo(_to.getTerm()) != 0)
		   return;
	   
	   // Merge colleciton frequency
	   this.coll_freq += _to.getCollectionFreq();
	   
	   // Merge document frequency & postings
	  Iterator it = _to.getPostings().keySet().iterator();
	   
	   while(it.hasNext())
	   {
		   Integer docID = (Integer)it.next();
		   if(this.postings.get(docID) == null)
		   {
			   // If doc id is not in this object, increment document frequency
			   this.doc_freq++;
			   this.postings.put(docID, _to.getPostings().get(docID));
		   }
		   else
		   {
			   ArrayList<Integer> _token_pos = this.postings.get(docID);
			   ArrayList<Integer> to_be_added = _to.getPostings().get(docID);
			   for(int i = 0; i < to_be_added.size(); ++i)
			   {
				   _token_pos.add(to_be_added.get(i));
			   }
			   
			   this.postings.remove(docID);
			   this.postings.put(docID, _token_pos);
		   }
	   }
   }
   
   public void writeExternal(final ObjectOutput out) throws IOException 
   {
      out.writeUTF(this.term);
      out.writeInt(this.coll_freq);
      out.writeInt(this.doc_freq);
      out.writeObject(this.postings);
   }

   public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException 
   {
      this.term = in.readUTF();
      this.coll_freq = in.readInt();
      this.doc_freq = in.readInt();
      this.postings = (Map<Integer, ArrayList<Integer>>)in.readObject();
   }

   public boolean equals(final Object o) 
   {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      
      final TermObject _termO = (TermObject) o;
      if (this.term != _termO.term) return false;
      
      return true;
   }
   
   public int compareTo(final Object o)
   {
	   	if (this == o) return 0;
	  	if (o == null || getClass() != o.getClass()) return -1;
	    
	  	final TermObject _termO = (TermObject) o;
	  
	  	String str1 = this.getTerm();
	  	String str2 = _termO.getTerm();
	  	
	  	str1 = str1 == null ? "" : str1;
	  	str2 = str2 == null ? "" : str2;
	  	if(str1.isEmpty() && str2.isEmpty())
	  		return 0;
	  	else if(str1.isEmpty())
	  		return 1;
	  	else if(str2.isEmpty())
	  		return -1;
	  	else
	  	{
		  	if(Character.isAlphabetic(str1.toCharArray()[0]) 
	  			&& !Character.isAlphabetic(str2.toCharArray()[0]))
			{
				return -1;
			}
			else if(!Character.isAlphabetic(str1.toCharArray()[0]) 
					&& Character.isAlphabetic(str2.toCharArray()[0]))
			{
				return 1;
			}
			else
			{
				return str1.compareTo(str2);
			}
	  	}
   }
}