package edu.umd.lib.wufoosysaid;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Text;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

/**
 * Servlet implementation class EntryController
 */
public class EntryController extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static Logger log = Logger.getLogger(EntryController.class);
  private static XMLOutputter output = new XMLOutputter(
      Format.getPrettyFormat());

  private static final String XPATH_ID = "//ID[.='%ID%']/../Label/text()|//ID[.='%ID%']/../Title[not(../SubField)]/text()";

  /**
   * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
   *      response)
   */
  @SuppressWarnings("unchecked")
  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    /* ServletContext used for setting attributes for debugging */
    ServletContext context = getServletContext();

    /*
     * Checks that the handshake key included in the request is valid, assuming
     * a handshake key was specified in the configuration. If not yet
     * configured, user is warned of security risk
     */

    String handshake = context.getInitParameter("handshakeKey");

    if (StringUtils.isEmpty(handshake)) {
      log.warn("No handshake key is set in handshake.xml. "
          + "Without authentication, your service may be vulnerable "
          + "to spam and other attacks.");
    } else {
      String requestKey = request.getParameter("HandshakeKey");
      if (StringUtils.isEmpty(requestKey)) {
        String error = "Handshake key is missing from request. Make sure "
            + "form notifications are properly configured";
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, error);
        return;
      } else if (!StringUtils.equals(handshake, requestKey)) {
        String error = "Invalid handshake key. Make sure the correct "
            + "handshake key is configured for your form notification";
        response.sendError(HttpServletResponse.SC_FORBIDDEN, error);
        return;
      }
    }

    /*
     * Extracts form hash from path to identify form and entry id for
     * identifying entries
     */
    String path = request.getRequestURI();
    String hash = request.getPathInfo().replace("/", "");
    String entryId = request.getParameter("EntryId");
    log.debug("POST path: " + path);
    log.debug("Hash of " + hash + " extracted.");

    /*
     * Creates a map of all parameters that represent form fields and their
     * values
     */
    Map<String, String> fields = new HashMap<String, String>();

    Map<String, String[]> parameterMap = request.getParameterMap();
    Set<String> parameterNames = parameterMap.keySet();
    for (String name : parameterNames) {
      if (name.contains("Field")) {
        fields.put(name, parameterMap.get(name)[0]);
      }
    }
    /* Removes field structure from fields map */
    fields.remove("FieldStructure");

    /*
     * Extracts FieldStructure from request, and performs a slight modification
     * so it can be properly converted to XML (it is passed as JSON). This will
     * be used to extract the titles of fields
     */
    String fieldStructure = null;
    try {
      fieldStructure = parameterMap.get("FieldStructure")[0];
    } catch (NullPointerException e) {
      log.error(
          "Error in Wufoo Form Webhook configuration, check the chbox for including field structures",
          e);
      response
      .sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Error in Wufoo Form Webhook configuration, can't find field structures");
      return;
    }
    fieldStructure = fieldStructure.replaceAll("Fields", "Field");

    /*
     * Converts the FieldStructure from JSON to XML, then creates a JDOM
     * document object which represents this XML, which will later be parsed for
     * the titles of fields
     */
    Document structure;
    String fieldXml;
    try {
      JSONObject fieldJson = new JSONObject(fieldStructure);
      fieldXml = XML.toString(fieldJson, "Fields");
      StringReader xmlReader = new StringReader(fieldXml);
      SAXBuilder builder = new SAXBuilder();
      structure = builder.build(xmlReader);
    } catch (JSONException e) {
      e.printStackTrace();
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Error in parsing fieldStructure JSON");
      return;
    } catch (JDOMException e) {
      e.printStackTrace();
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Error in coverting fieldStructure into XML");
      return;
    }
    // log.debug("Structure XML: \n" + output.outputString(structure));

    /*
     * Creates a new Entry with the form hash, entry id, and field values
     */

    Entry entry = new Entry(hash, entryId, fields);
    context.setAttribute("entry", entry);

    /*
     * Creates xml to represent the entry, beginning with the root entry element
     */
    Element root = new Element("entry");
    root.setAttribute("hash", hash);
    root.setAttribute("entryId", entryId);

    /*
     * Traverses the parameter map and creates an element for each form field
     */
    String[] fieldNames = new String[fields.keySet().size()];
    fieldNames = fields.keySet().toArray(fieldNames);

    for (String name : fieldNames) {
      Element field = new Element("field");
      field.setAttribute("id", name);
      field.setText(fields.get(name));

      /* Traverses field structure XML to determine title of field */
      String xpath = XPATH_ID.replace("%ID%", name);
      XPathExpression<Text> expression = XPathFactory.instance().compile(xpath,
          Filters.textOnly());
      Text titleText = expression.evaluateFirst(structure);
      if (titleText != null) {
        String title = titleText.getText();
        field.setAttribute("title", title);
      }
      root.addContent(field);
    }

    /*
     * Creates an XML document with the entry as the root element and then
     * outputs the document as a string
     */
    Document entryDoc = new Document(root);
    log.debug("Entry XML: \n" + output.outputString(entryDoc));
    /*
     * Creates a RequestBuilder that transforms Wufoo entry XML into SysAid
     * request XML
     */
    Document requestDoc;
    RequestBuilder builder;
    try {
      builder = new RequestBuilder(context, hash, entryDoc);
      requestDoc = builder.getRequest();
    } catch (JDOMException e) {
      String errormsg = "Exception occured while trying to parse DOM of "
          + hash + ".xsl. File may not be well-formed.";
      log.error(errormsg, e);
      response
      .sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, errormsg);
      return;
    } catch (MalformedURLException e) {
      String errormsg = "Malformed URL created from hash " + hash
          + ", check that hash is valid.";
      log.error(errormsg, e);
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, errormsg);
      return;
    } catch (IOException e) {
      String errormsg = "Exception occurred while trying to load " + hash
          + ".xsl. File may not exist or you may not have proper permissions.";
      log.error(errormsg, e);
      response.sendError(HttpServletResponse.SC_NOT_FOUND, errormsg);
      return;
    }

    if (requestDoc != null) {
      log.debug("Printing REQUEST DOC------------: \n"
          + output.outputString(requestDoc));
      builder.sendRequests();
      /*
       * Sets the response status code to OK and the response to the SysAid
       * Requests XML for debugging purposes
       */
      StringWriter sw = new StringWriter();
      output.output(entryDoc, sw);

      response.setCharacterEncoding("UTF-8");
      response.setContentType("text/xml");
      response.getWriter().write(sw.toString());
      response.setStatus(HttpServletResponse.SC_OK);
    } else {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Error occured while attempting to create requests from entry.");
      return;
      // BAD TEMPORARY FIX, Exit if no XSL was found!
      // (To avoid multiple do post requests by container)

      // System.exit(0);
    }
  }
}
