package com.shortener.url_shortener.domain.exceptions;

public abstract class  BaseException extends RuntimeException {

    private int code;

    public BaseException(String message, int code) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

}
