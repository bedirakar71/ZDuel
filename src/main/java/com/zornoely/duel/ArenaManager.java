package com.zornoely.duel;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class ArenaManager {
    private static final Pattern NAME_PATTERN = Pattern.compile("[A-Za-z0-9_\\-]{1,32}");

    private final ZornoElyDuel plugin;
    private final Map<String, Arena> arenas = new LinkedHashMap<>();
    private final File file;
    private FileConfiguration data = new YamlConfiguration();

    public ArenaManager(ZornoElyDuel plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "arenas.yml");
    }

    /** Nokta vs. içeren isimler YAML yolunu bozar, o yüzden sadece güvenli karakterler. */
    public static boolean isValidName(String name) {
        return name != null && NAME_PATTERN.matcher(name).matches();
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
            a.setRawSpawn1(loadSpot(s.getConfigurationSection("spawn1")));
            a.setRawSpawn2(loadSpot(s.getConfigurationSection("spawn2")));
            a.setType(ArenaType.parse(s.getString("type", "HEPSI")));
            arenas.put(name, a);
        }
    }

    public void save() {
        data.set("arenas", null);
        for (Arena a : arenas.values()) {
            String path = "arenas." + a.getName();
            saveSpot(path + ".spawn1", a.getRawSpawn1());
            saveSpot(path + ".spawn2", a.getRawSpawn2());
            data.set(path + ".type", a.getType().name());
        }
        try { data.save(file); }
        catch (IOException e) { plugin.getLogger().severe("Kaydedilemedi: " + e.getMessage()); }
    }

    private void saveSpot(String path, Arena.Spot s) {
        if (s == null) return;
        data.set(path + ".world", s.world);
        data.set(path + ".x", s.x);
        data.set(path + ".y", s.y);
        data.set(path + ".z", s.z);
        data.set(path + ".yaw", s.yaw);
        data.set(path + ".pitch", s.pitch);
    }

    private Arena.Spot loadSpot(ConfigurationSection s) {
        if (s == null) return null;
        String w = s.getString("world");
        if (w == null) return null;
        return new Arena.Spot(w,
                s.getDouble("x"), s.getDouble("y"), s.getDouble("z"),
                (float) s.getDouble("yaw"), (float) s.getDouble("pitch"));
    }

    /** Büyük/küçük harf fark etmez. */
    public Arena get(String name) {
        if (name == null) return null;
        Arena a = arenas.get(name);
        if (a != null) return a;
        for (Arena x : arenas.values()) {
            if (x.getName().equalsIgnoreCase(name)) return x;
        }
        return null;
    }

    public Collection<Arena> getAll()      { return arenas.values(); }
    public Set<String> getNames()          { return arenas.keySet(); }
    public boolean exists(String name)     { return get(name) != null; }

    public Arena create(String name) {
        if (!isValidName(name) || get(name) != null) return null;
        Arena a = new Arena(name);
        arenas.put(name, a);
        return a;
    }

    public boolean delete(String name) {
        Arena a = get(name);
        if (a == null) return false;
        a.restoreBlocks();
        arenas.remove(a.getName());
        return true;
    }

    public Arena findAvailable(ArenaType required) {
        for (Arena a : arenas.values()) {
            if (!a.isBusy() && a.isReady() && a.matchesType(required)) return a;
        }
        return null;
    }

    /** Arenayı boşa çıkarır ve maçta konan blokları geri alır. */
    public void release(String arenaName) {
        Arena a = arenas.get(arenaName);
        if (a == null) return;
        a.restoreBlocks();
        a.setBusy(false);
    }

    public void restoreAll() {
        for (Arena a : arenas.values()) a.restoreBlocks();
    }
}
