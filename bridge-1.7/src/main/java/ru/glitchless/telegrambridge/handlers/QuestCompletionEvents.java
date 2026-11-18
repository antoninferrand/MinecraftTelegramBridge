package ru.glitchless.telegrambridge.handlers;

import betterquesting.api.api.QuestingAPI;
import betterquesting.api.events.QuestEvent;
import betterquesting.api.properties.NativeProps;
import betterquesting.api.utils.UuidConverter;
import betterquesting.api2.cache.QuestCache;
import betterquesting.api.questing.IQuest;
import betterquesting.api.questing.IQuestLine;
import betterquesting.questing.QuestDatabase;
import betterquesting.questing.QuestLineDatabase;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StatCollector;
import org.apache.logging.log4j.LogManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Uses the bridge's existing asynchronous sender; no polling or separate service. */
public class QuestCompletionEvents {
    private final long startedAt;
    private final Set<String> announced = new HashSet<>();

    public QuestCompletionEvents() { this(System.currentTimeMillis()); }
    QuestCompletionEvents(long startedAt) { this.startedAt = startedAt; }

    @SubscribeEvent
    public void onQuestEvent(QuestEvent event) {
        if (event.getType() != QuestEvent.Type.COMPLETED || event.getQuestIDs().isEmpty()) return;
        try {
            EntityPlayerMP player = QuestingAPI.getPlayer(event.getPlayerID());
            if (player == null || player.worldObj.isRemote) return;
            for (UUID id : event.getQuestIDs()) {
                IQuest quest = QuestDatabase.INSTANCE.get(id);
                if (quest == null) continue;
                NBTTagCompound completion = quest.getCompletionInfo(event.getPlayerID());
                if (completion == null) continue;
                long timestamp = completion.getLong("timestamp");
                String title = StatCollector.translateToLocal(quest.getProperty(NativeProps.NAME));
                List<String> chapters = new ArrayList<>();
                for (IQuestLine line : QuestLineDatabase.INSTANCE.values()) {
                    if (line.containsKey(id)) chapters.add(StatCollector.translateToLocal(line.getUnlocalisedName()));
                }
                if (firstCompletion(event.getPlayerID(), id, timestamp)) {
                    // Visibility belongs to the reader, not the player who completed the quest.
                    for (Object online : MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
                        EntityPlayerMP reader = (EntityPlayerMP) online;
                        boolean available = QuestCache.isQuestShown(quest, QuestingAPI.getQuestingUUID(reader), reader);
                        reader.addChatMessage(formatMinecraft(player.getGameProfile().getName(), title, chapters, id, available));
                    }
                    ToTelegramEvent.broadcastToChats(format(player.getGameProfile().getName(), title, chapters, id, event.getPlayerID()));
                }
            }
        } catch (RuntimeException | LinkageError error) {
            LogManager.getLogger("telegrambridge").error("Quest announcement failed (" + error.getClass().getSimpleName() + ")");
        }
    }

    boolean firstCompletion(UUID player, UUID quest, long timestamp) {
        // Ignore progress restored on login and duplicate events, but allow repeatable quests.
        return timestamp > startedAt && announced.add(player + ":" + quest + ":" + timestamp);
    }

    static String plainMinecraft(String text) {
        return text.replaceAll("(?i)§[0-9a-fk-or]", "").replaceAll("[\\r\\n\\t]+", " ");
    }

    static IChatComponent formatMinecraft(String username, String title, List<String> chapters, UUID id, boolean available) {
        IChatComponent message = new ChatComponentText("§6[Quest] §f" + plainMinecraft(username) + " completed ");
        IChatComponent questLink = new ChatComponentText(plainMinecraft(title));
        if (available) {
            // The normal command path opens on the next GUI change (close chat with Escape).
            // Do not use the native share marker: it forces the client to display the encoded ID.
            questLink.getChatStyle().setColor(EnumChatFormatting.AQUA).setUnderlined(true)
                    .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                            "/bq_client show " + UuidConverter.encodeUuid(id)))
                    .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            new ChatComponentText("Click, then close chat with Escape to open this quest")));
        } else {
            questLink.getChatStyle().setColor(EnumChatFormatting.GRAY)
                    .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            new ChatComponentText("This quest is not yet visible in your quest book")));
        }
        message.appendSibling(questLink);
        if (!chapters.isEmpty()) {
            IChatComponent chapter = new ChatComponentText(" (" + plainMinecraft(String.join(" / ", chapters)) + ")");
            chapter.getChatStyle().setColor(EnumChatFormatting.GRAY);
            message.appendSibling(chapter);
        }
        return message;
    }

    static String escape(String text) {
        return text.replaceAll("(?i)§[0-9a-fk-or]", "").replace("\\", "\\\\")
                .replace("_", "\\_").replace("*", "\\*").replace("`", "\\`").replace("[", "\\[");
    }

    static String format(String username, String title, List<String> chapters, UUID id, UUID playerId) {
        String url = "https://mcmaps.hejmo.in/quests/#subject=player%3A" + playerId + "&quest=" + id;
        String text = "🏆 *" + escape(username) + "* completed [" + escape(title) + "](" + url + ")";
        if (!chapters.isEmpty()) text += "\n📖 " + escape(String.join(" / ", chapters));
        return text;
    }
}
