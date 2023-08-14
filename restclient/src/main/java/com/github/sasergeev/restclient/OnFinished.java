package com.github.sasergeev.restclient;

import java.io.ByteArrayOutputStream;

public interface OnFinished {
    void done(ByteArrayOutputStream buffer, String type);
}
