package ru.glitchless.telegrambridge.handlers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** ASCII emoji names for the legacy Minecraft font; no client mod is required. */
public final class MinecraftEmojiText {
    private static final Node ROOT = loadEmoji();

    private MinecraftEmojiText() {
    }

    public static String convert(String text) {
        StringBuilder result = new StringBuilder(text.length());
        for (int start = 0; start < text.length();) {
            Node node = ROOT;
            String replacement = null;
            int end = start;
            // Prefer the complete sequence over its individual emoji components.
            for (int cursor = start; cursor < text.length();) {
                int codePoint = text.codePointAt(cursor);
                node = node.children.get(codePoint);
                if (node == null) {
                    break;
                }
                cursor += Character.charCount(codePoint);
                if (node.alias != null) {
                    replacement = node.alias;
                    end = cursor;
                }
            }
            if (replacement != null) {
                result.append(replacement);
                start = end;
            } else {
                int codePoint = text.codePointAt(start);
                result.appendCodePoint(codePoint);
                start += Character.charCount(codePoint);
            }
        }
        return result.toString();
    }

    private static Node loadEmoji() {
        Node root = new Node();
        Pattern entry = Pattern.compile("^([0-9A-F ]+);\\s*[^#]+#\\s*\\S+\\s+E[0-9.]+\\s+(.+)$");
        InputStream data = MinecraftEmojiText.class.getResourceAsStream("/emoji/emoji-test.txt");
        if (data == null) {
            throw new IllegalStateException("Missing bundled emoji data");
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(data, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher match = entry.matcher(line);
                if (!match.matches()) {
                    continue;
                }
                Node node = root;
                for (String hex : match.group(1).trim().split(" +")) {
                    node = node.children.computeIfAbsent(Integer.parseInt(hex, 16), key -> new Node());
                }
                String name = Normalizer.normalize(match.group(2), Normalizer.Form.NFKD)
                        .toLowerCase(Locale.ROOT).replaceAll("\\p{M}", "")
                        .replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
                node.alias = ":" + name + ":";
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read bundled emoji data", e);
        }
        if (root.children.isEmpty()) {
            throw new IllegalStateException("Bundled emoji data has no entries");
        }
        return root;
    }

    private static final class Node {
        private final Map<Integer, Node> children = new HashMap<>();
        private String alias;
    }
}
