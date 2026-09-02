package com.gamor.mithrax.data.local.conversation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.TypeConverter;

import com.gamor.mithrax.domain.conversation.ActionItem;

import java.util.List;
import java.util.Map;

public final class ConversationConverters {

    @TypeConverter
    @NonNull
    public String fromStringList(@Nullable List<String> values) {
        return ConversationJson.stringList(values);
    }

    @TypeConverter
    @NonNull
    public List<String> toStringList(@Nullable String json) {
        return ConversationJson.parseStringList(json);
    }

    @TypeConverter
    @NonNull
    public String fromActionItems(@Nullable List<ActionItem> items) {
        return ConversationJson.actionItems(items);
    }

    @TypeConverter
    @NonNull
    public List<ActionItem> toActionItems(@Nullable String json) {
        return ConversationJson.parseActionItems(json);
    }

    @TypeConverter
    @NonNull
    public String fromStringMap(@Nullable Map<String, String> values) {
        return ConversationJson.stringMap(values);
    }

    @TypeConverter
    @NonNull
    public Map<String, String> toStringMap(@Nullable String json) {
        return ConversationJson.parseStringMap(json);
    }
}
