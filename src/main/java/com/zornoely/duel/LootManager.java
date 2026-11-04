package com.zornoely.duel;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class LootManager {
    public static final String LOOT_TITLE = "Ganimet Sandığı";

    private final ZornoElyDuel plugin;
    private final Map<UUID, List<ItemStack>> loots = new HashMap<>();
    private final Map<UUID, Long> expireTimes = new HashMap<>();
    private final Set<UUID> openInventories = new HashSet<>();
    private final int durationSeconds;

    public LootManager(ZornoElyDuel plugin) {
        this.plugin = plugin;
        this.durationSeconds = plugin.getConfig().getInt("settings.loot-duration-seconds", 40);
    }

    public void setLoot(UUID uuid, List<ItemStack> items) {
        List<ItemStack> existing = loots.getOrDefault(uuid, new ArrayList<>());
        existing.addAll(items);
        loots.put(uuid, existing);
        expireTimes.put(uuid, System.currentTimeMillis() + durationSeconds * 1000L);

        Player p = Bukkit.getPlayer(uuid);
        if (p != null && p.isOnline()) {
            p.sendMessage(ChatColor.GREEN + "Ganimet hazır! " + ChatColor.YELLOW + durationSeconds
                    + " saniye içinde /duel sandik ile aç.");
            startCountdown(p);
        }
    }

    private void startCountdown(Player p) {
        UUID uuid = p.getUniqueId();
        new BukkitRunnable() {
            int remaining = durationSeconds;
            @Override
            public void run() {
                if (!p.isOnline() || !loots.containsKey(uuid)) { cancel(); return; }
                if (remaining <= 0) { cancel(); return; }
                p.sendActionBar(Component.text("§e§lGANİMET: §f" + remaining + " §esaniye"));
                remaining--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public boolean hasLoot(UUID uuid) {
        Long t = expireTimes.get(uuid);
        return t != null && System.currentTimeMillis() <= t && loots.containsKey(uuid);
    }

    public void openLoot(Player p) {
        UUID uuid = p.getUniqueId();
        if (!hasLoot(uuid)) {
            p.sendMessage(ChatColor.RED + "Aktif bir ganimet sandığınız yok!");
            return;
        }
        List<ItemStack> items = loots.get(uuid);
        int rows = Math.max(1, Math.min(6, (items.size() + 8) / 9));
        Inventory inv = Bukkit.createInventory(null, rows * 9, LOOT_TITLE);
        for (int i = 0; i < items.size() && i < inv.getSize(); i++) {
            ItemStack it = items.get(i);
            if (it != null && it.getType() != Material.AIR) inv.setItem(i, it);
        }
        p.openInventory(inv);
        openInventories.add(uuid);
    }

    public void onInventoryClose(Player p, Inventory inv) {
        if (inv == null || !LOOT_TITLE.equals(inv.getTitle())) return;
        UUID uuid = p.getUniqueId();
        if (!openInventories.remove(uuid)) return;

        List<ItemStack> remaining = new ArrayList<>();
        for (ItemStack it : inv.getContents()) {
            if (it != null && it.getType() != Material.AIR) remaining.add(it);
        }

        if (remaining.isEmpty()) {
            loots.remove(uuid);
            expireTimes.remove(uuid);
            plugin.sendToHub(p);
        } else {
            loots.put(uuid, remaining);
        }
    }

    public void checkExpired() {
        long now = System.currentTimeMillis();
        List<UUID> expired = new ArrayList<>();
        for (Map.Entry<UUID, Long> e : expireTimes.entrySet()) {
            if (now >= e.getValue() && !openInventories.contains(e.getKey())) {
                expired.add(e.getKey());
            }
        }
        for (UUID uuid : expired) expireLoot(uuid);
    }

    private void expireLoot(UUID uuid) {
        loots.remove(uuid);
        expireTimes.remove(uuid);
        Player p = Bukkit.getPlayer(uuid);
        if (p != null && p.isOnline()) {
            p.sendMessage(ChatColor.RED + "Ganimet süren doldu!");
            plugin.sendToHub(p);
        }
    }

    public void remove(UUID uuid) {
        loots.remove(uuid);
        expireTimes.remove(uuid);
        openInventories.remove(uuid);
    }
}
