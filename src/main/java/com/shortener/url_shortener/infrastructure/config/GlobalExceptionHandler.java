package com.shortener.url_shortener.infrastructure.config;

import com.shortener.url_shortener.domain.model.BaseException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({BaseException.class})
    public ResponseEntity<Object> handleStudentNotFoundException(BaseException exception) {
        return ResponseEntity
                .status(exception.getCode())
                .body(exception.getMessage());
    }

}
