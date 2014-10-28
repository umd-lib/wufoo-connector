package edu.umd.lib.wufoosysaid;

import java.io.FileInputStream;
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
      // create sample xsl
      request_document = null;
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
    } /* Get AlephRx request parameters */
    else if (extractTargetType(req).equals("alephrx")) {
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
      log.debug("NO TARGET DEFINED IN REQUEST");
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
      log.debug("NO TARGET DEFINED IN REQUEST");
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
