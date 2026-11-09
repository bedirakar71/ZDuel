package com.zornoely.duel;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DuelListener implements Listener {
    private final ZornoElyDuel plugin;

    public DuelListener(ZornoElyDuel plugin) { this.plugin = plugin; }

    // ---------------------------------------------------------------
    // Düello boyunca HİÇBİR komut kullanılamaz.
    // Ganimet süresinde komutlar serbest: kazanan artık "düelloda" sayılmaz.
    // ---------------------------------------------------------------
    @EventHandler(priority = EventPriority.LOW)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();
        if (!plugin.getDuelManager().isInDuel(p.getUniqueId())) return;
        e.setCancelled(true);
        Msg.warn(p, "Düello bitene kadar hiçbir komut kullanamazsın!");
    }

    // ---------------------------------------------------------------
    // Ölüm → ganimet
    // ---------------------------------------------------------------
    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        Duel duel = plugin.getDuelManager().getDuel(victim.getUniqueId());
        if (duel == null) return;
        resolveLoss(victim, duel, false, e);
    }

    /**
     * Kaybedeni (ölen ya da oyundan çıkan) işler: eşyaları kazanana ganimet olur, düello biter.
     * @param death ölüm eventi (çıkışta null)
     * @return kazanan çevrimiçiydi ve düello sonuçlandı mı
     */
    private boolean resolveLoss(Player loser, Duel duel, boolean quit, PlayerDeathEvent death) {
        DuelManager dm = plugin.getDuelManager();
        UUID oppId = duel.getOpponent(loser.getUniqueId());
        Player winner = oppId != null ? Bukkit.getPlayer(oppId) : null;
        boolean winnerOnline = winner != null && winner.isOnline();

        // DİKKAT: getContents() zırh + offhand'i de içerir. Çifte saymamak için bölümler ayrı alınır.
        // try/catch: bir hata olsa bile düello mutlaka biter (komutlar kilitli kalmasın).
        List<ItemStack> loot = new ArrayList<>();
        try {
            collect(loot, loser.getInventory().getStorageContents());
            collect(loot, loser.getInventory().getArmorContents());
            collect(loot, loser.getInventory().getExtraContents());
        } catch (RuntimeException ex) {
            plugin.getLogger().log(java.util.logging.Level.WARNING, "Ganimet toplanamadı", ex);
            loot.clear();
        }

        boolean giveLoot = winnerOnline && !loot.isEmpty();
        if (giveLoot) {
            try {
                loser.getInventory().clear();
                loser.getInventory().setArmorContents(new ItemStack[4]);
                loser.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
            } catch (RuntimeException ex) {
                plugin.getLogger().log(java.util.logging.Level.WARNING, "Envanter temizlenemedi", ex);
            }
            if (death != null) {
                death.getDrops().clear();
                death.setDroppedExp(0);
            }
        }

        // Ganimet varsa arena, ganimet süreci bitene kadar meşgul kalır
        dm.endDuel(duel, oppId, quit ? "QUIT" : "DEATH", giveLoot);
        if (giveLoot) plugin.getLootManager().setLoot(winner.getUniqueId(), loot, duel.getArenaName());

        // Önce durumu ilerleten işler (hub / respawn), en sona mesajlar: mesajda hata çıksa bile oyuncular takılı kalmaz
        if (winnerOnline && !giveLoot) plugin.sendToHub(winner);

        if (!quit) {
            // Ölen: ölüm ekranında beklemeden otomatik respawn → hemen hub
            dm.markKilled(loser.getUniqueId());
            String winnerName = winnerOnline ? winner.getName() : "Rakibin";
            int lostItems = giveLoot ? loot.size() : 0;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!loser.isOnline()) return;
                if (loser.isDead()) loser.spigot().respawn();
                Msg.lose(loser, winnerName, lostItems);
            }, 1L);
        }

        if (winnerOnline) {
            Msg.win(winner, loser.getName(), quit, giveLoot ? loot.size() : 0,
                    plugin.getLootManager().getDurationSeconds());
        }
        return winnerOnline;
    }

    private static void collect(List<ItemStack> out, ItemStack[] items) {
        if (items == null) return;
        for (ItemStack it : items) {
            if (it != null && it.getType() != Material.AIR) out.add(it.clone());
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        if (plugin.getDuelManager().wasKilledInDuel(p.getUniqueId())) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.sendToHub(p), 2L);
        }
    }

    // Düelloda çıkış yapan geri girince: bilgi mesajı + direkt hub
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (!plugin.getDuelManager().wasQuitLoss(p.getUniqueId())) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) return;
            Msg.warn(p, "Düello sırasında oyundan çıktığın için yenilmiş sayıldın, eşyaların rakibine geçti.");
            plugin.sendToHub(p);
        }, 20L);
    }

    // ---------------------------------------------------------------
    // Blok koyma: Elytra modunda kapalı, Trap'te açık (maç sonunda geri alınır)
    // ---------------------------------------------------------------
    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        Duel duel = plugin.getDuelManager().getDuel(p.getUniqueId());
        if (duel == null) return;

        if (duel.getMode() == ArenaType.ELYTRA) {
            e.setCancelled(true);
            Msg.warn(p, "Bu modda blok koymak kapalı!");
            return;
        }
        Arena arena = plugin.getArenaManager().get(duel.getArenaName());
        if (arena != null) arena.recordBlock(e.getBlockReplacedState());
    }

    /** Düello sırasında kırılan bloklar da maç bitince eski haline döner. */
    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        Duel duel = plugin.getDuelManager().getDuel(e.getPlayer().getUniqueId());
        if (duel == null) return;
        Arena arena = plugin.getArenaManager().get(duel.getArenaName());
        if (arena != null) arena.recordBlock(e.getBlock().getState());
    }

    // ---------------------------------------------------------------
    // Düellocu sadece rakibine vurabilsin, dışarıdan kimse karışamasın
    // ---------------------------------------------------------------
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;

        Player attacker = null;
        if (e.getDamager() instanceof Player direct) {
            attacker = direct;
        } else if (e.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player shooter) {
            attacker = shooter;
        }
        if (attacker == null || attacker.equals(victim)) return;

        Duel victimDuel = plugin.getDuelManager().getDuel(victim.getUniqueId());
        Duel attackerDuel = plugin.getDuelManager().getDuel(attacker.getUniqueId());
        if (victimDuel == null && attackerDuel == null) return;   // düello dışı, karışma
        if (victimDuel != attackerDuel) e.setCancelled(true);     // aynı düelloda değiller
    }

    // ---------------------------------------------------------------
    // Menüler ve ganimet sandığı
    // ---------------------------------------------------------------
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        InventoryHolder holder = e.getInventory().getHolder();

        if (holder instanceof GUI.DuelHolder gui) {
            e.setCancelled(true);

            int slot = e.getRawSlot();
            ArenaType mode = slot == 11 ? ArenaType.ELYTRA
                           : slot == 15 ? ArenaType.TRAP : null;
            if (mode == null) return;   // boş slota tıklamak hiçbir şeyi bozmasın

            if (gui.getType().equals("queue")) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    p.closeInventory();
                    plugin.getQueueManager().toggleQueue(p, mode);
                });
            } else if (gui.getType().equals("invite")) {
                if (!p.hasMetadata("duel_target")) {
                    Bukkit.getScheduler().runTask(plugin, () -> p.closeInventory());
                    return;
                }
                String raw = p.getMetadata("duel_target").get(0).asString();
                p.removeMetadata("duel_target", plugin);   // sadece geçerli bir slota tıklanınca silinir

                Player found = null;
                try { found = Bukkit.getPlayer(UUID.fromString(raw)); }
                catch (IllegalArgumentException ignored) { }
                final Player target = found;

                Bukkit.getScheduler().runTask(plugin, () -> sendInvite(p, target, mode));
            }
        } else if (holder instanceof LootManager.LootHolder) {
            // Ganimet sandığından sadece ALINABİLİR; içeri eşya konamaz
            int raw = e.getRawSlot();
            boolean clickedTop = raw >= 0 && raw < e.getInventory().getSize();
            InventoryAction action = e.getAction();
            if (clickedTop) {
                if (!isTakeAction(action)) e.setCancelled(true);
            } else if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                e.setCancelled(true);   // kendi envanterinden shift-click ile sandığa atmayı engelle
            }
        }
    }

    private static boolean isTakeAction(InventoryAction a) {
        return switch (a) {
            case PICKUP_ALL, PICKUP_HALF, PICKUP_ONE, PICKUP_SOME,
                 MOVE_TO_OTHER_INVENTORY, COLLECT_TO_CURSOR,
                 DROP_ALL_SLOT, DROP_ONE_SLOT -> true;
            default -> false;
        };
    }

    private void sendInvite(Player p, Player target, ArenaType mode) {
        p.closeInventory();
        if (!p.isOnline()) return;
        if (target == null || !target.isOnline()) {
            Msg.warn(p, "Hedef oyuncu offline!");
            return;
        }
        if (plugin.getDuelManager().isInDuel(target.getUniqueId())) {
            Msg.warn(p, "Hedef oyuncu şu an düelloda!");
            return;
        }
        plugin.getInviteManager().createInvite(p.getUniqueId(), target.getUniqueId(), mode);
        Msg.ok(p, target.getName() + " oyuncusuna " + mode.getDisplayName() + " daveti gönderildi!");
        Msg.invite(target, p.getName(), mode);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        InventoryHolder holder = e.getInventory().getHolder();
        if (holder instanceof GUI.DuelHolder) {
            e.setCancelled(true);
            return;
        }
        if (holder instanceof LootManager.LootHolder) {
            int topSize = e.getInventory().getSize();
            for (int slot : e.getRawSlots()) {
                if (slot < topSize) {   // sandığın üstüne sürükleyerek eşya koymayı engelle
                    e.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        InventoryHolder holder = e.getInventory().getHolder();

        if (holder instanceof LootManager.LootHolder) {
            plugin.getLootManager().onInventoryClose(p, e.getInventory());
        } else if (holder instanceof GUI.DuelHolder gui && gui.getType().equals("invite")) {
            p.removeMetadata("duel_target", plugin);   // ESC ile kapatılırsa eski hedef kalmasın
        }
    }

    // ---------------------------------------------------------------
    // Çıkış: düelloda çıkan YENİLMİŞ sayılır, eşyaları rakibin ganimet sandığına düşer
    // ---------------------------------------------------------------
    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();
        DuelManager dm = plugin.getDuelManager();

        plugin.getQueueManager().removeFromAll(uuid);
        plugin.getInviteManager().removeAll(uuid);

        boolean wasInArena = false;
        Duel duel = dm.getDuel(uuid);
        if (duel != null) {
            wasInArena = true;
            if (resolveLoss(p, duel, true, null)) {
                dm.markQuitLoss(uuid);   // geri girince bilgi mesajı + hub
            }
        }
        if (plugin.getLootManager().hasLoot(uuid)) wasInArena = true;

        plugin.getLootManager().remove(uuid);
        dm.forget(uuid);   // ölüp respawn olmadan çıkanın işareti kalmasın
        p.removeMetadata("duel_target", plugin);

        // Arenada çıkan oyuncu tekrar girince arenanın ortasında doğmasın
        if (wasInArena) {
            Location spawn = plugin.getFallbackSpawn();
            if (spawn != null) p.teleport(spawn);
        }
    }

    // ---------------------------------------------------------------
    // NPC'ye sağ tık → sıra menüsü
    // ---------------------------------------------------------------
    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (!(e.getRightClicked() instanceof LivingEntity entity)) return;
        if (entity instanceof Player) return;

        Component customName = entity.customName();
        if (customName == null) return;

        String stripped = PlainTextComponentSerializer.plainText().serialize(customName);

        if (stripped.contains("Duel") || stripped.contains("PvP") || stripped.contains("ZornoEly")) {
            e.setCancelled(true);
            GUI.openQueue(e.getPlayer(), plugin);
        }
    }
}
