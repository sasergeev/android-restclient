package com.github.sasergeev.restclient;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

@FunctionalInterface
public interface OnError {
    void error(String error, HttpHeaders headers, HttpStatus status);
}
