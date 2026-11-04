package com.zornoely.duel;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InviteManager {
    private static final long EXPIRE_MS = 5 * 60 * 1000L;

    public static class Invite {
        public final UUID inviter, target;
        public final ArenaType mode;
        public final long createdAt;
        public Invite(UUID inviter, UUID target, ArenaType mode) {
            this.inviter = inviter;
            this.target = target;
            this.mode = mode;
            this.createdAt = System.currentTimeMillis();
        }
        public boolean isExpired() { return System.currentTimeMillis() - createdAt > EXPIRE_MS; }
    }

    private final ZornoElyDuel plugin;
    private final Map<UUID, Invite> invites = new HashMap<>();

    public InviteManager(ZornoElyDuel plugin) { this.plugin = plugin; }

    public void createInvite(UUID inviter, UUID target, ArenaType mode) {
        invites.put(inviter, new Invite(inviter, target, mode));
    }

    public Invite getInvite(UUID inviter) {
        Invite inv = invites.get(inviter);
        if (inv != null && inv.isExpired()) { invites.remove(inviter); return null; }
        return inv;
    }

    public void removeInvite(UUID inviter) { invites.remove(inviter); }

    public void cleanupExpired() {
        invites.entrySet().removeIf(e -> e.getValue().isExpired());
    }
}
