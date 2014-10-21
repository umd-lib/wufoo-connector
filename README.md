Wufoo Connector
=========================

A web service designed to automatically create help desk tickets for ticket tracking systems (example: SysAid(https://libticketingdev.umd.edu/webformsubmit?pageEncoding=utf-8) and AlephRx(http://alephrx.local/cgi-bin/api/reports)) from entries to  [Wufoo](http://www.wufoo.com/) forms to using Wufoo's [webhooks integration](http://help.wufoo.com/articles/en_US/SurveyMonkeyArticleType/Webhooks).


**Warning**: This is development level software, and has only been tested using our installation of SysAid Server. It hasn't been tested on SysAid Cloud and may be incompatible. It also may need to be modified for use with other SysAid installations.



Usage
-----------------
This web service uses the Maven Jetty plugin to run. To run locally, execute the following command from the project root folder:
	
	mvn jetty:run
	
Once running, Jetty will continue to scan the configuration for changes, allowing the addition and modification of form mappings without having to redeploy the service. By default, the service scans every 5 seconds, but this can be changed in the maven-jetty plugin configuration in [pom.xml](pom.xml).

Configuration
-----------------
**Note**: This web service is still in a prototype stage, and some of it's functionality is still unpolished. Ideally, the configuration needs of running this connector will be simplified in the future. 

Configuration for this connector is composed of three types:

* Configuration of the Wufoo forms you wish to map
* The creation of XSL files that map how forms are converted into tickets.
* Configuration of the connector itself

### Wufoo Form Configuration


The basic workflow as-of-now is as follows:

1. In Wufoo, enter the *Notifications* menu for the form you wish to configure, and add a *WebHook integration* from the *to Another Application* menu.
2. Enter the URL of your web service, followed by `/entry/[hash]`, where `[hash]` is the hash assigned to your Wufoo form. If you don't know it, it can be found in the *Api Information* menu under *Code*.

### XSL File Creation
1. In `WEB-INF/xsl`, create an XSL file with a filename of `[hash].xsl` which translates the entry XML structure into the requests XML structure, an example can be seen in `z1h2ln4i177snof.xsl`.
2. Under the root `Requests` element, create a `Request` element for each ticket you wish to be created for a form submission.
3. Under every request element include a 'target' element which defines the target url and other necessary information specific to destination for posting form information.
For example, in Sysaid we add the following:
-Url(The location where ticket submissions will be made for your SysAid installation. For us, we use the `webformsubmit` script as part of SysAid's built-in webform processing, but it may vary by installation.),
-Form Id(A unique key used to authorize requests to your help desk. It will be a series of three hexadecimal numbers separated by colons. These numbers can be positive or negative and are 8, 10, and 4 digits long, respectively.)
-Account Id(Your organization's account ID with SysAid. It can be found under the “About” menu in your help desk.).
4. The .xsl file contains a placeholder for alephRx request.
In AlpehRx we add:
-Url(The location where ticket submissions will be made for your SysAid installation. For us, we use the `webformsubmit` script as part of SysAid's built-in webform processing, but it may vary by installation.)

### Running on Vagrant
The application can be run on a tomcat instance running on a virtual machine that can be setup using Vagrant.

To do this follow the following steps:

1. Install Vagrant and VirtualBox.
2. Open `vagrant/Vagrantfile`.
3. Make sure that <target> in `config.vm.synced_folder "<target>", "/webapps"`, `<target>` points to the absolute path of `/wufoo-connector/target` on your computer, ie. something like `/users/user/git/wufoo-connector/target`
4. Note the port at `host:` under `config.vm.network:`. This is the port that the server will be available on. Default: 4545
5. Navigate to the vagrant folder in the root and run `vagrant up` on terminal. This will download the OS for the VM, the required files and deploy the webapp on a tomcat server running on the VM.
6. Go to [http://localhost:4545/wufoo-connector](http://localhost:4545/wufoo-connector) to access the deployed app.
7. To update the app thats deployed, run `mvn package` from the host machine.



Possible Upcoming Features
-----------------
* Support for WuFoo WebHook Handshake Key
* Easier mapping and configuration support

## License

[CC0](http://creativecommons.org/publicdomain/zero/1.0/)
