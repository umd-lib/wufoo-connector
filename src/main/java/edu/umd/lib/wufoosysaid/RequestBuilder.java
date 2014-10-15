package edu.umd.lib.wufoosysaid;

import java.io.FileInputStream;
import java.io.IOException;
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

  private final ServletContext context;
  private String accountID;
  private String formID;

  private final XSLTransformer transformer;
  private Document request;
  private String SysAidURL;
  private String AlephRxURL;

  /**
   * @param sc
   * @param hash
   */
  public RequestBuilder(ServletContext sc, String hash, Document entry)
      throws IOException, XSLTransformException {
    this.context = sc;
    /*
     * Constructs a transformer using the xsl document that the XSLGetter class
     * provides based on the form hash. This will be used to transform entry xml
     * to request xml.
     */
    Document xsl = XSLGetter.getXSLDocument(context, hash);
    if (xsl == null) {
      throw new IOException();
    }
    transformer = new XSLTransformer(xsl);

    try {
      request = transformer.transform(entry);
      log.debug("Request: \n" + output.outputString(request));
    } catch (XSLTransformException e) {
      log.error("Exception occured while attempting to transform XSL file.", e);
    }
    /*
     * Extracting root element from the xsl and processing its requests
     */
    Element root;

    try {
      root = request.getRootElement();
    } catch (IllegalStateException | NullPointerException e) {
      log.error("Exception occurred while getting root element of request "
          + "document. Will occur if sendRequests() is called before "
          + "buildRequestsDocument()", e);
      root = request.getRootElement();
    }
    /*
     * Processing sysaid and alephrx types of requests differently
     */
    List<Element> requests = root.getChildren("request");
    for (Element req : requests) {
      if (extractTargetType(req).equals("sysaid")) {
        log.debug("TARGET TYPE IS:----" + extractTargetType(req));
        getSysAidDetails(req);

      } else if (extractTargetType(req).equals("alephrx")) {
        log.debug("TARGET TYPE IS:----" + extractTargetType(req));
        getAlephRxDetails(req);
      }
    }

  }

  public void getAlephRxDetails(Element req) {
    this.AlephRxURL = extractUrl(req);
    if (StringUtils.isEmpty(AlephRxURL) || AlephRxURL.contains("example.com")) {
      log.warn("TargetURL (\"" + AlephRxURL
          + "\") appears empty or unchanged in webdefault.xml. "
          + "This value should be changed to reflect the location "
          + "of your Alephrx installation");
      AlephRxURL = null;
    } else {
      log.debug("TargetURL: " + AlephRxURL);
    }
  }

  public void getSysAidDetails(Element req) {
    /* Gets Sysaid request parameters */
    this.SysAidURL = extractUrl(req);
    this.formID = extractFormId(req);
    this.accountID = extractAccountId(req);
    if (StringUtils.isEmpty(SysAidURL) || SysAidURL.contains("example.com")) {
      log.warn("TargetURL (\"" + SysAidURL
          + "\") appears empty or unchanged in webdefault.xml. "
          + "This value should be changed to reflect the location "
          + "of your SysAid installation");
      SysAidURL = null;
    } else {
      log.debug("TargetURL: " + SysAidURL);
    }

    if (StringUtils.isBlank(accountID)) {
      log.warn("accountID appears empty or unchanged in webdefault.xml. "
          + "This value should be changed to your SysAid account id.");
    } else {
      log.debug("accountID: " + accountID);
    }

    if (StringUtils.isBlank(formID) || StringUtils.equals(formID, "0")) {
      log.warn("formID appears empty or unchanged in webdefault.xml. "
          + "This value should be changed so that your requests will be "
          + "properly authorized and accepted.");
    } else {
      log.debug("formID: " + formID);
    }
  }

  public void sendRequests() throws IOException {
    /*
     * Transforms each request element in the sysaid xml into a HTTP POST
     * request that has the parameters for a sysaid request encoded in the URL.
     * The request is then executed, at which point Sysaid will then use this
     * information to create a new request
     */

    Element root;

    try {
      root = request.getRootElement();
    } catch (IllegalStateException | NullPointerException e) {
      log.error("Exception occurred while getting root element of request "
          + "document. Will occur if sendRequests() is called before "
          + "buildRequestsDocument()", e);
      return;
    }

    List<Element> requests = root.getChildren("request");
    URI requestSysAidURI = null;
    URI requestAlephRxURI = null;
    /*
     * Service only attempts to create a URI from TargetURL if it appears
     * legitimate, to prevent invalid requests.
     */
    if (StringUtils.isBlank(SysAidURL)) {
      log.warn("Specified SysAidURL is either blank or unchanged from "
          + "default value. Will not attempt to execute HTTP request with "
          + "this URI. ");
    } else if (StringUtils.isBlank(AlephRxURL)) {
      log.warn("Specified AlephRxURL is either blank or unchanged from "
          + "default value. Will not attempt to execute HTTP request with "
          + "this URI. ");
    } else {
      try {
        requestSysAidURI = new URI(SysAidURL);
        requestAlephRxURI = new URI(AlephRxURL);
      } catch (URISyntaxException e) {
        log.error("Exception occured while attempting to create a URI "
            + "object with path " + SysAidURL + " or " + AlephRxURL
            + ". Verify that a valid URI " + "is specified in configuration.",
            e);
      }
    }

    for (Element req : requests) {
      if (extractTargetType(req).equals("alephrx")) {
        log.debug("ALPEHRX URI" + extractTargetType(req));
        doPostAlephRx(req, requestAlephRxURI);
      } else if (extractTargetType(req).equals("sysaid")) {
        log.debug("Sysaid URI" + extractTargetType(req));
        doPostSysaid(req, requestSysAidURI);
      }
    }
  }

  private void doPostAlephRx(Element req, URI requestAlpehRxURI)
      throws IOException {
    HttpPost httpPost;
    CloseableHttpClient httpclient = HttpClients.createDefault();
    CloseableHttpResponse response = null;
    if (requestAlpehRxURI != null) {
      httpPost = new HttpPost(requestAlpehRxURI);
    } else {
      httpPost = new HttpPost();
    }
    /*
     * Request parameters are encoded into the URL as Name-Value Pairs. To
     * create these pairs, the element names need to be translated into the
     * parameters expected by SysAid
     */

    List<NameValuePair> fields = extractAlephRxFields(req);
    log.debug("Fields are--" + fields);

    /*
     * Creates an entity from the list of parameters and associates it with the
     * POST request. This request is then executed if a valid URI was
     * constructed earlier
     */
    try {
      httpPost.setEntity(new UrlEncodedFormEntity(fields, "UTF-8"));
      if (requestAlpehRxURI != null) {
        response = httpclient.execute(httpPost);
        log.debug("Request parameters: \n" + fields);
        log.debug("Response: \n" + response.toString());
      }
    } catch (ClientProtocolException e) {
      log.error("ClientProtocolException occured while attempting to "
          + "execute POST request. Ensure this service is properly "
          + "configured and that the server you are attempting to make "
          + "a request to is currently running.", e);
    }
    /*
     * Closes httpclient regardless of rather it was successful or not. If no
     * response was received, request parameters are logged for debugging.
     */
    httpclient.close();
    if (response == null) {
      log.warn("Unable to execute POST request. Request parameters: \n"
          + fields);
    }

  }

  public void doPostSysaid(Element req, URI requestSysAidURI)
      throws IOException {
    HttpPost httpPost;
    CloseableHttpClient httpclient = HttpClients.createDefault();
    CloseableHttpResponse response = null;
    if (requestSysAidURI != null) {
      httpPost = new HttpPost(requestSysAidURI);
    } else {
      httpPost = new HttpPost();
    }

    /*
     * Request parameters are encoded into the URL as Name-Value Pairs. To
     * create these pairs, the element names need to be translated into the
     * parameters expected by SysAid
     */

    List<NameValuePair> fields = extractFields(req);

    /*
     * Creates an entity from the list of parameters and associates it with the
     * POST request. This request is then executed if a valid URI was
     * constructed earlier
     */
    try {
      httpPost.setEntity(new UrlEncodedFormEntity(fields, "UTF-8"));
      if (requestSysAidURI != null) {
        response = httpclient.execute(httpPost);
        log.debug("Request parameters: \n" + fields);
        log.debug("Response: \n" + response.toString());
      }
    } catch (ClientProtocolException e) {
      log.error("ClientProtocolException occured while attempting to "
          + "execute POST request. Ensure this service is properly "
          + "configured and that the server you are attempting to make "
          + "a request to is currently running.", e);
    }

    /*
     * Closes httpclient regardless of rather it was successful or not. If no
     * response was received, request parameters are logged for debugging.
     */
    httpclient.close();
    if (response == null) {
      log.warn("Unable to execute POST request. Request parameters: \n"
          + fields);
    }

  }

  public Document getRequest() {
    return request;
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

  protected List<NameValuePair> extractAlephRxFields(Element req) {
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

    // fields.add(new BasicNameValuePair("report", report.getText()));
    fields.add(new BasicNameValuePair("name", name.getText()));
    fields.add(new BasicNameValuePair("functional_area", functional_area
        .getText()));
    fields.add(new BasicNameValuePair("campus", campus.getText()));
    fields.add(new BasicNameValuePair("phone", phone.getText()));
    fields.add(new BasicNameValuePair("email", email.getText()));
    fields.add(new BasicNameValuePair("status", status.getText()));
    fields.add(new BasicNameValuePair("summary", summary.getText()));
    fields.add(new BasicNameValuePair("text", text.getText()));
    fields.add(new BasicNameValuePair("submitter_name", submitter_name
        .getText()));

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

  protected List<NameValuePair> extractFields(Element req) {
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
    fields.add(new BasicNameValuePair("accountID", accountID));
    fields.add(new BasicNameValuePair("formID", formID));

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
