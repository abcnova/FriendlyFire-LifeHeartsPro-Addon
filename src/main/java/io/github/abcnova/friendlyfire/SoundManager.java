package io.github.abcnova.friendlyfire;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

final class SoundManager {
    private final FriendlyFirePlugin plugin;
    private final Map<String, SoundSettings> sounds = new HashMap<>();
    private final Map<UUID, Long> lastSoundNanos = new HashMap<>();
    private boolean enabled;

    SoundManager(FriendlyFirePlugin plugin) {
        this.plugin = plugin;
    }

    void reload() {
        File file = new File(plugin.getDataFolder(), "sounds.yml");
        if (!file.exists()) plugin.saveResource("sounds.yml", false);
        plugin.updateResourceFile("sounds.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        enabled = config.getBoolean("sounds.enabled", true);
        sounds.clear();
        ConfigurationSection entries = config.getConfigurationSection("sounds.messages");
        if (entries == null) return;
        for (String key : entries.getKeys(false)) {
            String path = "sounds.messages." + key;
            if (!config.getBoolean(path + ".enabled", false)) continue;
            String raw = config.getString(path + ".sound", "");
            try {
                Sound sound = Sound.valueOf(raw.toUpperCase(Locale.ROOT));
                float volume = (float) Math.max(0.0, Math.min(1.0, config.getDouble(path + ".volume", 0.45)));
                float pitch = (float) Math.max(0.5, Math.min(2.0, config.getDouble(path + ".pitch", 1.0)));
                sounds.put(key, new SoundSettings(sound, volume, pitch));
            } catch (IllegalArgumentException error) {
                plugin.getLogger().warning(plugin.locale("log.invalid_sound", Map.of("sound", raw)));
            }
        }
    }

    void play(CommandSender sender, String key) {
        if (!enabled || !(sender instanceof Player player)) return;
        SoundSettings settings = sounds.get(key);
        if (settings == null) return;
        long now = System.nanoTime();
        long previous = lastSoundNanos.getOrDefault(player.getUniqueId(), 0L);
        if (now - previous < 80_000_000L) return;
        player.playSound(player.getLocation(), settings.sound(), settings.volume(), settings.pitch());
        lastSoundNanos.put(player.getUniqueId(), now);
    }

    void forget(UUID playerId) {
        lastSoundNanos.remove(playerId);
    }

    private record SoundSettings(Sound sound, float volume, float pitch) { }
}
