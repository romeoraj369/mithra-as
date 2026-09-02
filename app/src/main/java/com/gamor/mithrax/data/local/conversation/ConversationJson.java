package com.gamor.mithrax.data.local.conversation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gamor.mithrax.domain.conversation.ActionItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Small JSON codec for conversation list/map columns. Avoids a third-party parser.
 */
final class ConversationJson {

    private ConversationJson() {
    }

    @NonNull
    static String stringList(@Nullable List<String> values) {
        List<String> items = values == null ? Collections.emptyList() : values;
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append(quoted(items.get(i) == null ? "" : items.get(i)));
        }
        return json.append(']').toString();
    }

    @NonNull
    static List<String> parseStringList(@Nullable String json) {
        String body = arrayBody(json);
        if (body == null) {
            return new ArrayList<>();
        }
        List<String> values = new ArrayList<>();
        int i = 0;
        while (i < body.length()) {
            i = skipWs(body, i);
            if (i >= body.length()) {
                break;
            }
            if (body.charAt(i) != '"') {
                throw new IllegalArgumentException("Expected string in JSON array");
            }
            ParseResult parsed = readQuoted(body, i);
            values.add(parsed.value);
            i = skipWs(body, parsed.nextIndex);
            if (i < body.length() && body.charAt(i) == ',') {
                i++;
            }
        }
        return values;
    }

    @NonNull
    static String actionItems(@Nullable List<ActionItem> items) {
        List<ActionItem> values = items == null ? Collections.emptyList() : items;
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            ActionItem item = values.get(i);
            json.append("{\"text\":")
                    .append(quoted(item == null ? "" : item.text))
                    .append(",\"completed\":")
                    .append(item != null && item.completed)
                    .append('}');
        }
        return json.append(']').toString();
    }

    @NonNull
    static List<ActionItem> parseActionItems(@Nullable String json) {
        String body = arrayBody(json);
        if (body == null) {
            return new ArrayList<>();
        }
        List<ActionItem> items = new ArrayList<>();
        int i = 0;
        while (i < body.length()) {
            i = skipWs(body, i);
            if (i >= body.length()) {
                break;
            }
            if (body.charAt(i) != '{') {
                throw new IllegalArgumentException("Expected object in action item array");
            }
            int end = matchingBrace(body, i);
            items.add(parseActionItem(body.substring(i, end + 1)));
            i = skipWs(body, end + 1);
            if (i < body.length() && body.charAt(i) == ',') {
                i++;
            }
        }
        return items;
    }

    @NonNull
    static String stringMap(@Nullable Map<String, String> values) {
        Map<String, String> map = values == null ? Collections.emptyMap() : values;
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (!first) {
                json.append(',');
            }
            first = false;
            json.append(quoted(entry.getKey() == null ? "" : entry.getKey()))
                    .append(':')
                    .append(quoted(entry.getValue() == null ? "" : entry.getValue()));
        }
        return json.append('}').toString();
    }

    @NonNull
    static Map<String, String> parseStringMap(@Nullable String json) {
        if (json == null) {
            return new LinkedHashMap<>();
        }
        String trimmed = json.trim();
        if (trimmed.isEmpty() || "null".equals(trimmed)) {
            return new LinkedHashMap<>();
        }
        if (trimmed.charAt(0) != '{' || trimmed.charAt(trimmed.length() - 1) != '}') {
            throw new IllegalArgumentException("Expected JSON object");
        }
        String body = trimmed.substring(1, trimmed.length() - 1).trim();
        Map<String, String> map = new LinkedHashMap<>();
        if (body.isEmpty()) {
            return map;
        }
        int i = 0;
        while (i < body.length()) {
            i = skipWs(body, i);
            if (i >= body.length()) {
                break;
            }
            if (body.charAt(i) != '"') {
                throw new IllegalArgumentException("Expected metadata key");
            }
            ParseResult key = readQuoted(body, i);
            i = skipWs(body, key.nextIndex);
            if (i >= body.length() || body.charAt(i) != ':') {
                throw new IllegalArgumentException("Expected ':' in metadata");
            }
            i = skipWs(body, i + 1);
            if (i >= body.length() || body.charAt(i) != '"') {
                throw new IllegalArgumentException("Expected metadata value");
            }
            ParseResult value = readQuoted(body, i);
            map.put(key.value, value.value);
            i = skipWs(body, value.nextIndex);
            if (i < body.length() && body.charAt(i) == ',') {
                i++;
            }
        }
        return map;
    }

    @Nullable
    private static String arrayBody(@Nullable String json) {
        if (json == null) {
            return "";
        }
        String trimmed = json.trim();
        if (trimmed.isEmpty() || "null".equals(trimmed)) {
            return "";
        }
        if (trimmed.charAt(0) != '[' || trimmed.charAt(trimmed.length() - 1) != ']') {
            throw new IllegalArgumentException("Expected JSON array");
        }
        return trimmed.substring(1, trimmed.length() - 1).trim();
    }

    @NonNull
    private static ActionItem parseActionItem(@NonNull String json) {
        String text = "";
        boolean completed = false;
        String body = json.substring(1, json.length() - 1).trim();
        int i = 0;
        while (i < body.length()) {
            i = skipWs(body, i);
            if (i >= body.length()) {
                break;
            }
            ParseResult key = readQuoted(body, i);
            i = skipWs(body, key.nextIndex);
            if (i >= body.length() || body.charAt(i) != ':') {
                throw new IllegalArgumentException("Expected ':' in action item");
            }
            i = skipWs(body, i + 1);
            if ("text".equals(key.value)) {
                ParseResult value = readQuoted(body, i);
                text = value.value;
                i = value.nextIndex;
            } else if ("completed".equals(key.value)) {
                if (body.startsWith("true", i)) {
                    completed = true;
                    i += 4;
                } else if (body.startsWith("false", i)) {
                    completed = false;
                    i += 5;
                } else {
                    throw new IllegalArgumentException("Expected boolean completed");
                }
            } else {
                i = skipValue(body, i);
            }
            i = skipWs(body, i);
            if (i < body.length() && body.charAt(i) == ',') {
                i++;
            }
        }
        return new ActionItem(text, completed);
    }

    private static int skipValue(@NonNull String json, int start) {
        int i = skipWs(json, start);
        if (i < json.length() && json.charAt(i) == '"') {
            return readQuoted(json, i).nextIndex;
        }
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == ',' || c == '}' || c == ']') {
                break;
            }
            i++;
        }
        return i;
    }

    private static int matchingBrace(@NonNull String json, int start) {
        int depth = 0;
        boolean inString = false;
        boolean escape = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (inString) {
                if (escape) {
                    escape = false;
                } else if (c == '\\') {
                    escape = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
            } else if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        throw new IllegalArgumentException("Unclosed JSON object");
    }

    @NonNull
    private static String quoted(@NonNull String value) {
        StringBuilder json = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\':
                    json.append("\\\\");
                    break;
                case '"':
                    json.append("\\\"");
                    break;
                case '\n':
                    json.append("\\n");
                    break;
                case '\r':
                    json.append("\\r");
                    break;
                case '\t':
                    json.append("\\t");
                    break;
                default:
                    json.append(c);
            }
        }
        return json.append('"').toString();
    }

    @NonNull
    private static ParseResult readQuoted(@NonNull String json, int start) {
        if (start >= json.length() || json.charAt(start) != '"') {
            throw new IllegalArgumentException("Expected quoted string");
        }
        StringBuilder value = new StringBuilder();
        boolean escape = false;
        for (int i = start + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escape) {
                switch (c) {
                    case 'n':
                        value.append('\n');
                        break;
                    case 'r':
                        value.append('\r');
                        break;
                    case 't':
                        value.append('\t');
                        break;
                    case '"':
                    case '\\':
                    case '/':
                        value.append(c);
                        break;
                    default:
                        value.append(c);
                }
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else if (c == '"') {
                return new ParseResult(value.toString(), i + 1);
            } else {
                value.append(c);
            }
        }
        throw new IllegalArgumentException("Unclosed JSON string");
    }

    private static int skipWs(@NonNull String json, int index) {
        int i = index;
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
            i++;
        }
        return i;
    }

    private static final class ParseResult {
        final String value;
        final int nextIndex;

        ParseResult(String value, int nextIndex) {
            this.value = value;
            this.nextIndex = nextIndex;
        }
    }
}
