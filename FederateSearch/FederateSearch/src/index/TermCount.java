package index;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TermCount {
    
    
    private Map<String,Integer> doc_term_count=new HashMap<String,Integer>();
   public  Integer coll_count=0; 
   public TermCount(){
       
   }
     public TermCount(Map<String,Integer> tc){
         doc_term_count=tc;
     }

    public Integer getColl_count() {
        return coll_count;
    }
    public void setDoc_term_count(Map<String, Integer> doc_term_count) {
        this.doc_term_count = doc_term_count;
    }
    public void addDoc_term_count(String s,Integer i) {
       doc_term_count.put(s, i);
    }
    public Map<String, Integer> getDoc_term_count() {
        return doc_term_count;
    }
    public Integer sumMap(Map mp) {
        Iterator it = mp.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            coll_count+=(Integer) pair.getValue();
            //System.out.println(pair.getKey() + " = " + pair.getValue());
            it.remove(); // avoids a ConcurrentModificationException
        }
        return coll_count;
    }
     
    
  
}
