package com.zornoely.duel;

import java.util.Locale;

public enum ArenaType {
    ELYTRA("Elytra PvP"),
    TRAP("Trap PvP"),
    HEPSI("Hepsi");

    private final String displayName;

    ArenaType(String displayName) { this.displayName = displayName; }

    public String getDisplayName() { return displayName; }

    /** Komutlar için: geçersiz yazılırsa null döner. Locale.ROOT: Türkçe i/İ sorunu olmasın. */
    public static ArenaType fromString(String s) {
        if (s == null) return null;
        try { return ArenaType.valueOf(s.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) { return null; }
    }

    /** Config okurken: geçersizse HEPSI. */
    public static ArenaType parse(String s) {
        ArenaType t = fromString(s);
        return t == null ? HEPSI : t;
    }
}
