package com.gamor.mithrax.ui.tasks;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.gamor.mithrax.R;

public class TasksFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_simple_content, container, false);
        TextView title = view.findViewById(R.id.simpleTitle);
        TextView body = view.findViewById(R.id.simpleBody);
        title.setText(R.string.tasks_title);
        body.setText(R.string.tasks_placeholder_body);
        return view;
    }
}
