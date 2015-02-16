package edu.umd.lib.wufoosysaid;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

/**
 * Servlet implementation class UploadFile
 */
@WebServlet("/UploadFile")
public class UploadFile extends HttpServlet {
  private static final long serialVersionUID = 1L;
  static String XSL_PATH_File;

  /**
   * @see HttpServlet#HttpServlet()
   */
  public UploadFile() {
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
    // TODO Auto-generated method stub
  }

  /**
   * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
   *      response)
   */
  @SuppressWarnings("unchecked")
  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    /**
     * Get XSL file path from web.xml
     */

    XSL_PATH_File = getServletContext().getInitParameter("xslLocation");

    response.setContentType("text/html");
    PrintWriter out = response.getWriter();

    System.out.println("PATH XSL:" + XSL_PATH_File);

    /**
     * File upload logic
     */
    FileItemFactory factory = new DiskFileItemFactory();
    ServletFileUpload upload = new ServletFileUpload(factory);
    try {
      List<FileItem> fields = upload.parseRequest(request);

      Iterator<FileItem> it = fields.iterator();
      while (it.hasNext()) {

        FileItem fileItem = it.next();
        boolean isFormField = fileItem.isFormField();
        if (isFormField == false) {
          out.println("NAME: " + fileItem.getName() + "<br/>SIZE (BYTES): "
              + fileItem.getSize());
          out.println("<br> <b>The file has been successfully uploaded !<b>");
          out.println("<br><br><a href=\"upload.jsp\">Back</a>");

          File f = new File(XSL_PATH_File + "/" + fileItem.getName());
          fileItem.write(f);
        }

      }

    } catch (FileUploadException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
