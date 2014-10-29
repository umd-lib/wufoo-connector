package edu.umd.lib.wufoosysaid;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.transform.XSLTransformException;
import org.jdom2.transform.XSLTransformer;

public class RequestBuilder {
  private static Logger log = Logger.getLogger(RequestBuilder.class);
  private static XMLOutputter output = new XMLOutputter(
      Format.getPrettyFormat());

  private String sysaid_URL;
  private String sysaid_accountID;
  private String sysaid_formID;
  private URI requestSysAidURI = null;

  private String alephrx_URL;
  private URI requestAlephRxURI = null;

  private static String REGEX_LT = "__lt__.";
  private static String REGEX_GT = "__gt__.";
  private static String REGEX_AND = "__and__.";
  private static String REGEX_COLON = "__..__.";

  private XSLTransformer xsl_transformer;
  private Document request_document;

  private ServletContext context;

  /**
   * @param sc
   * @param xsl_hash
   * @param entry_document
   */
  public RequestBuilder(ServletContext sc, String xsl_hash,
      Document entry_document) throws IOException, XSLTransformException {
    this.context = sc;
    /*
     * Constructs a transformer using the xsl document that the XSLGetter class
     * provides based on the form hash. This will be used to transform entry xml
     * to request xml.
     */
    Document xsl_document = XSLGetter.getXSLDocument(context, xsl_hash);

    if (xsl_document != null) {
      xsl_transformer = new XSLTransformer(xsl_document);
      try {
        request_document = xsl_transformer.transform(entry_document);
        log.debug("Request Document: \n"
            + output.outputString(request_document));
      } catch (XSLTransformException e) {
        log.error("Exception occured while attempting to transform XSL file.",
            e);
      }

      // Extracting root element from the xsl and processing its requests
      Element root_element = request_document.getRootElement();
      List<Element> requests = root_element.getChildren("request");
      for (Element req : requests) {
        extractParams(req);
      }
    } else {
      createSampleXSL(entry_document);
      request_document = null;
    }
  }

  private void createSampleXSL(Document entry_document) {
    /* extracting data from incoming for post */
    Element entry_root = entry_document.getRootElement();
    String sample_filename = entry_root.getAttribute("hash").getValue();

    List<Element> children = entry_root.getChildren();

    /* populating list of tagnames and their values */
    List<String> sample_tagnames = new ArrayList<String>();
    List<String> sample_tagvalues = new ArrayList<String>();
    for (Element child : children) {
      sample_tagnames.add(child.getAttribute("title").getValue());
      sample_tagvalues.add(child.getValue());
    }

    /*
     * attempting to write XSL Stylesheet
     *
     * :, <, >, & are replaced by REGEX_COLON, REGEX_LT, REGEX_RT, REGEX_AND for
     * validation of XSL Document. Will be replaced back at the time of file
     * creation.
     */
    // <xsl:stylesheet>
    Element sample_root = new Element("xsl" + REGEX_COLON + "stylesheet");
    sample_root.setAttribute("version", "1.0");
    sample_root.setAttribute("xmlns" + REGEX_COLON + "xsl",
        "http://www.w3.org/1999/XSL/Transform");
    // <xsl:variable>
    Element sample_xsl_variable = new Element("xsl" + REGEX_COLON + "variable");
    sample_xsl_variable.setAttribute("name", "requestUser");
    sample_xsl_variable.setText(REGEX_LT
        + "xsl:value-of select=\"//field[@title='First']\"/" + REGEX_GT
        + REGEX_AND + "#160;" + REGEX_LT
        + "xsl:value-of select=\"field[@title='Last']\"" + REGEX_GT);
    sample_root.addContent(sample_xsl_variable);
    // </xsl:variable>

    // <xsl:template>
    Element sample_xsl_template = new Element("xsl" + REGEX_COLON + "template");
    sample_xsl_template.setAttribute("match", "/entry");

    // <requests>
    Element sample_req_root = new Element("requests");
    // <request>
    Element sample_req = new Element("request");

    // <target>
    Element sample_target = new Element("target");
    sample_target.setAttribute("type", "sample type");
    sample_target.setAttribute("comment", "sysaid/alephrx/etc..");

    // <url>
    Element sample_url = new Element("url");
    sample_url.setText("http://sample url");
    sample_target.addContent(sample_url);
    // </url>

    // <formId>
    Element sample_formID = new Element("formId");
    sample_formID.setText("sample form ID");
    sample_target.addContent(sample_formID);
    // </formId>

    // <accountId>
    Element sample_accountID = new Element("accountId");
    sample_accountID.setText("sample account ID");
    sample_target.addContent(sample_accountID);
    // </accountId>

    sample_req.addContent(sample_target);
    // </target>

    // <fields>
    for (int i = 0; i < sample_tagnames.size(); i++) {
      Element tmp = new Element(sample_tagnames.get(i).toLowerCase()
          .replaceAll(" ", "_").replaceAll("[?,;\":|@/\\\']", ""));
      tmp.setText(REGEX_LT + "xsl:value-of select=\"//field[@title='"
          + sample_tagnames.get(i) + "']\"/" + REGEX_GT);
      // tmp.setText(sample_tagvalues.get(i));
      sample_req.addContent(tmp);
    }
    // </fields>

    sample_req_root.addContent(sample_req);
    // </request>
    sample_xsl_template.addContent(sample_req_root);
    // </requests>
    sample_root.addContent(sample_xsl_template);
    // </xsl:template>
    // </xsl:stylesheet>

    /* preparing XSL Document to write in a file */
    Document sample_document = new Document(sample_root);

    /* create new file at xsl location */
    try {
      String filepath = context.getInitParameter("xslLocation") + "example/";
      new File(filepath).mkdirs();

      log.debug("Attempting to create sample xsl for " + sample_filename);
      File f = new File(String.format(filepath + "%s.xsl", "sample_"
          + sample_filename));
      f.createNewFile();

      /* write XSL Document to xsl file */
      BufferedWriter bw = new BufferedWriter(new FileWriter(f));
      // replacing REGEXes back
      bw.write(output.outputString(sample_document).replaceAll(REGEX_LT, "<")
          .replaceAll(REGEX_GT, ">").replaceAll(REGEX_AND, "&")
          .replaceAll(REGEX_COLON, ":"));
      bw.close();

      log.debug("Successfully created " + sample_filename + ".xsl at "
          + filepath);
    } catch (IOException e) {
      e.printStackTrace();

    }
  }

