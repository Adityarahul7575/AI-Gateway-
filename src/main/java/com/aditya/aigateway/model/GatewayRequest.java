package com.aditya.aigateway.model;

import lombok.Data;


import java.util.List;

@Data
public class GatewayRequest {

    private String clientId; //client set by auth filter


    //request parameters to define the model
    private String tier;   // fast | smart | cheap
    private String task;   // chat | code | summarize | analyze


    private List<Message> messages; //conversation

    private Integer maxTokens;
    private Double temperature;

    @Data
    public static class Message{
        private String role;  //user | assistant | system
        private String content;
    }
}
