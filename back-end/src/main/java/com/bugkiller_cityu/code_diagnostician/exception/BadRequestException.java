package com.bugkiller_cityu.code_diagnostician.exception;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
