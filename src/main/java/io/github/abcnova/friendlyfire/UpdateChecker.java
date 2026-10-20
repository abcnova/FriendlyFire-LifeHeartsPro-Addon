package io.github.abcnova.friendlyfire;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitTask;

final class UpdateChecker implements Listener {
    static final String MODRINTH_PAGE = "https://modrinth.com/plugin/friendlyfire-lifeheartspro-addon";
    static final String GITHUB_RELEASES = "https://github.com/abcnova/FriendlyFireLHPAddon/releases";
    private static final String API_URL = "https://api.modrinth.com/v2/project/friendlyfire-lifeheartspro-addon/version?featured=true&include_changelog=false";
    private static final Pattern VERSION_FIELD = Pattern.compile("\\\"version_number\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"");
    private static final int MAX_RESPONSE_CHARS = 1_000_000;
    private final FriendlyFirePlugin plugin;
    private final AtomicBoolean requestRunning = new AtomicBoolean();
    private BukkitTask task;
    private volatile String availableVersion;
    private volatile String announcedVersion;
    private volatile String currentVersion;
    private volatile boolean debugEnabled;
    private static final String USER_AGENT_PREFIX = "FriendlyFireLHPAddon/";

    UpdateChecker(FriendlyFirePlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    void reload() {
        stop();
        availableVersion = null;
        announcedVersion = null;
        currentVersion = plugin.getDescription().getVersion();
        debugEnabled = plugin.getConfig().getBoolean("debug.enabled", false);
        if (!plugin.getConfig().getBoolean("update-checker.enabled", true)) return;
        int hours = Math.max(6, Math.min(168, plugin.getConfig().getInt("update-checker.interval-hours", 12)));
        task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::checkAsync, 20L, hours * 72_000L);
    }

    void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void checkAsync() {
        if (!requestRunning.compareAndSet(false, true)) return;
        String current = currentVersion;
        try {
            String latest = fetchLatestVersion();
            if (latest == null || VersionComparator.compare(latest, current) <= 0) return;
            Bukkit.getScheduler().runTask(plugin, () -> announceIfNew(current, latest));
        } catch (Exception error) {
            if (debugEnabled) {
                plugin.getLogger().warning("Update check failed: " + error.getMessage());
            }
        } finally {
            requestRunning.set(false);
        }
    }

    private String fetchLatestVersion() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) URI.create(API_URL).toURL().openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", USER_AGENT_PREFIX + currentVersion);
        connection.setRequestProperty("Accept", "application/json");
        try {
            if (connection.getResponseCode() != 200) return null;
            StringBuilder body = new StringBuilder(4096);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                char[] buffer = new char[4096];
                int read;
                while ((read = reader.read(buffer)) >= 0) {
                    if (body.length() + read > MAX_RESPONSE_CHARS) return null;
                    body.append(buffer, 0, read);
                }
            }
            return parseLatestVersion(body.toString());
        } finally {
            connection.disconnect();
        }
    }

    static String parseLatestVersion(String json) {
        if (json == null || json.length() > MAX_RESPONSE_CHARS) return null;
        Matcher matcher = VERSION_FIELD.matcher(json);
        return matcher.find() ? matcher.group(1).replace("\\/", "/").replace("\\\"", "\"").replace("\\\\", "\\") : null;
    }

    private void announceIfNew(String current, String latest) {
        if (!plugin.isEnabled() || latest.equals(announcedVersion)) return;
        availableVersion = latest;
        announcedVersion = latest;
        plugin.getLogger().warning("FriendlyFire LHP Addon update available: " + latest + " (installed: " + current + ").");
        plugin.getLogger().warning("Modrinth: " + MODRINTH_PAGE + " | GitHub: " + GITHUB_RELEASES);
        for (Player player : Bukkit.getOnlinePlayers()) notifyIfAllowed(player, current, latest);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String latest = availableVersion;
        if (latest == null) return;
        Bukkit.getScheduler().runTask(plugin, () -> notifyIfAllowed(event.getPlayer(), currentVersion, latest));
    }

    private void notifyIfAllowed(Player player, String current, String latest) {
        if (!player.isOnline() || (!player.isOp() && !player.hasPermission("friendlyfire.update.notify"))) return;
        if (player.hasPermission("friendlyfire.update.notify") || player.isOp()) {
            player.sendMessage(plugin.locale("messages.update_available", Map.of("version", latest)));
            player.sendMessage(plugin.locale("messages.update_installed", Map.of("version", current)));
            net.md_5.bungee.api.chat.TextComponent links = new net.md_5.bungee.api.chat.TextComponent();
            links.addExtra(link(plugin.localeManager().plain("messages.update_modrinth_link"), MODRINTH_PAGE, net.md_5.bungee.api.ChatColor.GREEN));
            links.addExtra(new net.md_5.bungee.api.chat.TextComponent(" "));
            links.addExtra(link(plugin.localeManager().plain("messages.update_github_link"), GITHUB_RELEASES, net.md_5.bungee.api.ChatColor.AQUA));
            player.spigot().sendMessage(links);
        }
    }

    private static net.md_5.bungee.api.chat.TextComponent link(String label, String url, net.md_5.bungee.api.ChatColor color) {
        net.md_5.bungee.api.chat.TextComponent component = new net.md_5.bungee.api.chat.TextComponent(label);
        component.setColor(color);
        component.setUnderlined(true);
        component.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.OPEN_URL, url));
        return component;
    }
}
