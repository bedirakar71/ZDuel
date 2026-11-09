package com.zornoely.duel;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.logging.Level;

public class ZornoElyDuel extends JavaPlugin {
    private static ZornoElyDuel instance;
    private ArenaManager arenaManager;
    private DuelManager duelManager;
    private QueueManager queueManager;
    private LootManager lootManager;
    private InviteManager inviteManager;

    public static ZornoElyDuel getInstance() { return instance; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        migrateConfig();

        arenaManager  = new ArenaManager(this);
        lootManager   = new LootManager(this);
        inviteManager = new InviteManager(this);
        duelManager   = new DuelManager(this);
        queueManager  = new QueueManager(this);

        arenaManager.load();

        DuelCommand cmd = new DuelCommand(this);
        getCommand("duel").setExecutor(cmd);
        getCommand("duel").setTabCompleter(cmd);
        Bukkit.getPluginManager().registerEvents(new DuelListener(this), this);

        // Her saniye: düello süresi, ganimet süresi + geri sayım, davet temizliği, sıra eşleştirme
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            safe("düello süresi", () -> duelManager.checkTimeouts());
            safe("ganimet", () -> lootManager.tick());
            safe("davet", () -> inviteManager.cleanupExpired());
            safe("sıra", () -> queueManager.processQueues());
        }, 20L, 20L);

        getLogger().info("ZornoElyDuel aktif!");
    }

    @Override
    public void onDisable() {
        try {
            if (lootManager != null) lootManager.flushAll();   // bekleyen ganimetler oyunculara verilir
            if (duelManager != null) duelManager.evacuateAll(); // arenadaki oyuncular dışarı alınır
            if (arenaManager != null) {
                arenaManager.restoreAll();                      // Trap'te konan bloklar geri alınır
                arenaManager.save();
            }
        } catch (Throwable t) {
            getLogger().log(Level.WARNING, "Kapanış hatası", t);
        }
    }

    /** Eski config.yml (20 dk / 40 sn) bir kereliğine yeni değerlere (15 dk / 20 sn) çekilir. */
    private void migrateConfig() {
        if (getConfig().getInt("config-version", 1) >= 2) return;
        getConfig().set("settings.duel-duration-minutes", 15);
        getConfig().set("settings.loot-duration-seconds", 20);
        getConfig().set("config-version", 2);
        saveConfig();
    }

    private void safe(String name, Runnable task) {
        try {
            task.run();
        } catch (Throwable t) {
            getLogger().log(Level.WARNING, "Task hatası (" + name + ")", t);
        }
    }

    /** Hub komutu çalışmazsa kullanılacak yedek nokta: ana dünyanın spawn'ı. */
    public Location getFallbackSpawn() {
        List<World> worlds = Bukkit.getWorlds();
        return worlds.isEmpty() ? null : worlds.get(0).getSpawnLocation();
    }

    public void sendToHub(Player player) {
        if (player == null || !player.isOnline() || !isEnabled()) return;
        Bukkit.getScheduler().runTask(this, () -> {
            if (!player.isOnline()) return;
            String cmd = getConfig().getString("settings.hub-command", "hub");
            cmd = cmd == null ? "" : cmd.trim();
            if (cmd.startsWith("/")) cmd = cmd.substring(1);
            boolean ok = !cmd.isEmpty() && player.performCommand(cmd);
            if (!ok) {
                Location spawn = getFallbackSpawn();
                if (spawn != null) player.teleport(spawn);
            }
        });
    }

    public ArenaManager getArenaManager()     { return arenaManager; }
    public DuelManager getDuelManager()       { return duelManager; }
    public QueueManager getQueueManager()     { return queueManager; }
    public LootManager getLootManager()       { return lootManager; }
    public InviteManager getInviteManager()   { return inviteManager; }
}
