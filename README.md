# Telegram Bridge

# Install
1. Get download from [curseforge](https://www.curseforge.com/minecraft/mc-mods/telegram-bridge) and put in mods folder
2. Create your telegram bot in Telegram with [BotFather](t.me/BotFather)
3. Turn off Privacy mode in `Bot Settings->Group Privacy`
4. Add bot in telegram group
5. Add telegram chat id in telegrambridge.cfg (you can use [GetChatBot](t.me/GetChatBot))
6. Change text block in config 
7. Check it!
8. You can use [ServerLocalization](https://www.curseforge.com/minecraft/mc-mods/server-localization) for better support

# Building
Most modules build from the repository root:
```
./gradlew build
```

`bridge-1.20` is a **standalone build** and is deliberately not part of the root build. Forge 1.20.1
requires ForgeGradle 6, Gradle 8 and Java 17, which the legacy modules (ForgeGradle 4.1, and the
1.7.10 fork) cannot run on. Build it from its own directory with a Java 17 toolchain available:
```
cd bridge-1.20 && ./gradlew build
```

# Example
## Telegram
![](https://habrastorage.org/webt/c8/1i/v-/c81iv-lwbnc-d5ar-mn3xrpl6r8.png)
## Minecraft
![](https://habrastorage.org/webt/0b/j2/hz/0bj2hz7dy92bb87yivmhpujiate.png)

### GTNH 1.7.10 additions

- `/day` advances world time to the next morning from the configured Telegram chat.
- With BetterQuesting installed, completed quests are announced with the player's
  name, quest title and chapter names. Descriptions and IDs are omitted.
  The existing asynchronous Telegram sender handles delivery; no external service
  or save-file polling is required. Historical completions and duplicate events
  are ignored. Delivery is best effort and pending messages do not survive restart.

The quest integration targets BetterQuesting **3.7.15-GTNH**. For building and
running its five tests, set `GTNH_BQ_API` to the absolute path of the
[development JAR](https://github.com/GTNewHorizons/BetterQuesting/releases/download/3.7.15-GTNH/BetterQuesting-3.7.15-GTNH-dev.jar),
then run `./gradlew :bridge-1.7:build` with the existing Java 11 toolchain.
The development JAR is a compile/test dependency and is not bundled in the mod.
