package com.github.sasergeev.restclient;

import org.springframework.http.HttpHeaders;

import java.io.ByteArrayOutputStream;

public interface OnFinished {
    void done(ByteArrayOutputStream buffer, HttpHeaders headers);
}
