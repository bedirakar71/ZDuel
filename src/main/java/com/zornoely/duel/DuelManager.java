package com.zornoely.duel;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class DuelManager {
    private final ZornoElyDuel plugin;
    private final Map<UUID, Duel> activeDuels = new HashMap<>();
    private final Set<UUID> killedInDuel = new HashSet<>();
    private final Set<UUID> quitLosers = new HashSet<>();
    private final int durationMinutes;

    public DuelManager(ZornoElyDuel plugin) {
        this.plugin = plugin;
        this.durationMinutes = Math.max(1, plugin.getConfig().getInt("settings.duel-duration-minutes", 15));
    }

    public Duel getDuel(UUID uuid)      { return activeDuels.get(uuid); }
    public boolean isInDuel(UUID uuid)  { return activeDuels.containsKey(uuid); }

    public int countActive(ArenaType mode) {
        int count = 0;
        for (Duel d : new HashSet<>(activeDuels.values())) {
            if (d.getMode() == mode) count++;
        }
        return count;
    }

    public Duel startDuel(Player p1, Player p2, ArenaType mode) {
        if (p1 == null || p2 == null || !p1.isOnline() || !p2.isOnline()) return null;
        if (p1.getUniqueId().equals(p2.getUniqueId())) return null;
        if (isInDuel(p1.getUniqueId()) || isInDuel(p2.getUniqueId())) return null;
        if (plugin.getLootManager().hasLoot(p1.getUniqueId())
                || plugin.getLootManager().hasLoot(p2.getUniqueId())) return null;

        if (plugin.getClanHook().isSameClan(p1, p2)) {
            Msg.warn(p1, "Aynı klandan bir oyuncuyla düello yapamazsın!");
            Msg.warn(p2, "Aynı klandan bir oyuncuyla düello yapamazsın!");
            return null;
        }

        Arena arena = plugin.getArenaManager().findAvailable(mode);
        if (arena == null) {
            String msg = "Bu mod için uygun boş arena bulunamadı!";
            Msg.warn(p1, msg);
            Msg.warn(p2, msg);
            return null;
        }
        Location s1 = arena.getSpawn1();
        Location s2 = arena.getSpawn2();
        if (s1 == null || s2 == null) return null;

        // YENİ: Arenadaki eski düşen eşyaları temizle
        clearDrops(s1);
        clearDrops(s2);

        arena.setBusy(true);
        plugin.getQueueManager().removeFromAll(p1.getUniqueId());
        plugin.getQueueManager().removeFromAll(p2.getUniqueId());

        Duel duel = new Duel(p1.getUniqueId(), p2.getUniqueId(), arena.getName(), mode);
        activeDuels.put(p1.getUniqueId(), duel);
        activeDuels.put(p2.getUniqueId(), duel);

        p1.closeInventory();
        p2.closeInventory();
        boolean ok1 = p1.teleport(s1);
        boolean ok2 = p2.teleport(s2);
        if (!ok1 || !ok2) {
            endDuel(duel, null, "TELEPORT_FAIL");
            if (ok1) plugin.sendToHub(p1);
            if (ok2) plugin.sendToHub(p2);
            return null;
        }

        Msg.duelStart(p1, p2.getName(), mode, arena.getName(), durationMinutes);
        Msg.duelStart(p2, p1.getName(), mode, arena.getName(), durationMinutes);
        return duel;
    }

    private void clearDrops(Location loc) {
        if (loc == null || loc.getWorld() == null) return;
        loc.getWorld().getNearbyEntities(loc, 50, 50, 50, e -> e instanceof Item).forEach(Entity::remove);
    }

    public void endDuel(Duel duel, UUID winnerUuid, String reason) {
        endDuel(duel, winnerUuid, reason, false);
    }

    public void endDuel(Duel duel, UUID winnerUuid, String reason, boolean holdArena) {
        if (duel == null) return;
        boolean removed1 = activeDuels.remove(duel.getPlayer1(), duel);
        boolean removed2 = activeDuels.remove(duel.getPlayer2(), duel);
        if (!removed1 && !removed2) return;
        if (!holdArena) plugin.getArenaManager().release(duel.getArenaName());
    }

    public void evacuateAll() {
        Location spawn = plugin.getFallbackSpawn();
        for (UUID id : new HashSet<>(activeDuels.keySet())) {
            Player p = Bukkit.getPlayer(id);
            if (spawn != null && p != null && p.isOnline()) p.teleport(spawn);
        }
        activeDuels.clear();
    }

    public void checkTimeouts() {
        if (activeDuels.isEmpty()) return;
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
            String reason = "Süre doldu, kimse kazanamadı.";
            if (p1 != null) Msg.draw(p1, reason);
            if (p2 != null) Msg.draw(p2, reason);
            plugin.sendToHub(p1);
            plugin.sendToHub(p2);
        }
    }

    public void markKilled(UUID uuid)         { killedInDuel.add(uuid); }
    public boolean wasKilledInDuel(UUID uuid) { return killedInDuel.remove(uuid); }
    public void markQuitLoss(UUID uuid)       { quitLosers.add(uuid); }
    public boolean wasQuitLoss(UUID uuid)     { return quitLosers.remove(uuid); }
    public void forget(UUID uuid)             { killedInDuel.remove(uuid); }
}
