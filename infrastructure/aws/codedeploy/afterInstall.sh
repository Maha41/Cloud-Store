
#!/bin/bash

sudo systemctl stop tomcat.service

sudo rm -rf /opt/tomcat/apache-tomcat-9.0.17/webapps/docs  /opt/tomcat/apache-tomcat-9.0.17/webapps/examples /opt/tomcat/apache-tomcat-9.0.17/webapps/host-manager  /opt/tomcat/apache-tomcat-9.0.17/webapps/manager /opt/tomcat/apache-tomcat-9.0.17/webapps/ROOT

sudo chown tomcat:tomcat /opt/tomcat/apache-tomcat-9.0.17/webapps/webapp-0.0.1-SNAPSHOT.war
#
# cleanup log files
sudo rm -rf /opt/tomcat/apache-tomcat-9.0.17/webapps/logs/catalina*
sudo rm -rf /opt/tomcat/apache-tomcat-9.0.17/webapps/logs/*.log
sudo rm -rf /opt/tomcat/apache-tomcat-9.0.17/webapps/logs/*.txt
sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl -a fetch-config -m ec2 -c file:/opt/tomcat/apache-tomcat-9.0.17/cloudwatch-config.json -s
sudo systemctl start tomcat.service