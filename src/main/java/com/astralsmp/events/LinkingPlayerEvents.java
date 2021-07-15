package com.astralsmp.events;

import com.astralsmp.commands.DiscordLink;
import com.astralsmp.commands.DiscordUnlink;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * Не создавать объекты от класса!
 */
public class LinkingPlayerEvents implements Listener {

    /**
     * Если человек привязывал аккаунт, то при выходе с сервера он вновь получит доступ к привязке
     * @param event вызывается при выходе с сервера
     */
    @EventHandler
    public void onLinkingLeave(PlayerQuitEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();
        DiscordLink.UNFINISHED_LINKING.remove(playerUUID);
        DiscordUnlink.UNFINISHED_UNLINKING.remove(playerUUID);
    }

}
