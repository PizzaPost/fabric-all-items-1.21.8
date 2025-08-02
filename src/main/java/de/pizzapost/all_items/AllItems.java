package de.pizzapost.all_items;

import de.pizzapost.all_items.commands.ModCommands;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.WorldSavePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AllItems implements ModInitializer {
    public static final String MOD_ID = "all_items";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    static boolean started = false;
    static int sec = 0;
    static int min = 0;
    static int hours = 0;
    static int days = 0;
    static int collected_items = 0;
    static List<Item> items = new ArrayList<>();
    private static Thread timerThread;

    static List<String> blockedItems = List.of("minecraft:air", "minecraft:barrier", "minecraft:bedrock", "minecraft:chain_command_block", "minecraft:command_block", "minecraft:command_block_minecart", "minecraft:debug_stick", "minecraft:farmland", "minecraft:infested_chiseled_stone_bricks", "minecraft:infested_cobblestone", "minecraft:infested_cracked_stone_bricks", "minecraft:infested_deepslate", "minecraft:infested_mossy_stone_bricks", "minecraft:infested_stone", "minecraft:infested_stone_bricks", "minecraft:jigsaw", "minecraft:knowledge_book", "minecraft:light", "minecraft:mycelium", "minecraft:player_head", "minecraft:podzol", "minecraft:reinforced_deepslate", "minecraft:repeating_command_block", "minecraft:rooted_dirt", "minecraft.spawner", "minecraft:structure_block", "minecraft:structure_void", "minecraft:test_block", "minecraft:test_instance_block", "minecraft:trial_spawner", "minecraft:vault");
    static ServerBossBar collectedItemsBossbar = new ServerBossBar(Text.translatable("bossbar.all_items.collected_items", collected_items, items.size()), BossBar.Color.GREEN, BossBar.Style.PROGRESS);

    private static Path getSavePath(MinecraftServer server) {
        return server.getSavePath(WorldSavePath.ROOT).resolve("all_items_data.txt");
    }

    public static void saveData(MinecraftServer server) {
        try {
            Path path = getSavePath(server);
            Files.createDirectories(path.getParent());
            String data = started + ";" + collected_items + ";" + days + ";" + hours + ";" + min + ";" + sec;
            Files.write(path, data.getBytes());
        } catch (IOException e) {
            LOGGER.error("Failed to save timer state", e);
        }
    }

    public static void loadData(MinecraftServer server) {
        Path path = getSavePath(server);
        if (!Files.exists(path)) return;

        try {
            String[] parts = Files.readString(path).split(";");
            if (parts.length == 6) {
                started = Boolean.parseBoolean(parts[0]);
                collected_items = Integer.parseInt(parts[1]);
                days = Integer.parseInt(parts[2]);
                hours = Integer.parseInt(parts[3]);
                min = Integer.parseInt(parts[4]);
                sec = Integer.parseInt(parts[5]);
            }
        } catch (IOException | NumberFormatException e) {
            LOGGER.error("Failed to load timer state", e);
        }
    }

    @Override
    public void onInitialize() {
        ModCommands.registerModCommands();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            loadData(server);
            if (started) {
                startGame(server, true);
            }
        });
        ServerTickEvents.END_WORLD_TICK.register(world -> {
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            started = false;
            if (timerThread != null && timerThread.isAlive()) {
                timerThread.interrupt();
                timerThread = null;
            }
            collected_items = 0;
            sec = min = hours = days = 0;
            items.clear();
            collectedItemsBossbar.clearPlayers();
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (started) {
                collectedItemsBossbar.addPlayer(handler.getPlayer());
            }
        });
    }

    public static void startGame(MinecraftServer server, boolean force) {
        if (!force && started) return;
        if (timerThread != null && timerThread.isAlive()) return;

        started = true;

        if (items.isEmpty()) {
            shuffleItems();
        }

        timerThread = new Thread(() -> {
            while (started) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

                if (!server.isPaused()) {
                    sec++;
                    if (sec >= 60) {
                        sec = 0;
                        min++;
                        if (min >= 60) {
                            min = 0;
                            hours++;
                            if (hours >= 24) {
                                hours = 0;
                                days++;
                            }
                        }
                    }
                    saveData(server);
                }

                collectedItemsBossbar.setPercent((float) collected_items / items.size());
                collectedItemsBossbar.setName(Text.translatable("bossbar.all_items.collected_items", collected_items, items.size()));

                Text actionbarMessage = Text.translatable("actionbar.all_items.timer", days, hours, min, sec).formatted(Formatting.GOLD);
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    player.sendMessage(actionbarMessage, true);
                    collectedItemsBossbar.addPlayer(player);
                }
            }
        });

        timerThread.start();
    }

    public static void setGameTimer(MinecraftServer server, int d, int h, int m, int s) {
        days = d;
        hours = h;
        min = m;
        sec = s;
        saveData(server);
    }

    public static void shuffleItems() {
        for (Item item : Registries.ITEM) {
            if (!blockedItems.contains(item.toString()) && !item.toString().contains("spawn_egg")) {
                items.add(item);
            }
        }
        System.out.println(items);
        Collections.shuffle(items);
    }
}