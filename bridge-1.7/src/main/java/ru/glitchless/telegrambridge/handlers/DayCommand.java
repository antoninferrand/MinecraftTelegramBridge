package ru.glitchless.telegrambridge.handlers;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.server.MinecraftServer;
import ru.glitchless.telegrambridge.TelegramBridgeMod;
import ru.glitchless.telegrambridge.core.handlers.IMessageReceiver;
import ru.glitchless.telegrambridge.core.telegramapi.model.MessageObject;

import javax.annotation.Nonnull;
import java.util.Queue;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import ru.glitchless.telegrambridge.core.utils.HttpUtils;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DayCommand implements IMessageReceiver {
    private final Queue<MessageObject> requests = new ConcurrentLinkedQueue<MessageObject>();

    @Override
    public boolean onTelegramObjectMessage(@Nonnull MessageObject message) {
        String text = message.getText();
        if (text == null || !(text.equalsIgnoreCase("/day") ||
                text.equalsIgnoreCase("/day@minecraft_bridge_vc_bot"))) {
            return false;
        }
        requests.add(message);
        return true;
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MessageObject message;
        while ((message = requests.poll()) != null) {
            MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
            long time = server.getEntityWorld().getWorldTime();
            long delta = (1000 - (time % 24000) + 24000) % 24000;
            if (delta != 0) {
                server.getCommandManager().executeCommand(server, "time add " + delta);
            }
            final MessageObject completed = message;
            Thread reaction = new Thread(() -> {
                List<AbstractMap.SimpleEntry<String, Object>> params = new ArrayList<>();
                params.add(new AbstractMap.SimpleEntry<>("chat_id", completed.getChat().getId()));
                params.add(new AbstractMap.SimpleEntry<>("message_id", completed.getMessageId()));
                params.add(new AbstractMap.SimpleEntry<>("reaction", "[{\"type\":\"emoji\",\"emoji\":\"👍\"}]"));
                try {
                    HttpUtils.doPostRequest(TelegramBridgeMod.getContext().getBaseUrl() + "/setMessageReaction", params, TelegramBridgeMod.getContext().getLogger());
                } catch (Exception ex) {
                    TelegramBridgeMod.getContext().getLogger().warn("Telegram day reaction failed (" + ex.getClass().getSimpleName() + ")");
                }
            }, "telegram-day-reaction");
            reaction.setDaemon(true);
            reaction.start();
        }
    }
}
