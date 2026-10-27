package ru.glitchless.telegrambridge.handlers;

import betterquesting.api.events.QuestEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class QuestCompletionEventsTest {
    private final UUID player = new UUID(0, 1);
    private final UUID quest = new UUID(0, 2);

    @Test public void ignoresHistoricalAndDuplicateCompletions() {
        QuestCompletionEvents listener = new QuestCompletionEvents(1000);
        assertFalse(listener.firstCompletion(player, quest, 999));
        assertFalse(listener.firstCompletion(player, quest, 1000));
        assertTrue(listener.firstCompletion(player, quest, 1001));
        assertFalse(listener.firstCompletion(player, quest, 1001));
    }

    @Test public void allowsRepeatableQuestsAndDifferentPlayers() {
        QuestCompletionEvents listener = new QuestCompletionEvents(1000);
        assertTrue(listener.firstCompletion(player, quest, 1001));
        assertTrue(listener.firstCompletion(player, quest, 1002));
        assertTrue(listener.firstCompletion(new UUID(0, 3), quest, 1001));
    }

    @Test public void ignoresUpdatesResetsAndEmptyCompletionEvents() {
        QuestCompletionEvents listener = new QuestCompletionEvents(1000);
        listener.onQuestEvent(new QuestEvent(QuestEvent.Type.UPDATED, player, quest));
        listener.onQuestEvent(new QuestEvent(QuestEvent.Type.RESET, player, quest));
        listener.onQuestEvent(new QuestEvent(QuestEvent.Type.COMPLETED, player, Collections.<UUID>emptySet()));
    }

    @Test public void formatsNamesAndChaptersWithoutDescriptionsOrIds() {
        String text = QuestCompletionEvents.format("Player_one", "§9E_TOO_MANY_KEYS", Arrays.asList("§6Getting Started", "Tips"));
        assertEquals("🏆 *Player\\_one* completed *E\\_TOO\\_MANY\\_KEYS*\n📖 Getting Started / Tips", text);
        assertFalse(text.contains(player.toString()));
    }

    @Test public void escapesTelegramMarkdownAndHandlesNoChapter() {
        assertEquals("🏆 *Alice* completed *A \\*star\\* and \\[link]*",
                QuestCompletionEvents.format("Alice", "A *star* and [link]", Collections.<String>emptyList()));
    }
}
