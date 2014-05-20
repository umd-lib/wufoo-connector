package edu.umd.lib.wufoosysaid;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;

import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.transform.XSLTransformException;
import org.jdom2.transform.XSLTransformer;

public class RequestBuilder {
  private static Logger log = Logger.getLogger(RequestBuilder.class);
  private final ServletContext context;
  private final XSLTransformer transformer;
  private Document request;

  private static final String filePath = "/src/main/webapp/WEB-INF/xsl/%s.xsl";
  private final String SysAidURL, accountID, formID;

  public RequestBuilder(ServletContext sc, String hash) throws JDOMException,
      MalformedURLException, IOException {
    /*
     * Extracts necessary context parameters from ServletContext. Warns if they
     * have not been changed from their default configurations.
     */
    this.context = sc;
    this.SysAidURL = (String) context.getAttribute("SysAidURL");
    this.accountID = (String) context.getAttribute("accountID");
    this.formID = (String) context.getAttribute("formID");

    if (SysAidURL.contains("example.com")) {
      log.warn("SysAidURL appears unchanged in webdefault.xml. "
          + "This value should be changed to reflect the location "
          + "of your SysAid installation");
    }

    if (StringUtils.isEmpty(accountID)) {
      log.warn("accountID appears unchanged in webdefault.xml. "
          + "This value should be changed to your SysAid account id.");
    }

    if (StringUtils.equals(formID, "0")) {
      log.warn("formID appears unchanged in webdefault.xml. "
          + "This value should be changed so that your requests will be "
          + "properly authorized and accepted.");
    }
    /*
     * Constructs a transformer based on the xsl file corresponding to the given
     * form hash. This will be used to transform entry xml into request xml
     */
    log.debug("Attempting to locate XSL file for hash " + hash);
    String path = String.format(filePath, hash);
    URL xslUrl;
    xslUrl = context.getResource(path);
    log.debug("XSL URL: " + xslUrl);
    SAXBuilder sax = new SAXBuilder();
    Document xsl;
    xsl = sax.build(xslUrl);
    transformer = new XSLTransformer(xsl);
  }

  public Document buildRequest(Document entry) {
    /* Builds request xml from entry xml */
    Document request;
    try {
      request = transformer.transform(entry);
    } catch (XSLTransformException e) {
      log.error("Exception occured while attempting to transform XSL file.", e);
      return null;
    }
    return request;
  }

  public void sendRequests() throws IOException {
    /*
     * Transforms each request element in the sysaid xml into a HTTP POST
     * request that has the parameters for a sysaid request encoded in the URL.
     * The request is then executed, at which point Sysaid will then use this
     * information to create a new request
     */

    CloseableHttpClient httpclient = HttpClients.createDefault();
    Element root;

    try {
      root = request.getRootElement();
    } catch (IllegalStateException | NullPointerException e) {
      log.error("Exception occurred while getting root element of request "
          + "document. Will occur if sendRequests() is called before "
          + "buildRequests()", e);
      return;
    }

    List<Element> requests = root.getChildren("Request");
    for (Element req : requests) {
      HttpPost httpPost;
      try {
        httpPost = new HttpPost(SysAidURL);
      } catch (IllegalArgumentException e) {
        log.error("Exception occured while attempting to create a HttpPost "
            + "object with URL " + SysAidURL + ". Verify that a valid URI is "
            + "specified in configuration.", e);
        return;
      }

      /*
       * Request parameters are encoded into the URL as Name-Value Pairs. To
       * create these pairs, the element names need to be translated into the
       * parameters expected by SysAid
       */
      List<NameValuePair> fields = new ArrayList<NameValuePair>();
      Element desc = req.getChild("Description");
      Element category = req.getChild("Category");
      Element subcategory = req.getChild("Subcategory");
      Element title = req.getChild("Title");
      Element campus = req.getChild("USMAICampus");
      Element first = req.getChild("FirstName");
      Element last = req.getChild("LastName");
      Element email = req.getChild("Email");

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
        /* TODO: Test selecting of USMAI campus */
        fields.add(new BasicNameValuePair("Campus", campus.getText()));
      }
      try {
        httpPost.setEntity(new UrlEncodedFormEntity(fields, "UTF-8"));
        httpclient.execute(httpPost);
      } catch (ClientProtocolException e) {
        log.error("ClientProtocolException occured while attempting to "
            + "execute POST request. Ensure this service is properly "
            + "configured and that the server you are attempting to make "
            + "a request to is currently running.", e);
        return;
      }
    }
  }
}
