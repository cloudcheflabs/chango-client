package com.cloudcheflabs.chango.client;

import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Item;
import org.junit.Test;

public class S3ClientTestRunner {

    @Test
    public void listObjects() throws Exception {

        String bucket = System.getProperty("bucket");
        String accessKey = System.getProperty("accessKey");
        String secretKey = System.getProperty("secretKey");
        String endpoint = System.getProperty("endpoint");

        String objectName = "chango/json-files";

        MinioClient minioClient =
                MinioClient.builder()
                        .endpoint(endpoint)
                        .credentials(accessKey, secretKey)
                        .build();

        // Lists objects information recursively.
        Iterable<Result<Item>> results =
                minioClient.listObjects(
                        ListObjectsArgs.builder().bucket(bucket).recursive(true).build());

        for (Result<Item> result : results) {
            Item item = result.get();
            String currentObjectName = item.objectName();
            if(currentObjectName.equals(objectName)) {
                System.out.println(item.lastModified() + "\t" + item.size() + "\t" + item.objectName());
            } else if(currentObjectName.startsWith(objectName)) {
                if(!objectName.endsWith("/")) {
                    objectName = objectName + "/";
                }
                String restPath = currentObjectName.replaceAll(objectName, "");
                if(!restPath.contains("/")) {
                    System.out.println(item.lastModified() + "\t" + item.size() + "\t" + item.objectName());
                }
            }
        }
    }
}
