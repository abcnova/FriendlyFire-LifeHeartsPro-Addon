package io.github.abcnova.friendlyfire;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LingeringPotion;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Trident;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

final class DamageListener implements Listener {
    private final FriendlyFirePlugin plugin;
    private final Map<FeedbackKey, Long> feedbackCooldowns = new HashMap<>();
    private boolean protectProjectiles;
    private boolean protectTridents;
    private boolean protectPotions;
    private boolean protectWolves;
    private boolean bypassEnabled;
    private boolean attackerChat;
    private boolean victimChat;
    private boolean actionbarEnabled;
    private boolean soundEnabled;
    private long cooldownMillis;
    private long nextPruneNanos;
    private String attackerActionbar;
    private String victimActionbar;

    DamageListener(FriendlyFirePlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    void reload() {
        FileConfiguration config = plugin.getConfig();
        protectProjectiles = config.getBoolean("rules.protect_projectiles", true);
        protectTridents = config.getBoolean("rules.protect_tridents", true);
        protectPotions = config.getBoolean("rules.protect_potions", true);
        protectWolves = config.getBoolean("rules.protect_wolves", false);
        bypassEnabled = config.getBoolean("rules.bypass_permission_enabled", true);
        cooldownMillis = Math.max(0L, Math.min(60_000L, config.getLong("feedback.cooldown_ms", 1000L)));
        attackerChat = config.getBoolean("feedback.attacker_chat.enabled", true);
        victimChat = config.getBoolean("feedback.victim_chat.enabled", false);
        actionbarEnabled = config.getBoolean("feedback.actionbar.enabled", true);
        soundEnabled = config.getBoolean("feedback.sound.enabled", true);
        attackerActionbar = config.getString("feedback.actionbar.attacker", "&b&lFriendlyFire &8» &7Teammate damage was blocked.");
        victimActionbar = config.getString("feedback.actionbar.victim", "&b&lFriendlyFire &8» &7Your teammate could not damage you.");
        nextPruneNanos = System.nanoTime() + 600_000_000_000L;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null || attacker.getUniqueId().equals(victim.getUniqueId())) return;
        if (bypassEnabled && attacker.hasPermission("friendlyfireguard.bypass")) return;

        TeamResolver.TeamMatch match = plugin.getTeamResolver().resolve(attacker, victim);
        if (!match.available()) {
            event.setCancelled(true);
            return;
        }
        if (!match.sameTeam() || plugin.isFriendlyFireEnabledForTeam(match.attackerTeam())) return;

        event.setCancelled(true);
        if (!canSendFeedback(attacker, victim, match.attackerTeam())) return;
        if (attackerChat) attacker.sendMessage(plugin.locale("messages.friendlyfire_blocked"));
        if (victimChat) victim.sendMessage(plugin.locale("messages.friendlyfire_blocked_victim"));
        if (actionbarEnabled) {
            Chat.actionBar(attacker, plugin.localeManager().applyPrefix(attackerActionbar));
            Chat.actionBar(victim, plugin.localeManager().applyPrefix(victimActionbar));
        }
        if (soundEnabled) {
            plugin.getSoundManager().play((CommandSender) attacker, "friendlyfire_blocked_attacker");
            plugin.getSoundManager().play((CommandSender) victim, "friendlyfire_blocked_victim");
        }
    }

    void forget(UUID playerId) {
        feedbackCooldowns.keySet().removeIf(key -> key.attacker().equals(playerId) || key.victim().equals(playerId));
    }

    private boolean canSendFeedback(Player attacker, Player victim, String team) {
        if (cooldownMillis <= 0L) return true;
        long now = System.nanoTime();
        if (now >= nextPruneNanos) {
            long expiry = now - cooldownMillis * 1_000_000L;
            feedbackCooldowns.values().removeIf(previous -> previous < expiry);
            nextPruneNanos = now + 600_000_000_000L;
        }
        FeedbackKey key = new FeedbackKey(attacker.getUniqueId(), victim.getUniqueId(), team.toLowerCase(java.util.Locale.ROOT));
        Long previous = feedbackCooldowns.put(key, now);
        return previous == null || now - previous >= cooldownMillis * 1_000_000L;
    }

    private Player resolveAttacker(Entity damager) {
        if (damager instanceof Player player) return player;
        if (damager instanceof AreaEffectCloud cloud) {
            if (!protectPotions) return null;
            Object source = cloud.getSource();
            return source instanceof Player player ? player : null;
        }
        if (damager instanceof ThrownPotion || damager instanceof LingeringPotion) {
            if (!protectPotions || !(damager instanceof Projectile projectile)) return null;
            Object source = projectile.getShooter();
            return source instanceof Player player ? player : null;
        }
        if (damager instanceof Projectile projectile) {
            if (!protectProjectiles || (damager instanceof Trident && !protectTridents)) return null;
            Object source = projectile.getShooter();
            return source instanceof Player player ? player : null;
        }
        if (protectWolves && damager instanceof Wolf wolf) {
            AnimalTamer owner = wolf.getOwner();
            return owner instanceof Player player ? player : null;
        }
        return null;
    }

    private record FeedbackKey(UUID attacker, UUID victim, String team) { }
}
