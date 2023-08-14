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
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import static java.util.concurrent.Executors.newCachedThreadPool;

/**
 * Main class for asynchronous request to call API
 */
public final class RestClientBuilder<T extends Serializable> {
    private T instance;
    private OnSuccess<T> onSuccess;
    private OnError onError;
    private OnExecute onExecute;
    private OnProgress onProgress;
    private OnPreExecute onPreExecute;
    private OnPostExecute onPostExecute;
    private OnDownload onDownload;
    private OnFinished onFinished;
    private final Class<T> responseType;
    private String queryUrl = "";
    private final RestTemplate restTemplate;
    private final ExecutorService executorService;
    private final Message message = Message.obtain();
    private final Handler handler = HandlerCompat.createAsync(Looper.getMainLooper());
    private final HttpHeaders httpHeaders = new HttpHeaders();

    private RestClientBuilder(Class<T> clazz) {
        this.responseType = clazz;
        this.restTemplate = new RestTemplate();
        this.executorService = newCachedThreadPool();
        try {
            this.instance = clazz.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }
    }

    /**
     * Set base url of API
     */
    public RestClientBuilder<T> url(String url) {
        this.queryUrl = url;
        return this;
    }

    /**
     * Set URI
     */
    public RestClientBuilder<T> uri(String uri) {
        this.queryUrl = queryUrl.concat(uri);
        return this;
    }

    /**
     * Use this if required basic auth
     */
    public RestClientBuilder<T> auth(HttpBasicAuthentication basicAuthentication) {
        this.httpHeaders.setAuthorization(basicAuthentication);
        return this;
    }

    /**
     * Use this if required based on bearer token
     */
    public RestClientBuilder<T> auth(String jwt) {
        this.httpHeaders.add(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
        return this;
    }

    /**
     * Use this if required custom auth
     */
    public RestClientBuilder<T> auth(String header, String token) {
        this.httpHeaders.add(HttpHeaders.AUTHORIZATION, header + " " + token);
        return this;
    }

    /**
     * Use this for set additional headers
     */
    public RestClientBuilder<T> headers(Map<String, String> headers) {
        this.httpHeaders.setAll(headers);
        return this;
    }


    /**
     * Use this if required ssl-connection
     */
    public RestClientBuilder<T> ssl(InputStream x509Cert) {
        this.restTemplate.setRequestFactory(new OkHttpClientHttpRequestFactory(SSLHttpClient.getInstance(x509Cert)));
        return this;
    }

    /**
     * Handle success result by request
     */
    public RestClientBuilder<T> success(OnSuccess<T> onSuccess) {
        this.onSuccess = onSuccess;
        return this;
    }

    /**
     * Handle error result by request
     */
    public RestClientBuilder<T> error(OnError onError) {
        this.onError = onError;
        return this;
    }

    /**
     * Handle if on execute action
     */
    public RestClientBuilder<T> execute(OnExecute onExecute) {
        this.onExecute = onExecute;
        return this;
    }

    /**
     * Handle if pre execute action
     */
    public RestClientBuilder<T> before(OnPreExecute onPreExecute) {
        this.onPreExecute = onPreExecute;
        return this;
    }

    /**
     * Handle if post execute action
     */
    public RestClientBuilder<T> finish(OnPostExecute onPostExecute) {
        this.onPostExecute = onPostExecute;
        return this;
    }

    /**
     * Handle progress value while download file
     */
    public RestClientBuilder<T> progress(OnProgress onProgress) {
        this.onProgress = onProgress;
        return this;
    }

    /**
     * Handle file path and content type
     */
    public RestClientBuilder<T> done(OnDownload onDownload) {
        this.onDownload = onDownload;
        return this;
    }

    /**
     * Handle file path and content type
     */
    public RestClientBuilder<T> done(OnFinished onFinished) {
        this.onFinished = onFinished;
        return this;
    }

    /**
     * GET-request for API with some params in URI like - param1={param1}&param2={param1}
     */
    public RestClientBuilder<T> get(Object... params) {
        executorService.execute(() -> {
            if (onPreExecute != null)
                handler.post(() -> onPreExecute.before());
            HttpEntity<?> httpEntity = new HttpEntity<>(httpHeaders);
            ResponseEntity<?> responseEntity = null;
            try {
                responseEntity = restTemplate.exchange(queryUrl, HttpMethod.GET, httpEntity, responseType, params);
                if (onExecute != null)
                    handler.post(() -> onExecute.execute());
            } catch (HttpClientErrorException e) {
                responseEntity = new ResponseEntity<>(e.getResponseBodyAsString(), e.getStatusCode());
            } catch (HttpServerErrorException e) {
                responseEntity = new ResponseEntity<>(e.getResponseBodyAsString(), HttpStatus.INTERNAL_SERVER_ERROR);
            } catch (ResourceAccessException e) {
                responseEntity = new ResponseEntity<>("Timeout error connection", HttpStatus.GATEWAY_TIMEOUT);
            } finally {
                Bundle bundle = new Bundle();
                if (responseEntity != null) {
                    execute(responseEntity, bundle);
                }
                message.setTarget(handler);
                message.setData(bundle);
                handleMessage(message);
            }
        });
        return this;
    }

    /**
     * POST-request for API with body and params
     */
    public RestClientBuilder<T> post(MultiValueMap<String, Object> body, Object... params) {
        executorService.execute(() -> {
            if (onPreExecute != null)
                handler.post(() -> onPreExecute.before());
            this.httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<MultiValueMap<String, Object>> httpEntity = new HttpEntity<>(body, httpHeaders);
            postExecute(httpEntity, params);
        });
        return this;
    }

    /**
     * POST-request for API with JSON-body and params
     */
    public RestClientBuilder<T> post(Map<String, Object> body, Object... params) {
        executorService.execute(() -> {
            if (onPreExecute != null)
                handler.post(() -> onPreExecute.before());
            this.httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, httpHeaders);
            postExecute(httpEntity, params);
        });
        return this;
    }

