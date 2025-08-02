package de.pizzapost.all_items;

import de.pizzapost.all_items.commands.ModCommands;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AllItems implements ModInitializer {
    public static final String MOD_ID = "all_items";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final Path SAVE_PATH = Paths.get("config", "all_items_timer.txt");

    static boolean started = false;
    static int sec = 0;
    static int min = 0;
    static int hours = 0;
    static int days = 0;

    public static void saveTimer() {
        try {
            Files.createDirectories(SAVE_PATH.getParent());
            String data = started + ";" + days + ";" + hours + ";" + min + ";" + sec;
            Files.write(SAVE_PATH, data.getBytes());
        } catch (IOException e) {
            LOGGER.error("Failed to save timer state", e);
        }
    }

    public static void loadTimer() {
        if (!Files.exists(SAVE_PATH)) return;

        try {
            String[] parts = Files.readString(SAVE_PATH).split(";");
            if (parts.length == 5) {
                started = Boolean.parseBoolean(parts[0]);
                days = Integer.parseInt(parts[1]);
                hours = Integer.parseInt(parts[2]);
                min = Integer.parseInt(parts[3]);
                sec = Integer.parseInt(parts[4]);
            }
        } catch (IOException | NumberFormatException e) {
            LOGGER.error("Failed to load timer state", e);
        }
    }

    @Override
    public void onInitialize() {
        loadTimer();
        ModCommands.registerModCommands();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            startGame(server, true);
        });
    }

    public static void startGame(MinecraftServer server, boolean force) {
        if (!force) {
            if (started) return;
        }
        started = true;

        new Thread(() -> {
            while (started) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

                if (!server.isPaused()) {
                    AllItems.sec++;
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
                    saveTimer();
                }
                Text actionbarMessage = Text.translatable("actionbar.all_items.timer", days, hours, min, sec).formatted(Formatting.GOLD);
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    player.sendMessage(actionbarMessage, true);
                }
            }
        }).start();
    }

    public static void setGameTimer(int d, int h, int m, int s) {
        days = d;
        hours = h;
        min = m;
        sec = s;
        saveTimer();
    }
}