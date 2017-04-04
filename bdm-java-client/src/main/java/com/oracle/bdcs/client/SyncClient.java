package com.oracle.bdcs.client;

import com.oracle.bdcs.bdm.client.ApiClient;
import com.oracle.bdcs.bdm.client.Configuration;
import com.oracle.bdcs.bdm.client.api.DataMovements;
import com.oracle.bdcs.bdm.client.api.FileSystemProviders;
import com.oracle.bdcs.bdm.client.api.Files;
import com.oracle.bdcs.bdm.client.api.Sessions;
import com.oracle.bdcs.bdm.client.model.*;
import com.oracle.bdcs.bdm.client.model.Path;
import java.util.ArrayList;
import java.util.Base64;


public class SyncClient {

    public static void main(String... args) throws Exception {
        String tenant = "admin";
        String user = "oracle";
        String password = "xxxx";
        String url = "https://xxxxxx:33987/bdcs/api";

        String encoded = Base64.getEncoder().encodeToString(password.getBytes("UTF-8"));
        ApiClient client = new ApiClient();
        client.setBasePath(url)
                .setDebugging(false)
                .setVerifyingSsl(false)
                .setUsername(user);
        client.setPassword(encoded);
        Configuration.setDefaultApiClient(client);

        SessionRequest req = new SessionRequest();
        req.setPassword(encoded);
        req.setUsername(user);
        req.setTenant(tenant);
        SessionResponse resp = new Sessions().createSession(req);
        Configuration.getDefaultApiClient().setApiKey(resp.getToken());

        // list FS providers: HDFS, OSS, S3, BareMetal, ...
        System.out.println("Registered providers: ");
        System.out.println("Type  Uri");
        for(RestFileSystemProviderResponse r : new FileSystemProviders().listAllProviders(tenant, 0, 100).getItems()) {
            System.out.printf("%s,  %s %n",
                    r.getResponse().getProvider().getFileSystemType(),
                    r.getResponse().getProvider().getServiceUri());
        }

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

    }
}
