package com.astralsmp.commands;

import com.astralsmp.AstralThread;
import com.astralsmp.events.ClickEventCallback;
import com.astralsmp.modules.Config;
import com.astralsmp.modules.Database;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Команда для отвязки аккаунта через дискорд
 * Если игрока нет в базе данных, то отвязка не сработает
 * Не создавать инстансы, так как класс поддерживается EventHandler
 */
public class DiscordUnlink extends ListenerAdapter {

    /**
     * UNFINISHED_UNLINKING хранит UUID игрока, который начал привязку аккаунтов, но не закончил её.
     * ОБЯЗАТЕЛЬНО ВЫНОСИТЬ ПОЛЬЗОВАТЕЛЯ ИЗ СПИСКА ПРИ ВЫХОДЕ С СЕРВЕРА!
     */
    public static final Set<UUID> UNFINISHED_UNLINKING = new HashSet<>(16);

    /**
     * Метод - команда отвязки Майнкрафта от Дискорда через последний
     *
     * @param event вызывает метод при любом написанном сообщении в Дискорде
     */
    @SuppressWarnings("ConstantConditions")
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        // Если сообщение == !отвязать
        if (event.getMessage().getContentRaw().equals(AstralThread.PREFIX + "отвязать")) {
            User sender = event.getAuthor();
            MessageChannel channel = event.getChannel();
            // Проверка на наличие discord id в базе данных
            if (!Database.containsValue("discord_id = " + sender.getId(), "astral_linked_players")) {
                String message = sender.getName() + ", мне не удалось найти привязанный к Вашему дискорду аккаунт."
                        .replace("%sender", sender.getName());
                channel.sendMessageEmbeds(createEmbed(Config.getConfig().getString("discord.command.unlink.title"), "11", sender)).queue();
                return;
            }
            // Создание игрок от ника из базы данных
            Player target = Bukkit.getPlayer((String) Database.getObject("display_name", "discord_id = " + sender.getId(), "astral_linked_players"));
            // Попытка найти игрока на сервере
            if (target == null) {
                channel.sendMessage("Вы должны быть на сервере в момент отвязки аккаунтов").queue();
                return;
            }

            // Вторая проверка на правильность отвязки
            // Вообще хз зачем она тут нужна, но на всякий случай пока пусть будет
            String databaseUUID = (String) Database.getObject("uuid", "discord_id = " + sender.getId(), "astral_linked_players");
            UUID targetUUID = target.getUniqueId();
            if (!Objects.equals(databaseUUID, targetUUID.toString())) {
                System.out.println(Database.getObject("uuid", "discord_id = " + sender.getId(), "astral_linked_players"));
                channel.sendMessage("Данный игрок не привязан к вашему дискорд аккаунту.").queue();
                return;
            }
            if (UNFINISHED_UNLINKING.contains(targetUUID)) {
                channel.sendMessage("Вы не завершили предыдущую отвязку аккаунта").queue();
                return;
            }
            AtomicReference<Boolean> finished = new AtomicReference<>(false);
            UNFINISHED_UNLINKING.add(targetUUID);

            // Отвязка
            TextComponent unlink = new TextComponent("✔ Отвязать");
            unlink.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Отвязать аккаунт")));
            unlink.setColor(ChatColor.of(AstralThread.RED_COLOR));
            ClickEventCallback.execute(unlink, p -> {
                if (finished.get()) return;
                p.sendMessage("Ваш аккаунт более не привязан к " + sender.getAsTag());
                // Данные для удаления из бд
                Database.execute("DELETE FROM astral_linked_players WHERE discord_id = " + sender.getId());
                channel.sendMessage("Успешно отвязано!").queue();

                // чистка
                UNFINISHED_UNLINKING.remove(targetUUID);
                finished.set(true);
            });

            // Отмена отвязки
            TextComponent cancel = new TextComponent("⌀ Отмена");
            cancel.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Отменить привязку")));
            cancel.setColor(ChatColor.of(AstralThread.YELLOW_COLOR));
            ClickEventCallback.execute(cancel, p -> {
                if (finished.get()) return;
                p.sendMessage("Отвязка от аккаунта " + sender.getAsTag() + " была отменена.");
                channel.sendMessage(sender.getName() + ", отвязка аккаунтов была отменена.").queue();

                // чистка
                UNFINISHED_UNLINKING.remove(targetUUID);
                finished.set(true);
            });

            TextComponent slash = new TextComponent(" / ");
            target.spigot().sendMessage(unlink, slash, cancel);
        }

    }

    public MessageEmbed createEmbed(String title, String description, User sender) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(title);
        embedBuilder.setDescription(description);
        embedBuilder.setColor(new Color(67, 191, 90));
        embedBuilder.setFooter(sender.getAsTag(), sender.getAvatarUrl());
        embedBuilder.setTimestamp(Instant.now());
        return embedBuilder.build();
    }
}
