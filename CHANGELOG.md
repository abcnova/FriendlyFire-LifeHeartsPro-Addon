# FriendlyFire LHP Addon 1.1.3

- Modernized the LifeHeartsPro team API connection and cached its public method handles outside the damage-event path.
- Kept the required LifeHeartsPro dependency explicit; the add-on cannot start without it.
- Avoided triggering LifeHeartsPro's full gameplay reload when enabling its global team-damage gate.
- Enabled the optional Modrinth update checker by default, with a configurable 6–168 hour interval and OP/admin-only notices linking to Modrinth and GitHub.
- Added bounded feedback cleanup, sound caching, and player-quit cleanup for temporary player data.
- Relocated the add-on package to a public `io.github.abcnova` namespace and added English, German, French, and Spanish update messages.
- Kept the existing Paper and Spigot plugin identity, command aliases, team settings, and config migration behavior.
- Added build profiles for Spigot and Paper API compatibility checks at 1.21 and 26.3.
