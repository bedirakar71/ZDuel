package com.zornoely.duel;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;

public class QueueManager {
    private final ZornoElyDuel plugin;
    private final List<UUID> elytraQueue = new ArrayList<>();
    private final List<UUID> trapQueue = new ArrayList<>();

    public QueueManager(ZornoElyDuel plugin) { this.plugin = plugin; }

    public List<UUID> getQueue(ArenaType mode) {
        return mode == ArenaType.ELYTRA ? elytraQueue : trapQueue;
    }

    public boolean isInQueue(UUID uuid, ArenaType mode) {
        return getQueue(mode).contains(uuid);
    }

    public void toggleQueue(Player p, ArenaType mode) {
        List<UUID> queue = getQueue(mode);
        if (queue.contains(p.getUniqueId())) {
            queue.remove(p.getUniqueId());
            p.sendMessage(ChatColor.RED + modeName(mode) + " sırasından çıktın!");
            return;
        }
        getQueue(mode == ArenaType.ELYTRA ? ArenaType.TRAP : ArenaType.ELYTRA)
                .remove(p.getUniqueId());
        queue.add(p.getUniqueId());
        p.sendMessage(ChatColor.GREEN + modeName(mode) + " sırasına girdin!");
        tryMatch(mode);
    }

    private void tryMatch(ArenaType mode) {
        List<UUID> queue = getQueue(mode);
        queue.removeIf(uuid -> Bukkit.getPlayer(uuid) == null);

        while (queue.size() >= 2) {
            UUID u1 = queue.get(0);
            UUID u2 = queue.get(1);
            Player p1 = Bukkit.getPlayer(u1);
            Player p2 = Bukkit.getPlayer(u2);
            if (p1 == null || p2 == null) { queue.remove(0); continue; }

            queue.remove(0);
            queue.remove(0);

            if (plugin.getDuelManager().startDuel(p1, p2, mode) == null) {
                queue.add(0, u2);
                queue.add(0, u1);
                break;
            }
        }
    }

    public void removeFromAll(UUID uuid) {
        elytraQueue.remove(uuid);
        trapQueue.remove(uuid);
    }

    private String modeName(ArenaType t) {
        return t == ArenaType.ELYTRA ? "Elytra PvP" : "Trap PvP";
    }
}
