package com.github.sasergeev.restclient;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpBasicAuthentication;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class RestClient<T extends Serializable> extends AbstractRestClient {
    private String queryUrl;
    private OnSuccess<T> onSuccess;
    private OnError onError;
    private final Class<T> responseType;
    private final HttpHeaders httpHeaders;

    private RestClient(Class<T> responseType) {
        super();
        this.responseType = responseType;
        this.httpHeaders = new HttpHeaders();
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

    @Override
    protected String buildRequestUrl() {
        return this.queryUrl;
    }

}
