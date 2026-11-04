package com.zornoely.duel;

import org.bukkit.Location;

public class Arena {
    private final String name;
    private Location spawn1;
    private Location spawn2;
    private ArenaType type = ArenaType.HEPSI;
    private boolean busy = false;

    public Arena(String name) { this.name = name; }

    public String getName()         { return name; }
    public Location getSpawn1()     { return spawn1; }
    public void setSpawn1(Location l) { this.spawn1 = l; }
    public Location getSpawn2()     { return spawn2; }
    public void setSpawn2(Location l) { this.spawn2 = l; }
    public ArenaType getType()      { return type; }
    public void setType(ArenaType t) { this.type = t; }
    public boolean isBusy()         { return busy; }
    public void setBusy(boolean b)  { this.busy = b; }

    public boolean isReady() { return spawn1 != null && spawn2 != null; }

    public boolean matchesType(ArenaType required) {
        return type == ArenaType.HEPSI
            || required == ArenaType.HEPSI
            || type == required;
    }
}
