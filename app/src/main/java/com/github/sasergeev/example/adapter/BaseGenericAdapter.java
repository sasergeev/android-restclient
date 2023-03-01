package com.github.sasergeev.example.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.recyclerview.widget.RecyclerView;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * My Custom Generic Adapter for RecyclerView with utils methods for click by item, click by view and detect if end of list
 */
public class BaseGenericAdapter <T extends Serializable, V extends ViewDataBinding> extends RecyclerView.Adapter<BaseGenericAdapter<T,V>.BindingViewHolder> {
    private final List<T> list;
    private final int layoutId;
    private Scrollable scrollable;
    private Consumer<V> consumerView;
    private Consumer<V> consumerItem;
    private final BiConsumer<T,V> biConsumer;

    public BaseGenericAdapter(int layoutId, BiConsumer<T,V> biConsumer) {
        this.layoutId = layoutId;
        this.biConsumer = biConsumer;
        this.list = new ArrayList<>();
    }

    public BaseGenericAdapter(List<T> list, int layoutId, BiConsumer<T,V> biConsumer) {
        this.list = list;
        this.layoutId = layoutId;
        this.biConsumer = biConsumer;
    }

    @Override
    public BindingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        V binding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), layoutId, parent, false);
        return new BindingViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(BindingViewHolder holder, int position) {
        holder.binding(list.get(position));
        if (position == getItemCount() - 1 && scrollable != null) {
            scrollable.next();
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public void onClickItem(Consumer<V> consumer) {
        this.consumerItem = consumer;
    }

    public void onClickView(Consumer<V> consumer) {
        this.consumerView = consumer;
    }

    public void updateIfEndList(List<T> list, Scrollable scrollable) {
        this.list.addAll(list);
        this.scrollable = scrollable;
        notifyDataSetChanged();
    }

    public interface Scrollable {
        void next();
    }

    public interface Consumer<V> {
        void accept(V binding);
    }

    public interface BiConsumer<T,V> {
        void accept(T object, V binding);
    }

    public class BindingViewHolder extends RecyclerView.ViewHolder {
        final V binding;

        public BindingViewHolder(V binding) {
            super(binding.getRoot());
            this.binding = binding;
            if (consumerView != null)
                consumerView.accept(binding);
            if (consumerItem != null)
                consumerItem.accept(binding);
        }

        public void binding(T item) {
            biConsumer.accept(item, binding);
            binding.executePendingBindings();
        }
    }
}
