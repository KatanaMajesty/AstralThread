package com.astralsmp.events;

import com.astralsmp.modules.Discord;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class DiscordLink extends ListenerAdapter {

    private static final char PREFIX = Discord.PREFIX;

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        System.out.println(PREFIX);
        if (event.getMessage().getContentRaw().startsWith(PREFIX + "привязать")) {
            String message = event.getMessage().getContentRaw();
            String[] args = message.split(" ");
            if (args.length == 2) {
                Player target = Bukkit.getPlayer(args[1]);
                if (target != null && target.isOnline()) { // FIXME: 14.07.2021 завершить бы
                    TextComponent link = new TextComponent("Привязать");
                    link.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Привязать аккаунт")));
                    ClickEventCallback.execute(link, p -> p.sendMessage("привязано"));

                    TextComponent cancel = new TextComponent("Отмена");
                    cancel.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Отменить привязку")));
                    ClickEventCallback.execute(cancel, p -> p.sendMessage("отменено"));

                    TextComponent spam = new TextComponent("Спам");
                    spam.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Обозначить попытки привязки спамом")));
                    ClickEventCallback.execute(spam, p -> p.sendMessage("добавлено в спам"));

                    TextComponent slash = new TextComponent(" / ");
                    target.spigot().sendMessage(link, slash, cancel, slash, spam);
                } else {
                    System.out.println("Игрока нет");
                }
            } else {
                System.out.println("не 2 аргумента");
            }
        } else {
            System.out.println("Не равно");
        }
    }
}
