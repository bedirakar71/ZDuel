package com.zornoely.duel;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DuelListener implements Listener {
    private final ZornoElyDuel plugin;

    public DuelListener(ZornoElyDuel plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.LOW)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();
        if (!plugin.getDuelManager().isInDuel(p.getUniqueId())) return;
        String msg = e.getMessage().trim().toLowerCase();
        if (msg.startsWith("/duel ")) {
            String[] parts = msg.split("\\s+");
            if (parts.length >= 2 && parts[1].equals("berabere")) return;
        }
        e.setCancelled(true);
        p.sendMessage(ChatColor.RED + "Düellodayken başka komut kullanamazsınız! (Berabere: /duel berabere)");
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        Duel duel = plugin.getDuelManager().getDuel(victim.getUniqueId());
        if (duel == null) return;

        Player killer = victim.getKiller();
        if (killer == null) {
            UUID oppId = duel.getOpponent(victim.getUniqueId());
            killer = oppId != null ? Bukkit.getPlayer(oppId) : null;
        }

        List<ItemStack> loot = new ArrayList<>();
        for (ItemStack it : victim.getInventory().getContents()) {
            if (it != null && it.getType() != Material.AIR) loot.add(it.clone());
        }
        for (ItemStack it : victim.getInventory().getArmorContents()) {
            if (it != null && it.getType() != Material.AIR) loot.add(it.clone());
        }
        victim.getInventory().clear();
        victim.getInventory().setArmorContents(null);
        e.getDrops().clear();
        e.setDroppedExp(0);

        plugin.getDuelManager().endDuel(duel, killer != null ? killer.getUniqueId() : null, "DEATH");

        if (killer != null && killer.isOnline()) {
            killer.sendMessage(ChatColor.GREEN + "Düelloyu kazandın! " + ChatColor.YELLOW + "Ganimet için /duel sandik");
            if (!loot.isEmpty()) plugin.getLootManager().setLoot(killer.getUniqueId(), loot);
            else plugin.sendToHub(killer);
        }
        victim.sendMessage(ChatColor.RED + "Düelloyu kaybettin!");
        plugin.getDuelManager().markKilled(victim.getUniqueId());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        if (plugin.getDuelManager().wasKilledInDuel(e.getPlayer().getUniqueId())) {
            Player p = e.getPlayer();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (p.isOnline()) plugin.sendToHub(p);
            }, 5L);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        Duel duel = plugin.getDuelManager().getDuel(e.getPlayer().getUniqueId());
        if (duel == null) return;
        boolean allowed = switch (duel.getMode()) {
            case ELYTRA -> false;
            case TRAP   -> true;
            case HEPSI  -> true;
        };
        if (!allowed) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(ChatColor.RED + "Bu modda blok koymak kapalı!");
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        String title = e.getView().getTitle();

        if (GUI.QUEUE_TITLE.equals(title)) {
            e.setCancelled(true);
            int slot = e.getRawSlot();
            if (slot == 11) { p.closeInventory(); plugin.getQueueManager().toggleQueue(p, ArenaType.ELYTRA); }
            else if (slot == 15) { p.closeInventory(); plugin.getQueueManager().toggleQueue(p, ArenaType.TRAP); }
        } else if (GUI.INVITE_TITLE.equals(title)) {
            e.setCancelled(true);
            if (!p.hasMetadata("duel_target")) return;
            String targetUuid = p.getMetadata("duel_target").get(0).asString();
            p.removeMetadata("duel_target", plugin);
            Player target = Bukkit.getPlayer(UUID.fromString(targetUuid));

            int slot = e.getRawSlot();
            ArenaType mode = slot == 11 ? ArenaType.ELYTRA
                            : slot == 15 ? ArenaType.TRAP : null;
            if (mode == null) return;

            p.closeInventory();
            if (target == null || !target.isOnline()) {
                p.sendMessage(ChatColor.RED + "Hedef oyuncu offline!");
                return;
            }
            plugin.getInviteManager().createInvite(p.getUniqueId(), target.getUniqueId(), mode);
            String modeName = mode == ArenaType.ELYTRA ? "Elytra PvP" : "Trap PvP";
            p.sendMessage(ChatColor.GREEN + target.getName() + " oyuncusuna " + modeName + " daveti gönderildi!");
            target.sendMessage(ChatColor.YELLOW + p.getName() + " sana " + modeName + " daveti yolladı! Kabul: /duel kabul " + p.getName());
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        if (LootManager.LOOT_TITLE.equals(e.getView().getTitle())) {
            plugin.getLootManager().onInventoryClose(p, e.getInventory());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();

        plugin.getQueueManager().removeFromAll(uuid);

        Duel duel = plugin.getDuelManager().getDuel(uuid);
        if (duel != null) {
            UUID oppId = duel.getOpponent(uuid);
            Player opp = oppId != null ? Bukkit.getPlayer(oppId) : null;
            plugin.getDuelManager().endDuel(duel, oppId, "QUIT");
            if (opp != null && opp.isOnline()) {
                opp.sendMessage(ChatColor.GREEN + "Rakip çıktı, düelloyu kazandın!");
                plugin.sendToHub(opp);
            }
        }

        plugin.getLootManager().remove(uuid);
        p.removeMetadata("duel_target", plugin);
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        if (!(e.getRightClicked() instanceof LivingEntity entity)) return;
        if (entity instanceof Player) return;

        String name = entity.getCustomName();
        if (name == null) return;
        String stripped = ChatColor.stripColor(name);
        if (stripped == null) return;

        if (stripped.contains("Duel") || stripped.contains("PvP") || stripped.contains("ZornoEly")) {
            e.setCancelled(true);
            GUI.openQueue(e.getPlayer(), plugin);
        }
    }
}
