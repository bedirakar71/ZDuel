package com.zornoely.duel;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class GUI {
    public static final Component QUEUE_TITLE  = Component.text("» PvP Sırası", NamedTextColor.DARK_GRAY);
    public static final Component INVITE_TITLE = Component.text("» Düello Modu Seç", NamedTextColor.DARK_GRAY);

    public static class DuelHolder implements InventoryHolder {
        private final String type;
        private Inventory inventory;
        public DuelHolder(String type) { this.type = type; }
        public String getType() { return type; }
        void setInventory(Inventory inventory) { this.inventory = inventory; }
        @Override public Inventory getInventory() { return inventory; }
    }

    private static String rule(String label, boolean open) {
        return ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + label + ": "
                + (open ? ChatColor.GREEN + "Açık" : ChatColor.RED + "Kapalı");
    }

    private static String stat(String label, ChatColor color, int value) {
        return ChatColor.DARK_GRAY + "▪ " + ChatColor.GRAY + label + ": " + color + value;
    }

    public static void openQueue(Player p, ZornoElyDuel plugin) {
        DuelHolder holder = new DuelHolder("queue");
        Inventory inv = Bukkit.createInventory(holder, 27, QUEUE_TITLE);
        holder.setInventory(inv);

        QueueManager qm = plugin.getQueueManager();
        DuelManager dm = plugin.getDuelManager();
        boolean inElytra = qm.isInQueue(p.getUniqueId(), ArenaType.ELYTRA);
        boolean inTrap   = qm.isInQueue(p.getUniqueId(), ArenaType.TRAP);

        List<String> elytraLore = new ArrayList<>();
        elytraLore.add(rule("Elytra kullanımı", true));
        elytraLore.add(rule("Blok koyma", false));
        elytraLore.add(rule("Düelloda komut", false));
        elytraLore.add("");
        elytraLore.add(stat("Sıradaki Oyuncu", ChatColor.AQUA, qm.getQueue(ArenaType.ELYTRA).size()));
        elytraLore.add(stat("Aktif Maç", ChatColor.AQUA, dm.countActive(ArenaType.ELYTRA)));
        elytraLore.add("");
        elytraLore.add(inElytra ? ChatColor.RED + "» Çıkmak için tıkla!" : ChatColor.YELLOW + "» Sıraya girmek için tıkla!");
        inv.setItem(11, createItem(Material.ELYTRA, ChatColor.AQUA + "" + ChatColor.BOLD + "ELYTRA PVP", elytraLore));

        List<String> trapLore = new ArrayList<>();
        trapLore.add(rule("Ağ ve Blok", true));
        trapLore.add(rule("Elytra", true));
        trapLore.add(rule("Düelloda komut", false));
        trapLore.add("");
        trapLore.add(stat("Sıradaki Oyuncu", ChatColor.GOLD, qm.getQueue(ArenaType.TRAP).size()));
        trapLore.add(stat("Aktif Maç", ChatColor.GOLD, dm.countActive(ArenaType.TRAP)));
        trapLore.add("");
        trapLore.add(inTrap ? ChatColor.RED + "» Çıkmak için tıkla!" : ChatColor.YELLOW + "» Sıraya girmek için tıkla!");
        inv.setItem(15, createItem(Material.COBWEB, ChatColor.GOLD + "" + ChatColor.BOLD + "TRAP PVP", trapLore));

        p.openInventory(inv);
    }

    public static void openInvite(Player p, Player target) {
        DuelHolder holder = new DuelHolder("invite");
        Inventory inv = Bukkit.createInventory(holder, 27, INVITE_TITLE);
        holder.setInventory(inv);

        List<String> elytraLore = new ArrayList<>();
        elytraLore.add(rule("Blok", false));
        elytraLore.add(rule("Ağ", false));
        elytraLore.add(rule("Elytra", true));
        elytraLore.add(rule("Düelloda komut", false));
        elytraLore.add("");
        elytraLore.add(ChatColor.YELLOW + "Tıkla ve " + target.getName() + " oyuncusuna gönder!");
        inv.setItem(11, createItem(Material.ELYTRA, ChatColor.AQUA + "" + ChatColor.BOLD + "Elytra PvP Daveti", elytraLore));

        List<String> trapLore = new ArrayList<>();
        trapLore.add(rule("Blok", true));
        trapLore.add(rule("Ağ", true));
        trapLore.add(rule("Elytra", true));
        trapLore.add(rule("Düelloda komut", false));
        trapLore.add("");
        trapLore.add(ChatColor.YELLOW + "Tıkla ve " + target.getName() + " oyuncusuna gönder!");
        inv.setItem(15, createItem(Material.COBWEB, ChatColor.GOLD + "" + ChatColor.BOLD + "Trap PvP Daveti", trapLore));

        p.openInventory(inv);
    }

    private static ItemStack createItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
