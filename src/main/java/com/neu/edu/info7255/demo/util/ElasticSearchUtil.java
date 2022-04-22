package com.neu.edu.info7255.demo.util;

import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentType;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.List;

@Service
public class ElasticSearchUtil {


    private static Jedis jedis = new Jedis();

    private static RestHighLevelClient restHighLevelClient = new RestHighLevelClient(
            RestClient.builder(new HttpHost("localhost", 9200, "http")));

    private static final String IndexName = "plan-index";

    public boolean indexExists() throws IOException {
        GetIndexRequest request = new GetIndexRequest(IndexName);
        return restHighLevelClient.indices().exists(request, RequestOptions.DEFAULT);
    }


    public void createIndex() throws IOException {

        CreateIndexRequest request = new CreateIndexRequest(IndexName);
        request.settings(Settings.builder().put("index.number_of_shards", 1).put("index.number_of_replicas", 2));
        String mapping = getMapping();
        request.mapping(mapping, XContentType.JSON);

        restHighLevelClient.indices().create(request, RequestOptions.DEFAULT);

    }

    public void postDocument() throws IOException {
        if(!indexExists()) {
            createIndex();
        }

        List<String> messages = jedis.lrange("messageQueue", 0, -1);
        for (String message : messages) {
            JSONObject result = new JSONObject(message);
            JSONObject plan= new JSONObject(result.get("message").toString());
            IndexRequest request = new IndexRequest(IndexName);
            request.source(plan.toString(), XContentType.JSON);
            request.id(plan.get("objectId").toString());
            if (plan.has("parent_id")) {
                request.routing(plan.get("parent_id").toString());

            }
            IndexResponse indexResponse = restHighLevelClient.index(request, RequestOptions.DEFAULT);
            System.out.println("response id: "+indexResponse.getId());
        }
    }

    public void deleteDocument() throws IOException {

        DeleteIndexRequest request = new DeleteIndexRequest(IndexName);

        restHighLevelClient.indices().delete(request, RequestOptions.DEFAULT);


    }

    private static String getMapping() {
        String mapping = "{\r\n" +
                "    \"properties\": {\r\n" +
                "      \"objectId\": {\r\n" +
                "        \"type\": \"keyword\"\r\n" +
                "      },\r\n" +
                "      \"plan_join\":{\r\n" +
                "        \"type\": \"join\",\r\n" +
                "        \"relations\":{\r\n" +
                "          \"plan\": [\"planCostShares\", \"linkedPlanServices\"],\r\n" +
                "          \"linkedPlanServices\": [\"linkedService\", \"planserviceCostShares\"]\r\n" +
                "        }\r\n" +
                "      }\r\n" +
                "    }\r\n" +
                "  }\r\n" +
                "}";

        return mapping;
    }

}
