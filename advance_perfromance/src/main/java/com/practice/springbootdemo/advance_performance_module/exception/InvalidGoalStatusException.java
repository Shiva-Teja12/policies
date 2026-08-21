package com.practice.springbootdemo.advance_performance_module.exception;

public class InvalidGoalStatusException extends RuntimeException{
    public InvalidGoalStatusException(String message) {
        super(message);
    }
}
