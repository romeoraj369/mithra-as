package com.gamor.mithrax.ui.search;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gamor.mithrax.R;
import com.gamor.mithrax.domain.search.SearchHit;
import com.gamor.mithrax.domain.search.SearchResults;

import java.util.ArrayList;
import java.util.List;

public class SearchResultsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface Listener {
        void onHit(@NonNull SearchHit hit);
    }

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_HIT = 1;

    private final List<Row> rows = new ArrayList<>();
    private final Listener listener;

    public SearchResultsAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void submit(@NonNull SearchResults results) {
        rows.clear();
        if (!results.conversations.isEmpty()) {
            rows.add(Row.header("Conversations"));
            for (SearchHit hit : results.conversations) {
                rows.add(Row.hit(hit));
            }
        }
        if (!results.memories.isEmpty()) {
            rows.add(Row.header("Memories"));
            for (SearchHit hit : results.memories) {
                rows.add(Row.hit(hit));
            }
        }
        notifyDataSetChanged();
    }

    public void clear() {
        rows.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).hit == null ? TYPE_HEADER : TYPE_HIT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderHolder(inflater.inflate(R.layout.item_search_header, parent, false));
        }
        return new HitHolder(inflater.inflate(R.layout.item_search_hit, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);
        if (holder instanceof HeaderHolder) {
            ((HeaderHolder) holder).title.setText(row.header);
            return;
        }
        HitHolder hitHolder = (HitHolder) holder;
        SearchHit hit = row.hit;
        if (hit.kind == SearchHit.Kind.CONVERSATION) {
            hitHolder.kind.setText(R.string.search_kind_conversation);
        } else {
            hitHolder.kind.setText(R.string.search_kind_memory);
        }
        hitHolder.title.setText(hit.title);
        if (hit.snippet == null || hit.snippet.isEmpty()) {
            hitHolder.snippet.setVisibility(View.GONE);
        } else {
            hitHolder.snippet.setVisibility(View.VISIBLE);
            hitHolder.snippet.setText(hit.snippet);
        }
        hitHolder.meta.setText(metaLine(hit));
        hitHolder.itemView.setOnClickListener(v -> listener.onHit(hit));
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    @NonNull
    static String metaLine(@NonNull SearchHit hit) {
        if (hit.kind == SearchHit.Kind.CONVERSATION) {
            String status = value(hit, "status");
            String created = value(hit, "createdAt");
            if (!status.isEmpty() && !created.isEmpty()) {
                return status + " · " + created;
            }
            return created.isEmpty() ? status : created;
        }
        String type = value(hit, "type");
        String owner = value(hit, "owner");
        String deadline = value(hit, "deadline");
        StringBuilder line = new StringBuilder(type);
        if (!owner.isEmpty()) {
            line.append(" · ").append(owner);
        }
        if (!deadline.isEmpty()) {
            line.append(" · ").append(deadline);
        }
        return line.toString();
    }

    @NonNull
    private static String value(@NonNull SearchHit hit, @NonNull String key) {
        String value = hit.metadata.get(key);
        return value == null ? "" : value;
    }

    static final class Row {
        final String header;
        final SearchHit hit;

        private Row(String header, SearchHit hit) {
            this.header = header;
            this.hit = hit;
        }

        static Row header(String title) {
            return new Row(title, null);
        }

        static Row hit(SearchHit hit) {
            return new Row(null, hit);
        }
    }

    static final class HeaderHolder extends RecyclerView.ViewHolder {
        final TextView title;

        HeaderHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.searchSectionTitle);
        }
    }

    static final class HitHolder extends RecyclerView.ViewHolder {
        final TextView kind;
        final TextView title;
        final TextView snippet;
        final TextView meta;

        HitHolder(@NonNull View itemView) {
            super(itemView);
            kind = itemView.findViewById(R.id.searchHitKind);
            title = itemView.findViewById(R.id.searchHitTitle);
            snippet = itemView.findViewById(R.id.searchHitSnippet);
            meta = itemView.findViewById(R.id.searchHitMeta);
        }
    }
}
