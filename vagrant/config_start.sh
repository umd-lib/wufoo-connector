#Configure tomcat server
cd /home/vagrant/tomcat7
#Create a symbolic link for the wufoo-connector webapp to the tomcat deployment directory.
ln -s /webapps/wufoo-connector.war webapps/wufoo-connector.war
#start tomcat
./bin/startup.sh
#Change permissions and ownership from default
cd /home/vagrant
sudo chown -R vagrant tomcat7
sudo chmod a+rw -R tomcat7
mkdir -p resources/xsl

#config bash
cp -f /defaults/bash_profile /home/vagrant/.bash_profile
cp -f /defaults/conf/* /home/vagrant/tomcat7/conf