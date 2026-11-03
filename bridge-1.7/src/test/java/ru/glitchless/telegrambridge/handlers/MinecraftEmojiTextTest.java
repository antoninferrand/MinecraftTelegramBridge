package ru.glitchless.telegrambridge.handlers;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MinecraftEmojiTextTest {
    @Test
    public void convertsSupplementaryEmojiAndAdjacentEmoji() {
        assertEquals("Hi :thumbs_up::face_with_tears_of_joy:!",
                MinecraftEmojiText.convert("Hi 👍😂!"));
    }

    @Test
    public void matchesEntireSkinToneAndJoinedSequences() {
        assertEquals(":thumbs_up_medium_skin_tone: :woman_technologist: :family_man_woman_girl_boy:",
                MinecraftEmojiText.convert("👍🏽 👩‍💻 👨‍👩‍👧‍👦"));
    }

    @Test
    public void convertsFlagsKeycapsAndPresentationVariants() {
        assertEquals(":flag_france: :keycap_1: :red_heart: :red_heart: :rainbow_flag:",
                MinecraftEmojiText.convert("🇫🇷 1️⃣ ❤️ ❤ 🏳️‍🌈"));
        assertEquals(":keycap_1: :rainbow_flag:", MinecraftEmojiText.convert("1⃣ 🏳‍🌈"));
        assertEquals(":flag_england:", MinecraftEmojiText.convert(
                "\uD83C\uDFF4\uDB40\uDC67\uDB40\uDC62\uDB40\uDC65\uDB40\uDC6E\uDB40\uDC67\uDB40\uDC7F"));
    }

    @Test
    public void preservesTextFormattingAndNonEmojiUnicode() {
        String text = "§bÉlodie§f: déjà vu, 中文, 𐐀; 123 # * :thumbs_up: https://example.org";
        assertEquals(text, MinecraftEmojiText.convert(text));
        assertEquals("", MinecraftEmojiText.convert(""));
    }

    @Test
    public void supportsRecentEmojiAndAccentedNames() {
        assertEquals(":distorted_face: :flag_cote_d_ivoire:", MinecraftEmojiText.convert("🫪 🇨🇮"));
    }

    @Test
    public void everyBundledSequenceBecomesOneAsciiAlias() throws Exception {
        int checked = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                MinecraftEmojiText.class.getResourceAsStream("/emoji/emoji-test.txt"), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                StringBuilder emoji = new StringBuilder();
                for (String hex : line.split(";")[0].trim().split(" +")) {
                    emoji.appendCodePoint(Integer.parseInt(hex, 16));
                }
                assertTrue(line, MinecraftEmojiText.convert(emoji.toString()).matches(":[a-z0-9_]+:"));
                checked++;
            }
        }
        assertEquals(5225, checked);
    }
}
