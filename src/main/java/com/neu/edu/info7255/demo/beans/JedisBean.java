package com.neu.edu.info7255.demo.beans;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import redis.clients.jedis.Jedis;


public class JedisBean {

    Jedis jedis = new Jedis();

    public JSONObject get(String id) {

        JSONObject planObj = new JSONObject();
        Set<String> keys = jedis.keys(id + ":" + "*");

        for(String key : keys) {
            Set<String> set = jedis.smembers(key);

            if(set.size() > 1) {
                JSONArray jsonArr = new JSONArray();
                // call get recursively
                for (String next : set) jsonArr.put(get(next));
                planObj.put(key.substring(key.lastIndexOf(":") + 1), jsonArr);
            } else {
                JSONObject jsonObj = null;
                for (String next : set) jsonObj = get(next);
                planObj.put(key.substring(key.lastIndexOf(":") + 1), jsonObj);
            }

        }

        Map<String,String> simpleMap = jedis.hgetAll(id);
        for(String simpleKey : simpleMap.keySet()) {
            planObj.put(simpleKey, simpleMap.get(simpleKey));
        }

        return planObj;
    }

    public boolean add(JSONObject jsonObject, String prefix) {

        Map<String, String> map = new HashMap<>();
        for(Object key : jsonObject.keySet()) {
            String keyStr = String.valueOf(key);
            Object value = jsonObject.get(keyStr);
            String nextKey = prefix + ":" + keyStr;
            if(value instanceof JSONObject) {

                JSONObject nextJsonObj = (JSONObject) value;
                String nextPrefix = nextJsonObj.getString("objectType") + ":" + nextJsonObj.getString("objectId");
                jedis.sadd(nextKey, nextPrefix);
                add(nextJsonObj, nextPrefix);


            } else if (value instanceof JSONArray) {

                JSONArray jsonArray = (JSONArray) value;

                for (Object nextObj : jsonArray) {
                    JSONObject nextJsonObj = (JSONObject) nextObj;
                    String nextPrefix = nextJsonObj.getString("objectType") + ":" + nextJsonObj.getString("objectId");
                    jedis.sadd(nextKey, nextPrefix);
                    add(nextJsonObj, nextPrefix);
                }

            } else {
                map.put(keyStr, String.valueOf(value));
            }
        }
        jedis.hmset(prefix, map);
        return true;
    }

    // delete plan
    public boolean delete(String objectId) {
        Set<String> keys = jedis.keys(objectId + ":" + "*");
        for(String key : keys) {
            Set<String> set = jedis.smembers(key);
            for(String nextObjId : set) {
                delete(nextObjId);
            }
            jedis.del(key);
        }

        jedis.del(objectId);
        return true;
    }


    public boolean update(JSONObject jsonObject) {
        String prefix = jsonObject.getString("objectType") + ":" + jsonObject.getString("objectId");

        Map<String,String> map = jedis.hgetAll(prefix);
        if(map.isEmpty()) map = new HashMap<>();

        for(Object key : jsonObject.keySet()) {

            String keyStr = String.valueOf(key);
            Object value = jsonObject.get(keyStr);
            String nextKey = prefix + ":" + keyStr;

            if(value instanceof JSONObject) {

                JSONObject nextJsonObj = (JSONObject) value;
                String nextPrefix = nextJsonObj.getString("objectType") + ":" + nextJsonObj.getString("objectId");
                jedis.sadd(nextKey, nextPrefix);
                update(nextJsonObj);

            } else if (value instanceof JSONArray) {

                JSONArray jsonArray = (JSONArray) value;

                for (Object nextObj : jsonArray) {
                    JSONObject nextJsonObj = (JSONObject) nextObj;
                    String nextPrefix = nextJsonObj.getString("objectType") + ":" + nextJsonObj.getString("objectId");
                    jedis.sadd(nextKey, nextPrefix);
                    update(nextJsonObj);
                }

            } else {
                map.put(keyStr, String.valueOf(value));
            }
        }
        jedis.hmset(prefix, map);
        return true;
    }

}