package com.aditya.aigateway.provider;

import com.aditya.aigateway.model.GatewayRequest;
import com.aditya.aigateway.model.GatewayResponse;

public interface AiProvider {

    String getProviderName(); //provider model name

    //supporting model name
    boolean supports(String modelName);
    //API call
    GatewayResponse call(GatewayRequest request,String modelName);
}
