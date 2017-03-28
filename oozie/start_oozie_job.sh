#!/usr/bin/env bash

user="blaha"
oozie_url="https://xxxxx.us.oracle.com:11443/oozie/"
wf_dir="/user/$user/backup_wf"

kinit $user@BDACLOUDSERVICE.ORACLE.COM -k -t $user.keytab
klist

# hadoop fs -ls /user/$user

# setup workspace
hadoop fs -mkdir -p $wf_dir
hadoop fs -put -f -p workflow.xml $wf_dir
hadoop fs -put -f -p job.properties $wf_dir
hadoop fs -put -f -p backup_script.sh $wf_dir
hadoop fs -put -f -p $user.keytab $wf_dir
hadoop fs -chmod 755 $wf_dir/backup_script.sh

hadoop fs -ls $user.keytab $wf_dir


# kick off oozie job
job_id=`oozie job -oozie $oozie_url -config job.properties -run | awk '{print $2}'`
echo "Started oozie job id:"
echo $job_id
echo "Wait to complete oozie job"
sleep 60 
oozie job -oozie $oozie_url -info $job_id

echo "Print Yarn log"
yarn_app_id=`oozie job -oozie $oozie_url -info $job_id | grep job_ | awk '{print $3}' | sed "s/job/application/g"`
yarn logs -applicationId $yarn_app_id

oozie job -oozie $oozie_url -info $job_id 

kdestroy
