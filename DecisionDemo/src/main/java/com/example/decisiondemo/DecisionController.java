package com.example.decisiondemo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DecisionController {
    @GetMapping("/testCommunicate")
    public String sendResponse(@RequestParam("userName") String userName){
        String response = "Receive Message From " + userName;
        return response;
    }
}
