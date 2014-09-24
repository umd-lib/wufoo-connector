#Configure tomcat server
cd /home/vagrant/tomcat7
#replace server.xml to change webapp directory on new server
cp -f /defaults/server.xml conf/server.xml
#start tomcat
./bin/startup.sh
#Change permissions and ownership from default
cd /home/vagrant
sudo chown -R vagrant tomcat7
sudo chmod a+rw -R tomcat7

#config bash
cp -f /defaults/bash_profile /home/vagrant/.bash_profile
