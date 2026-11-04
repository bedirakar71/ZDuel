package com.zornoely.duel;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.*;

public class DuelCommand implements CommandExecutor, TabCompleter {
    private final ZornoElyDuel plugin;

    public DuelCommand(ZornoElyDuel plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(ChatColor.RED + "Sadece oyuncular kullanabilir.");
            return true;
        }
        if (args.length == 0) {
            p.sendMessage(ChatColor.RED + "Kullanım: /duel <davet|kabul|reddet|menu|sandik|berabere|list>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "berabere" -> handleBerabere(p);
            case "list"     -> handleList(p);
            case "tp"       -> handleTp(p, args);
            case "settype"  -> handleSetType(p, args);
            case "create"   -> handleCreate(p, args);
            case "delete", "sil" -> handleDelete(p, args);
            case "setspawn1" -> handleSetSpawn(p, args, 1);
            case "setspawn2" -> handleSetSpawn(p, args, 2);
            case "sandik"   -> plugin.getLootManager().openLoot(p);
            case "menu"     -> GUI.openQueue(p, plugin);
            case "davet"    -> handleDavet(p, args);
            case "kabul"    -> handleKabul(p, args);
            case "reddet"   -> handleReddet(p, args);
            default -> p.sendMessage(ChatColor.RED + "Bilinmeyen komut: " + args[0]);
        }
        return true;
    }

    private void handleBerabere(Player p) {
        Duel duel = plugin.getDuelManager().getDuel(p.getUniqueId());
        if (duel == null) {
            p.sendMessage(ChatColor.RED + "Aktif bir düelloda değilsiniz!");
            return;
        }
        UUID oppId = duel.getOpponent(p.getUniqueId());
        Player opp = oppId != null ? Bukkit.getPlayer(oppId) : null;

        plugin.getDuelManager().endDuel(duel, null, "DRAW");

        String msg = ChatColor.YELLOW + "Düello berabere sonlandırıldı!";
        p.sendMessage(msg);
        if (opp != null && opp.isOnline()) opp.sendMessage(msg);

        plugin.sendToHub(p);
        if (opp != null && opp.isOnline()) plugin.sendToHub(opp);
    }

    private void handleList(Player p) {
        if (plugin.getArenaManager().getAll().isEmpty()) {
            p.sendMessage(ChatColor.RED + "Hiç arena yok!");
            return;
        }
        p.sendMessage(ChatColor.YELLOW + "--- Arena Listesi ---");
        boolean admin = p.hasPermission("zornoely.admin");
        for (Arena a : plugin.getArenaManager().getAll()) {
            String status = a.isBusy() ? ChatColor.RED + "DOLU" : ChatColor.GREEN + "BOŞ";
            String tp = admin ? ChatColor.GREEN + " [IŞINLAN]" : "";
            p.sendMessage(ChatColor.GRAY + "- " + ChatColor.WHITE + a.getName()
                    + ChatColor.DARK_GRAY + " [" + status + ChatColor.DARK_GRAY + "]"
                    + ChatColor.GRAY + " (Tip: " + ChatColor.YELLOW + a.getType() + ChatColor.GRAY + ")"
                    + tp);
        }
    }

    private void handleTp(Player p, String[] args) {
        if (!p.hasPermission("zornoely.admin")) { p.sendMessage(ChatColor.RED + "Yetkin yok!"); return; }
        if (args.length < 2) { p.sendMessage(ChatColor.RED + "Kullanım: /duel tp <arena>"); return; }
        Arena a = plugin.getArenaManager().get(args[1]);
        if (a == null) { p.sendMessage(ChatColor.RED + "Arena bulunamadı!"); return; }
        if (a.getSpawn1() == null) { p.sendMessage(ChatColor.RED + "Spawn 1 ayarlanmamış!"); return; }
        p.teleport(a.getSpawn1());
        p.sendMessage(ChatColor.GREEN + "Işınlandın: " + a.getName());
    }

    private void handleSetType(Player p, String[] args) {
        if (!p.hasPermission("zornoely.admin")) { p.sendMessage(ChatColor.RED + "Yetkin yok!"); return; }
        if (args.length < 3) { p.sendMessage(ChatColor.RED + "Kullanım: /duel settype <arena> <elytra|trap|hepsi>"); return; }
        Arena a = plugin.getArenaManager().get(args[1]);
        if (a == null) { p.sendMessage(ChatColor.RED + "Arena bulunamadı!"); return; }
        ArenaType t = ArenaType.parse(args[2]);
        a.setType(t);
        plugin.getArenaManager().save();
        p.sendMessage(ChatColor.GREEN + "Arena " + a.getName() + " tipi: " + t);
    }

    private void handleCreate(Player p, String[] args) {
        if (!p.hasPermission("zornoely.admin")) { p.sendMessage(ChatColor.RED + "Yetkin yok!"); return; }
        if (args.length < 2) { p.sendMessage(ChatColor.RED + "Kullanım: /duel create <arena>"); return; }
        if (plugin.getArenaManager().create(args[1]) == null) {
            p.sendMessage(ChatColor.RED + "Bu isimde arena zaten var!");
            return;
        }
        plugin.getArenaManager().save();
        p.sendMessage(ChatColor.GREEN + "Arena oluşturuldu: " + args[1] + " (Tip: HEPSI)");
    }

