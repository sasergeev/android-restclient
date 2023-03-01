package com.github.sasergeev.restclient;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

@FunctionalInterface
public interface OnSuccess<V> {
    void success(V object, HttpHeaders headers, HttpStatus status);
}
