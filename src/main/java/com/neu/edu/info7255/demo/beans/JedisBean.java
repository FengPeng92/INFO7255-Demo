package com.neu.edu.info7255.demo.beans;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import redis.clients.jedis.Jedis;


public class JedisBean {

    private static final String SEP = ":";

    Jedis jedis = new Jedis();


    public JSONObject get(String id) {

        JSONObject jsonObj = new JSONObject();
        Set<String> keys = jedis.keys(id + SEP + "*");

        for(String key : keys) {
            Set<String> jsonSet = jedis.smembers(key);

            if(jsonSet.size() > 1) {
                JSONArray jsonArr = new JSONArray();
                Iterator<String> jsonKeySetIterator = jsonSet.iterator();
                while(jsonKeySetIterator.hasNext()) {
                    jsonArr.put(get(jsonKeySetIterator.next()));
                }
                jsonObj.put(key.substring(key.lastIndexOf(SEP) + 1), jsonArr);
            } else {
                Iterator<String> jsonKeySetIterator = jsonSet.iterator();
                JSONObject embdObject = null;
                while(jsonKeySetIterator.hasNext()) {
                    embdObject = get(jsonKeySetIterator.next());
                }
                jsonObj.put(key.substring(key.lastIndexOf(SEP) + 1), embdObject);
            }

        }

        Map<String,String> simpleMap = jedis.hgetAll(id);
        for(String simpleKey : simpleMap.keySet()) {
            jsonObj.put(simpleKey, simpleMap.get(simpleKey));
        }


        return jsonObj;
    }

    public boolean add(JSONObject jsonObject, String prefix) {

        Map<String, String> map = new HashMap<>();
        for(Object key : jsonObject.keySet()) {
            String attributeKey = String.valueOf(key);
            Object attributeVal = jsonObject.get(String.valueOf(key));
            String edge = attributeKey;
            if(attributeVal instanceof JSONObject) {

                JSONObject embdObject = (JSONObject) attributeVal;
                String setKey = prefix + SEP + edge;
                String embd_uuid = embdObject.get("objectType") + SEP + embdObject.getString("objectId");
                jedis.sadd(setKey, embd_uuid);
                add(embdObject, embd_uuid);

            } else if (attributeVal instanceof JSONArray) {

                JSONArray jsonArray = (JSONArray) attributeVal;
                Iterator<Object> jsonIterator = jsonArray.iterator();
                String setKey = prefix + SEP + edge;

                while(jsonIterator.hasNext()) {
                    JSONObject embdObject = (JSONObject) jsonIterator.next();
                    String embd_uuid = embdObject.get("objectType") + SEP + embdObject.getString("objectId");
                    jedis.sadd(setKey, embd_uuid);
                    add(embdObject, embd_uuid);
                }

            } else {
                map.put(attributeKey, String.valueOf(attributeVal));
            }
        }
        jedis.hmset(prefix, map);
        return true;
    }

    // delete plan
    public boolean delete(String objectId) {
        return deleteHelper("plan" + SEP + objectId);
    }

    public boolean deleteHelper(String uuid) {
        // recursively deleting all embedded json objects
        Set<String> keys = jedis.keys(uuid + SEP + "*");
        for(String key : keys) {
            Set<String> jsonKeySet = jedis.smembers(key);
            for(String embd_uuid : jsonKeySet) {
                deleteHelper(embd_uuid);
            }
            jedis.del(key);
        }

        // deleting simple fields
        jedis.del(uuid);
        jedis.close();
        return true;
    }


    public boolean update(JSONObject jsonObject) {
        String uuid = jsonObject.getString("objectType") + SEP + jsonObject.getString("objectId");

        Map<String,String> simpleMap = jedis.hgetAll(uuid);
        if(simpleMap.isEmpty()) {
            simpleMap = new HashMap<String,String>();
        }

        for(Object key : jsonObject.keySet()) {
            String attributeKey = String.valueOf(key);
            Object attributeVal = jsonObject.get(String.valueOf(key));
            String edge = attributeKey;

            if(attributeVal instanceof JSONObject) {

                JSONObject embdObject = (JSONObject) attributeVal;
                String setKey = uuid + SEP + edge;
                String embd_uuid = embdObject.get("objectType") + SEP + embdObject.getString("objectId");
                jedis.sadd(setKey, embd_uuid);
                update(embdObject);

            } else if (attributeVal instanceof JSONArray) {

                JSONArray jsonArray = (JSONArray) attributeVal;
                Iterator<Object> jsonIterator = jsonArray.iterator();
                String setKey = uuid + SEP + edge;

                while(jsonIterator.hasNext()) {
                    JSONObject embdObject = (JSONObject) jsonIterator.next();
                    String embd_uuid = embdObject.get("objectType") + SEP + embdObject.getString("objectId");
                    jedis.sadd(setKey, embd_uuid);
                    update(embdObject);
                }

            } else {
                simpleMap.put(attributeKey, String.valueOf(attributeVal));
            }
        }
        jedis.hmset(uuid, simpleMap);
        return true;
    }

}