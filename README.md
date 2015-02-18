# Wufoo Connector

A web service designed to automatically create help desk tickets for ticket tracking systems (example: SysAid(https://libticketing.umd.edu/webformsubmit?pageEncoding=utf-8) and AlephRx(http://alephrx.local/cgi-bin/api/reports)) from entries to  [Wufoo](http://www.wufoo.com/) forms to using Wufoo's [webhooks integration](http://help.wufoo.com/articles/en_US/SurveyMonkeyArticleType/Webhooks). The web service is a Java web application (wufoo-connector.war) that can be installed and executed under Tomcat, Jetty or other servlet containers. This document focus on the installation and configuration using Tomcat.

**Warning**: This is development level software, and has only been tested using our installation of SysAid Server. It hasn't been tested on SysAid Cloud and may be incompatible. It also may need to be modified for use with other SysAid installations.


## Configuration

Configuration for this connector is composed of three types:

* Configuration of the Wufoo forms you wish to map
* The creation of XSL files that map how forms are converted into tickets.
* Configuration of the connector itself

### Wufoo Form Configuration

The basic workflow as-of-now is as follows:

1. Create a wufoo form. 
2. A hash is generated for the form created which can be found in the *Api Information* menu under *Code*.
3. In Wufoo, enter the *Notifications* menu for the form you wish to configure, and add a *WebHook integration* from the *to Another Application* menu.
3. Enter the URL of your web service, followed by `/entry/[hash]`, where `[hash]` is the hash assigned to your Wufoo form. 
4. Request access to Wufoo Connector service (handshake key) and enter the key thus provided.
5. Ensure you check the check box *Include Field and Form Structures with Entry Data*.

### XSL File Creation

1. When a new form is created on wufoo and an entry is submitted, the first entry is a sample entry and will not be submitted to ticketing systems. 
2. A sample xsl will be generated based on the fields in the wufoo form called *sample_[hash].xsl*.
3. You can modify this file following the steps below.  

   a. Save the XSL file with a filename of `[hash].xsl` which translates the entry XML structure into the requests XML structure like `z1h2ln4i177snof.xsl`.
   b. Under the root `Requests` element, create a `Request` element for each ticket you wish to be created for a form submission.
   c. Under every request element include a 'target' element which defines the target url and other necessary information specific to destination for posting form information.
For example, in Sysaid we add the following:

      * Url(The location where ticket submissions will be made for your SysAid installation. For us, we use the `webformsubmit` script as part of SysAid's built-in webform processing, but it may vary by installation.)

      * Form Id(A unique key used to authorize requests to your help desk. It will be a series of three hexadecimal numbers separated by colons. These numbers can be positive or negative and are 8, 10, and 4 digits long, respectively.)
    
      * Account Id(Your organization's account ID with SysAid. It can be found under the “About” menu in your help desk.).

   d. The .xsl file contains a placeholder for alephRx request.
In AlpehRx we add:

      * Url(The location where ticket submissions will be made for your AlephRx installation.  


### Configuration of Connector
The wufoo connector root path is https://<server-domain-name>/wufoo-connector. The web service request is ~/wufoo-connector/entry/[hash] that is explained in the previous section - Wufoo Form Configuration. The connector accesses the XSL file through the configured location at server. 

There is an utility ~/wufoo-connector/upload.jsp to help to upload XSL to the server. It also allows to download any XSL files from the server. The configuration of /wufoo-connector/xsls context and "xslDownloadURL" parameter are for this purpose.

Configuration in Tomcat server.xml:

* Add wufoo-connector path context

```
<Context
          path="/wufoo-connector"
          docBase="wufoo-connector"
          reloadable="false"
          crossContext="false"
        />
```


* Add /wufoo-connector/xsls path context

```
<Context
	   docBase="/apps/cms/resources/wufoo-connector/xsl"
	   path="/wufoo-connector/xsls"
	/>
```


Configuration for Tomcat resources and context.xml:

* Create a folder on server for storing the xsl files for each wufoo form. For example, /apps/cms/resources/wufoo-connector/xsl.

* Add "xslLocation" parameter into Tomcat context.xml to set location of xsl folder for the wufoo-connector application.

```
For example:
<Parameter name="xslLocation" value="/apps/cms/resources/wufoo-connector/xsl/" override="false"/>
```
* Add "xslDownloadURL" parameter to context.xml to set the 

```
For example:
<Parameter name="xslDownloadURL" value="https://www.lib.umd.edu/wufoo-connector/xsls/"
	override="false"/>
```

## Apache Host Configuration

If the server has Apache as its web server, using the Library website domain and SSL, then the following configuration is needed and added to the VirtualHost section in httpd.conf. 

```
For example: 

<VirtualHost 129.2.19.172:443>
  ServerName www.lib.umd.edu
  ....
  RewriteCond   %{REQUEST_URI}    !^/wufoo-connector.*
  ....

  <Location "/wufoo-connector">
    Order allow,deny
    Allow from all
  </Location>
  .....
   ProxyPass /wufoo-connector http://localhost:9602/wufoo-connector
   ProxyPassReverse /wufoo-connector http://localhost:9602/wufoo-connector
  ....
</VirtualHost>
``` 


## Deploy to Server

1. Download source code from github at [Wufoo Connector](https://github.com/umd-lib/wufoo-connector)
2. Compile the code using

```
mvn package
```

3. Go to `target` folder in wufoo-connector project and transfer the `wufoo-connector.war` file to the webapps directory on server (/apps/cms/webapps).
4. Start the tomcat server from the folder where tomcat is installed (/apps/cms/tomcat-misc).

```
./control start
``` 

## Running on Vagrant (optional)
The application can be run on a tomcat instance running on a virtual machine that can be setup using Vagrant.

To do this follow the following steps:

1. Install Vagrant and VirtualBox.
2. Open `vagrant/Vagrantfile`.
3. Make sure that <target> in `config.vm.synced_folder "<target>", "/webapps"`, `<target>` points to the absolute path of `/wufoo-connector/target` on your computer, ie. something like `/users/user/git/wufoo-connector/target`
4. Note the port at `host:` under `config.vm.network:`. This is the port that the server will be available on. Default: 4545
5. Navigate to the vagrant folder in the root and run `vagrant up` on terminal. This will download the OS for the VM, the required files and deploy the webapp on a tomcat server running on the VM.
6. Go to [http://localhost:4545/wufoo-connector](http://localhost:4545/wufoo-connector) to access the deployed app.
7. To update the app thats deployed, run `mvn package` from the host machine.


##Running on jetty (optional)

This web service uses the Maven Jetty plugin to run. To run locally, execute the following command from the project root folder:
	
	mvn jetty:run
	
Once running, Jetty will continue to scan the configuration for changes, allowing the addition and modification of form mappings without having to redeploy the service. By default, the service scans every 5 seconds, but this can be changed in the maven-jetty plugin configuration in [pom.xml](pom.xml).



## License

[CC0](http://creativecommons.org/publicdomain/zero/1.0/)
