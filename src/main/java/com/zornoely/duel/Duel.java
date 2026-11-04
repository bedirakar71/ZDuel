package com.zornoely.duel;

import java.util.UUID;

public class Duel {
    private final UUID player1;
    private final UUID player2;
    private final String arenaName;
    private final ArenaType mode;
    private final long startTime;

    public Duel(UUID p1, UUID p2, String arena, ArenaType mode) {
        this.player1 = p1;
        this.player2 = p2;
        this.arenaName = arena;
        this.mode = mode;
        this.startTime = System.currentTimeMillis();
    }

    public UUID getPlayer1()        { return player1; }
    public UUID getPlayer2()        { return player2; }
    public String getArenaName()    { return arenaName; }
    public ArenaType getMode()      { return mode; }
    public long getStartTime()      { return startTime; }

    public UUID getOpponent(UUID me) {
        if (me.equals(player1)) return player2;
        if (me.equals(player2)) return player1;
        return null;
    }

    public boolean contains(UUID uuid) {
        return uuid.equals(player1) || uuid.equals(player2);
    }
}
