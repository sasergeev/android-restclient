package com.github.sasergeev.restclient;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import androidx.core.os.HandlerCompat;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.OkHttpClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public abstract class AbstractRestClient {

    private final RestTemplate restTemplate;
    private final Message message;
    private final Handler handler;
    private HttpHeaders httpHeaders;
    private HttpMethod httpMethod;
    private Runnable before;
    private Runnable execute;
    private Runnable after;
    private Consumer<Integer> onProgress;
    private BiConsumer<String, String> onDownload;
    private BiConsumer<ByteArrayOutputStream, HttpHeaders> onFinished;

    public AbstractRestClient() {
        this.restTemplate = new RestTemplate();
        this.message = Message.obtain();
        this.handler = HandlerCompat.createAsync(Looper.getMainLooper());
        this.httpHeaders = new HttpHeaders();
    }

    private ResponseEntity<Resource> execute(Object... params) {
        return restTemplate.exchange(buildRequestUrl(), HttpMethod.GET, new HttpEntity<>(buildHeaders()), Resource.class, params);
    }

    private <S> ResponseEntity<S> execute(HttpMethod httpMethod, Class<S> responseType, Object... params) {
        return restTemplate.exchange(buildRequestUrl(), httpMethod, new HttpEntity<>(buildHeaders()), responseType, params);
    }

    private <S, T> ResponseEntity<S> execute(HttpMethod httpMethod, T request, Class<S> responseType, Object... params) {
        return restTemplate.exchange(buildRequestUrl(), httpMethod, new HttpEntity<>(request, buildHeaders()), responseType, params);
    }

    private HttpHeaders buildHeaders() {
        return this.httpHeaders;
    }

    protected void setHttpHeaders(HttpHeaders httpHeaders) {
        this.httpHeaders = httpHeaders;
    }

    protected void setPreExecute(Runnable onPreExecute) {
        this.before = onPreExecute;
    }

    protected void setOnExecute(Runnable onExecute) {
        this.execute = onExecute;
    }

    protected void setPostExecute(Runnable onPostExecute) {
        this.after = onPostExecute;
    }

    protected void setOnProgress(Consumer<Integer> onProgress) {
        this.onProgress = onProgress;
    }

    protected void setOnDownload(BiConsumer<String, String> onDownload) {
        this.onDownload = onDownload;
    }

    protected void setOnFinished(BiConsumer<ByteArrayOutputStream, HttpHeaders> onFinished) {
        this.onFinished = onFinished;
    }

    protected abstract String buildRequestUrl();

    protected void executeRequest(String filePath,
                                  OnError onError,
                                  ExecutorService executor,
                                  Object... params) {
        Optional.ofNullable(before).ifPresent(handler::post);
        try {
            Optional.ofNullable(execute).ifPresent(handler::post);
            process(execute(params), filePath, onError, onProgress, onDownload);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            Optional.ofNullable(onError).
                    ifPresent(error -> error.error(e.getMessage(), e.getResponseHeaders(), e.getStatusCode()));
        } catch (ResourceAccessException e) {
            Optional.ofNullable(onError).
                    ifPresent(error -> error.error(e.getMessage(), null, HttpStatus.SERVICE_UNAVAILABLE));
        } finally {
            Optional.ofNullable(after).ifPresent(handler::post);
            executor.shutdown();
        }
    }

    protected void executeRequest(OnError onError,
                                  ExecutorService executor,
                                  Object... params) {
        Optional.ofNullable(before).ifPresent(handler::post);
        try {
            Optional.ofNullable(execute).ifPresent(handler::post);
            process(execute(params), onError, onFinished);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            Optional.ofNullable(onError).
                    ifPresent(error -> error.error(e.getMessage(), e.getResponseHeaders(), e.getStatusCode()));
        } catch (ResourceAccessException e) {
            Optional.ofNullable(onError).
                    ifPresent(error -> error.error(e.getMessage(), null, HttpStatus.SERVICE_UNAVAILABLE));
        } finally {
            Optional.ofNullable(after).ifPresent(handler::post);
            executor.shutdown();
        }
    }

    protected <S, T> void executeRequest(T body,
                                         Class<S> responseType,
                                         OnSuccess<S> onSuccess,
                                         OnError onError,
                                         Object... params) {
        Optional.ofNullable(before).ifPresent(handler::post);
        ResponseEntity<?> responseEntity = null;
        try {
            responseEntity = execute(httpMethod, body, responseType, params);
            Optional.ofNullable(execute).ifPresent(handler::post);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            responseEntity = new ResponseEntity<>(e.getResponseBodyAsString(), e.getStatusCode());
        } catch (ResourceAccessException e) {
            responseEntity = new ResponseEntity<>(e.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
        } finally {
            Optional.ofNullable(responseEntity).ifPresent(this:: executeResponse);
            handleMessage(message, responseType, onSuccess, onError);
        }
    }

    protected <S> void executeRequest(Class<S> responseType,
                                      OnSuccess<S> onSuccess,
                                      OnError onError,
                                      Object... params) {
        Optional.ofNullable(before).ifPresent(handler::post);
        ResponseEntity<?> responseEntity = null;
        try {
            responseEntity = execute(httpMethod, responseType, params);
            Optional.ofNullable(execute).ifPresent(handler::post);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            responseEntity = new ResponseEntity<>(e.getResponseBodyAsString(), e.getStatusCode());
        } catch (ResourceAccessException e) {
            responseEntity = new ResponseEntity<>(e.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
        } finally {
            Optional.ofNullable(responseEntity)
                    .ifPresent(this::executeResponse);
            handleMessage(message, responseType, onSuccess, onError);
        }
    }

    protected void setMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
    }

    protected void setSsl(InputStream inputStream) {
        this.restTemplate.setRequestFactory(new OkHttpClientHttpRequestFactory(SSLHttpClient.getInstance(inputStream)));
    }

    private void executeResponse(ResponseEntity<?> responseEntity) {
        Bundle bundle = new Bundle();
        message.setTarget(handler);
        message.setData(bundle);
        HttpStatus httpStatus = responseEntity.getStatusCode();
        bundle.putInt("Status", httpStatus.value());
        bundle.putSerializable("Headers", responseEntity.getHeaders());
        if (httpStatus.is2xxSuccessful()) {
            bundle.putSerializable("Object", (Serializable) responseEntity.getBody());
        } else if (httpStatus.is4xxClientError() || httpStatus.is5xxServerError()) {
            bundle.putString("Message", (String) responseEntity.getBody());
        }
    }

    private <S> void handleMessage(Message message, Class<S> responseType, OnSuccess<S> onSuccess, OnError onError) {
        handler.post(() -> {
            HttpStatus httpStatus = HttpStatus.valueOf(message.getData().getInt("Status"));
            if (httpStatus.is2xxSuccessful()) {
                S object = responseType.cast(message.getData().getSerializable("Object"));
                Optional.ofNullable(onSuccess)
                        .ifPresent(s -> s.success(object, (HttpHeaders) message.getData().getSerializable("Headers"), httpStatus));
            } else {
                Optional.ofNullable(onError)
                        .ifPresent(e -> e.error(message.getData().getString("Message"), (HttpHeaders) message.getData().getSerializable("Headers"), httpStatus));
            }
            Optional.ofNullable(after).ifPresent(handler::post);
        });
    }

    protected void process(ResponseEntity<Resource> responseEntity,
                           String filePath,
                           OnError onError,
                           Consumer<Integer> onProgress,
                           BiConsumer<String, String> onDownload) {
        try {
            MediaType mediaType = responseEntity.getHeaders().getContentType();
            String fileName = "file" + "_" + new Date().getTime() + "." + mediaType.getSubtype();
            String file = filePath + "/" + fileName;
            int size = (int) responseEntity.getBody().contentLength();
            InputStream inputStream = new BufferedInputStream(responseEntity.getBody().getInputStream(), size);
            OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file, false));
            byte[] buffer = new byte[2048];
            int count;
            int total = 0;
            int progress;
            while ((count = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, count);
                total += count;
                try {
                    if (onProgress != null) {
                        Thread.sleep(1);
                        progress = total * 100 / size;
                        int finalProgress = progress;
                        handler.post(() -> onProgress.accept(finalProgress));
                    }
                } catch (InterruptedException e) {
                    Optional.ofNullable(onError).
                            ifPresent(error -> error.error(e.getMessage(), responseEntity.getHeaders(), HttpStatus.INTERNAL_SERVER_ERROR));
                }
            }
            outputStream.flush();
            outputStream.close();
            inputStream.close();
            if (total == size) {
                Optional.ofNullable(onDownload)
                        .ifPresent(download -> handler.post(() -> download.accept(file, mediaType.toString())));
            } else {
                Optional.ofNullable(onError).
                        ifPresent(error -> error.error("Error while during downloading file", responseEntity.getHeaders(), HttpStatus.INTERNAL_SERVER_ERROR));
            }
        } catch (IOException e) {
            Optional.ofNullable(onError).
                    ifPresent(error -> error.error(e.getMessage(), responseEntity.getHeaders(), HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    protected void process(ResponseEntity<Resource> responseEntity,
                           OnError onError,
                           BiConsumer<ByteArrayOutputStream, HttpHeaders> onFinished) {
        try {
            HttpHeaders httpHeaders = responseEntity.getHeaders();
            int size = (int) responseEntity.getBody().contentLength();
            InputStream inputStream = new BufferedInputStream(responseEntity.getBody().getInputStream(), size);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[4096];
            int count;
            int total = 0;
            while ((count = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, count);
                total += count;
            }
            buffer.flush();
            buffer.close();
            inputStream.close();
            if (total == size) {
                Optional.ofNullable(onFinished)
                        .ifPresent(finish -> handler.post(() -> finish.accept(buffer, httpHeaders)));
            } else {
                Optional.ofNullable(onError).
                        ifPresent(error -> error.error("Error while during loading data", responseEntity.getHeaders(), HttpStatus.INTERNAL_SERVER_ERROR));
            }
        } catch (IOException e) {
            Optional.ofNullable(onError).
                    ifPresent(error -> error.error(e.getMessage(), responseEntity.getHeaders(), HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

}
