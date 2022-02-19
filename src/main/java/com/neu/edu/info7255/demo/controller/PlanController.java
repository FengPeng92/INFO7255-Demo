package com.neu.edu.info7255.demo.controller;

import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import redis.clients.jedis.Jedis;

import javax.servlet.annotation.HttpConstraint;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.*;

@RestController
public class PlanController {

    Jedis jedis = new Jedis();
    Map<String, String> eTags = new HashMap<>();



    @PostMapping("/addPlan")
    public ResponseEntity addPlan(@RequestBody(required = false) String planString) {
        HttpHeaders headers = new HttpHeaders();
        JSONObject planJson = new JSONObject(planString);
        JSONObject jsonSchema = new JSONObject(new JSONTokener(PlanController.class.getResourceAsStream("/schema.json")));
        Schema schema = SchemaLoader.load(jsonSchema);
        try {
            schema.validate(planJson);
        } catch (ValidationException e) {
            return new ResponseEntity("Invalid data", HttpStatus.BAD_REQUEST);
        }


        InputStream planStream = new ByteArrayInputStream(planString.getBytes());
        try {
            String etag = generateETagHeaderValue(planStream, false);;
            if (eTags.containsKey(etag)) {
                return new ResponseEntity("This plan is in the store", HttpStatus.NOT_MODIFIED);
            } else {
                jedis.set(planJson.getString("objectId"), planString);
                JSONObject responseBody = new JSONObject();
                responseBody.put("objectId", planJson.getString("objectId"));
                headers.setETag(etag);
                System.out.println();
                return new ResponseEntity(responseBody.toMap(), headers, HttpStatus.OK);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ResponseEntity("Invalid data", headers, HttpStatus.BAD_REQUEST);

    }


    @GetMapping("/getPlan/{id}")
    public ResponseEntity getPlan(@PathVariable(required = true) String id, @RequestHeader HttpHeaders headers) {

        if (jedis.get(id) == null) {
            return new ResponseEntity("Not found", HttpStatus.NOT_FOUND);
        }
        List<String> list = headers.getIfNoneMatch();
        if (!list.isEmpty()) {
            String requestETag = list.get(0);

            if (eTags.containsKey(requestETag)) {
                return new ResponseEntity("This plan is not modified", HttpStatus.NOT_MODIFIED);
            } else {
                eTags.put(requestETag, id);
                return new ResponseEntity(jedis.get(id), HttpStatus.ACCEPTED);
            }
        } else {
            return new ResponseEntity(jedis.get(id), HttpStatus.ACCEPTED);
        }

    }

    @DeleteMapping("/deletePlan/{id}")
    public ResponseEntity deletePlan(@PathVariable(required = true) String id) {

        for (String key : eTags.keySet()) {
            if (eTags.get(key).equals(id)) {
                eTags.remove(key);
            }
        }
        jedis.del(id);

        return new ResponseEntity("Delete Successfully", HttpStatus.ACCEPTED);
    }

    public String generateETagHeaderValue(InputStream inputStream, boolean isWeak) throws IOException {
        StringBuilder builder = new StringBuilder(37);
        if (isWeak) {
            builder.append("W/");
        }

        builder.append("\"0");
        DigestUtils.appendMd5DigestAsHex(inputStream, builder);
        builder.append('"');
        return builder.toString();
    }

}
