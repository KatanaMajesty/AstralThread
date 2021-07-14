package com.astralsmp.events;

import com.astralsmp.modules.Database;
import com.astralsmp.modules.Discord;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicReference;

public class DiscordLink extends ListenerAdapter {

    private static final char PREFIX = Discord.PREFIX;

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getMessage().getContentRaw().startsWith(PREFIX + "привязать")) {
            String message = event.getMessage().getContentRaw();
            User sender = event.getAuthor();
            MessageChannel channel = event.getChannel();
            String[] args = message.split(" ");
            if (args.length == 2) {
                Player target = Bukkit.getPlayer(args[1]);
                if (target != null && target.isOnline()) { // FIXME: 14.07.2021 завершить бы
                    AtomicReference<Boolean> finished = new AtomicReference<>(false);

                    TextComponent link = new TextComponent("Привязать");
                    link.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Привязать аккаунт")));
                    ClickEventCallback.execute(link, p -> {
                        if (finished.get()) return;
                        p.sendMessage("привязано");
                        channel.sendMessage(sender.getAsTag() + " привязан к аккаунту " + target.getDisplayName()).queue();
                        Object[] userInfo = {target.getUniqueId(), target.getDisplayName(), sender.getId()};
                        Database.insertValues(userInfo, "astral_linked_players");
                        finished.set(true);
                    });

                    TextComponent cancel = new TextComponent("Отмена");
                    cancel.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Отменить привязку")));
                    ClickEventCallback.execute(cancel, p -> {
                        if (finished.get()) return;
                        p.sendMessage("отменено");
                        channel.sendMessage(sender.getName() + ", привязка аккаунтов была отменена.").queue();
                        finished.set(true);
                    });

                    TextComponent spam = new TextComponent("Спам");
                    spam.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Обозначить попытки привязки спамом")));
                    ClickEventCallback.execute(spam, p -> {
                        if (finished.get()) return;
                        p.sendMessage("добавлено в спам");
                        finished.set(true);
                    });

                    TextComponent slash = new TextComponent(" / ");
                    target.spigot().sendMessage(link, slash, cancel, slash, spam);
                } else {
                    channel.sendMessage("Нет такого игрока на сервере").queue();
                }
            } else {
                channel.sendMessage("Не 2 аргумента").queue();
            }
        }
    }
}
