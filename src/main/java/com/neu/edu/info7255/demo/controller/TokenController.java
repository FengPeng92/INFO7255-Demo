package com.neu.edu.info7255.demo.controller;

import com.neu.edu.info7255.demo.util.JwtTokenUtil;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
public class TokenController {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @GetMapping("/generateToken")
    public ResponseEntity<String> generateToken() {
        String token = jwtTokenUtil.generateToken();
        JSONObject result = new JSONObject();
        result.put("token", token);
        return new ResponseEntity(result.toString(), HttpStatus.CREATED);
    }

    @PostMapping("/validateToken")
    public boolean validateToken(@RequestHeader HttpHeaders requestHeader) {
        String authorization = requestHeader.getFirst("Authorization");
        if (authorization == null || authorization.isBlank()) return false;
        String token = authorization.split(" ")[1];
        return jwtTokenUtil.validateToken(token);
    }
}
