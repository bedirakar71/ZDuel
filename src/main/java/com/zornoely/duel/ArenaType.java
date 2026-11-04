package com.zornoely.duel;

public enum ArenaType {
    ELYTRA, TRAP, HEPSI;

    public static ArenaType parse(String s) {
        if (s == null) return HEPSI;
        try { return ArenaType.valueOf(s.toUpperCase()); }
        catch (IllegalArgumentException e) { return HEPSI; }
    }
}
