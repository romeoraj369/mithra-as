package com.gamor.mithrax.device.llama;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gamor.mithrax.domain.conversation.ActionItem;
import com.gamor.mithrax.domain.memory.ExtractedMemory;
import com.gamor.mithrax.domain.memory.MemoryType;
import com.gamor.mithrax.domain.understanding.ConversationInsights;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Qwen JSON responses into domain objects with safe fallbacks.
 */
public final class QwenResponseParser {

  private static final String TAG = "QwenResponseParser";
  private static final Pattern JSON_BLOCK = Pattern.compile("\\{[\\s\\S]*\\}");
  private static final String NOT_FOUND_PREFIX = "NOT_FOUND:";

  private QwenResponseParser() {
  }

  @NonNull
  public static ConversationInsights parseInsights(@NonNull String raw) throws ParseException {
    JSONObject json = extractJsonObject(raw);
    String summary = optString(json, "summary");
    List<String> keyPoints = parseStringArray(json.optJSONArray("keyPoints"));
    List<ActionItem> actionItems = parseActionItems(json.optJSONArray("actionItems"));
    List<String> importantFacts = parseStringArray(json.optJSONArray("importantFacts"));
    if (summary.isEmpty() && keyPoints.isEmpty() && actionItems.isEmpty() && importantFacts.isEmpty()) {
      throw new ParseException("Structured insights were empty");
    }
    return new ConversationInsights(summary, keyPoints, actionItems, importantFacts);
  }

  @NonNull
  public static String parseAnswer(@NonNull String raw) throws ParseException {
    String trimmed = stripThinking(raw).trim();
    if (trimmed.isEmpty()) {
      throw new ParseException("Empty answer");
    }
    if (trimmed.startsWith(NOT_FOUND_PREFIX) || trimmed.toLowerCase(Locale.US).contains("not found")) {
      return trimmed.startsWith(NOT_FOUND_PREFIX)
              ? trimmed.substring(NOT_FOUND_PREFIX.length()).trim()
              : trimmed;
    }
    return trimmed;
  }

  public static boolean isNotFoundAnswer(@NonNull String answer) {
    String lower = answer.toLowerCase(Locale.US);
    return lower.contains("not found")
            || lower.contains("could not find")
            || lower.contains("no information");
  }

  @NonNull
  public static List<ExtractedMemory> parseMemories(@NonNull String raw) throws ParseException {
    JSONObject json = extractJsonObject(raw);
    JSONArray memories = json.optJSONArray("memories");
    if (memories == null) {
      throw new ParseException("Missing memories array");
    }
    List<ExtractedMemory> parsed = new ArrayList<>();
    for (int i = 0; i < memories.length(); i++) {
      JSONObject item = memories.optJSONObject(i);
      if (item == null) {
        continue;
      }
      String typeRaw = optString(item, "type");
      MemoryType type = parseMemoryType(typeRaw);
      String content = optString(item, "content");
      if (content.isEmpty()) {
        continue;
      }
      float confidence = (float) item.optDouble("confidence", 0.75d);
      Map<String, String> metadata = parseMetadata(item.optJSONObject("metadata"));
      metadata.put("origin", "qwen3");
      parsed.add(new ExtractedMemory(type, content, confidence, metadata));
    }
    if (parsed.isEmpty()) {
      throw new ParseException("No memories parsed");
    }
    return parsed;
  }

  @NonNull
  private static JSONObject extractJsonObject(@NonNull String raw) throws ParseException {
    String candidate = stripThinking(raw).trim();
    try {
      return new JSONObject(candidate);
    } catch (JSONException ignored) {
      Matcher matcher = JSON_BLOCK.matcher(candidate);
      if (matcher.find()) {
        try {
          return new JSONObject(matcher.group());
        } catch (JSONException e) {
          throw new ParseException("Malformed JSON response");
        }
      }
      throw new ParseException("Response did not contain JSON");
    }
  }

  private static final String THINK_CLOSE = "</" + "think>";
  private static final int THINK_CLOSE_LEN = THINK_CLOSE.length();

  @NonNull
  private static String stripThinking(@NonNull String raw) {
    String trimmed = raw.trim();
    int end = trimmed.indexOf(THINK_CLOSE);
    if (end >= 0) {
      return trimmed.substring(end + THINK_CLOSE_LEN).trim();
    }
    return trimmed;
  }

  @NonNull
  private static List<String> parseStringArray(@Nullable JSONArray array) {
    List<String> values = new ArrayList<>();
    if (array == null) {
      return values;
    }
    for (int i = 0; i < array.length(); i++) {
      String value = array.optString(i, "").trim();
      if (!value.isEmpty()) {
        values.add(value);
      }
    }
    return values;
  }

  @NonNull
  private static List<ActionItem> parseActionItems(@Nullable JSONArray array) {
    List<ActionItem> items = new ArrayList<>();
    if (array == null) {
      return items;
    }
    for (int i = 0; i < array.length(); i++) {
      Object entry = array.opt(i);
      if (entry instanceof JSONObject) {
        JSONObject object = (JSONObject) entry;
        String text = optString(object, "text");
        if (!text.isEmpty()) {
          items.add(new ActionItem(text, object.optBoolean("completed", false)));
        }
      } else {
        String text = array.optString(i, "").trim();
        if (!text.isEmpty()) {
          items.add(new ActionItem(text, false));
        }
      }
    }
    return items;
  }

  @NonNull
  private static Map<String, String> parseMetadata(@Nullable JSONObject metadata) {
    Map<String, String> values = new LinkedHashMap<>();
    if (metadata == null) {
      return values;
    }
    Iterator<String> keys = metadata.keys();
    while (keys.hasNext()) {
      String key = keys.next();
      String value = metadata.optString(key, "").trim();
      if (!value.isEmpty()) {
        values.put(key, value);
      }
    }
    return values;
  }

  @NonNull
  private static MemoryType parseMemoryType(@NonNull String raw) {
    try {
      return MemoryType.valueOf(raw.trim().toUpperCase(Locale.US));
    } catch (IllegalArgumentException e) {
      Log.w(TAG, "Unknown memory type: " + raw);
      return MemoryType.CONTEXT;
    }
  }

  @NonNull
  private static String optString(@NonNull JSONObject json, @NonNull String key) {
    return json.optString(key, "").trim();
  }

  public static final class ParseException extends Exception {
    public ParseException(@NonNull String message) {
      super(message);
    }
  }
}
