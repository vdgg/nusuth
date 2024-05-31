<html>
<head>
<title>Directory listing</title>
</head>
<body>
<%@ page language="java" import="com.azoft.nusuth.util.*,java.io.*,java.util.*, java.net.URLDecoder" %>
<%
  String docBase     = (String)request.getAttribute("docBase");
  String uri         = (String)request.getAttribute("uri");
  String fileString  = (String)request.getAttribute("file");
  contextPath = request.getContextPath();
  File file = new File(fileString);
  File[] files = file.listFiles();
  LinkedList realFiles = new LinkedList();
  LinkedList dirs = new LinkedList();
  for (int i=0; i < files.length; i++){
    if (files[i].isDirectory()) {
      dirs.add(files[i]);
    } else {
      realFiles.add(files[i]);
    }
  }

%>
<%!
   String contextPath = null;
%>
<%!
    private String cutString(String s) {
      if (s.length() == 0) {
        return "";
      }
      if(s.endsWith("/")) {
        return cutString(s.substring(0, s.length() - 1));
      } else {
        String tmp = s.substring(0, s.lastIndexOf("/"));
        if (tmp.length() == 0 && contextPath.length() == 0) {
          return "/";
        } else {
          return tmp;
        } 
      }
    }
%>
<H2>Directory listing for <%= URLDecoder.decode(uri) %> </H2><BR><BR>
<TABLE WIDTH=80%>
<TH></TH>
<TH ALIGN="LEFT">Name</TH>
<TH ALIGN="LEFT">Size</TH>
<TH ALIGN="LEFT">Date</TH>
<TR><TD>Folders:</TD><TD></TD><TD></TD><TD></TD></TR>
<%
  if( !cutString(uri).equals("") || (contextPath.length() == 0 && !(uri.equals("") || uri.equals("/")))) {
%>
  <TR><TD></TD><TD><A HREF="<%= cutString(uri) %>"> .. </A> </TD><TD></TD></TR>
<%
  }
  for (int i=0; i < dirs.size(); i++) {
%>
  <TR>
  <TD></TD><TD><A HREF="<%= uri + (uri.endsWith("/") ? "" : "/") + ((File)dirs.get(i)).getName() %>"> <%= ((File)dirs.get(i)).getName() %>  </A> </TD>
  <TD></TD>
  <%
    HttpDate httpDate = new HttpDate();
  %>
  <TD> <%= new String(httpDate.convert(((File)dirs.get(i)).lastModified()), 0, 16) %> </TD>
  </TR>
<%
  }
%>
<TR><TD>Files:</TD><TD></TD><TD></TD><TD></TD></TR>
<%
  for (int i = 0; i < realFiles.size(); i++) {
%>
  <TR>
  <TD></TD><TD><A HREF="<%= uri + (uri.endsWith("/") ? "" : "/") + ((File)realFiles.get(i)).getName() %>"> <%= ((File)realFiles.get(i)).getName() %> </A> </TD>
  <%
    long fileSize = ((File)realFiles.get(i)).length();
    String size;
    if(fileSize < 1000)
        size = fileSize + "b";
    else
    if(fileSize < 1000000)
    {
      size = String.valueOf((double)fileSize / 1024);
      if(size.length() > 5) {
          size = size.substring(0, 5) + "Kb";
      } else {
          size = size + "Kb";
      }
    }
    else
    {
      size = String.valueOf((double)fileSize / 1048576);
      if(size.length() > 5) {
          size = size.substring(0, 5) + "Mb";
      } else {
          size = size + "Mb";
      }
    }
  %>
  <TD> <%= size %> </TD>
  <%
    HttpDate httpDate = new HttpDate();
  %>
  <TD>  <%= new String(httpDate.convert(((File)realFiles.get(i)).lastModified()), 0, 16) %>   </TD>
  </TR>
<%
  }
%>
</TABLE>
</body>
</html>