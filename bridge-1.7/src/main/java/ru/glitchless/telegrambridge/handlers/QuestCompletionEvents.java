package ru.glitchless.telegrambridge.handlers;

import betterquesting.api.api.QuestingAPI;
import betterquesting.api.events.QuestEvent;
import betterquesting.api.properties.NativeProps;
import betterquesting.api.questing.IQuest;
import betterquesting.api.questing.IQuestLine;
import betterquesting.questing.QuestDatabase;
import betterquesting.questing.QuestLineDatabase;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
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
                    ToTelegramEvent.broadcastToChats(format(player.getGameProfile().getName(), title, chapters));
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

    static String escape(String text) {
        return text.replaceAll("(?i)§[0-9a-fk-or]", "").replace("\\", "\\\\")
                .replace("_", "\\_").replace("*", "\\*").replace("`", "\\`").replace("[", "\\[");
    }

    static String format(String username, String title, List<String> chapters) {
        String text = "🏆 *" + escape(username) + "* completed *" + escape(title) + "*";
        if (!chapters.isEmpty()) text += "\n📖 " + escape(String.join(" / ", chapters));
        return text;
    }
}
