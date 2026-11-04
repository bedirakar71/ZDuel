package com.zornoely.duel;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;

public class DuelManager {
    private final ZornoElyDuel plugin;
    private final Map<UUID, Duel> activeDuels = new HashMap<>();
    private final Set<UUID> killedInDuel = new HashSet<>();
    private final int durationMinutes;

    public DuelManager(ZornoElyDuel plugin) {
        this.plugin = plugin;
        this.durationMinutes = plugin.getConfig().getInt("settings.duel-duration-minutes", 20);
    }

    public Duel getDuel(UUID uuid)      { return activeDuels.get(uuid); }
    public boolean isInDuel(UUID uuid)  { return activeDuels.containsKey(uuid); }

    public Duel startDuel(Player p1, Player p2, ArenaType mode) {
        if (p1 == null || p2 == null || !p1.isOnline() || !p2.isOnline()) return null;

        Arena arena = plugin.getArenaManager().findAvailable(mode);
        if (arena == null) {
            String msg = ChatColor.RED + "Bu mod için uygun boş arena bulunamadı!";
            p1.sendMessage(msg);
            p2.sendMessage(msg);
            return null;
        }

        arena.setBusy(true);
        p1.teleport(arena.getSpawn1());
        p2.teleport(arena.getSpawn2());

        Duel duel = new Duel(p1.getUniqueId(), p2.getUniqueId(), arena.getName(), mode);
        activeDuels.put(p1.getUniqueId(), duel);
        activeDuels.put(p2.getUniqueId(), duel);

        String modeName = mode == ArenaType.ELYTRA ? "Elytra PvP" : "Trap PvP";
        String msg = ChatColor.GREEN + "Düello başladı! Mod: " + ChatColor.YELLOW + modeName
                + ChatColor.GREEN + " | Arena: " + ChatColor.YELLOW + arena.getName()
                + ChatColor.GREEN + " | Süre: " + ChatColor.YELLOW + durationMinutes + " dk";
        p1.sendMessage(msg);
        p2.sendMessage(msg);
        return duel;
    }

    public void endDuel(Duel duel, UUID winnerUuid, String reason) {
        if (duel == null) return;
        Arena arena = plugin.getArenaManager().get(duel.getArenaName());
        if (arena != null) arena.setBusy(false);
        activeDuels.remove(duel.getPlayer1());
        activeDuels.remove(duel.getPlayer2());
    }

    public void checkTimeouts() {
        long limit = durationMinutes * 60_000L;
        long now = System.currentTimeMillis();
        Set<Duel> toEnd = new HashSet<>();
        for (Duel d : activeDuels.values()) {
            if (now - d.getStartTime() >= limit) toEnd.add(d);
        }
        for (Duel d : toEnd) {
            endDuel(d, null, "TIMEOUT");
            Player p1 = Bukkit.getPlayer(d.getPlayer1());
            Player p2 = Bukkit.getPlayer(d.getPlayer2());
            String msg = ChatColor.RED + "Düello süresi doldu, berabere bitti!";
            if (p1 != null) p1.sendMessage(msg);
            if (p2 != null) p2.sendMessage(msg);
            plugin.sendToHub(p1);
            plugin.sendToHub(p2);
        }
    }

    public void markKilled(UUID uuid)         { killedInDuel.add(uuid); }
    public boolean wasKilledInDuel(UUID uuid) { return killedInDuel.remove(uuid); }
}
