package com.zornoely.duel;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class QueueManager {
    private final ZornoElyDuel plugin;
    private final List<UUID> elytraQueue = new ArrayList<>();
    private final List<UUID> trapQueue = new ArrayList<>();
    private final Set<UUID> warnedClanBlock = new HashSet<>();

    public QueueManager(ZornoElyDuel plugin) { this.plugin = plugin; }

    public List<UUID> getQueue(ArenaType mode) {
        return mode == ArenaType.ELYTRA ? elytraQueue : trapQueue;
    }

    public boolean isInQueue(UUID uuid, ArenaType mode) {
        return getQueue(mode).contains(uuid);
    }

    public void toggleQueue(Player p, ArenaType mode) {
        UUID id = p.getUniqueId();
        List<UUID> queue = getQueue(mode);

        if (queue.contains(id)) {
            queue.remove(id);
            Msg.warn(p, mode.getDisplayName() + " sırasından çıktın.");
            return;
        }
        if (plugin.getDuelManager().isInDuel(id)) {
            Msg.warn(p, "Düellodayken sıraya giremezsin!");
            return;
        }
        if (plugin.getLootManager().hasLoot(id)) {
            Msg.warn(p, "Önce ganimetini almalısın! (/duel sandik)");
            return;
        }

        getQueue(mode == ArenaType.ELYTRA ? ArenaType.TRAP : ArenaType.ELYTRA).remove(id);
        queue.add(id);
        Msg.ok(p, mode.getDisplayName() + " sırasına girdin, rakip aranıyor...");
        tryMatch(mode);

        if (queue.contains(id) && queue.size() >= 2) {
            Msg.info(p, "Tüm arenalar dolu veya uygun rakip yok, biri boşalınca düello otomatik başlayacak.");
        }
    }

    public void processQueues() {
        tryMatch(ArenaType.ELYTRA);
        tryMatch(ArenaType.TRAP);
    }

    private void tryMatch(ArenaType mode) {
        List<UUID> queue = getQueue(mode);
        if (queue.isEmpty()) return;

        queue.removeIf(id -> {
            Player pl = Bukkit.getPlayer(id);
            return pl == null || !pl.isOnline()
                    || plugin.getDuelManager().isInDuel(id)
                    || plugin.getLootManager().hasLoot(id);
        });

        while (queue.size() >= 2 && plugin.getArenaManager().findAvailable(mode) != null) {
            UUID u1 = queue.remove(0);
            Player p1 = Bukkit.getPlayer(u1);
            if (p1 == null) continue;

            int partnerIdx = -1;
            for (int i = 0; i < queue.size(); i++) {
                Player cand = Bukkit.getPlayer(queue.get(i));
                if (cand == null) continue;
                if (plugin.getClanHook().isSameClan(p1, cand)) continue;
                partnerIdx = i;
                break;
            }

            if (partnerIdx == -1) {
                if (p1.isOnline() && warnedClanBlock.add(u1)) {
                    Msg.warn(p1, "Sıradaki diğer oyuncular aynı klandan; eşleşemiyorsun. Farklı bir oyuncu bekleniyor...");
                }
                queue.add(0, u1);
                break;
            }

            UUID u2 = queue.remove(partnerIdx);
            Player p2 = Bukkit.getPlayer(u2);
            if (p2 == null) { queue.add(0, u1); continue; }

            if (plugin.getDuelManager().startDuel(p1, p2, mode) == null) {
                String fail = "Düello başlatılamadı, lütfen tekrar sıraya gir.";
                Msg.warn(p1, fail);
                Msg.warn(p2, fail);
            }
        }
    }

    public void removeFromAll(UUID uuid) {
        elytraQueue.remove(uuid);
        trapQueue.remove(uuid);
        warnedClanBlock.remove(uuid);
    }
}
