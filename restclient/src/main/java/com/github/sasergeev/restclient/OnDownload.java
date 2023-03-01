package com.github.sasergeev.restclient;

public interface OnDownload {
    void done(String file, String type);
}
