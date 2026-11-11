package com.zornoely.duel;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class DuelCommand implements CommandExecutor, TabCompleter {
    private static final List<String> ADMIN_SUBS =
            List.of("create", "delete", "sil", "setspawn1", "setspawn2", "settype", "tp", "forceend", "release");

    private final ZornoElyDuel plugin;

    public DuelCommand(ZornoElyDuel plugin) { this.plugin = plugin; }

    private boolean isAdmin(CommandSender s) {
        return s.hasPermission("zornoely.admin") || s.isOp();
    }

    private boolean denyIfNotAdmin(CommandSender s) {
        if (isAdmin(s)) return false;
        Msg.warn(s, "Yetkin yok!");
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String sub = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "";

        if (!(sender instanceof Player p)) {
            switch (sub) {
                case "forceend", "bitir" -> handleForceEnd(sender, args);
                case "release"           -> handleRelease(sender, args);
                default -> sender.sendMessage("Konsol komutları: /duel forceend <oyuncu> | /duel release <arena>");
            }
            return true;
        }
        if (args.length == 0) {
            Msg.warn(p, "Kullanım: /duel <davet|kabul|reddet|menu|sandik|list>");
            return true;
        }

        switch (sub) {
            case "list"              -> handleList(p);
            case "tp"                -> handleTp(p, args);
            case "settype"           -> handleSetType(p, args);
            case "create"            -> handleCreate(p, args);
            case "delete", "sil"     -> handleDelete(p, args);
            case "setspawn1"         -> handleSetSpawn(p, args, 1);
            case "setspawn2"         -> handleSetSpawn(p, args, 2);
            case "sandik", "sandık"  -> plugin.getLootManager().openLoot(p);
            case "menu", "menü"      -> GUI.openQueue(p, plugin);
            case "davet"             -> handleDavet(p, args);
            case "kabul"             -> handleKabul(p, args);
            case "reddet"            -> handleReddet(p, args);
            case "forceend", "bitir" -> handleForceEnd(p, args);
            case "release"           -> handleRelease(p, args);
            default -> Msg.warn(p, "Bilinmeyen komut: " + args[0]);
        }
        return true;
    }

    private void handleList(Player p) {
        if (plugin.getArenaManager().getAll().isEmpty()) {
            Msg.warn(p, "Hiç arena yok!");
            return;
        }
        Msg.info(p, "Arena listesi:");
        boolean admin = isAdmin(p);
        for (Arena a : plugin.getArenaManager().getAll()) {
            Component line = Component.text("  ▪ ", NamedTextColor.DARK_GRAY)
                    .append(Component.text(a.getName(), NamedTextColor.WHITE))
                    .append(Component.text(" [", NamedTextColor.DARK_GRAY))
                    .append(a.isBusy() ? Component.text("DOLU", NamedTextColor.RED)
                                       : Component.text("BOŞ", NamedTextColor.GREEN))
                    .append(Component.text("]", NamedTextColor.DARK_GRAY))
                    .append(Component.text(" (Tip: ", NamedTextColor.GRAY))
                    .append(Component.text(a.getType().name(), NamedTextColor.YELLOW))
                    .append(Component.text(")", NamedTextColor.GRAY));
            if (admin) {
                line = line.append(Component.text(" [IŞINLAN]", NamedTextColor.GREEN)
                        .clickEvent(ClickEvent.runCommand("/duel tp " + a.getName()))
                        .hoverEvent(HoverEvent.showText(Component.text("Arenaya ışınlan", NamedTextColor.GRAY))));
            }
            p.sendMessage(line);
        }
    }

    private void handleTp(Player p, String[] args) {
        if (denyIfNotAdmin(p)) return;
        if (args.length < 2) { Msg.warn(p, "Kullanım: /duel tp <arena>"); return; }
        Arena a = plugin.getArenaManager().get(args[1]);
        if (a == null) { Msg.warn(p, "Arena bulunamadı!"); return; }
        Location l = a.getSpawn1();
        if (l == null) { Msg.warn(p, "Spawn 1 ayarlanmamış veya dünyası yüklü değil!"); return; }
        p.teleport(l);
        Msg.ok(p, "Işınlandın: " + a.getName());
    }

    private void handleSetType(Player p, String[] args) {
        if (denyIfNotAdmin(p)) return;
        if (args.length < 3) { Msg.warn(p, "Kullanım: /duel settype <arena> <elytra|trap|hepsi>"); return; }
        Arena a = plugin.getArenaManager().get(args[1]);
        if (a == null) { Msg.warn(p, "Arena bulunamadı!"); return; }
        ArenaType t = ArenaType.fromString(args[2]);
        if (t == null) { Msg.warn(p, "Geçersiz tip! Kullan: elytra, trap veya hepsi"); return; }
        a.setType(t);
        plugin.getArenaManager().save();
        Msg.ok(p, "Arena " + a.getName() + " tipi: " + t);
    }

    private void handleCreate(Player p, String[] args) {
        if (denyIfNotAdmin(p)) return;
        if (args.length < 2) { Msg.warn(p, "Kullanım: /duel create <arena>"); return; }
        if (!ArenaManager.isValidName(args[1])) {
            Msg.warn(p, "Arena adı sadece harf, rakam, _ ve - içerebilir (en fazla 32 karakter).");
            return;
        }
        Arena created = plugin.getArenaManager().create(args[1]);
        if (created == null) {
            Msg.warn(p, "Bu isimde arena zaten var!");
            return;
        }
        plugin.getArenaManager().save();
        Msg.ok(p, "Arena oluşturuldu: " + created.getName() + " (Tip: HEPSI)");
    }

    private void handleDelete(Player p, String[] args) {
        if (denyIfNotAdmin(p)) return;
        if (args.length < 2) { Msg.warn(p, "Kullanım: /duel delete <arena>"); return; }
        Arena a = plugin.getArenaManager().get(args[1]);
        if (a == null) { Msg.warn(p, "Arena bulunamadı!"); return; }
        if (a.isBusy()) { Msg.warn(p, "Arenada şu an maç var, silinemez!"); return; }
        plugin.getArenaManager().delete(a.getName());
        plugin.getArenaManager().save();
        Msg.ok(p, "Arena silindi: " + a.getName());
    }

    private void handleSetSpawn(Player p, String[] args, int num) {
        if (denyIfNotAdmin(p)) return;
        if (args.length < 2) { Msg.warn(p, "Kullanım: /duel setspawn" + num + " <arena>"); return; }
        Arena a = plugin.getArenaManager().get(args[1]);
        if (a == null) { Msg.warn(p, "Arena bulunamadı!"); return; }
        if (num == 1) a.setSpawn1(p.getLocation());
        else          a.setSpawn2(p.getLocation());
        plugin.getArenaManager().save();
        Msg.ok(p, "Spawn " + num + " ayarlandı: " + a.getName());
    }

    private void handleDavet(Player p, String[] args) {
        if (args.length < 2) { Msg.warn(p, "Kullanım: /duel davet <oyuncu>"); return; }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) { Msg.warn(p, "Oyuncu bulunamadı!"); return; }
        if (target.equals(p)) { Msg.warn(p, "Kendine davet atamazsın!"); return; }
        if (plugin.getDuelManager().isInDuel(target.getUniqueId())) {
            Msg.warn(p, "Bu oyuncu şu an düelloda!");
            return;
        }
        if (plugin.getClanHook().isSameClan(p, target)) {
            Msg.warn(p, "Aynı klandan bir oyuncuya düello daveti gönderemezsin!");
            return;
        }
        if (plugin.getLootManager().hasLoot(p.getUniqueId())) {
            Msg.warn(p, "Önce ganimetini almalısın! (/duel sandik)");
            return;
        }
        p.setMetadata("duel_target", new FixedMetadataValue(plugin, target.getUniqueId().toString()));
        GUI.openInvite(p, target);
    }

    private void handleKabul(Player p, String[] args) {
        if (args.length < 2) { Msg.warn(p, "Kullanım: /duel kabul <oyuncu>"); return; }
        Player inviter = Bukkit.getPlayerExact(args[1]);
        if (inviter == null) { Msg.warn(p, "Oyuncu offline!"); return; }
        InviteManager.Invite inv = plugin.getInviteManager().getInvite(inviter.getUniqueId());
        if (inv == null || !inv.target.equals(p.getUniqueId())) {
            Msg.warn(p, "Aktif davet yok!");
            return;
        }
        if (plugin.getClanHook().isSameClan(inviter, p)) {
            plugin.getInviteManager().removeInvite(inviter.getUniqueId());
            Msg.warn(p, "Aynı klandan olduğunuz için bu düelloyu kabul edemezsin!");
            return;
        }
        DuelManager dm = plugin.getDuelManager();
        if (dm.isInDuel(inviter.getUniqueId())) {
            plugin.getInviteManager().removeInvite(inviter.getUniqueId());
            Msg.warn(p, "Davet eden oyuncu şu an düelloda!");
            return;
        }
        if (plugin.getLootManager().hasLoot(p.getUniqueId())) {
            Msg.warn(p, "Önce ganimetini almalısın! (/duel sandik)");
            return;
        }
        if (plugin.getLootManager().hasLoot(inviter.getUniqueId())) {
            Msg.warn(p, "Davet eden oyuncu şu an ganimetini topluyor, birazdan tekrar dene.");
            return;
        }
        if (plugin.getArenaManager().findAvailable(inv.mode) == null) {
            Msg.warn(p, "Şu an boş arena yok, biraz sonra tekrar dene.");
            return;
        }
        plugin.getInviteManager().removeInvite(inviter.getUniqueId());
        if (dm.startDuel(inviter, p, inv.mode) == null) {
            Msg.warn(p, "Düello başlatılamadı!");
            Msg.warn(inviter, "Düello başlatılamadı!");
        }
    }

    private void handleReddet(Player p, String[] args) {
        if (args.length < 2) { Msg.warn(p, "Kullanım: /duel reddet <oyuncu>"); return; }
        Player inviter = Bukkit.getPlayerExact(args[1]);
        if (inviter == null) { Msg.warn(p, "Oyuncu offline!"); return; }
        InviteManager.Invite inv = plugin.getInviteManager().getInvite(inviter.getUniqueId());
        if (inv == null || !inv.target.equals(p.getUniqueId())) {
            Msg.warn(p, "Aktif davet yok!");
            return;
        }
        plugin.getInviteManager().removeInvite(inviter.getUniqueId());
        Msg.ok(p, "Daveti reddettin.");
        Msg.warn(inviter, p.getName() + " davetini reddetti.");
    }

    private void handleForceEnd(CommandSender s, String[] args) {
        if (denyIfNotAdmin(s)) return;
        if (args.length < 2) { Msg.warn(s, "Kullanım: /duel forceend <oyuncu>"); return; }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) { Msg.warn(s, "Oyuncu bulunamadı!"); return; }
        Duel duel = plugin.getDuelManager().getDuel(target.getUniqueId());
        if (duel == null) { Msg.warn(s, "Bu oyuncu düelloda değil!"); return; }

        UUID oppId = duel.getOpponent(target.getUniqueId());
        Player opp = oppId != null ? Bukkit.getPlayer(oppId) : null;
        plugin.getDuelManager().endDuel(duel, null, "FORCE");

        String reason = "Düello bir yönetici tarafından sonlandırıldı.";
        Msg.draw(target, reason);
        plugin.sendToHub(target);
        if (opp != null && opp.isOnline()) {
            Msg.draw(opp, reason);
            plugin.sendToHub(opp);
        }
        Msg.ok(s, "Düello sonlandırıldı.");
    }

    private void handleRelease(CommandSender s, String[] args) {
        if (denyIfNotAdmin(s)) return;
        if (args.length < 2) { Msg.warn(s, "Kullanım: /duel release <arena>"); return; }
        Arena a = plugin.getArenaManager().get(args[1]);
        if (a == null) { Msg.warn(s, "Arena bulunamadı!"); return; }
        plugin.getArenaManager().release(a.getName());
        Msg.ok(s, "Arena boşa çıkarıldı: " + a.getName());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> out = new ArrayList<>();
        boolean admin = isAdmin(sender);

        if (args.length == 1) {
            List<String> opts = new ArrayList<>(List.of("davet", "kabul", "reddet", "menu", "sandik", "list"));
            if (admin) opts.addAll(ADMIN_SUBS);
            String typed = args[0].toLowerCase(Locale.ROOT);
            for (String s : opts) if (s.startsWith(typed)) out.add(s);
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            String typed = args[1].toLowerCase(Locale.ROOT);
            if (List.of("davet", "kabul", "reddet").contains(sub) || (admin && sub.equals("forceend"))) {
                for (Player pl : Bukkit.getOnlinePlayers())
                    if (pl.getName().toLowerCase(Locale.ROOT).startsWith(typed)) out.add(pl.getName());
            } else if (admin && ADMIN_SUBS.contains(sub)) {
                for (String name : plugin.getArenaManager().getNames())
                    if (name.toLowerCase(Locale.ROOT).startsWith(typed)) out.add(name);
            }
        } else if (args.length == 3 && admin && args[0].equalsIgnoreCase("settype")) {
            String typed = args[2].toLowerCase(Locale.ROOT);
            for (String s : List.of("elytra", "trap", "hepsi"))
                if (s.startsWith(typed)) out.add(s);
        }
        return out;
    }
    }