    /**
     * POST-request for API with POJO-class and params
     */
    public RestClientBuilder<T> post(T object, Object... params) {
        executorService.execute(() -> {
            if (onPreExecute != null)
                handler.post(() -> onPreExecute.before());
            this.httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<T> httpEntity = new HttpEntity<>(object, httpHeaders);
            postExecute(httpEntity, params);
        });
        return this;
    }

    /**
     * POST-request for API with some files
     */
    public RestClientBuilder<T> post(Map<String, String> body, String keyFile, MediaType mediaType, ByteArrayResource... resources) {
        executorService.execute(() -> {
            if (onPreExecute != null)
                handler.post(() -> onPreExecute.before());
            MultiValueMap<String, Object> requestBody = new LinkedMultiValueMap<>();
            this.httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpHeaders imageHeaders = new HttpHeaders();
            imageHeaders.setContentType(mediaType);
            for (ByteArrayResource resource : resources) {
                if (resource != null) {
                    HttpEntity<ByteArrayResource> imageResources = new HttpEntity<>(resource, imageHeaders);
                    requestBody.add(keyFile, imageResources);
                }
            }
            HttpHeaders textHeaders = new HttpHeaders();
            textHeaders.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));
            for (Map.Entry<String, String> entry : body.entrySet()) {
                HttpEntity<String> stringEntity = new HttpEntity<>(entry.getValue(), textHeaders);
                requestBody.add(entry.getKey(), stringEntity);
            }
            HttpEntity<MultiValueMap<String, Object>> httpEntity = new HttpEntity<>(requestBody, httpHeaders);
            postExecute(httpEntity);
        });
        return this;
    }

    /**
     * PUT-request for API with JSON-body and params
     */
    public RestClientBuilder<T> put(Map<String, Object> body, Object... params) {
        executorService.execute(() -> {
            if (onPreExecute != null)
                handler.post(() -> onPreExecute.before());
            this.httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, httpHeaders);
            putExecute(httpEntity, params);
        });
        return this;
    }

    /**
     * PUT-request for API with POJO-class and params
     */
    public RestClientBuilder<T> put(T object, Object... params) {
        executorService.execute(() -> {
            if (onPreExecute != null)
                handler.post(() -> onPreExecute.before());
            this.httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<T> httpEntity = new HttpEntity<>(object, httpHeaders);
            putExecute(httpEntity, params);
        });
        return this;
    }

    /**
     * PUT-request for API with body and params
     */
    public RestClientBuilder<T> put(MultiValueMap<String, Object> body, Object... params) {
        executorService.execute(() -> {
            if (onPreExecute != null)
                handler.post(() -> onPreExecute.before());
            this.httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<MultiValueMap<String, Object>> httpEntity = new HttpEntity<>(body, httpHeaders);
            putExecute(httpEntity, params);
        });
        return this;
    }

    /**
     * PUT-request for API with some files
     */
    public RestClientBuilder<T> put(Map<String, String> body, String key, MediaType mediaType, ByteArrayResource... resources) {
        executorService.execute(() -> {
            if (onPreExecute != null)
                handler.post(() -> onPreExecute.before());
            MultiValueMap<String, Object> requestBody = new LinkedMultiValueMap<>();
            this.httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpHeaders imageHeaders = new HttpHeaders();
            imageHeaders.setContentType(mediaType);
            for (ByteArrayResource resource : resources) {
                if (resource != null) {
                    HttpEntity<ByteArrayResource> imageResources = new HttpEntity<>(resource, imageHeaders);
                    requestBody.add(key, imageResources);
                }
            }
            HttpHeaders textHeaders = new HttpHeaders();
            textHeaders.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));
            for (Map.Entry<String, String> entry : body.entrySet()) {
                HttpEntity<String> stringEntity = new HttpEntity<>(entry.getValue(), textHeaders);
                requestBody.add(entry.getKey(), stringEntity);
            }
            HttpEntity<MultiValueMap<String, Object>> httpEntity = new HttpEntity<>(requestBody, httpHeaders);
            putExecute(httpEntity);
        });
        return this;
    }

    /**
     * DELETE-request for API with params
     */
    public RestClientBuilder<T> delete(Object... params) {
        executorService.execute(() -> {
            if (onPreExecute != null)
                handler.post(() -> onPreExecute.before());
            HttpEntity<?> httpEntity = new HttpEntity<>(httpHeaders);
            ResponseEntity<?> responseEntity = null;
            try {
                responseEntity = restTemplate.exchange(queryUrl, HttpMethod.DELETE, httpEntity, responseType, params);
                if (onExecute != null)
                    handler.post(() -> onExecute.execute());
            } catch (HttpClientErrorException e) {
                responseEntity = new ResponseEntity<>(e.getResponseBodyAsString(), e.getStatusCode());
            } catch (HttpServerErrorException e) {
                responseEntity = new ResponseEntity<>(e.getResponseBodyAsString(), HttpStatus.INTERNAL_SERVER_ERROR);
            } catch (ResourceAccessException e) {
                responseEntity = new ResponseEntity<>("Network error connection", HttpStatus.GATEWAY_TIMEOUT);
            } finally {
                Bundle bundle = new Bundle();
                if (responseEntity != null) {
                    execute(responseEntity, bundle);
                }
                message.setTarget(handler);
                message.setData(bundle);
                handleMessage(message);
            }
        });
        return this;
    }

    private void handleMessage(Message message) {
        handler.post(() -> {
            HttpStatus httpStatus = HttpStatus.valueOf(message.getData().getInt("Status"));
            if (httpStatus.is2xxSuccessful()) {
                instance = responseType.cast(message.getData().getSerializable("Object"));
                if (onSuccess != null)
                    onSuccess.success(instance, (HttpHeaders) message.getData().getSerializable("Headers"), httpStatus);
            } else if (httpStatus.is4xxClientError() || httpStatus.is5xxServerError()) {
                if (onError != null)
                    onError.error(message.getData().getString("Message"), (HttpHeaders) message.getData().getSerializable("Headers"), httpStatus);
            }
            if (onPostExecute != null)
                handler.post(() -> onPostExecute.finish());
            executorService.shutdown();
        });
    }

    private void execute(ResponseEntity<?> responseEntity, Bundle bundle) {
        HttpStatus httpStatus = responseEntity.getStatusCode();
        bundle.putInt("Status", responseEntity.getStatusCode().value());
        if (httpStatus.is2xxSuccessful()) {
            bundle.putSerializable("Object", (Serializable) responseEntity.getBody());
            bundle.putSerializable("Headers", responseEntity.getHeaders());
        } else {
            bundle.putString("Message", (String) responseEntity.getBody());
        }
    }

    private void postExecute(HttpEntity<?> httpEntity) {
        ResponseEntity<?> responseEntity = null;
        try {
            responseEntity = restTemplate.exchange(queryUrl, HttpMethod.POST, httpEntity, responseType);
            if (onExecute != null)
                handler.post(() -> onExecute.execute());
        } catch (HttpClientErrorException e) {
            responseEntity = new ResponseEntity<>(e.getResponseBodyAsString(), e.getStatusCode());
        } catch (HttpServerErrorException e) {
            responseEntity = new ResponseEntity<>(e.getResponseBodyAsString(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (ResourceAccessException e) {
            responseEntity = new ResponseEntity<>("Network error connection", HttpStatus.GATEWAY_TIMEOUT);
        } finally {
            Bundle bundle = new Bundle();
            if (responseEntity != null) {
                execute(responseEntity, bundle);
            }
            message.setTarget(handler);
            message.setData(bundle);
            handleMessage(message);
        }
    }

    private void postExecute(HttpEntity<?> httpEntity, Object... params) {
        ResponseEntity<?> responseEntity = null;
        try {
            responseEntity = restTemplate.exchange(queryUrl, HttpMethod.POST, httpEntity, responseType, params);
            if (onExecute != null)
                handler.post(() -> onExecute.execute());
        } catch (HttpClientErrorException e) {
            responseEntity = new ResponseEntity<>(e.getResponseBodyAsString(), e.getStatusCode());
        } catch (HttpServerErrorException e) {
            responseEntity = new ResponseEntity<>(e.getResponseBodyAsString(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (ResourceAccessException e) {
            responseEntity = new ResponseEntity<>("Network error connection", HttpStatus.GATEWAY_TIMEOUT);
        } finally {
            Bundle bundle = new Bundle();
            if (responseEntity != null) {
                execute(responseEntity, bundle);
            }
            message.setTarget(handler);
            message.setData(bundle);
            handleMessage(message);
        }
    }

    private void putExecute(HttpEntity<?> httpEntity) {
        ResponseEntity<?> responseEntity = null;
        try {
            responseEntity = restTemplate.exchange(queryUrl, HttpMethod.PUT, httpEntity, responseType);
            if (onExecute != null)
                handler.post(() -> onExecute.execute());
        } catch (HttpClientErrorException e) {
            responseEntity = new ResponseEntity<>(e.getResponseBodyAsString(), e.getStatusCode());
        } catch (HttpServerErrorException e) {
            responseEntity = new ResponseEntity<>(e.getResponseBodyAsString(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (ResourceAccessException e) {
            responseEntity = new ResponseEntity<>("Network error connection", HttpStatus.GATEWAY_TIMEOUT);
        } finally {
            Bundle bundle = new Bundle();
            if (responseEntity != null) {
                execute(responseEntity, bundle);
            }
            message.setTarget(handler);
            message.setData(bundle);
            handleMessage(message);
        }
    }

    private void putExecute(HttpEntity<?> httpEntity, Object... params) {
        ResponseEntity<?> responseEntity = null;
        try {
            responseEntity = restTemplate.exchange(queryUrl, HttpMethod.PUT, httpEntity, responseType, params);
            if (onExecute != null)
                handler.post(() -> onExecute.execute());
        } catch (HttpClientErrorException e) {
            responseEntity = new ResponseEntity<>(e.getResponseBodyAsString(), e.getStatusCode());
        } catch (HttpServerErrorException e) {
            responseEntity = new ResponseEntity<>(e.getResponseBodyAsString(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (ResourceAccessException e) {
            responseEntity = new ResponseEntity<>("Network error connection", HttpStatus.GATEWAY_TIMEOUT);
        } finally {
            Bundle bundle = new Bundle();
            if (responseEntity != null) {
                execute(responseEntity, bundle);
            }
            message.setTarget(handler);
            message.setData(bundle);
            handleMessage(message);
        }
    }

    /**
     * Download file with some params
     */
    public void download(String filePath, Object... params) {
        executorService.execute(() -> {
            if (onPreExecute != null)
                handler.post(() -> onPreExecute.before());
            HttpEntity<?> httpEntity = new HttpEntity<>(httpHeaders);
            ResponseEntity<Resource> responseEntity;
            try {
                responseEntity = restTemplate.exchange(queryUrl, HttpMethod.GET, httpEntity, Resource.class, params);
                if (onExecute != null)
                    handler.post(() -> onExecute.execute());
                process(responseEntity, filePath);
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                if (onError != null)
                    handler.post(() -> onError.error(e.getMessage(), e.getResponseHeaders(), e.getStatusCode()));
            } catch (ResourceAccessException e) {
                if (onError != null)
                    handler.post(() -> onError.error("Network error connection", (HttpHeaders) message.getData().getSerializable("Headers"), HttpStatus.SERVICE_UNAVAILABLE));
            } finally {
                if (onPostExecute != null)
                    handler.post(() -> onPostExecute.finish());
                executorService.shutdown();
            }
        });
    }

    /**
     * Download file with some params
     */
    public void download(Object... params) {
        executorService.execute(() -> {
            if (onPreExecute != null)
                handler.post(() -> onPreExecute.before());
            HttpEntity<?> httpEntity = new HttpEntity<>(httpHeaders);
            ResponseEntity<Resource> responseEntity;
            try {
                responseEntity = restTemplate.exchange(queryUrl, HttpMethod.GET, httpEntity, Resource.class, params);
                if (onExecute != null)
                    handler.post(() -> onExecute.execute());
                process(responseEntity);
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                if (onError != null)
                    handler.post(() -> onError.error(e.getMessage(), e.getResponseHeaders(), e.getStatusCode()));
            } catch (ResourceAccessException e) {
                if (onError != null)
                    handler.post(() -> onError.error("Network error connection", (HttpHeaders) message.getData().getSerializable("Headers"), HttpStatus.SERVICE_UNAVAILABLE));
            } finally {
                if (onPostExecute != null)
                    handler.post(() -> onPostExecute.finish());
                executorService.shutdown();
            }
        });
    }

    private void process(ResponseEntity<Resource> responseEntity, String filePath) {
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
                        handler.post(() -> onProgress.progress(finalProgress));
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            outputStream.flush();
            outputStream.close();
            inputStream.close();
            if (total == size) {
                if (onDownload != null)
                    handler.post(() -> onDownload.done(file, mediaType.toString()));
            } else {
                if (onError != null)
                    onError.error("Error while saving file...", responseEntity.getHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void process(ResponseEntity<Resource> responseEntity) {
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
                if (onFinished != null)
                    handler.post(() -> onFinished.done(buffer, httpHeaders));
            } else {
                if (onError != null)
                    onError.error("Error while loading data...", responseEntity.getHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Create instance RestClient
     */
    public static <T extends Serializable> RestClientBuilder<T> build(Class<T> clazz) {
        return new RestClientBuilder<>(clazz);
    }
}
