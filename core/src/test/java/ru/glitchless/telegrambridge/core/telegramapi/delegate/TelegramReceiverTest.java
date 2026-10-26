package ru.glitchless.telegrambridge.core.telegramapi.delegate;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.glitchless.telegrambridge.core.config.TelegramBridgeConfig;
import ru.glitchless.telegrambridge.core.telegramapi.TelegramContext;
import ru.glitchless.telegrambridge.core.telegramapi.model.UpdateObject;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class TelegramReceiverTest {
    private final Gson gson = new Gson();
    private final AtomicInteger calls = new AtomicInteger();
    private List<String> previousChatIds;
    private TelegramReceiver receiver;

    @Before
    public void setUp() {
        previousChatIds = TelegramBridgeConfig.chat_ids;
        TelegramBridgeConfig.chat_ids = Arrays.asList("-100123", "456");
        receiver = new TelegramReceiver(new TelegramContext(LogManager.getLogger()));
        receiver.addListener(message -> { calls.incrementAndGet(); return false; });
        receiver.addListener(message -> { calls.incrementAndGet(); return true; });
    }

    @After
    public void tearDown() {
        TelegramBridgeConfig.chat_ids = previousChatIds;
    }

    @Test
    public void dispatchesAllHandlersForConfiguredChatsRegardlessOfSender() {
        dispatch("{\"message\":{\"chat\":{\"id\":-100123},\"from\":{\"id\":1},\"text\":\"/players\"}}");
        dispatch("{\"message\":{\"chat\":{\"id\":-100123},\"from\":{\"id\":2},\"text\":\"/status\"}}");
        dispatch("{\"message\":{\"chat\":{\"id\":456},\"text\":\"hello\"}}");
        assertEquals(6, calls.get());
    }

    @Test
    public void rejectsUnconfiguredOrMissingChatForEveryHandler() {
        dispatch("{\"message\":{\"chat\":{\"id\":999},\"from\":{\"id\":1},\"text\":\"/players\"}}");
        dispatch("{\"message\":{\"chat\":{\"id\":1},\"from\":{\"id\":1},\"text\":\"/status\"}}");
        dispatch("{\"message\":{\"text\":\"hello\"}}");
        dispatch("{\"message\":{\"chat\":{},\"text\":\"hello\"}}");
        TelegramBridgeConfig.chat_ids = Arrays.asList("0");
        dispatch("{\"message\":{\"chat\":{},\"text\":\"hello\"}}");
        TelegramBridgeConfig.chat_ids = null;
        dispatch("{\"message\":{\"chat\":{\"id\":999},\"text\":\"hello\"}}");
        assertEquals(0, calls.get());
    }

    private void dispatch(String json) {
        receiver.processUpdate(gson.fromJson(json, UpdateObject.class));
    }
}
