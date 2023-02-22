package com.example.hackathon.exception;

import lombok.Getter;

@Getter

public class CustomException extends RuntimeException{

    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super();
        this.errorCode = errorCode;
    }
}
