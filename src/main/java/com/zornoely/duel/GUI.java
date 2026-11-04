package com.zornoely.duel;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public class GUI {
    public static final Component QUEUE_TITLE  = Component.text("ZornoEly PvP Sırası", NamedTextColor.DARK_PURPLE);
    public static final Component INVITE_TITLE = Component.text("Düello Modu Seç", NamedTextColor.DARK_GRAY);

    public static void openQueue(Player p, ZornoElyDuel plugin) {
        Inventory inv = Bukkit.createInventory(null, 27, QUEUE_TITLE);

        int activeMatches = 0;
        for (Arena a : plugin.getArenaManager().getAll()) if (a.isBusy()) activeMatches++;

        QueueManager qm = plugin.getQueueManager();
        boolean inElytra = qm.isInQueue(p.getUniqueId(), ArenaType.ELYTRA);
        boolean inTrap   = qm.isInQueue(p.getUniqueId(), ArenaType.TRAP);
        int elytraSize = qm.getQueue(ArenaType.ELYTRA).size();
        int trapSize   = qm.getQueue(ArenaType.TRAP).size();

        inv.setItem(11, createItem(Material.ELYTRA,
                ChatColor.AQUA + "" + ChatColor.BOLD + "ELYTRA PVP SIRA",
                Arrays.asList(
                        ChatColor.GRAY + "Elitra kullanımı açık.",
                        ChatColor.DARK_GRAY + "------------------------",
                        ChatColor.YELLOW + "Sıradaki: " + ChatColor.WHITE + elytraSize,
                        ChatColor.YELLOW + "Maçtaki Arenalar: " + ChatColor.WHITE + activeMatches,
                        ChatColor.DARK_GRAY + "------------------------",
                        inElytra ? ChatColor.GREEN + "" + ChatColor.BOLD + "[SIRADASIN - Çıkmak için Tıkla]"
                                 : ChatColor.YELLOW + "Sıraya girmek için tıkla!",
                        "",
                        ChatColor.RED + "" + ChatColor.BOLD + "DİKKAT:",
                        ChatColor.RED + "Kendi eşyalarınız ile girersiniz!",
                        ChatColor.RED + "Ölürseniz eşyalarınız gider!")));

        inv.setItem(15, createItem(Material.COBWEB,
                ChatColor.GOLD + "" + ChatColor.BOLD + "TRAP PVP SIRA",
                Arrays.asList(
                        ChatColor.GRAY + "Ağlar ve bloklar açık.",
                        ChatColor.DARK_GRAY + "------------------------",
                        ChatColor.YELLOW + "Sıradaki: " + ChatColor.WHITE + trapSize,
                        ChatColor.YELLOW + "Maçtaki Arenalar: " + ChatColor.WHITE + activeMatches,
                        ChatColor.DARK_GRAY + "------------------------",
                        inTrap ? ChatColor.GREEN + "" + ChatColor.BOLD + "[SIRADASIN - Çıkmak için Tıkla]"
                               : ChatColor.YELLOW + "Sıraya girmek için tıkla!",
                        "",
                        ChatColor.RED + "" + ChatColor.BOLD + "DİKKAT:",
                        ChatColor.RED + "Kendi eşyalarınız ile girersiniz!",
                        ChatColor.RED + "Ölürseniz eşyalarınız gider!")));

        p.openInventory(inv);
    }

    public static void openInvite(Player p, Player target) {
        Inventory inv = Bukkit.createInventory(null, 27, INVITE_TITLE);

        inv.setItem(11, createItem(Material.ELYTRA,
                ChatColor.AQUA + "Elytra PvP Daveti",
                Arrays.asList(
                        ChatColor.GRAY + "- Blok: " + ChatColor.GREEN + "Açık",
                        ChatColor.GRAY + "- Ağ: "   + ChatColor.RED + "Kapalı",
                        ChatColor.GRAY + "- Elytra: " + ChatColor.GREEN + "Açık",
                        "",
                        ChatColor.YELLOW + "Tıkla ve " + target.getName() + " oyuncusuna gönder!")));

        inv.setItem(15, createItem(Material.COBWEB,
                ChatColor.GOLD + "Trap PvP Daveti",
                Arrays.asList(
                        ChatColor.GRAY + "- Blok: " + ChatColor.GREEN + "Açık",
                        ChatColor.GRAY + "- Ağ: "   + ChatColor.GREEN + "Açık",
                        ChatColor.GRAY + "- Elytra: " + ChatColor.RED + "Kapalı",
                        "",
                        ChatColor.YELLOW + "Tıkla ve " + target.getName() + " oyuncusuna gönder!")));

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
