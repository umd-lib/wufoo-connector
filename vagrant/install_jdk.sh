#Download and install JDK
if [ ! -z `which java` ]; then
	echo "Java already installed"
else
	echo "Installing Java"
	wget \
	  --no-verbose \
	  --no-check-certificate \
	  --no-cookies \
	  --header "Cookie: oraclelicense=accept-securebackup-cookie" \
	  http://download.oracle.com/otn-pub/java/jdk/7u67-b01/jdk-7u67-linux-x64.rpm 
	rpm -Uvh jdk-7u67-linux-x64.rpm
fi 
