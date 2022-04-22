package com.neu.edu.info7255.demo.util;


import com.neu.edu.info7255.demo.service.MessageQueueService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.exceptions.JedisException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Service
public class MessageQueueUtil {

    @Autowired
    private MessageQueueService messageQueueService;

    private Map<String, String[]> relationMap = new HashMap<>();

    public void indexQueue(JSONObject jsonObject, String objectId) {
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
                    messageQueueService.addToMessageQueue(nextJsonObj.toString(), false);

                } else if (value instanceof JSONArray) {

                    JSONArray jsonArray = (JSONArray) value;

                    for (Object nextObj : jsonArray) {
                        JSONObject nextJsonObj = (JSONObject) nextObj;
                        nextJsonObj.put("parent_id", objectId);

                        String nextObjectId = nextJsonObj.getString("objectId");
                        relationMap.put(nextObjectId, new String[]{objectId, keyStr});
                        indexQueue(nextJsonObj, nextObjectId);
                    }
                } else {
                    simpleMap.put(keyStr, String.valueOf(value));
                }
            }

            JSONObject joinObj = new JSONObject();
            System.out.println("78: " + objectId);

            JSONObject obj1 = new JSONObject(simpleMap);
            obj1.put("plan_join", joinObj);
            if(!simpleMap.containsKey("planType")){

                joinObj.put("name", relationMap.get(objectId)[1]);
                joinObj.put("parent", relationMap.get(objectId)[0]);
                obj1.put("parent_id", relationMap.get(objectId)[0]);
            } else {
                joinObj.put("name", "plan");
            }
            messageQueueService.addToMessageQueue(obj1.toString(), false);
        }
        catch(JedisException e) {
            e.printStackTrace();
        }
    }

}


