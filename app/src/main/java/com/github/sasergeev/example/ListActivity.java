package com.github.sasergeev.example;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import com.github.sasergeev.example.adapter.BaseGenericAdapter;
import com.github.sasergeev.example.databinding.ListActivityBinding;
import com.github.sasergeev.example.databinding.ListItemLayoutBinding;
import com.github.sasergeev.example.pojo.DummyData;
import com.github.sasergeev.example.pojo.DummyModel;
import com.github.sasergeev.restclient.RestClientBuilder;
import java.util.Collections;

public class ListActivity extends AppCompatActivity {
    private BaseGenericAdapter<DummyModel, ListItemLayoutBinding> baseGenericAdapter;
    private int i = 0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ListActivityBinding listActivityBinding = DataBindingUtil.setContentView(this, R.layout.list_activity);
        baseGenericAdapter = new BaseGenericAdapter<>(R.layout.list_item_layout, (object, binding) -> binding.setModel(object)); // configuring my custom adapter
        listActivityBinding.recycler.setAdapter(baseGenericAdapter);
        paginationData(i, listActivityBinding.progress, listActivityBinding.empty);
    }

    // Sample code for paging with recursion!
    private void paginationData(int page, ProgressBar progress, TextView empty) {
        RestClientBuilder.build(DummyData.class)
                .url("https://dummyapi.io/data/v1/") // it's API for testing pagination
                .uri("user?page={page}}&limit={limit}") // number of page and limit of items
                .headers(Collections.singletonMap("app-id", "63fdd7aae0280133037f1a93")) // add additional header for calling API
                .get(page, 10)
                .before(() -> progress.setVisibility(View.VISIBLE))
                .finish(() -> progress.setVisibility(View.INVISIBLE))
                .success((object, headers, status) -> {
                    if (object.getData().size() == 0 && i == 0)
                        empty.setVisibility(View.VISIBLE);
                    if (!object.getData().isEmpty())
                        baseGenericAdapter.updateIfEndList(object.getData(), () -> paginationData(++i, progress, empty));
                })
                .error((error, headers, status) -> Toast.makeText(this, error, Toast.LENGTH_LONG).show());
    }
}
