package com.neu.edu.info7255.demo.controller;

import com.neu.edu.info7255.demo.beans.JedisBean;
import com.neu.edu.info7255.demo.util.JwtTokenUtil;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.*;

@RestController
public class PlanController {
    Map<String, String> eTags = new HashMap<>();

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private JedisBean jedisBean;

    @PostMapping("/plan")
    public ResponseEntity addPlan(@RequestBody(required = false) String planString, @RequestHeader HttpHeaders requestHeader) {

        HttpHeaders headers = new HttpHeaders();
        JSONObject planJson = new JSONObject(planString);
        if (!isAuthorized(requestHeader)) return new ResponseEntity("Unauthorized Token", HttpStatus.BAD_REQUEST);
        if (!isValidatedSchema(planJson)) return new ResponseEntity("Invalid data", HttpStatus.BAD_REQUEST);

        InputStream planStream = new ByteArrayInputStream(planString.getBytes());
        try {
            String etag = generateETagHeaderValue(planStream, false);

            if (eTags.containsKey(etag)) {
                return new ResponseEntity("This plan is in the store", HttpStatus.NOT_MODIFIED);
            } else {
                //jedis.set(planJson.getString("objectId"), planString);
                jedisBean.add(planJson, planJson.getString("objectType") + ":" + planJson.getString("objectId"));
                JSONObject responseBody = new JSONObject();
                responseBody.put("objectId", planJson.getString("objectId"));
                headers.setETag(etag);
                return new ResponseEntity(responseBody.toMap(), headers, HttpStatus.OK);

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ResponseEntity("Invalid data", headers, HttpStatus.BAD_REQUEST);

    }


    @GetMapping("/plan/{id}")
    public ResponseEntity getPlan(@PathVariable(required = true) String id, @RequestHeader HttpHeaders headers) {

        JSONObject planObj = jedisBean.get("plan:" + id);
        if (planObj == null) return new ResponseEntity("Not found", HttpStatus.NOT_FOUND);
        List<String> list = headers.getIfNoneMatch();
        if (!list.isEmpty()) {
            String requestETag = list.get(0);

            if (eTags.containsKey(requestETag)) {
                return new ResponseEntity("This plan is not modified", HttpStatus.NOT_MODIFIED);
            } else {
                eTags.put(requestETag, id);
                return new ResponseEntity(planObj.toString(), HttpStatus.ACCEPTED);
            }
        } else {
            return new ResponseEntity(planObj.toString(), HttpStatus.ACCEPTED);
        }

    }

    @DeleteMapping("/plan/{id}")
    public ResponseEntity deletePlan(@PathVariable(required = true) String id) {

        boolean isFound = false;
        for (String key : eTags.keySet()) {
            if (eTags.get(key).equals(id)) {
                eTags.remove(key);
                isFound = true;
            }
        }

        jedisBean.delete(id);
        if (!isFound) return new ResponseEntity("Not Found", HttpStatus.NOT_FOUND);


        return new ResponseEntity("Delete Successfully", HttpStatus.ACCEPTED);
    }

    @PatchMapping("/plan/{id}")
    public ResponseEntity patchPlan(@PathVariable(required = true) String id, @RequestBody(required = false) String planString, @RequestHeader HttpHeaders requestHeader) {
        JSONObject planJson = new JSONObject(planString);
        HttpHeaders headers = new HttpHeaders();
        if (!isAuthorized(requestHeader)) return new ResponseEntity("Unauthorized Token", HttpStatus.BAD_REQUEST);
        if (!isValidatedSchema(planJson)) return new ResponseEntity("Invalid data", HttpStatus.BAD_REQUEST);

        String oldTag = null;
        for (String key : eTags.keySet()) {
            if (eTags.get(key).equals(id)) {
                oldTag = key;
                break;
            }
        }

        if (oldTag != null) eTags.remove(oldTag);

        InputStream planStream = new ByteArrayInputStream(planString.getBytes());
        try {
            String etag = generateETagHeaderValue(planStream, false);

            if (jedisBean.update(planJson)) {
                headers.setETag(etag);
                return new ResponseEntity("plan update successfully", headers, HttpStatus.OK);
            } else {
                return new ResponseEntity("plan update failed", headers, HttpStatus.OK);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ResponseEntity("Invalid data", headers, HttpStatus.BAD_REQUEST);

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

    public boolean isValidatedSchema(JSONObject planJson) {
        JSONObject jsonSchema = new JSONObject(new JSONTokener(PlanController.class.getResourceAsStream("/schema.json")));
        Schema schema = SchemaLoader.load(jsonSchema);
        try {
            schema.validate(planJson);
        } catch (ValidationException e) {
            return false;
        }
        return true;
    }

    public boolean isAuthorized(HttpHeaders requestHeader) {
        String authorization = requestHeader.getFirst("Authorization");
        if (authorization == null || authorization.isBlank()) return false;
        String token = authorization.split(" ")[1];
        return jwtTokenUtil.validateToken(token);
    }


}
