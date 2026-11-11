package com.zornoely.duel;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

public class ClanHook {
    private final ZornoElyDuel plugin;
    private final boolean enabled;
    private final String placeholder;
    private final boolean papiAvailable;
    private Method setPlaceholdersMethod;

    public ClanHook(ZornoElyDuel plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean("settings.clan-check.enabled", true);
        this.placeholder = plugin.getConfig().getString("settings.clan-check.placeholder", "%apexclan_clan%");
        this.papiAvailable = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");

        if (enabled && papiAvailable) {
            try {
                Class<?> papiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                setPlaceholdersMethod = papiClass.getMethod("setPlaceholders", org.bukkit.OfflinePlayer.class, String.class);
                plugin.getLogger().info("Clan kontrolü aktif (PlaceholderAPI: " + placeholder + ")");
            } catch (Throwable t) {
                plugin.getLogger().warning("PlaceholderAPI sınıfı yüklenemedi, clan kontrolü kapalı.");
                setPlaceholdersMethod = null;
            }
        }
    }

    public boolean isEnabled() { return enabled && setPlaceholdersMethod != null; }

    public boolean isSameClan(Player a, Player b) {
        if (!isEnabled() || a == null || b == null) return false;
        if (a.getUniqueId().equals(b.getUniqueId())) return false;
        String clanA = getClan(a);
        String clanB = getClan(b);
        if (isBlank(clanA) || isBlank(clanB)) return false;
        return clanA.equalsIgnoreCase(clanB);
    }

    private boolean isBlank(String s) {
        if (s == null) return true;
        String t = s.trim();
        if (t.isEmpty() || t.equals(placeholder)) return true;
        String l = t.toLowerCase();
        return l.equals("yok") || l.equals("none") || l.equals("null") || l.equals("belirsiz");
    }

    private String getClan(Player p) {
        if (setPlaceholdersMethod == null) return null;
        try { return (String) setPlaceholdersMethod.invoke(null, p, placeholder); }
        catch (Throwable t) { return null; }
    }
}
