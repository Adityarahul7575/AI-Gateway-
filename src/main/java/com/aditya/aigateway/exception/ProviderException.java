package com.aditya.aigateway.exception;

import com.aditya.aigateway.router.ProviderRouter;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ProviderException extends RuntimeException{
    private final String provider;
    private final HttpStatus status;

    public ProviderException(String message,String provider,HttpStatus status){
        super(message);
        this.provider = provider;
        this.status = status;
    }
}
