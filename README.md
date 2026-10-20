<p align="center">
  <img src="assets/FriendlyFire-Modrinth.jpeg" alt="FriendlyFire (LifeHeartsPro Addon)" width="320">
</p>

<h1 align="center">FriendlyFire (LifeHeartsPro Addon)</h1>

**FriendlyFire LHP Addon adds per-team friendly-fire controls to LifeHeartsPro.** It protects teammates across melee, projectiles, potions and optional tamed-wolf attacks, with configurable feedback and four built-in languages.

[Modrinth](https://modrinth.com/plugin/friendlyfire-lifeheartspro-addon) · [CurseForge](https://www.curseforge.com/minecraft/bukkit-plugins/friendlyfire-lifeheartspro-addon) · [SpigotMC](https://www.spigotmc.org/resources/friendlyfire-lifeheartspro-addon.134347/) · [Hangar](https://hangar.papermc.io/ABC_Nova/FriendlyFire) · [Source and releases](https://github.com/abcnova/FriendlyFire-LifeHeartsPro-Addon) · [Report an issue](https://github.com/abcnova/FriendlyFire-LifeHeartsPro-Addon/issues)

**Required plugin:** [LifeHeartsPro](https://github.com/abcnova/LifeHeartsPro) · [Modrinth](https://modrinth.com/plugin/lifeheartspro) · [CurseForge](https://www.curseforge.com/minecraft/bukkit-plugins/lifeheartspro) · [SpigotMC](https://www.spigotmc.org/resources/lifeheartspro.133466/) · [Hangar](https://hangar.papermc.io/ABC_Nova/LifeHeartsPro)

## Features

- Enable or block friendly fire independently for each LifeHeartsPro team.
- Protect melee attacks, arrows and other projectiles, tridents, splash and lingering potions, area-effect clouds, and optionally tamed wolves.
- Configure per-team defaults, bypass permission, attacker and victim messages, action bars, sounds, and feedback cooldowns.
- Manage settings with `/friendlyfire team <team> <on|off>` and inspect them with `/friendlyfire info <team>`.
- Update configuration and the four locale files without overwriting existing team settings.
- Check Modrinth for updates in the background and notify the console and server operators with Modrinth and GitHub links.

## Install

1. Install and enable LifeHeartsPro first.
2. Put `friendlyfire-lhp-addon-1.2.0.jar` in the server's `plugins` folder.
3. Restart the server. Bukkit loads the add-on after LifeHeartsPro and refuses to enable it if the required plugin is missing.

The add-on sets `teams.friendly-fire: true` in LifeHeartsPro when per-team control needs it. It reloads only LifeHeartsPro's configuration; it does not trigger a full gameplay reload or reset online lives.

## Compatibility

One Java 21 bytecode JAR targets the Bukkit API shared by Paper and Spigot. The API is compile-checked against Minecraft 1.21 and 26.3. Paper 1.21.x servers run on Java 21; Paper 26.1 and later require Java 25. The add-on uses no version-specific Minecraft internals.

## Commands and permissions

| Command | Permission | Default |
| --- | --- | --- |
| `/friendlyfire reload` | `friendlyfire.reload` | Operator |
| `/friendlyfire team <team> <on\|off>` | `friendlyfire.manage` | Operator |
| `/friendlyfire info <team>` | `friendlyfire.info` | Operator |

`friendlyfireguard.bypass` skips the add-on's team protection when `rules.bypass_permission_enabled` is enabled. `friendlyfire.update.notify` allows update notices; operators receive them automatically.

## Configuration and privacy

All options in `config.yml` and `sounds.yml` are documented in English. Messages are available in English, German, French and Spanish under `locale/`.

When update checks are enabled, the add-on makes a periodic request to the public Modrinth version API. It sends the add-on version in its User-Agent and does not include player names, server address, configuration or gameplay data.

## Build

Requires JDK 25 and Maven 3.9+.

```powershell
mvn -B clean verify
mvn -B -Pspigot-26.3 clean verify
mvn -B -Ppaper-1.21 clean verify
mvn -B -Ppaper-26.3 clean verify
```

Each build produces `target/friendlyfire-lhp-addon-1.2.0.jar`. Release artifacts are compiled against the lowest supported API so the same file serves all supported versions.

## Related project

FriendlyFire is a companion project for [LifeHeartsPro](https://github.com/abcnova/LifeHeartsPro). See its README for the companion link and LifeHeartsPro downloads.
