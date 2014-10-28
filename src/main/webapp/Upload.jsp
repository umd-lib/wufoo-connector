<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"%>

<%@ page import="java.io.BufferedReader" %>
<%@ page import="java.io.File" %>
<%@ page import="java.io.FileReader" %>
<%@ page import="java.io.IOException"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Upload Page</title>
<script src="http:////cdnjs.cloudflare.com/ajax/libs/jquery/2.1.1/jquery.js"></script>
<script src="http://cdnjs.cloudflare.com/ajax/libs/twitter-bootstrap/3.2.0/js/bootstrap.min.js"></script>
<link href="http://cdnjs.cloudflare.com/ajax/libs/twitter-bootstrap/3.2.0/css/bootstrap.min.css" rel="stylesheet"/>
<script type="text/javascript">

$(document).ready(function(){
    $("#selectBoxId").change(function(){
        console.log($(this).val());
            
        var pathOfFile = "http://localhost:8080/xsls/"+$(this).val();
        console.log(pathOfFile);
        $("#anchorId").attr("href",pathOfFile);
        
    });
    
});

</script>
</head>
<body> 
<h2 align="center"> Wufoo-Connector</h2>
<h4 align="center"> Admin Interface for Upload and Download of XSLs</h4>
<div class="container">
   <div class="row" style="margin-top:50px;">
        <div class="col-md-4"></div>
        <div class="col-md-4">
  <div class="">
                 <select class="form-control" id="selectBoxId">
                 <option value="" default selected>Select XSL</option>
                <%
                    File folder = new File("/apps/apache-tomcat-7.0.56/webapps/xsls/");
                    File[] listOfFiles = folder.listFiles();

                    for (File file : listOfFiles) {
                            if (file.isFile()) {
                            out.println("<option value='"+file.getName()+"'>"+ file.getName()+"</option>");
                            }
                    }
                    
                %>
                </select> 
        </div>
        </div>
        <div class="col-md-4"></div>
  </div>
  <div class="row" align="center">
    <a href="" id="anchorId" download><button class="btn btn-primary" style="margin:10px">Download</button> </a>
  <form name="form1" id="form1" action="UploadFile" method="post" enctype="multipart/form-data">
     <input type="hidden" name="hiddenfield1" value="ok">
     <br/>
     <input type="file" size="50" name="file3">
     <br/>
     <input type="submit" class="btn btn-primary" value="Upload"> 
  </form>
   </div>
    
</div>
</body>
</html>