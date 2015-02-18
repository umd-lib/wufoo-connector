Wufoo Connector
=========================

A web service designed to automatically create help desk tickets for ticket tracking systems (example: SysAid(https://libticketingdev.umd.edu/webformsubmit?pageEncoding=utf-8) and AlephRx(http://alephrx.local/cgi-bin/api/reports)) from entries to  [Wufoo](http://www.wufoo.com/) forms to using Wufoo's [webhooks integration](http://help.wufoo.com/articles/en_US/SurveyMonkeyArticleType/Webhooks).


**Warning**: This is development level software, and has only been tested using our installation of SysAid Server. It hasn't been tested on SysAid Cloud and may be incompatible. It also may need to be modified for use with other SysAid installations.



Configuration
-----------------
**Note**: This web service is still in a prototype stage, and some of it's functionality is still unpolished. Ideally, the configuration needs of running this connector will be simplified in the future. 

Configuration for this connector is composed of three types:

* Configuration of the Wufoo forms you wish to map
* The creation of XSL files that map how forms are converted into tickets.
* Configuration of the connector itself

### Configuration of Connector
Follow the steps below before making any settings for the Wufoo Connector:

1. Configure log4j for Wufoo Connector. Add log4j to CATALINA_OPTS
```
setenv CATALINA_OPTS "-server -XX:MaxPermSize=256m -Xmx1024M -Xms1024M -Dlog4j.configuration=file:${CATALINA_BASE}/conf/log4j.xml" 
```

2. Add /apps/cms/tomcat-misc/conf/log4j.xml

``` 
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE log4j:configuration SYSTEM "log4j.dtd">
<log4j:configuration xmlns:log4j="http://jakarta.apache.org/log4j/">

    <!-- wufoo-connector.log -->
       <appender name="wufoo" class="org.apache.log4j.DailyRollingFileAppender">
        <param name="File" value="${catalina.base}/logs/wufoo-connector.log"/>
        <param name="Append" value="true"/>
        <layout class="org.apache.log4j.PatternLayout">
            <param name="ConversionPattern" value="%d{dd.MM.yyyy HH:mm:ss} %-5p [%C.%M():%L] %m%n"/>
        </layout>
    </appender>

    <logger name="edu.umd.lib.wufoosysaid.EntryController">
        <level value="debug"/>
	<appender-ref ref="wufoo"/>
    </logger>

</log4j:configuration>
```
3.Add the following configuration to httpd.conf in Apache (wwwdev.lib.umd.edu)

```
<VirtualHost 129.2.19.175:443>
  ServerName wwwdev.lib.umd.edu
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
### Connector Settings
Follow the steps below for Wufoo Connector settings:

1. Installation : Create a folder on server for storing the xsl files for each wufoo form.

2. Add the following lines to `context.xml` to set location of xsl folder on server.
```
<Parameter name="xslLocation" value="/Users/rohit89/Desktop/xsls_server/" override="false"/>
```

3. Add the following lines to `server.xml` inside Host tag to give redirection path for downloading xsls
```
<Context docBase="/Users/rohit89/Desktop/xsls_server" path="/wufoo-connector/xsls" />
```

4. With this you will be able to find the files (eg. /Users/rohit89/Desktop/xsls_server/m6fwrmr1oj910z.xsl) under:
`http://localhost:8080/wufoo-connector/xsls/m6fwrmr1oj910z.xsl`

### Build and deploy wufoo connector on tomcat

1. Download source code from github at [Wufoo Connector](https://github.com/umd-lib/wufoo-connector)
2. Compile the code using
```
mvn package
```

3. Go to `target` folder in wufoo-connector project and paste the `wufoo-connector.war` file in webapps directory on server.
4. Start the tomcat server from the folder where tomcat is installed.

```
./control start
```

## Wufoo Form Configuration


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

### XSL File Modification
1. Save the XSL file with a filename of `[hash].xsl` which translates the entry XML structure into the requests XML structure like `z1h2ln4i177snof.xsl`.
2. Under the root `Requests` element, create a `Request` element for each ticket you wish to be created for a form submission.
3. Under every request element include a 'target' element which defines the target url and other necessary information specific to destination for posting form information.
For example, in Sysaid we add the following:

    * Url(The location where ticket submissions will be made for your SysAid installation. For us, we use the `webformsubmit` script as part of SysAid's built-in webform processing, but it may vary by installation.)

    * Form Id(A unique key used to authorize requests to your help desk. It will be a series of three hexadecimal numbers separated by colons. These numbers can be positive or negative and are 8, 10, and 4 digits long, respectively.)
    
    * Account Id(Your organization's account ID with SysAid. It can be found under the “About” menu in your help desk.).

4. The .xsl file contains a placeholder for alephRx request.
In AlpehRx we add:

   * Url(The location where ticket submissions will be made for your AlephRx installation. 



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
