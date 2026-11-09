package com.zornoely.duel;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;

import java.util.ArrayDeque;
import java.util.Deque;

public class Arena {

    /** Dünya henüz yüklenmemiş olsa bile koordinatlar kaybolmasın diye ham saklanır. */
    public static class Spot {
        public final String world;
        public final double x, y, z;
        public final float yaw, pitch;

        public Spot(String world, double x, double y, double z, float yaw, float pitch) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }

        public static Spot of(Location l) {
            if (l == null || l.getWorld() == null) return null;
            return new Spot(l.getWorld().getName(), l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch());
        }

        public Location toLocation() {
            World w = Bukkit.getWorld(world);
            return w == null ? null : new Location(w, x, y, z, yaw, pitch);
        }
    }

    private static final int MAX_RECORDED_BLOCKS = 20000;

    private final String name;
    private Spot spawn1;
    private Spot spawn2;
    private ArenaType type = ArenaType.HEPSI;
    private boolean busy = false;
    private final Deque<BlockState> placedBlocks = new ArrayDeque<>();

    public Arena(String name) { this.name = name; }

    public String getName()           { return name; }

    public Location getSpawn1()       { return spawn1 == null ? null : spawn1.toLocation(); }
    public Location getSpawn2()       { return spawn2 == null ? null : spawn2.toLocation(); }
    public Spot getRawSpawn1()        { return spawn1; }
    public Spot getRawSpawn2()        { return spawn2; }
    public void setSpawn1(Location l) { this.spawn1 = Spot.of(l); }
    public void setSpawn2(Location l) { this.spawn2 = Spot.of(l); }
    public void setRawSpawn1(Spot s)  { this.spawn1 = s; }
    public void setRawSpawn2(Spot s)  { this.spawn2 = s; }

    public ArenaType getType()        { return type; }
    public void setType(ArenaType t)  { this.type = t; }
    public boolean isBusy()           { return busy; }
    public void setBusy(boolean b)    { this.busy = b; }

    /** İki spawn da tanımlı VE dünyaları yüklü olmalı. */
    public boolean isReady() { return getSpawn1() != null && getSpawn2() != null; }

    public boolean matchesType(ArenaType required) {
        return type == ArenaType.HEPSI
            || required == ArenaType.HEPSI
            || type == required;
    }

    /** Düello sırasında konan bloğun eski halini kaydeder (maç bitince geri alınır). */
    public void recordBlock(BlockState replacedState) {
        if (replacedState == null || placedBlocks.size() >= MAX_RECORDED_BLOCKS) return;
        placedBlocks.push(replacedState);
    }

    /** Kaydedilen blokları en yeniden en eskiye doğru eski haline döndürür. */
    public void restoreBlocks() {
        BlockState st;
        while ((st = placedBlocks.poll()) != null) {
            try { st.update(true, false); }
            catch (Exception ignored) { }
        }
    }
}
