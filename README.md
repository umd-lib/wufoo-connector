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

## Running on Server
### Apache Host Configuration

If the server has Apache as its web server and use the Library website domain and SSL, then RewriteCond, \<Location\>, ProxyPass and ProxyPassReserve configuration are needed and added to the VirtualHost section in httpd.conf. 

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


### Deploy to Server

* Download source code from github at [Wufoo Connector](https://github.com/umd-lib/wufoo-connector)
* Compile the code using

```
mvn package
```

* Go to `target` folder in wufoo-connector project and transfer the `wufoo-connector.war` file to the webapps directory on server (/apps/cms/webapps).
* Start the tomcat server from the folder where tomcat is installed (/apps/cms/tomcat-misc).

```
cd /apps/cms/tomcat-misc
./control start
``` 

## Running on Vagrant (local)
The application can be run on a tomcat instance running on a virtual machine that can be setup using Vagrant.

### Vagrant Configuration and Execution
To do this follow the following steps:

* Install [Vagrant](https://www.vagrantup.com/) and [VirtualBox](https://www.virtualbox.org/).
* At local development, chagne directory to vagrant and open Vagrantfile (/apps/git/wufoo-connector/vagrant).
* Make sure that \<target> is specified in config.vm.synced_folder property where it points to the absolute path of the maven build target. For example, /users/user/git/wufoo-connector/target or /apps/git/wufoo-connector/target.

```
For example:
config.vm.synced_folder "/apps/git/wufoo-connector/target", "/webapps"
```

* The host:\<port> is specified in config.vm.network prperty. This is the port that Tomcat server will be listening. The default is host:4545.

```
For example:
config.vm.network :forwarded_port, host:4545, guest:8080
```
* Review tomcat configuration files to be deployed to Vagrant in ~vagrant/defaults/conf
  * Make sure the xslLocation and xslDownloadURL parameters are defined in context.xml
  * Make sure docBase of /home/vagrant/resources/xsl is mapped to /wufoo-connector/xsl in server.xml

```
context.xml:
 
<Parameter name="xslLocation" value="/home/vagrant/resources/xsl/" override="false"/>
<Parameter name="xslDownloadURL" value="http://localhost:4545/wufoo-connector/xsl/" override="false"/>

```

```
server.xml:

<Context docBase="/home/vagrant/resources/xsl" path="/wufoo-connector/xsl" />

```

* Run "vagrant up". This will download the OS for the VM, the required files and deploy the webapp on a tomcat server running on the VM.

```
$ vagrant up
```
* Go to [http://localhost:4545/wufoo-connector](http://localhost:4545/wufoo-connector) to access the deployed app.
* Visit [http://localhot:4545/wufoo-connector/upload.jsp](http://localhot:4545/wufoo-connector/upload.jsp) to load xsl template.
* To update the app that is deployed,

```
$ cd /apps/git/wufoo-connector
$ mvn pakcage
```
* To execute the vagrant provision if config_start.sh is changed,

```
$ cd /apps/git/wufoo-connector/vagrant
$ vagrant provision
```


### Vagrant Machine Environment

* To access shell of the Vagrant machine

```
$ cd /apps/git/wufoo-connector/vagrant
$ vagrant ssh
(will be at home of vagrant: /home/vagrant)
```
* Tomcat environment
  * Tomcat files are under /home/vagrant/tomcat7.
  * Tomcat logs are at /home/vagant/tomcat7/logs.
  * Shutdown tomcat by command ~/tomcat7/bin/shutdown.sh
  * Statrt tomcat by command ~/tomcat/bin/startup.sh

* XSL files should be located in /home/vagrant/resources/xsl so that wufoo-connector can access.
* The XSL file can be uploaded to /home/vagrant/resources/xsl from [http://localhot:4545/wufoo-connector/upload.jsp](http://localhot:4545/wufoo-connector/upload.jsp).

## License

[CC0](http://creativecommons.org/publicdomain/zero/1.0/)
