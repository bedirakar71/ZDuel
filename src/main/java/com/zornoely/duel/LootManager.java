package com.zornoely.duel;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class LootManager {
    public static final Component LOOT_TITLE = Component.text("» Ganimet Sandığı", NamedTextColor.DARK_RED);
    private static final int MAX_SLOTS = 54;
    private static final int BAR_SEGMENTS = 12;

    public static class LootHolder implements InventoryHolder {
        private Inventory inventory;
        void setInventory(Inventory inventory) { this.inventory = inventory; }
        @Override public Inventory getInventory() { return inventory; }
    }

    private final ZornoElyDuel plugin;
    private final Map<UUID, List<ItemStack>> loots = new HashMap<>();
    private final Map<UUID, Long> expireTimes = new HashMap<>();
    private final Map<UUID, String> heldArenas = new HashMap<>();   // ganimet bitene kadar meşgul tutulan arena
    private final Set<UUID> openInventories = new HashSet<>();
    private final int durationSeconds;

    public LootManager(ZornoElyDuel plugin) {
        this.plugin = plugin;
        this.durationSeconds = Math.max(1, plugin.getConfig().getInt("settings.loot-duration-seconds", 20));
    }

    public int getDurationSeconds() { return durationSeconds; }

    /** Kazananın mesajını Msg.win gönderir; burada sadece ganimet kaydedilir ve geri sayım başlar. */
    public void setLoot(UUID uuid, List<ItemStack> items, String arenaName) {
        List<ItemStack> list = loots.computeIfAbsent(uuid, k -> new ArrayList<>());
        list.addAll(items);
        expireTimes.put(uuid, System.currentTimeMillis() + durationSeconds * 1000L);

        if (arenaName != null) {
            String old = heldArenas.put(uuid, arenaName);
            if (old != null && !old.equals(arenaName)) plugin.getArenaManager().release(old);
        }

        Player p = Bukkit.getPlayer(uuid);
        if (p != null && p.isOnline()) sendBar(p, durationSeconds);
    }

    public boolean hasLoot(UUID uuid) {
        Long t = expireTimes.get(uuid);
        return t != null && System.currentTimeMillis() < t && loots.containsKey(uuid);
    }

    public void openLoot(Player p) {
        UUID uuid = p.getUniqueId();
        if (!hasLoot(uuid)) {
            Msg.warn(p, "Aktif bir ganimet sandığın yok!");
            return;
        }
        if (openInventories.contains(uuid)) return;

        List<ItemStack> items = loots.get(uuid);
        if (items.size() > MAX_SLOTS) {
            // Sandığa sığmayan fazlalık kaybolmasın: doğrudan envantere ver
            List<ItemStack> extra = new ArrayList<>(items.subList(MAX_SLOTS, items.size()));
            items.subList(MAX_SLOTS, items.size()).clear();
            giveOrDrop(p, extra);
            Msg.info(p, "Sandığa sığmayan eşyalar doğrudan envanterine verildi.");
        }

        int rows = Math.max(1, (items.size() + 8) / 9);
        LootHolder holder = new LootHolder();
        Inventory inv = Bukkit.createInventory(holder, rows * 9, LOOT_TITLE);
        holder.setInventory(inv);
        for (int i = 0; i < items.size(); i++) inv.setItem(i, items.get(i));

        if (p.openInventory(inv) != null) openInventories.add(uuid);
    }

    public void onInventoryClose(Player p, Inventory inv) {
        UUID uuid = p.getUniqueId();
        if (!openInventories.remove(uuid)) return;
        if (!loots.containsKey(uuid)) return;

        List<ItemStack> remaining = new ArrayList<>();
        for (ItemStack it : inv.getContents()) {
            if (it != null && it.getType() != Material.AIR) remaining.add(it);
        }

        if (remaining.isEmpty()) {
            finish(uuid);
            Msg.lootTaken(p);
            plugin.sendToHub(p);
        } else {
            loots.put(uuid, remaining);
        }
    }

    /** Her saniye çağrılır: süresi dolanları kapatır, kalanlara geri sayımı gösterir. */
    public void tick() {
        if (expireTimes.isEmpty()) return;
        long now = System.currentTimeMillis();
        for (UUID uuid : new ArrayList<>(expireTimes.keySet())) {
            Long t = expireTimes.get(uuid);
            if (t == null) continue;
            Player p = Bukkit.getPlayer(uuid);
            if (now >= t) {
                expireLoot(uuid, p);
            } else if (p != null && p.isOnline()) {
                sendBar(p, (int) Math.ceil((t - now) / 1000.0));
            }
        }
    }

    private void expireLoot(UUID uuid, Player p) {
        boolean wasOpen = openInventories.contains(uuid);
        finish(uuid);   // openInventories'den de çıkarır, böylece close eventi bir şey yapmaz
        if (p != null && p.isOnline()) {
            if (wasOpen) p.closeInventory();   // açık sandık süre bitince kapanır, kalan eşyalar gider
            Msg.lootExpired(p);
            plugin.sendToHub(p);
        }
    }

    /** Ganimet sürecini bitirir ve tutulan arenayı boşa çıkarır. */
    private void finish(UUID uuid) {
        loots.remove(uuid);
        expireTimes.remove(uuid);
        openInventories.remove(uuid);
        String arena = heldArenas.remove(uuid);
        if (arena != null) plugin.getArenaManager().release(arena);
    }

    /** Oyuncu çıkınca çağrılır. */
    public void remove(UUID uuid) {
        finish(uuid);
    }

    /** Eklenti kapanırken bekleyen ganimetleri oyunculara ver (kaybolmasın). */
    public void flushAll() {
        for (UUID uuid : new ArrayList<>(loots.keySet())) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && openInventories.contains(uuid)) p.closeInventory();   // kalanı loots'a yazar
            List<ItemStack> items = loots.get(uuid);
            if (p != null && p.isOnline()) {
                if (items != null) giveOrDrop(p, items);
                Msg.info(p, "Sunucu kapanıyor, ganimetin envanterine verildi.");
                Location spawn = plugin.getFallbackSpawn();
                if (spawn != null) p.teleport(spawn);   // arenada kalmasınlar
            }
            finish(uuid);
        }
    }

    private void giveOrDrop(Player p, List<ItemStack> items) {
        for (ItemStack it : items) {
            if (it == null || it.getType() == Material.AIR) continue;
            Map<Integer, ItemStack> left = p.getInventory().addItem(it);
            for (ItemStack rest : left.values()) {
                p.getWorld().dropItemNaturally(p.getLocation(), rest);
            }
        }
    }

    /** Action bar: ilerleme çubuklu geri sayım (yeşil → sarı → kırmızı). */
    private void sendBar(Player p, int secondsLeft) {
        int filled = (int) Math.ceil(BAR_SEGMENTS * (double) secondsLeft / durationSeconds);
        filled = Math.max(0, Math.min(BAR_SEGMENTS, filled));
        NamedTextColor color = filled > BAR_SEGMENTS * 2 / 3 ? NamedTextColor.GREEN
                : filled > BAR_SEGMENTS / 3 ? NamedTextColor.YELLOW : NamedTextColor.RED;
        p.sendActionBar(Component.text("GANİMET ", NamedTextColor.GOLD, TextDecoration.BOLD)
                .append(Component.text("█".repeat(filled), color))
                .append(Component.text("░".repeat(BAR_SEGMENTS - filled), NamedTextColor.DARK_GRAY))
                .append(Component.text(" " + secondsLeft + " sn", NamedTextColor.WHITE)));
    }
}
