package com.astralsmp.events;

import com.astralsmp.commands.DiscordLink;
import com.astralsmp.commands.DiscordUnlink;
import com.astralsmp.modules.Discord;
import net.dv8tion.jda.api.entities.User;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Не создавать объекты от класса!
 */
public class LinkingPlayerEvents implements Listener {

    /**
     * Если человек привязывал аккаунт, то при выходе с сервера он вновь получит доступ к привязке
     * @param event вызывается при выходе с сервера
     */
    // TODO: 17.07.2021 доделать связь с конфигом и сделать всё по красоте
    // подавление предупреждений чисто чтобы видеть разницу между callback и lamda expression
    @SuppressWarnings("all")
    @EventHandler
    public void onLinkingLeave(PlayerQuitEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();
        // Проверка на наличие игрока в списке отвязывающих свой аккаунт
        if (DiscordUnlink.UNFINISHED_UNLINKING.containsKey(playerUUID)) {
            // Присылает пользователю в лс сообщение о выходе игрока с сервера
            Discord.jda.retrieveUserById(DiscordUnlink.UNFINISHED_UNLINKING.get(playerUUID)).map((Function<User, Object>) user -> user).queue(user -> {
                User sender = (User) user;
                sender.openPrivateChannel().flatMap(privateChannel -> privateChannel.sendMessage("Время для отвязки аккаунта истекло или Вы вышли с сервера " +
                        "во время незавершённого процесса отвязки.")).queue(null, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) {
                        System.out.println("Закрыта личка");
                    }
                });
            });
            DiscordUnlink.UNFINISHED_UNLINKING.remove(playerUUID);
        }
        // Проверка на наличие игрока в списка привязывающих свой аккаунт
        if (DiscordLink.UNFINISHED_LINKING.containsKey(playerUUID)) {
            Discord.jda.retrieveUserById(DiscordLink.UNFINISHED_LINKING.get(playerUUID)).map(new Function<User, Object>() {
                @Override
                public Object apply(User user) {
                    return user;
                }
            }).queue(new Consumer<Object>() {
                @Override
                public void accept(Object user) {
                    User sender = (User) user;
                    sender.openPrivateChannel().flatMap(privateChannel -> privateChannel.sendMessage("Время для привязки аккаунта истекло или владелец аккаунта, " +
                            "к которому вы пытались привязать свой Дискорд аккаунт, вышел с сервера.")).queue(null, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) {
                            System.out.println("Закрыта личка");
                        }
                    });
                }
            });
            DiscordLink.UNFINISHED_LINKING.remove(playerUUID);
        }
    }

}
