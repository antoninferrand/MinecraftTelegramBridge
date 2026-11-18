package ru.glitchless.telegrambridge.handlers;

import betterquesting.api.events.QuestEvent;
import net.minecraft.event.ClickEvent;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class QuestCompletionEventsTest {
    private final UUID player = new UUID(0, 1);
    private final UUID quest = new UUID(0, 2);

    private IChatComponent serialized(IChatComponent message) {
        return IChatComponent.Serializer.func_150699_a(IChatComponent.Serializer.func_150696_a(message));
    }

    @Test public void availableQuestHasCleanTitleAndHiddenCommandAfterSerialization() {
        IChatComponent message = serialized(QuestCompletionEvents.formatMinecraft("Player_one", "§1You Reap...",
                Arrays.asList("§6And So, It\nBegins", "Tips"), quest, true));
        String text = message.getUnformattedText();
        assertTrue(text.contains("Player_one completed You Reap... (And So, It Begins / Tips)"));
        assertFalse(text.contains("\n"));
        assertFalse(text.contains("sharequest"));
        assertFalse(text.contains("AAAAAAAA"));
        IChatComponent link = (IChatComponent) message.getSiblings().get(0);
        assertEquals("You Reap...", link.getUnformattedText());
        assertEquals(ClickEvent.Action.RUN_COMMAND, link.getChatStyle().getChatClickEvent().getAction());
        assertEquals("/bq_client show AAAAAAAAAAAAAAAAAAAAAg==", link.getChatStyle().getChatClickEvent().getValue());
        assertTrue(link.getChatStyle().getChatHoverEvent().getValue().getUnformattedText().contains("Escape"));
        IChatComponent chapter = (IChatComponent) message.getSiblings().get(1);
        assertNull(chapter.getChatStyle().getChatClickEvent());
    }

    @Test public void unavailableReaderGetsTheTitleWithoutMisleadingClickAction() {
        IChatComponent message = serialized(QuestCompletionEvents.formatMinecraft("Alice", "First quest",
                Collections.<String>emptyList(), quest, false));
        IChatComponent link = (IChatComponent) message.getSiblings().get(0);
        assertEquals("First quest", link.getUnformattedText());
        assertNull(link.getChatStyle().getChatClickEvent());
        assertEquals(EnumChatFormatting.GRAY, link.getChatStyle().getColor());
        assertTrue(link.getChatStyle().getChatHoverEvent().getValue().getUnformattedText().contains("not yet visible"));
    }

    @Test public void differentMessagesKeepIndependentQuestCommandsAndReaderStyles() {
        UUID secondQuest = new UUID(-1, -1);
        IChatComponent first = serialized(QuestCompletionEvents.formatMinecraft("Alice", "First quest",
                Collections.<String>emptyList(), quest, true));
        IChatComponent second = serialized(QuestCompletionEvents.formatMinecraft("Bob", "Second quest",
                Collections.<String>emptyList(), secondQuest, true));
        IChatComponent unavailable = serialized(QuestCompletionEvents.formatMinecraft("Bob", "Second quest",
                Collections.<String>emptyList(), secondQuest, false));
        IChatComponent firstLink = (IChatComponent) first.getSiblings().get(0);
        IChatComponent secondLink = (IChatComponent) second.getSiblings().get(0);
        assertEquals("/bq_client show AAAAAAAAAAAAAAAAAAAAAg==", firstLink.getChatStyle().getChatClickEvent().getValue());
        assertEquals("/bq_client show _____________________w==", secondLink.getChatStyle().getChatClickEvent().getValue());
        assertNull(((IChatComponent) unavailable.getSiblings().get(0)).getChatStyle().getChatClickEvent());
        assertFalse(second.getUnformattedText().contains("_____________________w=="));
    }

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
