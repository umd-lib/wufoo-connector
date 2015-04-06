# Wufoo Connector

Wufoo Connector is a web service designed as a bridge between the Wufoo forms and target applications. Through Wufoo API, the form is integrated with Wufoo Connector. By passing parameters and accessing a designed XSL file, the Wufoo Connector recieves Wufoo form data, re-formates the data and forwards the data to the specified target application. The field mappings between the Wufoo form and the target application are specified in the XSL file. SSDR has implemented the integration of Wufoo Connector to the followig target applications. There is no doubt that the extension to other target applications can be applied.

* [SysAid Ticketing System](https://libticketing.umd.edu/webformsubmit?pageEncoding=utf-8)
* [AlephRx Ticketing System](http://alephrx.local/cgi-bin/api/reports)
* Libi Online Student Application 

**Warning**: For SysAid integration, it has only been tested using our installation of SysAid Server. It hasn't been tested on SysAid Cloud and may be incompatible. It also may need to be modified for use with other SysAid installations.

  
## Configuration of Connector

The web service is a Java web application (wufoo-connector.war) that can be installed and executed under Tomcat, Jetty or other servlet containers. This document focus on the installation and configuration using Tomcat. The configuration using Vagrant virtual machine is also introduced for a local environment.

The wufoo connector root path is https://<server-domain-name>/wufoo-connector. The web service request is ~/wufoo-connector/entry/[hash] that is explained in the later section - Wufoo Form Configuration. The connector accesses the XSL file of name **[hash].xsl** at server side, perform the request processing, and then forward the data to target application.

There is an utility ~/wufoo-connector/upload.jsp to help to upload/download XSL file to and from the server. The configuration of ~/wufoo-connector/xsls context and "xslDownloadURL" parameter are for this purpose. (TODO - need to update dependong on the final implementation)

### Tomcat Configuration

Configuration in Tomcat server.xml:

* Add wufoo-connector web application context

```
<Context
     path="/wufoo-connector"
     docBase="wufoo-connector"
     reloadable="false"
     crossContext="false"
/>
```

* Map /wufoo-connector/xsls path to a server folder (used by ~/wufoo-connector/upload.jsp)

```
<Context
	 docBase="/apps/cms/resources/wufoo-connector/xsl"
	 path="/wufoo-connector/xsl"
/>
```
Configuration for Tomcat context.xml

* Add "xslLocation" parameter for the location of xsl files

```
For example:

<Parameter name="xslLocation" value="/apps/cms/resources/wufoo-connector/xsl/" override="false"/>

(where /apps/cms/resources/wufoo-connector/xsl is the folder at the server for all xsl files)
```

* Add "xslDownloadURL" parameter for the location of uploading/downloading the XSL file from the Upload utility (~/wufoo-connector/upload.jsp)

```
For example:

<Parameter name="xslDownloadURL" value="/wufoo-connector/xsl/"
	override="false"/>
	
```

### Apache Host Configuration

If the server has Apache as its web server, then add the web application to one of the virtual hosts. Here is an example to add the connector to the SSL of www.lib.umd.edu.


```
For example: 

<VirtualHost 129.2.19.172:443>
  ServerName www.lib.umd.edu
  
  ....
  RewriteCond   %{REQUEST_URI}    !^/wufoo-connector.*
  ....
  RewriteRule ^/(.*)$ http://www.lib.umd.edu/$1 [R,L]

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

## Deploy Connector to Server

* Download source code from github at [Wufoo Connector](https://github.com/umd-lib/wufoo-connector)
* Compile the code using

```
mvn package
```

* Go to **target** folder in wufoo-connector project and transfer the **wufoo-connector.war** file to the webapps directory on server (/apps/cms/webapps).
* Start the tomcat server from the folder where tomcat is installed (/apps/cms/tomcat-misc).

```
cd /apps/cms/tomcat-misc
./control start
``` 

When the connector is running, two types of URL are served:
 
* ~/wufoo-connector/entry/[hash] - used by Wufoo form to connect to the connector ([hash] is explained in Wufoo Form Configuration.)
* ~/wufoo-connector/upload.jsp - a utility for uploading/downloading XSL files

## How Does Connector Work

How to make the Wufoo form work with the connector and a target application? The following two steps are needed to achieve the goal.

* Configration of the Wufoo form
* The creation and development of XSL file
  * specify the target application
  * map how forms are converted into target databases or dataset.


### Wufoo Form Configuration

The Wufoo Form Configuration includes to create a new form, create a Web Hook integration and setup the Web Hook with a target. The target is a URL of Wufoo Connector with parameters. When the form is submitted, the Wufoo-Connector is foewarded. Wufoo-Connector processes the request according to a designed XSL and then forwards the data to the specified target. Here are steps to create and configure the Wufoo form.

* Login to Wufoo form [https://libumd.wufoo.com/login](https://libumd.wufoo.com/login)
* Create a wufoo form. 
* Create a WebHook Integration for the form
  * Select "Notifications" from Form Manager for the form
  * Select WebHook from the pull-down list
  * Select "Add Integration"
  * A panel with type of "as a WebHook" will be created
* Enter parameters for WebHook from the panel
  * WebHook URL: The URL that Wufoo will send an HTTP POST of the data entered into this form. In this case, it is the URL of a running Wufoo Connector.
    * Get API Hash Key for the form
      * Select "Share" from Form Manager for the form
      * Select "API Informatin" near the top-right
      * Retrive the "Hash" value (Ex, mvfv3v80rlf7gy)
    * The URL shall be [wufoo-connector]/entry/[hash]. (Ex, [wufoo-connector] can be https://www.lib.umd.edu/wufoo-connector)
  * WebHook Handshake Key: This is a a key that you choose as an authentication mechanism to prevent spam to your Web Hook. (TODO - Need information about how the key is generated.)
  * Check "*Include Field and Form Structures with Entry Data*"
  * Select "Save"
  
  
  ```
  Example:
  
  Your WebHook URL: https://www.lib.umd.edu/wufoo-connector/entry/mvfv3v80rlf7gy
  Your WebHook Handshake Key: testing (TBD - how to generate the key?)
  
  ```

### XSL File Creation and Development

#### Initial Sample XSL Creation

When a new form is created on wufoo and configuration is complete, the form is ready for connecting to Wufoo Connector. When an entry is submitted from the form, the first entry is a sample entry and will not be submitted to the target application. A sample xsl will be generated based on the fields in the wufoo form called **sample_[hash].xsl**.  The ~/wufoo-connector/upload.jsp utility can be used to check the existance of the file.

Here is an example of sample XSL file. The form contains "First Name" and "Last Name" two fields.

(TODO - need to replace by a more generic sample)

```
<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:variable name="requestUser">
      <xsl:value-of select="//field[@title='First']"/>&#160;<xsl:value-of select="field[@title='Last']"/>
  </xsl:variable>
  <xsl:template match="/entry">
    <requests>
      <request>
        <target type="sample type">
          <!-- target must include:
			type = sysaid / alephrx
			destination URL:
			SYSAID: ( https://libticketingdev.umd.edu/webformsubmit?pageEncoding=utf-8 ) or ALEPHRX: ( http://alephrx.local/cgi-bin/api/reports )
			form ID = form_ID
			account ID = account_ID
			-->
          <url>http://sample url</url>
          <formId>sample form ID</formId>
          <accountId>sample account ID</accountId>
        </target>
        <category />
        <subcategory />
        <title />
        <status />
        <attachments />
        <urgency />
        <priority />
        <due_date />
        <main_asset />
        <submit_user />
        <request_user_first_name />
        <request_user_last_name />
        <request_user_email />
        <usmai_campus />
        <assigned_to />
        <technician_group />
        <actions />
        <location />
        <description>Description :
			First Name: <xsl:value-of select="//field[@id='Field2']"/>
			Last Name: <xsl:value-of select="//field[@id='Field1']"/></description>
      </request>
    </requests>
  </xsl:template>
</xsl:stylesheet>

```

(TODO - need to explain the structure of the XSL a little bit)

#### Connector XSL Development

The connector XSL file must be named **[hash].xsl** that is stored at the server location spsecfied in **xslLocation**. You can modify this sample file following the steps below.  

* Download the **sample_[hash].xsl** to local file from ~/wufoo-connector/upload.jsp
* Rename the XSL file to **[hash].xsl**.
* Modify **[hash].xsl** (TODO - need to come up a better guideline)
  * Under the root *Requests* element, create a *Request* element for each field that you wish to be created for a form submission.
  * Under every request element include a 'target' element which defines the target application URL and other necessary information specific to destination for posting form information.

For example, in Sysaid we add the following:

      * Url(The location where ticket submissions will be made for your SysAid installation. For us, we use the `webformsubmit` script as part of SysAid's built-in webform processing, but it may vary by installation.)

      * Form Id(A unique key used to authorize requests to your help desk. It will be a series of three hexadecimal numbers separated by colons. These numbers can be positive or negative and are 8, 10, and 4 digits long, respectively.)
    
      * Account Id(Your organization's account ID with SysAid. It can be found under the “About” menu in your help desk.).

The .xsl file contains a placeholder for alephRx request.
In AlpehRx we add:

      * Url(The location where ticket submissions will be made for your AlephRx installation.  

 * Upload the complete **[hash].xsl** to server using ~/wufoo-connector/upload.jsp


A complete **[hash].xsl** example:

```
<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:variable name="requestUser">
    	<xsl:value-of select="//field[@title='First']"/>&#160;<xsl:value-of select="field[@title='Last']"/>
    </xsl:variable>
    <xsl:template match="/entry">
        <requests>
            <request>
                <target type="sysaid">
                    <url>https://libticketingdev.umd.edu/webformsubmit?pageEncoding=utf-8</url>
                    <formId>1300b3c2:12460019b64:-8000</formId>
                    <accountId>umlibraryitd</accountId>
                </target>
                <title>sample titile</title>
                <category>Web</category>
                <subcategory>Libi (Intranet)</subcategory>
                <request_user_email><xsl:value-of select="//field[@title='Email']"/></request_user_email>
                <request_user_first_name><xsl:value-of select="//field[@title='First']"/></request_user_first_name>
                <request_user_last_name><xsl:value-of select="//field[@title='Last']"/></request_user_last_name>
                <description>
                    Request User:&#x9;&#x9;&#x9;&#x9;<xsl:value-of select="//field[@title='First']"/><xsl:text> </xsl:text><xsl:value-of select="field[@title='Last']"/> (<xsl:value-of select="//field[@title='Email']"/>)

                    Name of group:&#x9;&#x9;&#x9;&#x9;<xsl:value-of select="//field[@id='Field334']"/>

                    Group description:&#x9;&#x9;&#x9;&#x9;<xsl:value-of select="//field[@id='Field335']"/>

                    Minutes folder:&#x9;&#x9;&#x9;&#x9;<xsl:value-of select="//field[@id='Field336']"/>
                </description>
                <usmaiCampus><xsl:value-of select="//field[@title='USMAI Campus']"/></usmaiCampus>
            </request>
            <request><!-- AlephRx request -->
                <target type="alephrx">
                    <url>http://usmaidev.lib.umd.edu/alephrx/cgi-bin/api/reports</url>
                </target>
                <name><xsl:value-of select="concat(//field[@title='First'],' ',//field[@title='Last'])"/></name>
                <functional_area>other</functional_area>
                <campus><xsl:value-of select="//field[@title='USMAI Campus']"/></campus>
                <phone>301-555-0123</phone>
                <email><xsl:value-of select="//field[@title='Email']"/></email>
                <status>new</status>
                <summary><xsl:value-of select="//field[@title='Description']"/></summary>
                <text>text</text>
                <submitter_name>A.N. Other</submitter_name>
            </request>

        </requests>
    </xsl:template>
</xsl:stylesheet>

```


## Running on Vagrant (local)

The connector can be run on a tomcat instance within Vagrant virtual machine.

### Vagrant Configuration
To do this, follow the steps:

* Install [Vagrant](https://www.vagrantup.com/) and [VirtualBox](https://www.virtualbox.org/).
* At local development, chagne directory to vagrant and open **Vagrantfile** (/apps/git/wufoo-connector/vagrant).
  * Make sure that the webapp path is specified in config.vm.synced_folder property where it points to the absolute path of the maven build target. For example, /users/user/git/wufoo-connector/target or /apps/git/wufoo-connector/target.
  * The host:\<port> is specified in config.vm.network prperty. This is the port that Tomcat server will be listening. The default is host:4545.


```
For example:
config.vm.synced_folder "/apps/git/wufoo-connector/target", "/webapps"
```

```
For example:
config.vm.network :forwarded_port, host:4545, guest:8080
```
* Review tomcat configuration files that are deployed to Vagrant. The files are at ~vagrant/defaults/conf.
  * Make sure the xslLocation and xslDownloadURL parameters are defined in context.xml
  * Make sure docBase of /home/vagrant/resources/xsl is mapped in server.xml

```
context.xml:

<Parameter name="xslLocation" value="/home/vagrant/resources/xsl/" override="false"/>
<Parameter name="xslDownloadURL" value="/wufoo-connector/xsl/" override="false"/>

```

```
server.xml:

<Context docBase="/home/vagrant/resources/xsl" path="/wufoo-connector/xsl" />

```

### Vagrant Execution

Run "vagrant up". This will download the OS for the VM and the required files, and deploy the webapp on a tomcat server running on the VM.

```
$ cd /apps/git/wufoo-connector/vagrant
$ vagrant up
```

The connector should be running.

* Visit [http://localhost:4545/wufoo-connector](http://localhost:4545/wufoo-connector) and a welcome page is displayed. That means the connector is up running.
* Visit [http://localhot:4545/wufoo-connector/upload.jsp](http://localhot:4545/wufoo-connector/upload.jsp), upload/download utility, to upload/download xsl files.

When the java or jsp code is changed, rebuild the war file and Vagrant war file will be updated too.

(Due to the configuration: config.vm.synced_folder "/apps/git/wufoo-connector/target", "/webapps")

```
$ mvn pakcage
```
Execute the vagrant provision if any file under ~vagrant/defaults/conf (ex, server.xml, context.xml) is changed.

```
$ vagrant provision
```

To test the connector, follow the steps:

* Upload a valid [hash].xsl that is associated with a Wufoo form
* Enter data to the Wufoo form and submit it
* Check if the target application receives the data as expected


### Vagrant Machine Environment

To access shell of the Vagrant machine,

```
$ cd /apps/git/wufoo-connector/vagrant
$ vagrant ssh
(will be at home of vagrant: /home/vagrant)
```
Tomcat environment

* Tomcat home are /home/vagrant/tomcat7.
* Tomcat logs are at /home/vagant/tomcat7/logs.
* Shutdown tomcat by command ~/tomcat7/bin/shutdown.sh
* Statrt tomcat by command ~/tomcat/bin/startup.sh
* XSL files should be located at /home/vagrant/resources/xsl.

## License

[CC0](http://creativecommons.org/publicdomain/zero/1.0/)
