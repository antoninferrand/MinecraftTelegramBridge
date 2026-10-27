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
import java.util.concurrent.ConcurrentLinkedQueue;

public class DayCommand implements IMessageReceiver {
    private final Queue<String> requests = new ConcurrentLinkedQueue<String>();

    @Override
    public boolean onTelegramObjectMessage(@Nonnull MessageObject message) {
        String text = message.getText();
        if (text == null || !(text.equalsIgnoreCase("/day") ||
                text.equalsIgnoreCase("/day@minecraft_bridge_vc_bot"))) {
            return false;
        }
        requests.add(message.getChat().getId().toString());
        return true;
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        String chat;
        while ((chat = requests.poll()) != null) {
            MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
            long time = server.getEntityWorld().getWorldTime();
            long delta = (1000 - (time % 24000) + 24000) % 24000;
            if (delta != 0) {
                server.getCommandManager().executeCommand(server, "time add " + delta);
            }
            final String replyChat = chat;
            Thread reply = new Thread(() -> TelegramBridgeMod.getContext().sendMessage(replyChat, "Set the time to day."), "telegram-day-reply");
            reply.setDaemon(true);
            reply.start();
        }
    }
}
