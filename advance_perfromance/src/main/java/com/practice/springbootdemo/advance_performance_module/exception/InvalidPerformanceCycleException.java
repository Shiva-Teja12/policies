package com.practice.springbootdemo.advance_performance_module.exception;

public class InvalidPerformanceCycleException extends RuntimeException{
    public InvalidPerformanceCycleException(String message) {
        super(message);
    }
}
