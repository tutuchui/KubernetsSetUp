package com.example.apigatewaydemo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

@RestController
public class ApiGatewayController {
    @GetMapping("/")
    public String testExternal(){
        return "This is the exposed API gateway application";
    }

    @GetMapping("/helloDecision")
    public String helloDecision() throws IOException {
        URL decisionURL = new URL("http://decision-service:9898/testCommunicate?userName=Yutong");
        String decisionResponse = ApiUtil.sendGetRequest(decisionURL);
        return decisionResponse;
    }
}
