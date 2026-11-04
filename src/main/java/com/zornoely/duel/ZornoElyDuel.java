package com.zornoely.duel;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

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

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            try {
                duelManager.checkTimeouts();
                lootManager.checkExpired();
                inviteManager.cleanupExpired();
            } catch (Throwable t) {
                getLogger().warning("Task hatası: " + t.getMessage());
            }
        }, 20L, 20L);

        getLogger().info("ZornoElyDuel aktif!");
    }

    @Override
    public void onDisable() {
        if (arenaManager != null) arenaManager.save();
    }

    public void sendToHub(Player player) {
        if (player == null || !player.isOnline()) return;
        String cmd = getConfig().getString("settings.hub-command", "hub");
        Bukkit.getScheduler().runTask(this, () -> {
            if (player.isOnline()) player.performCommand(cmd);
        });
    }

    public ArenaManager getArenaManager()     { return arenaManager; }
    public DuelManager getDuelManager()       { return duelManager; }
    public QueueManager getQueueManager()     { return queueManager; }
    public LootManager getLootManager()       { return lootManager; }
    public InviteManager getInviteManager()   { return inviteManager; }
}
