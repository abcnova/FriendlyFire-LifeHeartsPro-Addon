package io.github.abcnova.friendlyfire;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import org.bukkit.configuration.file.YamlConfiguration;

final class LocaleManager {
    private final FriendlyFirePlugin plugin;
    private YamlConfiguration messages;

    LocaleManager(FriendlyFirePlugin plugin) {
        this.plugin = plugin;
    }

    void reload() {
        for (String locale : new String[]{"en_us", "de_eu", "fr_fr", "es_es"}) {
            String path = "locale/" + locale + ".yml";
            File file = new File(plugin.getDataFolder(), path);
            if (!file.exists()) plugin.saveResource(path, false);
            plugin.updateResourceFile(path);
        }
        String selected = normalize(plugin.getConfig().getString("locale.default", "en_us"));
        File file = new File(plugin.getDataFolder(), "locale/" + selected + ".yml");
        if (!file.isFile()) {
            plugin.getLogger().warning("Unknown locale '" + selected + "'; using en_us.");
            file = new File(plugin.getDataFolder(), "locale/en_us.yml");
        }
        messages = YamlConfiguration.loadConfiguration(file);
        try (InputStreamReader reader = new InputStreamReader(plugin.getResource("locale/" + file.getName()), StandardCharsets.UTF_8)) {
            messages.setDefaults(YamlConfiguration.loadConfiguration(reader));
        } catch (Exception error) {
            plugin.getLogger().warning("Could not load bundled locale defaults: " + error.getMessage());
        }
        migratePrefix(file);
    }

    String format(String path) {
        return format(path, Map.of());
    }

    String format(String path, Map<String, String> values) {
        String value = applyPrefix(messages.getString(path, path));
        for (Map.Entry<String, String> entry : values.entrySet()) {
            value = value.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return Chat.format(value);
    }

    String applyPrefix(String text) {
        return text.replace("{prefix}", messages.getString("prefix", "&b&lFriendlyFire &8» &7"));
    }

    String plain(String path) {
        return messages.getString(path, path);
    }

    private void migratePrefix(File file) {
        String prefix = messages.getString("prefix");
        if (prefix == null || (!prefix.contains("&8# &7") && !prefix.contains("&8? &7"))) return;
        messages.set("prefix", prefix.replace("&8# &7", "&8» &7").replace("&8? &7", "&8» &7"));
        try {
            messages.save(file);
        } catch (Exception error) {
            plugin.getLogger().warning("Could not update locale prefix: " + error.getMessage());
        }
    }

    private static String normalize(String locale) {
        return locale == null || locale.isBlank() ? "en_us" : locale.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }
}