  private void extractParams(Element req) {
    if (extractTargetType(req).equals("sysaid")) {
      log.debug("TARGET TYPE IS:----" + extractTargetType(req));
      /* Gets Sysaid request parameters */
      String tmp_url = extractUrl(req);
      String tmp_formID = extractFormId(req);
      String tmp_accountID = extractAccountId(req);

      if (StringUtils.isEmpty(tmp_url) || tmp_url.contains("example.com")) {
        log.warn("TargetURL (\""
            + tmp_url
            + "\") appears empty or unchanged in SYSAID request element in your xsl. "
            + "This value should be changed to reflect the location "
            + "of your SYSAID installation");
      } else {
        this.sysaid_URL = tmp_url;
        log.debug("TargetURL: " + sysaid_URL);
      }

      if (StringUtils.isBlank(tmp_formID)
          || StringUtils.equals(tmp_formID, "0")) {
        log.warn("formID appears empty or unchanged in SYSAID request element in your xsl. "
            + "This value should be changed so that your requests will be "
            + "properly authorized and accepted BY SYSAID.");
      } else {
        this.sysaid_formID = tmp_formID;
        log.debug("formID: " + sysaid_formID);
      }

      if (StringUtils.isBlank(tmp_accountID)) {
        log.warn("accountID appears empty or unchanged in SYSAID request element in your xsl. "
            + "This value should be changed to your SYSAID account id.");
      } else {
        this.sysaid_accountID = tmp_accountID;
        log.debug("accountID: " + sysaid_accountID);
      }
    } else if (extractTargetType(req).equals("alephrx")) {
      log.debug("TARGET TYPE IS:----" + extractTargetType(req));
      String tmp_url = extractUrl(req);

      if (StringUtils.isEmpty(tmp_url) || tmp_url.contains("example.com")) {
        log.warn("TargetURL (\""
            + tmp_url
            + "\") appears empty or unchanged in ALEPHRX request element in your xsl. "
            + "This value should be changed to reflect the location "
            + "of your ALEPHRX installation");
      } else {
        this.alephrx_URL = tmp_url;
        log.debug("TargetURL: " + alephrx_URL);
      }
    } else {
      log.error("NO TARGET DEFINED IN REQUEST. Check xsl for this form.");
    }
  }

