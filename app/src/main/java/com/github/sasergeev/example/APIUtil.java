package com.github.sasergeev.example;

import android.app.Activity;
import android.util.Log;

import com.github.sasergeev.example.pojo.Contracts;
import com.github.sasergeev.restclient.RestClient;

import org.springframework.http.HttpStatus;

import java.util.function.BiConsumer;

public class APIUtil {

    public static void fetchContracts(Activity activity, int page, BiConsumer<Contracts, Integer> consumer) {
        RestClient.build(Contracts.class)
                .url("https://app.ezaimy.com:27073/")
                .uri("api/v2/get/contracts" + "?page={page}&size={size}")
                .auth("eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJlLnZvbGtvdmEiLCJqdGkiOiLQktC-0LvQutC-0LLQsCDQldC70LXQvdCwINCQ0LvQtdC60YHQsNC90LTRgNC-0LLQvdCwIiwiYXVkIjoiZG9tYWludXNlciIsImF1dGhvcml0aWVzIjpbeyJhdXRob3JpdHkiOiJtb2JpbGVhY2Nlc3MifSx7ImF1dGhvcml0eSI6Im1vYmlsZWFnZW50In1dLCJleHAiOjE5MzgzNDcyOTZ9.AJR5OcfXa2XC7K1wW6eAxZsgvG_bjf1rOvk403pn14L1g7K3tvlxhDS6pT_vrp4oFV5DOXDU79cShbVTG6RU7w")
                .ssl(activity.getResources().openRawResource(R.raw.public_ssl))
                .get(page, 50)
                .success((object, headers, status) -> consumer.accept(object, page))
                .error((error, headers, status) -> {
                    if (status == HttpStatus.UNAUTHORIZED) {
                        Log.d("UNAUTHORIZED", error);
                    } else {
                        Log.d("OTHER", error);
                    }
                });
    }

}
