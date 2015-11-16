package infsci.is.api;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import search.SearchResult;
import facebookapi.PostContent;
import facebookapi.Query;

/**
 * Servlet implementation class FTsearch
 */
@WebServlet("/FTsearch")
public class FTsearch extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public FTsearch() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	    String query=request.getParameter("queries");
            System.out.println(query);
           Query qu=new Query("/Users/wuhao/Documents/Infromation Retrieval&Storage/Final Project/indexFloder",query);
           List<SearchResult> results=qu.run();
           PostContent pcontent=new PostContent("/Users/wuhao/Documents/Infromation Retrieval&Storage/Final Project/collection_original.txt" );
           
           
           
           Map<String,String>postobject=pcontent.run();
           if(postobject==null){System.out.println("not get pcontent");}
           else{
               System.out.println("get content!!!!!!!");
           }
          //System.out.println(postobject.get("TW371")); 
          request.setAttribute("postResult",postobject);
          
          request.setAttribute("searchResult",results);
          ServletContext ctx = request.getServletContext();
          RequestDispatcher rd = ctx.getRequestDispatcher("/result.jsp");
          rd.forward(request, response);
          //response.sendRedirect("result.jsp");
        /**
           for( SearchResult result:results ){
               System.out.println(" Q0 "+result.docno()+" MYRUN");
        
               
         
       }
**/
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

}
