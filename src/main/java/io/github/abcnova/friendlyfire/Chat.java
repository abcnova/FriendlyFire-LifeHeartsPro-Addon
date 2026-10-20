package io.github.abcnova.friendlyfire;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

final class Chat {
    private Chat() { }

    static String format(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    static void actionBar(Player player, String text) {
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                TextComponent.fromLegacyText(format(text)));
    }

}
