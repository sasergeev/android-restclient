# android-rest-client
Simple async REST client in using declarative style with. Essentially as wrapper of RestTemplate by Spring framework for Android.

## Add the JitPack repository to your build file
```
allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```

## Add the dependency
```
dependencies {
    implementation 'com.github.sasergeev:android-restclient:latest.release'
    
    implementation 'org.springframework.android:spring-android-rest-template:2.0.0.M3'
    implementation 'com.squareup.okhttp:okhttp:2.7.5'
    implementation 'com.fasterxml.jackson.core:jackson-core:2.9.8'
    implementation 'com.fasterxml.jackson.core:jackson-databind:2.9.8'
    implementation 'com.fasterxml.jackson.core:jackson-annotations:2.9.8'
}
```

## Features
- GET, POST, PUT, DELETE-requests with params
- Download file method

## Usage
```java
private void fetchData(String id) {
        RestClientBuilder.build(YourPojo.class)
                .url("https://your-backend/api/v1/" + id) // base url
                .get() // GET-request or post(), put(), delete()
                //.url("https://your-base-backend-url/") |
                //.uri("api/v1/{id}")                    | instead of above
                //.get(id)
                .auth("your_jwt_toke") // for auth calling
                .ssl(getResources().openRawResource(R.raw.your_public_key)) // for https calling|
                .success((object, headers, status) -> {
                  // handle response here: pojo object, headers and status code 2xx
                })
                .error((error, headers, status) -> {
                  // handle response here: error message, headers and status code 4xx or 5xx
                });
    }
```
