#Download tomcat
if [ -d "/home/vagrant/tomcat7" ]; then
	echo "Tomcat already present"
else
	echo "Downloading tomcat"
	wget \
	  --no-verbose \
	  https://archive.apache.org/dist/tomcat/tomcat-7/v7.0.55/bin/apache-tomcat-7.0.55.tar.gz
	tar xzf apache-tomcat-7.0.55.tar.gz
	mv apache-tomcat-7.0.55 tomcat7
fi
