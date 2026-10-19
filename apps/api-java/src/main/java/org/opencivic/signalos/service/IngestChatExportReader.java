package org.opencivic.signalos.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reads a WhatsApp or Telegram chat export into raw text records.
 *
 * Two shapes are accepted because both are what these apps actually export:
 * a JSON array of message objects, and the CSV text form WhatsApp produces.
 * Sender and timestamp are carried through so the resulting signal can cite where it came
 * from; only the message text becomes the civic report body.
 */
public final class IngestChatExportReader {

    /** A chat message reduced to the fields the civic model cares about. */
    public record Message(int lineNumber, String sender, String timestamp, String text) {}

    private static final List<String> TEXT_KEYS = List.of("text", "message", "body", "content");
    private static final List<String> SENDER_KEYS = List.of("sender", "from", "author", "authorName", "name");
    private static final List<String> TIMESTAMP_KEYS = List.of("timestamp", "date", "datetime", "time");

    private IngestChatExportReader() {}

    public static List<Message> readJson(String content, ObjectMapper mapper) {
        List<Message> messages = new ArrayList<>();
        JsonNode root;
        try {
            root = mapper.readTree(content);
        } catch (Exception e) {
            throw new IllegalArgumentException("Chat export is not valid JSON: " + e.getMessage());
        }

        JsonNode array = root;
        if (root.isObject() && root.has("messages")) {
            array = root.get("messages");
        }
        if (array == null || !array.isArray()) {
            throw new IllegalArgumentException("Chat export must be a JSON array of messages, or an object with a 'messages' array.");
        }

        int index = 0;
        for (JsonNode node : array) {
            index++;
            String text = firstString(node, TEXT_KEYS);
            String sender = firstString(node, SENDER_KEYS);
            String timestamp = firstString(node, TIMESTAMP_KEYS);
            messages.add(new Message(index, sender, timestamp, text));
        }
        return messages;
    }

    public static List<Message> readCsvText(String content) {
        IngestCsvReader.Parsed parsed = IngestCsvReader.parse(content);
        List<String> header = parsed.header();
        int textIndex = header.indexOf("message");
        if (textIndex < 0) {
            textIndex = header.indexOf("text");
        }
        if (textIndex < 0) {
            throw new IllegalArgumentException("Chat export CSV must contain a 'message' or 'text' column.");
        }
        int senderIndex = indexOfAny(header, SENDER_KEYS);
        int timestampIndex = indexOfAny(header, TIMESTAMP_KEYS);

        List<Message> messages = new ArrayList<>();
        for (IngestCsvReader.Row row : parsed.rows()) {
            String text = valueAt(row.values(), textIndex);
            String sender = senderIndex < 0 ? null : valueAt(row.values(), senderIndex);
            String timestamp = timestampIndex < 0 ? null : valueAt(row.values(), timestampIndex);
            messages.add(new Message(row.lineNumber(), sender, timestamp, text));
        }
        return messages;
    }

    private static int indexOfAny(List<String> header, List<String> candidates) {
        for (String candidate : candidates) {
            int idx = header.indexOf(candidate);
            if (idx >= 0) {
                return idx;
            }
            for (int i = 0; i < header.size(); i++) {
                if (header.get(i).toLowerCase(Locale.ROOT).startsWith(candidate)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static String valueAt(List<String> values, int index) {
        return index >= 0 && index < values.size() ? values.get(index) : null;
    }

    private static String firstString(JsonNode node, List<String> keys) {
        for (String key : keys) {
            JsonNode value = node.get(key);
            if (value != null && !value.isNull() && !value.asText().isBlank()) {
                return value.asText();
            }
        }
        return null;
    }
}