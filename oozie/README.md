# Oozie example

## Introduction
[Oracle distributed copy (odcp)](http://docs.oracle.com/cloud/latest/bigdata-cloud/CSBDI/GUID-62F78D81-A27B-4618-BC28-1D3FAC5B2CB9.htm#CSBDI-GUID-62F78D81-A27B-4618-BC28-1D3FAC5B2CB9)
is command line utility that used for distributed copy large data sets between HDFS, S3, OSS and Bare Metal 
Object Store sources. The tool supports incremental copy, resume failed transfer and more 
features that are useful for integration in Big Data processing pipe lines. This example shows
the integration with [Apache Oozie](http://oozie.apache.org/) that is job workflow scheduler installed
and configured in Oracle Big Data Cloud Service. We will use service with enabled Kerberos service. 

## Steps
We will create workflow that has two actions: delete files in HDFS and run odcp to copy data 
from HDFS to Oracle Object Store. At the end is verified script output to check the transfer
completed successfully. The workflow leverages bash script that encapsulates hadoop and odcp 
invocation.

### Kerberos keytab
The bash script is executed automatically and we need to generate keytab file for Kerberos 
authentication, see example how to create principal (with name blaha) and keytab via ktutil command:
```bash
ktutil
ktutil:  addent -password -p blaha@BDACLOUDSERVICE.ORACLE.COM -k 1 -e rc4-hmac
Password for blaha@BDACLOUDSERVICE.ORACLE.COM:
ktutil:  addent -password -p blaha@BDACLOUDSERVICE.ORACLE.COM -k 1 -e aes256-cts
Password for blaha@BDACLOUDSERVICE.ORACLE.COM:
ktutil:  wkt /home/blaha/blaha.keytab
ktutil:  quit
```
Anyone with read permission on a keytab file can use all the keys in the file. To prevent misuse, 
restrict access permissions for any keytab files you create. 

### Shell script
The script calls hadoop fs command to clean up destination and odcp to transfer data. Please, read odcp documentation
to get more info about command arguments. At the end is printed message to console that is further checked in oozie
worflow. See script backup_script.sh

### Oozie workflow
Oooze worflow is defined as Directed Acyclical Graph (DAG) of actions. The action is computation task (M-R job, 
shell command) that is executed on node in Hadoop cluster. The workflow is defined in xml file (workflow.xml), see example
```xml
<workflow-app xmlns="uri:oozie:workflow:0.4" name="backup-wf">
    <start to="shell"/>

    <action name="shell">
        <shell xmlns="uri:oozie:shell-action:0.2">
            <job-tracker>${jobTracker}</job-tracker>
        <name-node>${nameNode}</name-node>
        <configuration>
            <property>
                <name>mapred.job.queue.name</name>
                <value>${queueName}</value>
            </property>
        </configuration>
            <exec>backup_script.sh</exec>
            <argument>${user}</argument>
            <file>backup_script.sh</file>
            <file>blaha.keytab</file>
            <capture-output/>
        </shell>
        <ok to="check-output"/>
        <error to="fail"/>
    </action>

    <decision name="check-output">
        <switch>
            <case to="end">
                ${wf:actionData('shell')['completed']}
            </case>
            <default to="fail"/>
        </switch>
    </decision>

    <kill name="fail">
        <message>Workflow failed, error message[${wf:errorMessage(wf:lastErrorNode())}]</message>
    </kill>

    <end name="end"/>
</workflow-app>
```
Where is shell action that executes bash script with appropriate arguments. Keytab and scripts are defined in
file elements to be copied to local execution file system. Based on action outcome the flow fails or checks
 script's output.

### Run Oozie job
To start job, we need to copy required data to HDFS and run job through oozie command line interface, see
```bash
user="blaha"
oozie_url="https://xxxxx.us.oracle.com:11443/oozie/"
wf_dir="/user/$user/backup_wf"

hadoop fs -mkdir -p $wf_dir
hadoop fs -put -f -p workflow.xml $wf_dir
hadoop fs -put -f -p job.properties $wf_dir
hadoop fs -put -f -p backup_script.sh $wf_dir
hadoop fs -put -f -p $user.keytab $wf_dir
hadoop fs -chmod 755 $wf_dir/backup_script.sh

oozie job -oozie $oozie_url -config job.properties -run
```

### Debug Oozie job
We can get Oozie job's info as
```bash
job_id=`oozie job -oozie $oozie_url -config job.properties -run | awk '{print $2}'`
oozie job -oozie $oozie_url -info $job_id
```
and yarn logs
```bash
yarn_app_id=`oozie job -oozie $oozie_url -info $job_id | grep job_ | awk '{print $3}' | sed "s/job/application/g"`
yarn logs -applicationId $yarn_app_id
```