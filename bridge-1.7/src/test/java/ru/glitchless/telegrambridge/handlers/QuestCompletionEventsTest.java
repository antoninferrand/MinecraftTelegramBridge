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

    @Test public void formatsNamesAndChaptersWithPlayerQuestLink() {
        String text = QuestCompletionEvents.format("Player_one", "§9E_TOO_MANY_KEYS", Arrays.asList("§6Getting Started", "Tips"), quest, player);
        assertEquals("🏆 *Player\\_one* completed [E\\_TOO\\_MANY\\_KEYS](https://mcmaps.hejmo.in/quests/#subject=player%3A00000000-0000-0000-0000-000000000001&quest=00000000-0000-0000-0000-000000000002)\n📖 Getting Started / Tips", text);
    }

    @Test public void linksKnownQuests() {
        assertEquals("🏆 *wiikip* completed [Ready, Set, Go!](https://mcmaps.hejmo.in/quests/#subject=player%3A00000000-0000-0000-0000-000000000001&quest=00000000-0000-0000-0000-00000000000f)\n📖 Tier 0 - Stone Age",
                QuestCompletionEvents.format("wiikip", "Ready, Set, Go!", Arrays.asList("Tier 0 - Stone Age"), new UUID(0, 15), player));
        assertEquals("🏆 *Coshyperbolixx* completed [\"Baked\" Potatoes](https://mcmaps.hejmo.in/quests/#subject=player%3A00000000-0000-0000-0000-000000000001&quest=00000000-0000-0000-0000-0000000001ba)\n📖 And So, It Begins",
                QuestCompletionEvents.format("Coshyperbolixx", "\"Baked\" Potatoes", Arrays.asList("And So, It Begins"), new UUID(0, 442), player));
    }

    @Test public void linksTheCompletingPlayerUsingFullUuids() {
        UUID completingPlayer = UUID.fromString("cc80284a-fb73-4f36-94ec-a1c6f3082ddb");
        String text = QuestCompletionEvents.format("A", "B", Collections.<String>emptyList(), new UUID(-1, -1), completingPlayer);
        assertEquals("🏆 *A* completed [B](https://mcmaps.hejmo.in/quests/#subject=player%3Acc80284a-fb73-4f36-94ec-a1c6f3082ddb&quest=ffffffff-ffff-ffff-ffff-ffffffffffff)", text);
    }

    @Test public void escapesTelegramMarkdownAndHandlesNoChapter() {
        assertEquals("🏆 *Alice* completed [A \\*star\\* and \\[link]](https://mcmaps.hejmo.in/quests/#subject=player%3A00000000-0000-0000-0000-000000000001&quest=00000000-0000-0000-0000-000000000002)",
                QuestCompletionEvents.format("Alice", "A *star* and [link]", Collections.<String>emptyList(), quest, player));
    }
}
