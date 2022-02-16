package com.neu.edu.info7255.demo.controller;

import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.web.bind.annotation.*;
import redis.clients.jedis.Jedis;

import java.util.HashMap;
import java.util.Map;

@RestController
public class PlanController {

    Jedis jedis = new Jedis();
    @PostMapping("/plan")
    public Map<String, String> addPlan(@RequestBody(required = false) String planString) {
        Map<String, String> map = new HashMap<>();
        JSONObject planJson = new JSONObject(planString);
        JSONObject jsonSchema = new JSONObject(new JSONTokener(PlanController.class.getResourceAsStream("/schema.json")));
        Schema schema = SchemaLoader.load(jsonSchema);
        try {
            schema.validate(planJson);

            map.put("objectId", planJson.getString("objectId"));
            jedis.set(planJson.getString("objectId"), planString);
        } catch (ValidationException e) {
            System.out.println(e.getMessage());
        }

        return map;
    }


    @GetMapping("/getPlan/{id}")
    public String getPlan(@PathVariable(required = true) String id) {

        String planString = jedis.get(id);
        return planString;

    }

    @DeleteMapping("/deletePlan/{id}")
    public void deletePlan(@PathVariable(required = true) String id) {
        System.out.println(id);
        jedis.del(id);
    }

}