  public void sendRequests() throws IOException {

    /*
     * Transforms each request element in the sysaid xml into a HTTP POST
     * request that has the parameters for a sysaid request encoded in the URL.
     * The request is then executed, at which point Sysaid will then use this
     * information to create a new request
     */

    Element root = request_document.getRootElement();
    List<Element> requests = root.getChildren("request");
    /*
     * Service only attempts to create a URI from TargetURL if it appears
     * legitimate, to prevent invalid requests.
     */
    if (StringUtils.isBlank(sysaid_URL)) {
      log.warn("Specified SYSAID URL is either blank or unchanged from "
          + "default value. Can't create URI with this URL. Will not attempt to execute HTTP request.");
    } else {
      try {
        this.requestSysAidURI = new URI(sysaid_URL);
      } catch (URISyntaxException e) {
        log.error("Exception occured while attempting to create SYSAID URI "
            + "object with path " + sysaid_URL + ". Verify that a valid URI "
            + "is specified in configuration.", e);
        return;
      }
    }

    if (StringUtils.isBlank(alephrx_URL)) {
      log.warn("Specified ALEPHRX URL is either blank or unchanged from "
          + "default value. Can't create URI with this URL. Will not attempt to execute HTTP request.");
    } else {
      try {
        this.requestAlephRxURI = new URI(alephrx_URL);
      } catch (URISyntaxException e) {
        log.error("Exception occured while attempting to create ALEPHRX URI "
            + "object with path " + alephrx_URL + ". Verify that a valid URI "
            + "is specified in configuration.", e);
        return;
      }
    }

    for (Element req : requests) {
      processPostRequest(req);
    }
  }

