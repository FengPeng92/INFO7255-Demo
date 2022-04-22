package com.neu.edu.info7255.demo.config;

import org.apache.http.client.CredentialsProvider;
import org.apache.http.HttpHost;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration;

@Configuration
public class ElasticSearchConfig {
    @Bean
    public RestHighLevelClient ecClient() {

        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

        RestClientBuilder builder = RestClient.builder(new HttpHost("localhost", 9200,"http"))
                .setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                    @Override
                    public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                        return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    }
                });


        RestHighLevelClient client = new RestHighLevelClient(builder);
        return client;
    }


}