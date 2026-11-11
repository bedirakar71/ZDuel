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
    private ClanHook clanHook;

    public static ZornoElyDuel getInstance() { return instance; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        migrateConfig();

        arenaManager  = new ArenaManager(this);
        lootManager   = new LootManager(this);
        inviteManager = new InviteManager(this);
        clanHook      = new ClanHook(this);
        duelManager   = new DuelManager(this);
        queueManager  = new QueueManager(this);

        arenaManager.load();

        DuelCommand cmd = new DuelCommand(this);
        getCommand("duel").setExecutor(cmd);
        getCommand("duel").setTabCompleter(cmd);
        Bukkit.getPluginManager().registerEvents(new DuelListener(this), this);

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
            if (lootManager != null) lootManager.flushAll();
            if (duelManager != null) duelManager.evacuateAll();
            if (arenaManager != null) {
                arenaManager.restoreAll();
                arenaManager.save();
            }
        } catch (Throwable t) {
            getLogger().log(Level.WARNING, "Kapanış hatası", t);
        }
    }

    private void migrateConfig() {
        org.bukkit.configuration.file.FileConfiguration cfg = getConfig();
        int version = cfg.getInt("config-version", 1);
        if (version >= 3) return;

        if (version < 2) {
            cfg.set("settings.duel-duration-minutes", 15);
            cfg.set("settings.loot-duration-seconds", 20);
        }
        if (!cfg.isSet("settings.clan-check.enabled"))
            cfg.set("settings.clan-check.enabled", true);
        if (!cfg.isSet("settings.clan-check.placeholder"))
            cfg.set("settings.clan-check.placeholder", "%apexclan_clan%");
        if (!cfg.isSet("settings.auto-open-loot"))
            cfg.set("settings.auto-open-loot", true);
        cfg.set("config-version", 3);
        saveConfig();
    }

    private void safe(String name, Runnable task) {
        try {
            task.run();
        } catch (Throwable t) {
            getLogger().log(Level.WARNING, "Task hatası (" + name + ")", t);
        }
    }

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
    public ClanHook getClanHook()             { return clanHook; }
}
