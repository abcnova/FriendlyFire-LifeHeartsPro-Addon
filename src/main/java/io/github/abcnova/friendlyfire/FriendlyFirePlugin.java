package io.github.abcnova.friendlyfire;

import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class FriendlyFirePlugin extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    private TeamResolver teamResolver;
    private DamageListener damageListener;
    private LocaleManager localeManager;
    private SoundManager soundManager;
    private UpdateChecker updateChecker;
    private BukkitTask teamSyncTask;

    @Override
    public void onEnable() {
        Plugin lifeHearts = Bukkit.getPluginManager().getPlugin("LifeHeartsPro");
        if (lifeHearts == null || !lifeHearts.isEnabled()) {
            getLogger().severe("LifeHeartsPro is required. FriendlyFire LHP Addon will be disabled.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        updateResourceFile("config.yml");
        reloadConfig();
        migrateUpdateCheckerDefault();
        updateResourceFile("sounds.yml");
        localeManager = new LocaleManager(this);
        localeManager.reload();
        soundManager = new SoundManager(this);
        soundManager.reload();

        try {
            teamResolver = new TeamResolver(this, lifeHearts);
        } catch (ReflectiveOperationException | RuntimeException error) {
            getLogger().severe("Could not connect to the LifeHeartsPro team API: " + error.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        if (!synchronizeGlobalFriendlyFire(lifeHearts)) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        damageListener = new DamageListener(this);
        PluginCommand command = getCommand("friendlyfire");
        if (command == null) {
            getLogger().severe("The friendlyfire command is missing from plugin.yml.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        command.setExecutor(this);
        command.setTabCompleter(this);
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(damageListener, this);
        syncKnownTeamSettings();
        teamSyncTask = Bukkit.getScheduler().runTaskTimer(this, this::syncKnownTeamSettings, 1200L, 1200L);
        updateChecker = new UpdateChecker(this);
        updateChecker.reload();
        getLogger().info("FriendlyFire LHP Addon " + getDescription().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        if (teamSyncTask != null) teamSyncTask.cancel();
        if (updateChecker != null) updateChecker.stop();
    }

    void reloadPluginState() {
        reloadConfig();
        updateResourceFile("config.yml");
        reloadConfig();
        updateResourceFile("sounds.yml");
        localeManager.reload();
        soundManager.reload();
        damageListener.reload();
        Plugin lifeHearts = Bukkit.getPluginManager().getPlugin("LifeHeartsPro");
        if (lifeHearts != null && !synchronizeGlobalFriendlyFire(lifeHearts)) {
            getLogger().severe("Could not synchronize LifeHeartsPro friendly-fire settings.");
        }
        syncKnownTeamSettings();
        updateChecker.reload();
    }

    String locale(String path) {
        return localeManager.format(path);
    }

    String locale(String path, Map<String, String> values) {
        return localeManager.format(path, values);
    }

    LocaleManager localeManager() {
        return localeManager;
    }

    TeamResolver getTeamResolver() {
        return teamResolver;
    }

    SoundManager getSoundManager() {
        return soundManager;
    }

    boolean isFriendlyFireEnabledForTeam(String teamName) {
        if (teamName == null || teamName.isBlank()) return true;
        String path = "team_settings." + normalizeTeamKey(teamName) + ".friendly_fire";
        if (getConfig().contains(path)) return getConfig().getBoolean(path);
        boolean enabled = getConfig().getBoolean("teams.default_friendly_fire", false);
        if (getConfig().getBoolean("teams.auto_create_team_entries", true)) {
            getConfig().set(path, enabled);
            saveConfig();
        }
        return enabled;
    }

    private boolean setFriendlyFireForTeam(String teamName, boolean enabled) {
        if (teamName == null || teamName.isBlank()) return false;
        getConfig().set("team_settings." + normalizeTeamKey(teamName) + ".friendly_fire", enabled);
        saveConfig();
        return true;
    }

    private boolean teamExists(String teamName) {
        return teamName != null && teamResolver.knownTeams().stream().anyMatch(team -> team.equalsIgnoreCase(teamName));
    }

    private String findTeamName(String input) {
        if (input == null || input.isBlank()) return null;
        return teamResolver.knownTeams().stream().filter(team -> team.equalsIgnoreCase(input)).findFirst().orElse(input);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("friendlyfire.reload")) {
                feedback(sender, "messages.no_permission", "no_permission");
                return true;
            }
            reloadPluginState();
            feedback(sender, "messages.reload_success", "reload_success");
            return true;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
            if (!sender.hasPermission("friendlyfire.info")) {
                feedback(sender, "messages.no_permission", "no_permission");
                return true;
            }
            if (!getConfig().getBoolean("commands.info_command_enabled", true)) {
                feedback(sender, "messages.feature_disabled", "feature_disabled");
                return true;
            }
            String team = findTeamName(args[1]);
            if (!teamExists(team)) {
                sender.sendMessage(locale("messages.team_not_found", Map.of("team", args[1])));
                soundManager.play(sender, "team_not_found");
                return true;
            }
            sender.sendMessage(locale("messages.team_info_header", Map.of("team", team)));
            sender.sendMessage(locale(isFriendlyFireEnabledForTeam(team)
                    ? "messages.team_info_status_on" : "messages.team_info_status_off"));
            sender.sendMessage(locale("messages.team_info_size", Map.of("size", String.valueOf(teamResolver.teamSize(team)))));
            soundManager.play(sender, "team_info");
            return true;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("team")) {
            if (!sender.hasPermission("friendlyfire.manage")) {
                feedback(sender, "messages.no_permission", "no_permission");
                return true;
            }
            String requested = args[1];
            String state = args[2].toLowerCase(Locale.ROOT);
            if (!state.equals("on") && !state.equals("off")) {
                sender.sendMessage(locale("messages.usage_team", Map.of("command", label)));
                soundManager.play(sender, "usage");
                return true;
            }
            String team = findTeamName(requested);
            if (!teamExists(team)) {
                sender.sendMessage(locale("messages.team_not_found", Map.of("team", requested)));
                soundManager.play(sender, "team_not_found");
                return true;
            }
            boolean enabled = state.equals("on");
            setFriendlyFireForTeam(team, enabled);
            String key = enabled ? "messages.team_ff_enabled" : "messages.team_ff_disabled";
            sender.sendMessage(locale(key, Map.of("team", team, "state", state)));
            soundManager.play(sender, enabled ? "team_ff_enabled" : "team_ff_disabled");
            return true;
        }
        sendHelp(sender, label);
        soundManager.play(sender, "help");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            ArrayList<String> options = new ArrayList<>(List.of("reload", "team"));
            if (getConfig().getBoolean("commands.info_command_enabled", true)) options.add("info");
            return filterSuggestions(options, args[0]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("team") || args[0].equalsIgnoreCase("info"))) {
            if (args[0].equalsIgnoreCase("info") && !getConfig().getBoolean("commands.info_command_enabled", true)) return List.of();
            return filterSuggestions(new ArrayList<>(teamResolver.knownTeams()), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("team")) return filterSuggestions(List.of("on", "off"), args[2]);
        return List.of();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        damageListener.forget(event.getPlayer().getUniqueId());
        soundManager.forget(event.getPlayer().getUniqueId());
    }

    private void feedback(CommandSender sender, String messageKey, String soundKey) {
        sender.sendMessage(locale(messageKey));
        soundManager.play(sender, soundKey);
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(locale("messages.help_header"));
        sender.sendMessage(locale("messages.help_reload", Map.of("command", label)));
        sender.sendMessage(locale("messages.help_team", Map.of("command", label)));
        if (getConfig().getBoolean("commands.info_command_enabled", true)) {
            sender.sendMessage(locale("messages.help_info", Map.of("command", label)));
        }
    }

    private boolean synchronizeGlobalFriendlyFire(Plugin lifeHearts) {
        if (!getConfig().getBoolean("lifeheartspro.force_global_friendly_fire_true", true)) {
            getLogger().warning("LifeHeartsPro global friendly fire is not being changed; per-team ON settings may not override its global protection.");
            return true;
        }
        try {
            Object config = lifeHearts.getClass().getMethod("getConfig").invoke(lifeHearts);
            Method getBoolean = config.getClass().getMethod("getBoolean", String.class, boolean.class);
            if ((boolean) getBoolean.invoke(config, "teams.friendly-fire", false)) return true;
            config.getClass().getMethod("set", String.class, Object.class).invoke(config, "teams.friendly-fire", true);
            lifeHearts.getClass().getMethod("saveConfig").invoke(lifeHearts);
            lifeHearts.getClass().getMethod("reloadConfig").invoke(lifeHearts);
            getLogger().info("Enabled LifeHeartsPro global friendly fire so the add-on can enforce per-team settings.");
            return true;
        } catch (ReflectiveOperationException | ClassCastException error) {
            getLogger().severe("Could not synchronize LifeHeartsPro's global friendly-fire setting: " + error.getMessage());
            return false;
        }
    }

    private void migrateUpdateCheckerDefault() {
        if (getConfig().getBoolean("update-checker.migration-auto-enabled", false)) return;
        getConfig().set("update-checker.enabled", true);
        getConfig().set("update-checker.migration-auto-enabled", true);
        saveConfig();
    }

    private void syncKnownTeamSettings() {
        if (!getConfig().getBoolean("teams.auto_create_team_entries", true)) return;
        Set<String> teams = teamResolver.knownTeams();
        boolean changed = false;
        boolean defaultState = getConfig().getBoolean("teams.default_friendly_fire", false);
        Set<String> knownKeys = teams.stream().map(this::normalizeTeamKey).collect(Collectors.toSet());
        for (String team : teams) {
            String path = "team_settings." + normalizeTeamKey(team) + ".friendly_fire";
            if (!getConfig().contains(path)) {
                getConfig().set(path, defaultState);
                changed = true;
            }
        }
        if (getConfig().getBoolean("teams.remove_deleted_team_entries", false)) {
            ConfigurationSection section = getConfig().getConfigurationSection("team_settings");
            if (section != null) {
                for (String key : new LinkedHashSet<>(section.getKeys(false))) {
                    if (!knownKeys.contains(key)) {
                        getConfig().set("team_settings." + key, null);
                        changed = true;
                    }
                }
            }
        }
        if (changed) saveConfig();
    }

    void updateResourceFile(String resourcePath) {
        File file = new File(getDataFolder(), resourcePath);
        if (!file.exists()) {
            saveResource(resourcePath, false);
            return;
        }
        try (InputStreamReader reader = new InputStreamReader(getResource(resourcePath), StandardCharsets.UTF_8)) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(reader);
            YamlConfiguration current = YamlConfiguration.loadConfiguration(file);
            if (copyMissingDefaults(current, defaults)) {
                current.save(file);
                getLogger().info("Added missing default settings to " + resourcePath + ".");
            }
        } catch (Exception error) {
            getLogger().warning("Could not update " + resourcePath + " defaults: " + error.getMessage());
        }
    }

    private boolean copyMissingDefaults(ConfigurationSection current, ConfigurationSection defaults) {
        boolean changed = false;
        for (String key : defaults.getKeys(false)) {
            Object value = defaults.get(key);
            if (value instanceof ConfigurationSection defaultSection) {
                ConfigurationSection currentSection = current.getConfigurationSection(key);
                if (currentSection == null) {
                    currentSection = current.createSection(key);
                    changed = true;
                }
                changed |= copyMissingDefaults(currentSection, defaultSection);
            } else if (!current.contains(key)) {
                current.set(key, value);
                changed = true;
            }
        }
        return changed;
    }

    private String normalizeTeamKey(String team) {
        return team.trim().toLowerCase(Locale.ROOT);
    }

    private List<String> filterSuggestions(List<String> input, String current) {
        String needle = current.toLowerCase(Locale.ROOT);
        return input.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(needle))
                .sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }
}
