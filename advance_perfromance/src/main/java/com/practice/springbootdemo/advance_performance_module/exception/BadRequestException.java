package com.practice.springbootdemo.advance_performance_module.exception;

public class BadRequestException extends RuntimeException{
    public BadRequestException(String message) {
        super(message);
    }
}
