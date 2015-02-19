package edu.umd.lib.wufoosysaid;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class DownloadFile
 */
@WebServlet("/DownloadFile")
public class DownloadFile extends HttpServlet implements javax.servlet.Servlet {
  private static final long serialVersionUID = 10098799L;
  private static final int BUFSIZE = 4096;
  String filePath;

  /**
   * @see HttpServlet#HttpServlet()
   */
  public DownloadFile() {
    super();
    // TODO Auto-generated constructor stub
  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
   *      response)
   */
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    ServletContext sc = getServletContext();

    /**
     * Get XSL file path from web.xml and filename from Upload.jsp in request
     */
    String filePath = sc.getInitParameter("xslLocation");
    String filename = request.getParameter("filename");

    /**
     * Write file on file system by reading contents of file on server
     */
    System.out.println("File Path is" + filePath + " and File Name is "
        + filename);
    File file = new File(filePath);
    int length = 0;
    ServletOutputStream outStream = response.getOutputStream();
    response.setContentType("text/html");
    response.setContentLength((int) file.length());
    String fileName = (new File(filePath)).getName();
    response.setHeader("Content-Disposition", "attachment; filename=\""
        + fileName + "\"");

    byte[] byteBuffer = new byte[BUFSIZE];
    DataInputStream in = new DataInputStream(new FileInputStream(file));

    while ((in != null) && ((length = in.read(byteBuffer)) != -1)) {
      outStream.write(byteBuffer, 0, length);
    }

    in.close();
    outStream.close();
  }

  /**
   * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
   *      response)
   */
  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    // TODO Auto-generated method stub
  }

}
