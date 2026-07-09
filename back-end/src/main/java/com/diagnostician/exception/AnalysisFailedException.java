package com.diagnostician.exception;

public class AnalysisFailedException extends RuntimeException {
    public AnalysisFailedException(String message) {
        super(message);
    }
}
