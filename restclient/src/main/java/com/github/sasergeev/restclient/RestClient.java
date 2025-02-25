package com.github.sasergeev.restclient;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.core.os.HandlerCompat;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpBasicAuthentication;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.OkHttpClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class RestClient<T extends Serializable> {
    private final ExecutorService executorService;
    private final RestTemplate restTemplate;
    private final Message message;
    private final Handler handler;
    private String queryUrl;
    private OnSuccess<T> onSuccess;
    private OnError onError;
    private final Class<T> responseType;
    private HttpHeaders httpHeaders;
    private HttpMethod httpMethod;
    private Runnable before;
    private Runnable execute;
    private Runnable after;
    private Consumer<Integer> onProgress;
    private BiConsumer<String, String> onDownload;
    private BiConsumer<ByteArrayOutputStream, HttpHeaders> onLoad;

    private RestClient(Class<T> responseType) {
        this.executorService = Executors.newCachedThreadPool();
        this.restTemplate = new RestTemplate();
        this.message = Message.obtain();
        this.handler = HandlerCompat.createAsync(Looper.getMainLooper());
        this.httpHeaders = new HttpHeaders();
        this.responseType = responseType;
    }

    public static <T extends Serializable> RestClient<T> build(Class<T> clazz) {
        return new RestClient<>(clazz);
    }

    /**
     * Set base url of API
     */
    public RestClient<T> url(String url) {
        this.queryUrl = url;
        return this;
    }

    /**
     * Set URI
     */
    public RestClient<T> uri(String uri) {
        this.queryUrl = queryUrl.concat(uri);
        return this;
    }

    /**
     * Use this if required basic auth
     */
    public RestClient<T> auth(HttpBasicAuthentication basicAuthentication) {
        httpHeaders.setAuthorization(basicAuthentication);
        return this;
    }

    /**
     * Use this if required based on bearer token
     */
    public RestClient<T> auth(String jwt) {
        String BEARER_PREFIX = "Bearer ";
        httpHeaders.add(HttpHeaders.AUTHORIZATION, BEARER_PREFIX.concat(jwt));
        return this;
    }

    /**
     * Use this if required custom auth
     */
    public RestClient<T> auth(String header, String token) {
        httpHeaders.add(HttpHeaders.AUTHORIZATION, header + " " + token);
        return this;
    }

    /**
     * Use this for set additional headers
     */
    public RestClient<T> headers(Map<String, String> headers) {
        httpHeaders.setAll(headers);
        return this;
    }

    /**
     * Use this if required ssl-connection
     */
    public RestClient<T> ssl(InputStream x509Cert) {
        setSsl(x509Cert);
        return this;
    }

    /**
     * Handle success result by request
     */
    public RestClient<T> success(OnSuccess<T> onSuccess) {
        this.onSuccess = onSuccess;
        return this;
    }

    /**
     * Handle error result by request
     */
    public RestClient<T> error(OnError onError) {
        this.onError = onError;
        return this;
    }

    public RestClient<T> execute(Runnable onExecute) {
        setOnExecute(onExecute);
        return this;
    }

    /**
     * Handle if pre execute action
     */
    public RestClient<T> before(Runnable onPreExecute) {
        setPreExecute(onPreExecute);
        return this;
    }

    /**
     * Handle if post execute action
     */
    public RestClient<T> finish(Runnable onPostExecute) {
        setPostExecute(onPostExecute);
        return this;
    }

    /**
     * Handle progress value while download file
     */
    public RestClient<T> progress(Consumer<Integer> onProgress) {
        setOnProgress(onProgress);
        return this;
    }

    /**
     * Handle file path and content type
     */
    public RestClient<T> onDownload(BiConsumer<String, String> onDownload) {
        setOnDownload(onDownload);
        return this;
    }

    /**
     * Handle file path and content type
     */
    public RestClient<T> onLoad(BiConsumer<ByteArrayOutputStream, HttpHeaders> onLoad) {
        setOnLoad(onLoad);
        return this;
    }

    /**
     * GET-request for API with some params in URI like - param1={param1}&param2={param1}
     */
    public RestClient<T> get(Object... params) {
        setMethod(HttpMethod.GET);
        setHttpHeaders(httpHeaders);
        executeRequest(responseType, onSuccess, onError, params);
        return this;
    }

    /**
     * POST-request for API with body and params
     */
    public RestClient<T> post(MultiValueMap<String, Object> body, Object... params) {
        setMethod(HttpMethod.POST);
        this.httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        setHttpHeaders(httpHeaders);
        executeRequest(body, responseType, onSuccess, onError, params);
        return this;
    }

    /**
     * POST-request for API with JSON-body and params
     */
    public RestClient<T> post(Map<String, Object> body, Object... params) {
        setMethod(HttpMethod.POST);
        this.httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        setHttpHeaders(httpHeaders);
        executeRequest(body, responseType, onSuccess, onError, params);
        return this;
    }

    /**
     * POST-request for API with POJO-class and params
     */
    public <S> RestClient<T> post(S body, Object... params) {
        setMethod(HttpMethod.POST);
        this.httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        setHttpHeaders(httpHeaders);
        executeRequest(body, responseType, onSuccess, onError, params);
        return this;
    }

    /**
     * POST-request for API with some files
     */
    public RestClient<T> post(Map<String, String> body, String key, MediaType mediaType, ByteArrayResource... resources) {
        setMethod(HttpMethod.POST);
        this.httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
        setHttpHeaders(httpHeaders);
        MultiValueMap<String, Object> requestBody = new LinkedMultiValueMap<>();
        HttpHeaders imageHeaders = new HttpHeaders();
        imageHeaders.setContentType(mediaType);
        Arrays.stream(resources).forEach(resource -> Optional.ofNullable(resource)
                .ifPresent(result -> requestBody.add(key, new HttpEntity<>(result, imageHeaders))));
        HttpHeaders textHeaders = new HttpHeaders();
        textHeaders.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));
        body.forEach((k, v) -> requestBody.add(k, new HttpEntity<>(v, textHeaders)));
        executeRequest(requestBody, responseType, onSuccess, onError);
        return this;
    }

    /**
     * POST-request for API with some files
     */
    public RestClient<T> post(String key, Collection<ByteArrayResource> resources, Object... params) {
        setMethod(HttpMethod.POST);
        this.httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
        setHttpHeaders(httpHeaders);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        resources.forEach(resource -> Optional.ofNullable(resource).ifPresent(result -> body.add(key, result)));
        executeRequest(body, responseType, onSuccess, onError, params);
        return this;
    }

    /**
     * PUT-request for API with body and params
     */
    public RestClient<T> put(MultiValueMap<String, Object> body, Object... params) {
        setMethod(HttpMethod.PUT);
        this.httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        setHttpHeaders(httpHeaders);
        executeRequest(body, responseType, onSuccess, onError, params);
        return this;
    }

    /**
     * PUT-request for API with JSON-body and params
     */
    public RestClient<T> put(Map<String, Object> body, Object... params) {
        setMethod(HttpMethod.PUT);
        this.httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        setHttpHeaders(httpHeaders);
        executeRequest(body, responseType, onSuccess, onError, params);
        return this;
    }

    /**
     * PUT-request for API with POJO-class and params
     */
    public <S> RestClient<T> put(S body, Object... params) {
        setMethod(HttpMethod.PUT);
        this.httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        setHttpHeaders(httpHeaders);
        executeRequest(body, responseType, onSuccess, onError, params);
        return this;
    }

    /**
     * PUT-request for API with some files
     */
    public RestClient<T> put(Map<String, String> body, String key, MediaType mediaType, ByteArrayResource... resources) {
        setMethod(HttpMethod.POST);
        this.httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
        setHttpHeaders(httpHeaders);
        MultiValueMap<String, Object> requestBody = new LinkedMultiValueMap<>();
        HttpHeaders imageHeaders = new HttpHeaders();
        imageHeaders.setContentType(mediaType);
        Arrays.stream(resources).forEach(resource -> Optional.ofNullable(resource)
                .ifPresent(result -> requestBody.add(key, new HttpEntity<>(result, imageHeaders))));
        HttpHeaders textHeaders = new HttpHeaders();
        textHeaders.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));
        body.forEach((k, v) -> requestBody.add(k, new HttpEntity<>(v, textHeaders)));
        executeRequest(requestBody, responseType, onSuccess, onError);
        return this;
    }

    /**
     * PUT-request for API with some files
     */
    public RestClient<T> put(String key, Collection<ByteArrayResource> resources, Object... params) {
        setMethod(HttpMethod.PUT);
        this.httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
        setHttpHeaders(httpHeaders);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        resources.forEach(resource -> Optional.ofNullable(resource).ifPresent(result -> body.add(key, result)));
        executeRequest(body, responseType, onSuccess, onError, params);
        return this;
    }

    /**
     * DELETE-request for API with params
     */
    public RestClient<T> delete(Object... params) {
        setMethod(HttpMethod.DELETE);
        setHttpHeaders(httpHeaders);
        executeRequest(responseType, onSuccess, onError, params);
        return this;
    }

    /**
     * Download file with some params
     */
    public void download(String filePath, Object... params) {
        setHttpHeaders(httpHeaders);
        executeRequest(filePath, onError, params);
    }

    /**
     * Download file with some params
     */
    public void load(Object... params) {
        setHttpHeaders(httpHeaders);
        executeRequest(onError, params);
    }

    private ResponseEntity<Resource> execute(Object... params) {
        return restTemplate.exchange(buildRequestUrl(), HttpMethod.GET, new HttpEntity<>(buildHeaders()), Resource.class, params);
    }

    private String buildRequestUrl() {
        return this.queryUrl;
    }

    private <S> ResponseEntity<S> execute(HttpMethod httpMethod, Class<S> responseType, Object... params) {
        return restTemplate.exchange(buildRequestUrl(), httpMethod, new HttpEntity<>(buildHeaders()), responseType, params);
    }

    private <S, V> ResponseEntity<S> execute(HttpMethod httpMethod, V request, Class<S> responseType, Object... params) {
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

    protected void setOnLoad(BiConsumer<ByteArrayOutputStream, HttpHeaders> onLoad) {
        this.onLoad = onLoad;
    }

    protected void executeRequest(String filePath,
                                  OnError onError,
                                  Object... params) {
        executorService.execute(() -> {
            Optional.ofNullable(before).ifPresent(handler::post);
            try {
                Optional.ofNullable(execute).ifPresent(handler::post);
                process(execute(params), filePath, onError, onProgress, onDownload);
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                Optional.ofNullable(onError).
                        ifPresent(error -> error.error(e.getMessage(), e.getResponseHeaders(), e.getStatusCode()));
            } catch (ResourceAccessException e) {
                Optional.ofNullable(onError)
                        .ifPresent(error -> error.error(e.getMessage(), null, HttpStatus.SERVICE_UNAVAILABLE));
            } finally {
                Optional.ofNullable(after).ifPresent(handler::post);
                executorService.shutdown();
            }
        });
    }

    protected void executeRequest(OnError onError,
                                  Object... params) {
        executorService.execute(() -> {
            Optional.ofNullable(before).ifPresent(handler::post);
            try {
                Optional.ofNullable(execute).ifPresent(handler::post);
                process(execute(params), onError, onLoad);
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                Optional.ofNullable(onError).
                        ifPresent(error -> error.error(e.getMessage(), e.getResponseHeaders(), e.getStatusCode()));
            } catch (ResourceAccessException e) {
                Optional.ofNullable(onError)
                        .ifPresent(error -> error.error(e.getMessage(), null, HttpStatus.SERVICE_UNAVAILABLE));
            } finally {
                Optional.ofNullable(after).ifPresent(handler::post);
                executorService.shutdown();
            }
        });
    }

    protected <S, V> void executeRequest(V body,
                                         Class<S> responseType,
                                         Object... params) {
        executorService.execute(() -> {
            Optional.ofNullable(before).ifPresent(handler::post);
            ResponseEntity<?> responseEntity = null;
            try {
                Optional.ofNullable(execute).ifPresent(handler::post);
                responseEntity = execute(httpMethod, body, responseType, params);
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                responseEntity = new ResponseEntity<>(e.getResponseBodyAsString(), e.getStatusCode());
            } catch (ResourceAccessException e) {
                responseEntity = new ResponseEntity<>(e.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
            } finally {
                Optional.ofNullable(responseEntity).ifPresent(this:: executeResponse);
                handleMessage(message);
                executorService.shutdown();
            }
        });
    }

    protected <S> void executeRequest(Class<S> responseType,
                                      Object... params) {
        executorService.execute(() -> {
            Optional.ofNullable(before).ifPresent(handler::post);
            ResponseEntity<?> responseEntity = null;
            try {
                Optional.ofNullable(execute).ifPresent(handler::post);
                responseEntity = execute(httpMethod, responseType, params);
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                responseEntity = new ResponseEntity<>(e.getResponseBodyAsString(), e.getStatusCode());
            } catch (ResourceAccessException e) {
                responseEntity = new ResponseEntity<>(e.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
            } finally {
                Optional.ofNullable(responseEntity).ifPresent(this::executeResponse);
                handleMessage(message);
                executorService.shutdown();
            }
        });
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

    private void handleMessage(Message message) {
        handler.post(() -> {
            HttpStatus httpStatus = HttpStatus.valueOf(message.getData().getInt("Status"));
            if (httpStatus.is2xxSuccessful()) {
                T object = responseType.cast(message.getData().getSerializable("Object"));
                Optional.ofNullable(onSuccess)
                        .ifPresent(s -> s.success(object, (HttpHeaders) message.getData().getSerializable("Headers"), httpStatus));
            } else {
                Optional.ofNullable(onError)
                        .ifPresent(e -> e.error(message.getData().getString("Message"), (HttpHeaders) message.getData().getSerializable("Headers"), httpStatus));
            }
            Optional.ofNullable(after).ifPresent(handler::post);
        });
    }

    private void process(ResponseEntity<Resource> responseEntity,
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
                    Optional.ofNullable(onError)
                            .ifPresent(error -> error.error(e.getMessage(), responseEntity.getHeaders(), HttpStatus.INTERNAL_SERVER_ERROR));
                }
            }
            outputStream.flush();
            outputStream.close();
            inputStream.close();
            if (total == size) {
                Optional.ofNullable(onDownload)
                        .ifPresent(download -> handler.post(() -> download.accept(file, mediaType.toString())));
            } else {
                Optional.ofNullable(onError)
                        .ifPresent(error -> error.error("Error while during downloading file", responseEntity.getHeaders(), HttpStatus.INTERNAL_SERVER_ERROR));
            }
        } catch (IOException e) {
            Optional.ofNullable(onError)
                    .ifPresent(error -> error.error(e.getMessage(), responseEntity.getHeaders(), HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    private void process(ResponseEntity<Resource> responseEntity,
                         OnError onError,
                         BiConsumer<ByteArrayOutputStream, HttpHeaders> onLoad) {
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
                Optional.ofNullable(onLoad)
                        .ifPresent(load -> handler.post(() -> load.accept(buffer, httpHeaders)));
            } else {
                Optional.ofNullable(onError)
                        .ifPresent(error -> error.error("Error while during loading data", responseEntity.getHeaders(), HttpStatus.INTERNAL_SERVER_ERROR));
            }
        } catch (IOException e) {
            Optional.ofNullable(onError)
                    .ifPresent(error -> error.error(e.getMessage(), responseEntity.getHeaders(), HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

}