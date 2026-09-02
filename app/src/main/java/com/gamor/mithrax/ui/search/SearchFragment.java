package com.gamor.mithrax.ui.search;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gamor.mithrax.MithraXApplication;
import com.gamor.mithrax.R;
import com.gamor.mithrax.data.local.conversation.ConversationEntity;
import com.gamor.mithrax.data.local.search.SearchRepository;
import com.gamor.mithrax.domain.search.SearchHit;
import com.gamor.mithrax.domain.search.SearchResults;
import com.gamor.mithrax.ui.meetings.RecordingPlaybackDialog;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SearchFragment extends Fragment {

    private static final long DEBOUNCE_MS = 350L;

    private final Handler debounce = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Runnable runSearch = this::submitCurrentQuery;

    private TextInputEditText input;
    private RecyclerView list;
    private View idleState;
    private View loadingState;
    private View emptyState;
    private View errorState;
    private TextView emptyBody;
    private TextView errorText;
    private SearchResultsAdapter adapter;
    private String lastQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        input = view.findViewById(R.id.searchInput);
        list = view.findViewById(R.id.searchResults);
        idleState = view.findViewById(R.id.searchIdle);
        loadingState = view.findViewById(R.id.searchLoading);
        emptyState = view.findViewById(R.id.searchEmpty);
        errorState = view.findViewById(R.id.searchError);
        emptyBody = view.findViewById(R.id.searchEmptyBody);
        errorText = view.findViewById(R.id.searchErrorText);
        Button retry = view.findViewById(R.id.searchRetry);

        adapter = new SearchResultsAdapter(this::onHit);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);

        retry.setOnClickListener(v -> submitCurrentQuery());
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                debounce.removeCallbacks(runSearch);
                submitCurrentQuery();
                return true;
            }
            return false;
        });
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                debounce.removeCallbacks(runSearch);
                debounce.postDelayed(runSearch, DEBOUNCE_MS);
            }
        });
        showIdle();
    }

    @Override
    public void onDestroyView() {
        debounce.removeCallbacks(runSearch);
        input = null;
        super.onDestroyView();
    }

    private MithraXApplication app() {
        return (MithraXApplication) requireContext().getApplicationContext();
    }

    private void submitCurrentQuery() {
        if (input == null) {
            return;
        }
        String query = input.getText() == null ? "" : input.getText().toString().trim();
        lastQuery = query;
        if (query.isEmpty()) {
            adapter.clear();
            showIdle();
            return;
        }
        showLoading();
        app().getSearchRepository().search(query, new SearchRepository.Callback() {
            @Override
            public void onResults(@NonNull SearchResults results) {
                if (!isAdded() || !query.equals(lastQuery)) {
                    return;
                }
                if (results.isEmpty()) {
                    adapter.clear();
                    showEmpty(query);
                } else {
                    adapter.submit(results);
                    showResults();
                }
            }

            @Override
            public void onError(@NonNull String message) {
                if (!isAdded() || !query.equals(lastQuery)) {
                    return;
                }
                showError(message);
            }
        });
    }

    private void onHit(@NonNull SearchHit hit) {
        if (hit.kind == SearchHit.Kind.CONVERSATION) {
            openConversation(hit.id);
            return;
        }
        String sourceId = hit.sourceConversationId;
        StringBuilder body = new StringBuilder(hit.snippet);
        String type = hit.metadata.get("type");
        String owner = hit.metadata.get("owner");
        String deadline = hit.metadata.get("deadline");
        if (type != null && !type.isEmpty()) {
            body.append("\n\n").append(getString(R.string.search_memory_type, type));
        }
        if (owner != null && !owner.isEmpty()) {
            body.append('\n').append(getString(R.string.search_memory_owner, owner));
        }
        if (deadline != null && !deadline.isEmpty()) {
            body.append('\n').append(getString(R.string.search_memory_deadline, deadline));
        }
        AlertDialog.Builder dialog = new AlertDialog.Builder(requireContext())
                .setTitle(hit.title)
                .setMessage(body.toString())
                .setNegativeButton(android.R.string.ok, null);
        if (sourceId != null && !sourceId.isEmpty()) {
            dialog.setPositiveButton(R.string.search_open_source, (d, w) -> openConversation(sourceId));
        }
        dialog.show();
    }

    private void openConversation(@NonNull String conversationId) {
        io.execute(() -> {
            ConversationEntity conversation = app().getConversationRepository().getByIdSync(conversationId);
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) {
                    return;
                }
                if (conversation == null) {
                    new AlertDialog.Builder(requireContext())
                            .setMessage(R.string.search_source_missing)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                    return;
                }
                File file = new File(conversation.audioPath);
                if (!file.exists()) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle(conversation.title)
                            .setMessage(conversation.transcript == null || conversation.transcript.isEmpty()
                                    ? getString(R.string.recording_file_missing)
                                    : conversation.transcript)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                    return;
                }
                RecordingPlaybackDialog.newInstance(
                                conversation.title,
                                conversation.audioPath,
                                conversation.transcript == null ? "" : conversation.transcript)
                        .show(getParentFragmentManager(), "playback");
            });
        });
    }

    private void showIdle() {
        idleState.setVisibility(View.VISIBLE);
        loadingState.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);
        errorState.setVisibility(View.GONE);
        list.setVisibility(View.GONE);
    }

    private void showLoading() {
        idleState.setVisibility(View.GONE);
        loadingState.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
        errorState.setVisibility(View.GONE);
        list.setVisibility(View.GONE);
    }

    private void showResults() {
        idleState.setVisibility(View.GONE);
        loadingState.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);
        errorState.setVisibility(View.GONE);
        list.setVisibility(View.VISIBLE);
    }

    private void showEmpty(@NonNull String query) {
        idleState.setVisibility(View.GONE);
        loadingState.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
        errorState.setVisibility(View.GONE);
        list.setVisibility(View.GONE);
        emptyBody.setText(getString(R.string.search_empty_body, query));
    }

    private void showError(@NonNull String message) {
        idleState.setVisibility(View.GONE);
        loadingState.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);
        errorState.setVisibility(View.VISIBLE);
        list.setVisibility(View.GONE);
        errorText.setText(getString(R.string.search_error_message, message));
    }
}
