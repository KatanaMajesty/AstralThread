package com.astralsmp.commands;

import com.astralsmp.AstralThread;
import com.astralsmp.events.TextComponentCallback;
import com.astralsmp.modules.Formatter;
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
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class DiscordLink extends ListenerAdapter {

    /**
     * UNFINISHED_LINKING хранит UUID игрока, который начал привязку аккаунтов, но не закончил её.
     * ОБЯЗАТЕЛЬНО ВЫНОСИТЬ ПОЛЬЗОВАТЕЛЯ ИЗ СПИСКА ПРИ ВЫХОДЕ С СЕРВЕРА!
     */
    public static final Set<UUID> UNFINISHED_LINKING = new HashSet<>(16);
    private static final Map<String, UUID> MINECRAFT_SPAM_MAP = new HashMap<>();
    // Навести красоту с помощью конфига!
    private static final FileConfiguration config = Config.getConfig();
    private final String DATABASE_TABLE = "astral_linked_players";

    /**
     * Команда привязки аккаунта дискорд к майнкрафт
     * @param event вызывает метод при любом сообщении от пользователя
     */
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        // Если сообщение равно !привязать
        if (event.getMessage().getContentRaw().startsWith(AstralThread.PREFIX + "привязать")) {
            String message = event.getMessage().getContentRaw();
            User sender = event.getAuthor();
            MessageChannel channel = event.getChannel();
            String[] args = message.split(" ");
            // Если не 2 аргумента в сообщении
            if (args.length != 2) {
                channel.sendMessageEmbeds(errorEmbed("Неверное количество аргументов", "Для выполнения команды нужно 2 аргумента. Было обработано: " + args.length +
                        "\nСинтаксис команды: `" + AstralThread.PREFIX + "привязать [ник в Майнкрафт]`", sender)).queue();
                return;
            }
            Player target = Bukkit.getPlayer(args[1]);
            // Если игрок присутствует на сервере
            if (target != null) {
                // Если в бд уже есть привязанный дискорд аккаунт
                if (Database.containsValue("discord_id = " + sender.getId(), DATABASE_TABLE)) {
                    channel.sendMessageEmbeds(errorEmbed("Ошибка привязки", "Не удалось привязать аккаунт " + target.getDisplayName() +
                            " к Вашему Дискорд аккаунту, так как к вам уже привязан аккаунт " +
                            Database.getObject("display_name", "discord_id = " + sender.getId(), DATABASE_TABLE) +
                            ". Вы можете отвязать его используя кооманду `!отвязать`", sender)).queue();
                    return;
                }
                // Если в бд уже есть майнкрафт аккаунт, к которому попытались привязать дискорд
                if (Database.containsValue(String.format("uuid = '%s'", target.getUniqueId()), DATABASE_TABLE)) {
                    channel.sendMessage("Невозможно привязать дискорд к аккаунту " + target.getDisplayName() + ", так как он уже имеет привязку").queue();
                    return;
                }
                // Проверка на наличие пользователя в списке спама
                if (MINECRAFT_SPAM_MAP.containsKey(sender.getId()) && MINECRAFT_SPAM_MAP.get(sender.getId()).equals(target.getUniqueId())) {
                    channel.sendMessage("Невозможно выполнить привязку к данному аккаунту," +
                            "так как его владелец обозначил Ваши попытки привязки спамом.").queue();
                    return;
                }
                UUID targetUUID = target.getUniqueId();
                // Если игрок не завершил предыдущую привязку
                if (UNFINISHED_LINKING.contains(targetUUID)) {
                    channel.sendMessage("Владелец этого майнкрафт аккаунта не завершил предыдущую привязку").queue();
                    return;
                }
                // Обозначение незаконченной привязки
                UNFINISHED_LINKING.add(targetUUID);
                AtomicReference<Boolean> finished = new AtomicReference<>(false);

                // Привязка аккаунта - подтверждение
                TextComponent link = new TextComponent("✔ Привязать");
                link.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new Text(Formatter.colorize(String.format("&%sПривязать аккаунт к Дискорду", AstralThread.GRAY_COLOR)))));
                link.setColor(ChatColor.of(AstralThread.GREEN_COLOR));

                // Функции при нажатии
                TextComponentCallback.execute(link, p -> {
                    // Если игрок уже нажал на кнопку и пытается нажать на другую
                    if (finished.get()) return;

                    // Ответ пользователю в случае отсутствия ошибок
                    p.sendMessage("Ваш аккаунт был успешно привязан к " + sender.getAsTag());
                    channel.sendMessage(sender.getAsTag() + " привязан к аккаунту " + target.getDisplayName()).queue();
                    // Данные для вноса в бд
                    Object[] userInfo = {target.getUniqueId(), target.getDisplayName(), sender.getId()};
                    Database.insertValues(userInfo, DATABASE_TABLE);

                    // Обозначение завершения привязки
                    UNFINISHED_LINKING.remove(targetUUID);
                    finished.set(true);
                });

                // Отмена привязки
                TextComponent cancel = new TextComponent("⌀ Отмена");
                cancel.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new Text(Formatter.colorize(String.format("&%sОтменить привязку", AstralThread.GRAY_COLOR))))
                );
                cancel.setColor(ChatColor.of(AstralThread.RED_COLOR));

                // Функции при нажатии
                TextComponentCallback.execute(cancel, p -> {
                    // Если игрок уже нажал на кнопку и пытается нажать на другую
                    if (finished.get()) return;

                    // Ответ пользователю в случае отсутствия ошибок
                    p.sendMessage("Привязка к аккаунту " + sender.getAsTag() + " была отменена.");
                    channel.sendMessage(sender.getName() + ", привязка аккаунтов была отменена.").queue();

                    // Обозначение завершения привязки
                    UNFINISHED_LINKING.remove(targetUUID);
                    finished.set(true);
                });

                // Обозначить спамом
                // Тут нужно дописать отправку жалобы на спам всем модераторам, которые в сети
                TextComponent spam = new TextComponent("✎ Спам");
                spam.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new Text(Formatter.colorize(String.format("&%sОбозначить попытки привязки спамом", AstralThread.GRAY_COLOR)))));
                spam.setColor(ChatColor.of(AstralThread.YELLOW_COLOR));

                // Функции при нажатии
                TextComponentCallback.execute(spam, p -> {
                    // Если игрок уже нажал на кнопку и пытается нажать на другую
                    if (finished.get()) return;

                    // Ответ пользователю в случае отсутствия ошибок
                    p.sendMessage("Попытки привязки от данного игрока более Вас не потревожат, модераторы получат уведомление о спаме.");
                    MINECRAFT_SPAM_MAP.put(sender.getId(), target.getUniqueId());

                    // Обозначение завершения привязки
                    UNFINISHED_LINKING.remove(targetUUID);
                    finished.set(true);
                });

                TextComponent slash = new TextComponent(" / ");
                slash.setColor(ChatColor.of(AstralThread.GRAY_COLOR));

                target.spigot().sendMessage(link, slash, cancel, slash, spam);
            } else {
                channel.sendMessage("Нет такого игрока на сервере").queue();
            }
        }
    }

    public MessageEmbed errorEmbed(String title, String description, User sender) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(title);
        embedBuilder.setDescription(description);
        embedBuilder.setColor(Formatter.hexColorToRGB(AstralThread.RED_COLOR));
        embedBuilder.setFooter(sender.getAsTag(), sender.getAvatarUrl());
        embedBuilder.addField(AstralThread.PREFIX + "помощь","Просмотреть список всех доступных команд", true);
        embedBuilder.setTimestamp(Instant.now());
        return embedBuilder.build();
    }

}
