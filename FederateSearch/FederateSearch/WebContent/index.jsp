<%@ page import="java.util.List" %>
<%@ page import="search.SearchResult" %>
<%@ page import="java.util.Map" %>
<!DOCTYPE html>

<html>
<head>
	<title>INFSCI 2711 Query Project</title>

	<meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="css/bootstrap.min.css" rel="stylesheet" media="screen">
    <link rel="icon" href="img/sql2.png" type="image/png">
    	 <link rel="stylesheet" type="text/css" href="css/style.css" title="style" />
</head>
<body style="margin: 25px;">
	<div class="row">
			
			<div class="page-header">
				
			  	<h3 class="text-center text-muted">Federate Search Facebook & Twitter.</h3>
			</div>
	

			<div id="queryContainer">
			
	        <form action="FTsearch" method="get">
				<div id="querybox">
					
	       <input type="text" name="queries" style="height:40px;font-size:44pt;" align="middle"/>
				</div>
	
	           <div id="runbutton">
				<button class="btn btn-primary pull-right" type="submit">Run</button>
				</div>
			</form>
				
			
	</div>
	
    <script type="text/javascript" src="javascripts/jquery-2.1.3.min.js"></script>
    <script type="text/javascript" src="javascripts/bootstrap.min.js"></script>

</body>
</html>
