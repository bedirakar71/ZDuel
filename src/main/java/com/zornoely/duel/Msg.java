package com.zornoely.duel;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;

public final class Msg {
    private Msg() { }

    public static final String PREFIX =
            ChatColor.DARK_GRAY + "[" + ChatColor.RED + "Düello" + ChatColor.DARK_GRAY + "] ";
    private static final String LINE =
            ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH
                    + "                                        " + ChatColor.RESET;

    public static void info(CommandSender s, String text) { s.sendMessage(PREFIX + ChatColor.GRAY + text); }
    public static void ok(CommandSender s, String text)   { s.sendMessage(PREFIX + ChatColor.GREEN + text); }
    public static void warn(CommandSender s, String text) { s.sendMessage(PREFIX + ChatColor.RED + text); }

    private static void box(Player p, String... lines) {
        p.sendMessage(LINE);
        for (String l : lines) p.sendMessage(l);
        p.sendMessage(LINE);
    }

    private static void title(Player p, String title, NamedTextColor color, String subtitle) {
        p.showTitle(Title.title(
                Component.text(title, color, TextDecoration.BOLD),
                Component.text(subtitle, NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(3), Duration.ofMillis(700))));
    }

    public static void duelStart(Player p, String opponent, ArenaType mode, String arena, int minutes) {
        title(p, "DÜELLO!", NamedTextColor.RED, opponent + " ile karşı karşıyasın");
        box(p,
                ChatColor.RED + "" + ChatColor.BOLD + " ⚔ DÜELLO BAŞLADI",
                ChatColor.GRAY + " Rakip: " + ChatColor.WHITE + opponent,
                ChatColor.GRAY + " Mod: " + ChatColor.YELLOW + mode.getDisplayName()
                        + ChatColor.DARK_GRAY + "  •  " + ChatColor.GRAY + "Arena: " + ChatColor.YELLOW + arena,
                ChatColor.GRAY + " Süre: " + ChatColor.YELLOW + minutes + " dk"
                        + ChatColor.DARK_GRAY + "  •  " + ChatColor.RED + "Komutlar kapalı!",
                ChatColor.GRAY + " Kazanan, rakibin tüm eşyalarını alır.");
    }

    public static void invite(Player target, String inviterName, ArenaType mode) {
        Component accept = Component.text("[KABUL]", NamedTextColor.GREEN, TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/duel kabul " + inviterName))
                .hoverEvent(HoverEvent.showText(Component.text("Daveti kabul et", NamedTextColor.GRAY)));
        Component reject = Component.text("[RED]", NamedTextColor.RED, TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/duel reddet " + inviterName))
                .hoverEvent(HoverEvent.showText(Component.text("Daveti reddet", NamedTextColor.GRAY)));

        target.sendMessage(LINE);
        target.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + " ⚔ DÜELLO DAVETİ");
        target.sendMessage(ChatColor.WHITE + " " + inviterName + ChatColor.GRAY + " sana "
                + ChatColor.YELLOW + mode.getDisplayName() + ChatColor.GRAY + " daveti gönderdi.");
        target.sendMessage(Component.text(" ").append(accept).append(Component.text("   ")).append(reject));
        target.sendMessage(ChatColor.DARK_GRAY + " Davet 5 dakika geçerli.");
        target.sendMessage(LINE);
    }

    public static void win(Player p, String loserName, boolean quit, int itemCount, int lootSeconds) {
        title(p, "ZAFER!", NamedTextColor.GOLD, quit ? loserName + " oyundan çıktı" : loserName + " yenildi");
        p.sendMessage(LINE);
        p.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + " ✦ ZAFER! " + ChatColor.GRAY
                + (quit ? loserName + " oyundan çıktı, düelloyu sen kazandın." : loserName + " yenildi."));
        if (itemCount > 0) {
            p.sendMessage(ChatColor.GRAY + " Ganimet: " + ChatColor.YELLOW + itemCount + " eşya"
                    + ChatColor.DARK_GRAY + "  •  " + ChatColor.GRAY + lootSeconds + " saniyen var");
            p.sendMessage(ChatColor.GREEN + " Sandık açılıyor... "
                    + ChatColor.DARK_GRAY + "(Kapatırsan: /duel sandik)");
        } else {
            p.sendMessage(ChatColor.GRAY + " Rakibinin eşyası yoktu, hub'a gönderiliyorsun...");
        }
        p.sendMessage(LINE);
    }

    public static void lose(Player p, String winnerName, int lostItems) {
        title(p, "YENİLDİN", NamedTextColor.RED, winnerName + " kazandı");
        p.sendMessage(LINE);
        p.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + " ✖ YENİLDİN " + ChatColor.GRAY
                + winnerName + " düelloyu kazandı.");
        if (lostItems > 0) {
            p.sendMessage(ChatColor.GRAY + " Eşyaların " + ChatColor.WHITE + winnerName
                    + ChatColor.GRAY + " oyuncusuna ganimet oldu.");
        }
        p.sendMessage(ChatColor.GRAY + " Hub'a gönderiliyorsun...");
        p.sendMessage(LINE);
    }

    public static void draw(Player p, String reason) {
        title(p, "BERABERE", NamedTextColor.YELLOW, reason);
        box(p,
                ChatColor.YELLOW + "" + ChatColor.BOLD + " » BERABERE",
                ChatColor.GRAY + " " + reason,
                ChatColor.GRAY + " Hub'a gönderiliyorsun...");
    }

    public static void lootTaken(Player p) {
        title(p, "GANİMET ALINDI", NamedTextColor.GREEN, "Hub'a gönderiliyorsun");
        box(p,
                ChatColor.GREEN + "" + ChatColor.BOLD + " ✔ Ganimet toplandı!",
                ChatColor.GRAY + " Hub'a gönderiliyorsun...");
    }

    public static void lootExpired(Player p) {
        title(p, "SÜRE DOLDU", NamedTextColor.RED, "Kalan eşyalar kayboldu");
        box(p,
                ChatColor.RED + "" + ChatColor.BOLD + " ✖ Ganimet süren doldu!",
                ChatColor.GRAY + " Alınmayan eşyalar kayboldu, hub'a gönderiliyorsun...");
    }

    // --- YENİ EKLENEN METOTLAR (Hata veren kısımlar) ---

    public static void lootClaimedWait(Player p) {
        title(p, "GANİMET ALINDI", NamedTextColor.GREEN, "Sürenin dolmasını bekle");
        box(p,
                ChatColor.GREEN + "" + ChatColor.BOLD + " ✔ Ganimeti topladın!",
                ChatColor.GRAY + " Ancak arenadan çıkmak için sürenin dolmasını beklemelisin...");
    }

    public static void lootEnded(Player p) {
        title(p, "SÜRE DOLDU", NamedTextColor.GREEN, "Hub'a gönderiliyorsun");
        box(p,
                ChatColor.GREEN + "" + ChatColor.BOLD + " ✔ Süre doldu!",
                ChatColor.GRAY + " Hub'a gönderiliyorsun...");
    }
}