    private void handleDelete(Player p, String[] args) {
        if (!p.hasPermission("zornoely.admin")) { p.sendMessage(ChatColor.RED + "Yetkin yok!"); return; }
        if (args.length < 2) { p.sendMessage(ChatColor.RED + "Kullanım: /duel delete <arena>"); return; }
        if (!plugin.getArenaManager().delete(args[1])) {
            p.sendMessage(ChatColor.RED + "Arena bulunamadı!");
            return;
        }
        plugin.getArenaManager().save();
        p.sendMessage(ChatColor.GREEN + "Arena silindi: " + args[1]);
    }

    private void handleSetSpawn(Player p, String[] args, int num) {
        if (!p.hasPermission("zornoely.admin")) { p.sendMessage(ChatColor.RED + "Yetkin yok!"); return; }
        if (args.length < 2) { p.sendMessage(ChatColor.RED + "Kullanım: /duel setspawn" + num + " <arena>"); return; }
        Arena a = plugin.getArenaManager().get(args[1]);
        if (a == null) { p.sendMessage(ChatColor.RED + "Arena bulunamadı!"); return; }
        if (num == 1) a.setSpawn1(p.getLocation());
        else          a.setSpawn2(p.getLocation());
        plugin.getArenaManager().save();
        p.sendMessage(ChatColor.GREEN + "Spawn " + num + " ayarlandı: " + a.getName());
    }

    private void handleDavet(Player p, String[] args) {
        if (args.length < 2) { p.sendMessage(ChatColor.RED + "Kullanım: /duel davet <oyuncu>"); return; }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { p.sendMessage(ChatColor.RED + "Oyuncu bulunamadı!"); return; }
        if (target.equals(p)) { p.sendMessage(ChatColor.RED + "Kendine davet atamazsın!"); return; }
        p.setMetadata("duel_target", new FixedMetadataValue(plugin, target.getUniqueId().toString()));
        GUI.openInvite(p, target);
    }

    private void handleKabul(Player p, String[] args) {
        if (args.length < 2) { p.sendMessage(ChatColor.RED + "Kullanım: /duel kabul <oyuncu>"); return; }
        Player sender = Bukkit.getPlayer(args[1]);
        if (sender == null) { p.sendMessage(ChatColor.RED + "Oyuncu offline!"); return; }
        InviteManager.Invite inv = plugin.getInviteManager().getInvite(sender.getUniqueId());
        if (inv == null || !inv.target.equals(p.getUniqueId())) {
            p.sendMessage(ChatColor.RED + "Aktif davet yok!");
            return;
        }
        plugin.getInviteManager().removeInvite(sender.getUniqueId());
        if (plugin.getDuelManager().startDuel(sender, p, inv.mode) == null) {
            p.sendMessage(ChatColor.RED + "Düello başlatılamadı!");
        }
    }

    private void handleReddet(Player p, String[] args) {
        if (args.length < 2) { p.sendMessage(ChatColor.RED + "Kullanım: /duel reddet <oyuncu>"); return; }
        Player sender = Bukkit.getPlayer(args[1]);
        if (sender == null) { p.sendMessage(ChatColor.RED + "Oyuncu offline!"); return; }
        InviteManager.Invite inv = plugin.getInviteManager().getInvite(sender.getUniqueId());
        if (inv == null || !inv.target.equals(p.getUniqueId())) {
            p.sendMessage(ChatColor.RED + "Aktif davet yok!");
            return;
        }
        plugin.getInviteManager().removeInvite(sender.getUniqueId());
        p.sendMessage(ChatColor.GREEN + "Daveti reddettin.");
        sender.sendMessage(ChatColor.RED + p.getName() + " davetini reddetti.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> out = new ArrayList<>();
        boolean admin = sender.hasPermission("zornoely.admin");

        if (args.length == 1) {
            List<String> opts = new ArrayList<>(List.of("davet", "kabul", "reddet", "menu", "sandik", "berabere", "list"));
            if (admin) opts.addAll(List.of("create", "delete", "sil", "setspawn1", "setspawn2", "settype", "tp"));
            for (String s : opts) if (s.startsWith(args[0].toLowerCase())) out.add(s);
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (List.of("davet", "kabul", "reddet").contains(sub)) {
                for (Player p : Bukkit.getOnlinePlayers())
                    if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) out.add(p.getName());
            } else if (admin && List.of("create", "delete", "sil", "setspawn1", "setspawn2", "settype", "tp").contains(sub)) {
                for (String name : plugin.getArenaManager().getNames())
                    if (name.toLowerCase().startsWith(args[1].toLowerCase())) out.add(name);
            }
        } else if (args.length == 3 && admin && args[0].equalsIgnoreCase("settype")) {
            for (String s : List.of("elytra", "trap", "hepsi"))
                if (s.startsWith(args[2].toLowerCase())) out.add(s);
        }
        return out;
    }
}
