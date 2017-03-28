#!/bin/bash -e

user="$1"
keytab=$user.keytab
principal="$user@BDACLOUDSERVICE.ORACLE.COM"
src="hdfs:///user/oracle/hi.txt"
# swift://CONTAINER.STORAGE-DOMAIN/....
dst="swift://blaha.a1111/hi.txt"

export HADOOP_CONF_DIR=/etc/hadoop/conf
hadoop fs \
-libjars /opt/oracle/bda/bdcs/bdcs-rest-api-app/current/lib-hadoop/hadoop-openstack-spoc-2.7.2.jar \
-rm -f $dst

odcp -V --krb-keytab $keytab --krb-principal $principal \
--block-size 536870912 --executor-cores 2 --executor-memory 1 --sync \
$src $dst

echo "completed=true"
exit 0
