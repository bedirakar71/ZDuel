package com.zornoely.duel;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ArenaManager {
    private final ZornoElyDuel plugin;
    private final Map<String, Arena> arenas = new LinkedHashMap<>();
    private final File file;
    private FileConfiguration data;

    public ArenaManager(ZornoElyDuel plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "arenas.yml");
    }

    public void load() {
        try {
            if (!file.exists()) {
                plugin.getDataFolder().mkdirs();
                file.createNewFile();
            }
        } catch (IOException e) {
            plugin.getLogger().severe("arenas.yml oluşturulamadı: " + e.getMessage());
        }
        data = YamlConfiguration.loadConfiguration(file);
        arenas.clear();
        ConfigurationSection sec = data.getConfigurationSection("arenas");
        if (sec == null) return;
        for (String name : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(name);
            if (s == null) continue;
            Arena a = new Arena(name);
            a.setSpawn1(loadLocation(s.getConfigurationSection("spawn1")));
            a.setSpawn2(loadLocation(s.getConfigurationSection("spawn2")));
            a.setType(ArenaType.parse(s.getString("type", "HEPSI")));
            arenas.put(name, a);
        }
    }

    public void save() {
        data.set("arenas", null);
        for (Arena a : arenas.values()) {
            String path = "arenas." + a.getName();
            saveLocation(path + ".spawn1", a.getSpawn1());
            saveLocation(path + ".spawn2", a.getSpawn2());
            data.set(path + ".type", a.getType().name());
        }
        try { data.save(file); }
        catch (IOException e) { plugin.getLogger().severe("Kaydedilemedi: " + e.getMessage()); }
    }

    private void saveLocation(String path, Location l) {
        if (l == null || l.getWorld() == null) return;
        data.set(path + ".world", l.getWorld().getName());
        data.set(path + ".x", l.getX());
        data.set(path + ".y", l.getY());
        data.set(path + ".z", l.getZ());
        data.set(path + ".yaw", l.getYaw());
        data.set(path + ".pitch", l.getPitch());
    }

    private Location loadLocation(ConfigurationSection s) {
        if (s == null) return null;
        String w = s.getString("world");
        if (w == null) return null;
        World world = Bukkit.getWorld(w);
        if (world == null) return null;
        return new Location(world,
                s.getDouble("x"), s.getDouble("y"), s.getDouble("z"),
                (float) s.getDouble("yaw"), (float) s.getDouble("pitch"));
    }

    public Arena get(String name)          { return arenas.get(name); }
    public Collection<Arena> getAll()      { return arenas.values(); }
    public Set<String> getNames()          { return arenas.keySet(); }
    public boolean exists(String name)     { return arenas.containsKey(name); }

    public Arena create(String name) {
        if (arenas.containsKey(name)) return null;
        Arena a = new Arena(name);
        arenas.put(name, a);
        return a;
    }

    public boolean delete(String name) {
        Arena removed = arenas.remove(name);
        if (removed != null) { removed.setBusy(false); return true; }
        return false;
    }

    public Arena findAvailable(ArenaType required) {
        for (Arena a : arenas.values()) {
            if (!a.isBusy() && a.isReady() && a.matchesType(required)) return a;
        }
        return null;
    }
}