  private void processPostRequest(Element req) {
    if (extractTargetType(req).equals("sysaid")) {
      log.debug("SYSAID URI: " + extractTargetType(req));
      try {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        CloseableHttpResponse response = null;

        if (this.requestSysAidURI != null) {
          /*
           * Request parameters are encoded into the URL as Name-Value Pairs. To
           * create these pairs, the element names need to be translated into
           * the parameters expected by SysAid
           */
          /*
           * Creates an entity from the list of parameters and associates it
           * with the POST request. This request is then executed if a valid URI
           * was constructed earlier
           */
          /*
           * Closes httpclient regardless of rather it was successful or not. If
           * no response was received, request parameters are logged for
           * debugging.
           */

          HttpPost httpPost = new HttpPost(requestSysAidURI);

          List<NameValuePair> fields = extractFields_SysAid(req);
          log.debug("Request parameters: \n" + fields);

          httpPost.setEntity(new UrlEncodedFormEntity(fields, "UTF-8"));
          response = httpclient.execute(httpPost);
          httpclient.close();

          if (response != null) {
            log.debug("Response: \n" + response.toString());
          } else {
            log.warn("Unable to execute POST request.\nRequest parameters: \n"
                + fields);
          }
          response = null;
          httpPost = null;
          httpclient = null;
        } else {
          log.error("requestSysAidURI empty!");
          return;
        }
      } catch (ClientProtocolException e) {
        log.error("ClientProtocolException occured while attempting to "
            + "execute POST request. Ensure this service is properly "
            + "configured and that the server you are attempting to make "
            + "a request to is currently running.", e);
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
    } else if (extractTargetType(req).equals("alephrx")) {
      log.debug("ALEPHRX URI: " + extractTargetType(req));
      try {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        CloseableHttpResponse response = null;

        if (this.requestAlephRxURI != null) {
          /*
           * Request parameters are encoded into the URL as Name-Value Pairs. To
           * create these pairs, the element names need to be translated into
           * <<<<<<< HEAD the parameters expected by SysAid ======= the
           * parameters expected by AlephRx >>>>>>>
           * a2a59b1d9339bd471567f8cf2c8cb17846065b74
           */
          /*
           * Creates an entity from the list of parameters and associates it
           * with the POST request. This request is then executed if a valid URI
           * was constructed earlier
           */
          /*
           * Closes httpclient regardless of rather it was successful or not. If
           * no response was received, request parameters are logged for
           * debugging.
           */

          HttpPost httpPost = new HttpPost(requestAlephRxURI);

          List<NameValuePair> fields = extractFields_AlephRX(req);
          log.debug("Request parameters: \n" + fields);

          httpPost.setEntity(new UrlEncodedFormEntity(fields, "UTF-8"));
          response = httpclient.execute(httpPost);
          httpclient.close();

          if (response != null) {
            log.debug("Response: \n" + response.toString());
          } else {
            log.warn("Unable to execute POST request.\nRequest parameters: \n"
                + fields);
          }
          response = null;
          httpPost = null;
          httpclient = null;
        } else {
          log.error("requestAlephRxURI empty!");
          return;
        }
      } catch (ClientProtocolException e) {
        log.error("ClientProtocolException occured while attempting to "
            + "execute POST request. Ensure this service is properly "
            + "configured and that the server you are attempting to make "
            + "a request to is currently running.", e);
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
    } else {
      log.error("NO TARGET DEFINED IN REQUEST");
    }
  }

  public Document getRequest() {
    return request_document;
  }

  protected String extractUrl(Element req) {
    /* Extracts url from request */
    Element target = req.getChild("target");
    List<Element> items = target.getChildren();
    Element url = items.get(0);

    return url.getText();
  }

  protected String extractFormId(Element req) {
    /* Extracts formId from request */
    Element target = req.getChild("target");
    List<Element> items = target.getChildren();
    Element formId = items.get(1);

    return formId.getText();
  }

  protected String extractAccountId(Element req) {
    /* Extracts accountId from request */
    Element target = req.getChild("target");
    List<Element> items = target.getChildren();
    Element accountId = items.get(2);

    return accountId.getText();
  }

  protected String extractTargetType(Element req) {
    /* Extracts destination target from request */
    Element target = req.getChild("target");
    String targetType = target.getAttributeValue("type");

    return targetType;
  }

  protected List<NameValuePair> extractFields_AlephRX(Element req) {

    /*
     * Constructs a list of parameters from a Request element. These parameters
     * follow the format used by AlephRx to submit webforms and can be used to
     * create an UrlEncodedFormEntity.
     */

    List<NameValuePair> fields = new ArrayList<NameValuePair>();

    Element name = req.getChild("name");
    Element functional_area = req.getChild("functional_area");
    Element campus = req.getChild("campus");
    Element phone = req.getChild("phone");
    Element email = req.getChild("email");
    Element status = req.getChild("status");
    Element summary = req.getChild("summary");
    Element text = req.getChild("text");
    Element submitter_name = req.getChild("submitter_name");

    /* Request summary */
    fields.add(new BasicNameValuePair("summary", summary.getText()));

    /* Request detail */
    fields.add(new BasicNameValuePair("text", text.getText()));

    /* Name of the submit user */
    fields.add(new BasicNameValuePair("submitter_name", submitter_name
        .getText()));

    /* Name and e-mail are used to determine request user */
    fields.add(new BasicNameValuePair("name", name.getText()));
    fields.add(new BasicNameValuePair("email", email.getText()));
    fields.add(new BasicNameValuePair("phone", phone.getText()));

    /* Functional Area and campus to which this request belongs */
    fields.add(new BasicNameValuePair("functional_area", functional_area
        .getText()));
    fields.add(new BasicNameValuePair("campus", campus.getText()));

    /* Request status */
    fields.add(new BasicNameValuePair("status", status.getText()));

    /* Request report */
    // fields.add(new BasicNameValuePair("report", report.getText()));

    /*
     * The relevant USMAI campus for the request, will only be applicable for
     * some forms.
     */
    if (campus != null) {
      String usmai = campus.getText();
      Properties prop = new Properties();
      try {
        prop.load(new FileInputStream("src/main/resources/campus.properties"));
        String campusValue = prop.getProperty(usmai);
        if (!StringUtils.isBlank(campusValue)) {
          fields.add(new BasicNameValuePair("cust_list1", campusValue));
        }
      } catch (IOException e) {
        log.error("Error loading campus.properties, USMAI Campus will not be "
            + "included in request, e");
      }
    }
    return fields;
  }

  protected List<NameValuePair> extractFields_SysAid(Element req) {
    /*
     * Constructs a list of parameters from a Request element. These parameters
     * follow the format used by SysAid to submit webforms and can be used to
     * create an UrlEncodedFormEntity.
     */

    List<NameValuePair> fields = new ArrayList<NameValuePair>();
    Element desc = req.getChild("description");
    Element category = req.getChild("category");
    Element subcategory = req.getChild("subcategory");
    Element title = req.getChild("title");
    Element campus = req.getChild("usmaiCampus");
    Element first = req.getChild("firstName");
    Element last = req.getChild("lastName");
    Element email = req.getChild("email");

    /* Request description */
    fields.add(new BasicNameValuePair("desc", desc.getText()));

    /* Used for authentication */
    fields.add(new BasicNameValuePair("accountID", sysaid_accountID));
    fields.add(new BasicNameValuePair("formID", sysaid_formID));

    /* Category and Subcategory */
    fields.add(new BasicNameValuePair("problem_type", category.getText()));
    fields.add(new BasicNameValuePair("subcategory", subcategory.getText()));

    /* Request subject */
    fields.add(new BasicNameValuePair("title", title.getText()));

    /* Name and e-mail are used to determine submit user and request user */
    fields.add(new BasicNameValuePair("firstName", first.getText()));
    fields.add(new BasicNameValuePair("lastName", last.getText()));
    fields.add(new BasicNameValuePair("email", email.getText()));

    /*
     * The relevant USMAI campus for the request, will only be applicable for
     * some forms.
     */
    if (campus != null) {
      String usmai = campus.getText();
      Properties prop = new Properties();
      try {
        prop.load(new FileInputStream("src/main/resources/campus.properties"));
        String campusValue = prop.getProperty(usmai);
        if (!StringUtils.isBlank(campusValue)) {
          fields.add(new BasicNameValuePair("cust_list1", campusValue));
        }
      } catch (IOException e) {
        log.error("Error loading campus.properties, USMAI Campus will not be "
            + "included in request, e");
      }
    }
    return fields;
  }
};
