# Big Data Manager Java client

Big Data Manager is component that provides unified interface for
accessing and transferring data sored in various stores in Oracle Public Cloud.
Big Data Manager is installed by default in BDCS during service instance 
provisioning.
  
Big Data Manager Java SDK sources are available [here](https://github.com/jazeman/bdm-java-sdk).

Example shows how to authenticate, list objects in HDFS, OSS and transfer
data.
  
```java
// list files in HDFS
        FileSystemOperation op = new FileSystemOperation()
                .operation("LIST")
                .provider("hdfs").path("hdfs:///user/").rawMode(false);
        for(RestFileResponse fileResponse : new Files().listFiles(tenant, op, "0", "100", false).getItems()) {
            System.out.println(fileResponse.getResponse());
        }


        // start data movement job
        // copy oss:///blaha/hi.txt to hdfs:///user/oracle
        ArrayList<Path> sources = new ArrayList<Path>();
        sources.add(new Path("oss:///blaha/hi.txt", "oss"));
        DataMovementRequest request = new DataMovementRequest()
                .sources(sources)
                .destination(new Path("hdfs:///user/oracle", "hdfs"))
                .requestType("COPY")
                .numberOfExecutorNodes(3)
                .numberOfThreadsPerNode(2)
                .memorySizePerNode("1G").driverMemorySize("1G")
                .debugLevel(DataMovementRequest.DebugLevelEnum.INFO);
        RestJobResponse response = new DataMovements().create(tenant, request);
        System.out.println(response.getResponse());
```  