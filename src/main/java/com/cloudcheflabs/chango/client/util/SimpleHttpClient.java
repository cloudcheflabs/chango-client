package com.cloudcheflabs.chango.client.util;


import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;

import java.util.concurrent.TimeUnit;

public class SimpleHttpClient {
    private OkHttpClient client = this.buildClient();

    public SimpleHttpClient() {
    }

    public OkHttpClient getClient() {
        return this.client;
    }

    private OkHttpClient buildClient() {
        OkHttpClient.Builder builder = (new OkHttpClient.Builder()).connectTimeout(600L, TimeUnit.SECONDS).readTimeout(600L, TimeUnit.SECONDS).writeTimeout(600L, TimeUnit.SECONDS).connectionPool(new ConnectionPool(5, 60L, TimeUnit.SECONDS));
        return builder.build();
    }
}