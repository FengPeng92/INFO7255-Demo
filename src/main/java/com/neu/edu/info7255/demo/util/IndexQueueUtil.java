package com.neu.edu.info7255.demo.util;


import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;

import java.util.HashMap;
import java.util.Map;

@Service
public class IndexQueueUtil {

    private static Jedis jedis = new Jedis();

    private Map<String, String[]> relationMap = new HashMap<>();

    public void addToQueue(JSONObject jsonObject, String objectId) {
        try {

            Map<String,String> simpleMap = new HashMap<>();

            for(Object key : jsonObject.keySet()) {
                String keyStr = String.valueOf(key);

                Object value = jsonObject.get(String.valueOf(key));

                if(value instanceof JSONObject) {
                    JSONObject nextJsonObj = (JSONObject) value;
                    JSONObject joinObj = new JSONObject();

                    joinObj.put("name", keyStr);

                    joinObj.put("parent", objectId);
                    nextJsonObj.put("plan_join", joinObj);
                    nextJsonObj.put("parent_id", objectId);
                    jedis.lpush("indexQueue", nextJsonObj.toString());

                } else if (value instanceof JSONArray) {

                    JSONArray jsonArray = (JSONArray) value;

                    for (Object nextObj : jsonArray) {
                        JSONObject nextJsonObj = (JSONObject) nextObj;
                        nextJsonObj.put("parent_id", objectId);

                        String nextObjectId = nextJsonObj.getString("objectId");
                        relationMap.put(nextObjectId, new String[]{objectId, keyStr});
                        addToQueue(nextJsonObj, nextObjectId);
                    }
                } else {
                    simpleMap.put(keyStr, String.valueOf(value));
                }
            }

            JSONObject joinObj = new JSONObject();
            System.out.println("78: " + objectId);

            JSONObject currentObj = new JSONObject(simpleMap);
            if(!simpleMap.containsKey("planType")){
                joinObj.put("name", relationMap.get(objectId)[1]);
                joinObj.put("parent", relationMap.get(objectId)[0]);
                currentObj.put("parent_id", relationMap.get(objectId)[0]);
            } else {
                joinObj.put("name", "plan");
            }
            currentObj.put("plan_join", joinObj);
            jedis.lpush("indexQueue", currentObj.toString());
        }
        catch(JedisException e) {
            e.printStackTrace();
        }
    }

}


