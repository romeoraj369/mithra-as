package com.gamor.mithrax.ui.meetings;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import androidx.core.content.ContextCompat;

import com.gamor.mithrax.R;
import com.gamor.mithrax.data.local.conversation.ConversationEntity;

import java.text.DateFormat;
import java.util.Date;

public class RecordingListAdapter extends ListAdapter<ConversationEntity, RecordingListAdapter.Holder> {

    public interface Listener {
        void onOpen(@NonNull ConversationEntity recording);

        void onDelete(@NonNull ConversationEntity recording);
    }

    private final Listener listener;
    private final DateFormat dateFormat =
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);

    public RecordingListAdapter(@NonNull Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recording, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        ConversationEntity recording = getItem(position);
        holder.title.setText(recording.title);
        holder.date.setText(dateFormat.format(new Date(recording.createdAtMillis)));
        holder.duration.setText(RecordingPresentation.formatDuration(recording.durationMillis));
        holder.status.setText(RecordingPresentation.statusLabelRes(recording.processingStatus));
        holder.status.setTextColor(ContextCompat.getColor(
                holder.itemView.getContext(),
                RecordingPresentation.statusColorRes(recording.processingStatus)));
        if (recording.transcript == null || recording.transcript.trim().isEmpty()) {
            holder.transcript.setVisibility(View.GONE);
        } else {
            holder.transcript.setVisibility(View.VISIBLE);
            holder.transcript.setText(recording.transcript);
        }
        holder.itemView.setOnClickListener(v -> listener.onOpen(recording));
        holder.deleteButton.setOnClickListener(v -> listener.onDelete(recording));
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView date;
        final TextView duration;
        final TextView status;
        final TextView transcript;
        final ImageButton deleteButton;

        Holder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.recordingTitle);
            date = itemView.findViewById(R.id.recordingDate);
            duration = itemView.findViewById(R.id.recordingDuration);
            status = itemView.findViewById(R.id.recordingStatus);
            transcript = itemView.findViewById(R.id.recordingTranscript);
            deleteButton = itemView.findViewById(R.id.recordingDelete);
        }
    }

    private static final DiffUtil.ItemCallback<ConversationEntity> DIFF =
            new DiffUtil.ItemCallback<ConversationEntity>() {
                @Override
                public boolean areItemsTheSame(@NonNull ConversationEntity oldItem,
                                               @NonNull ConversationEntity newItem) {
                    return oldItem.id.equals(newItem.id);
                }

                @Override
                public boolean areContentsTheSame(@NonNull ConversationEntity oldItem,
                                                  @NonNull ConversationEntity newItem) {
                    return oldItem.title.equals(newItem.title)
                            && oldItem.createdAtMillis == newItem.createdAtMillis
                            && oldItem.updatedAtMillis == newItem.updatedAtMillis
                            && oldItem.durationMillis == newItem.durationMillis
                            && oldItem.audioPath.equals(newItem.audioPath)
                            && oldItem.processingStatus.equals(newItem.processingStatus)
                            && oldItem.transcript.equals(newItem.transcript)
                            && oldItem.summary.equals(newItem.summary);
                }
            };
}
