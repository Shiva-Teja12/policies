package com.practice.springbootdemo.advance_performance_module.exception;

public class BusinessAuthorizationException extends RuntimeException{

    public BusinessAuthorizationException(String message) {
        super(message);
    }
}
