package com.astralsmp.commands;

import com.astralsmp.AstralThread;
import com.astralsmp.events.TextComponentCallback;
import com.astralsmp.modules.*;
import com.astralsmp.modules.Formatter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class DiscordLink extends ListenerAdapter {

    private static final String LINK_TITLE = Config.getConfig().getString("discord.command.link.title");
    private static final String LINK_NO_PERM = Config.getConfig().getString("discord.command.link.no_perm");
    private static final String LINK_COOLDOWN = Config.getConfig().getString("discord.command.link.cooldown");
    private static final String LINK_NO_ARGUMENTS = Config.getConfig().getString("discord.command.link.no_arguments");
    private static final String LINK_ALREADY_LINKED = Config.getConfig().getString("discord.command.link.already_linked");
    private static final String LINK_NOT_WHITELISTED = Config.getConfig().getString("discord.command.link.not_whitelisted");
    private static final String LINK_PLAYER_OFFLINE = Config.getConfig().getString("discord.command.link.player_offline");
    private static final String LINK_SPAM_REPORTED = Config.getConfig().getString("discord.command.link.spam_reported");
    private static final String LINK_NOT_FINISHED = Config.getConfig().getString("discord.command.link.not_finished");
    private static final String LINK_SENT = Config.getConfig().getString("discord.command.link.sent");
    private static final String LINK_SUCCESS = Config.getConfig().getString("discord.command.link.success");
    private static final String LINK_CANCELED = Config.getConfig().getString("discord.command.link.canceled");
    private static final String LINK_SPAMMED = Config.getConfig().getString("discord.command.link.spammed");
    private static final String COMMAND_HELP_TITLE = Config.getConfig().getString("discord.command.help_title");
    private static final String COMMAND_HELP_FIELD = Config.getConfig().getString("discord.command.help_field");

    /**
     * UNFINISHED_LINKING хранит UUID игрока, который начал привязку аккаунтов, но не закончил её.
     * ОБЯЗАТЕЛЬНО ВЫНОСИТЬ ПОЛЬЗОВАТЕЛЯ ИЗ СПИСКА ПРИ ВЫХОДЕ С СЕРВЕРА!
     * @see com.astralsmp.events.LinkingPlayerEvents#onLinkingLeave(PlayerQuitEvent) 
     */
    public static final Map<UUID, String> UNFINISHED_LINKING = new HashMap<>();
    private static final Map<String, UUID> MINECRAFT_SPAM_MAP = new HashMap<>();
    private static final Set<String> DM_DISABLED = new HashSet<>();
    private final DiscordCooldownManager COOLDOWN_MANAGER = new DiscordCooldownManager();
    private final String DATABASE_TABLE = "astral_linked_players";

    private boolean hasRole(Member member, String roleID) {
        List<Role> roles = member.getRoles();
        Role neededRole = roles.stream().filter(role -> role.getId().equals(roleID)).findFirst().orElse(null);
        return neededRole != null;
    }
    /**
     * Команда привязки аккаунта дискорд к майнкрафт
     * @param event вызывает метод при любом сообщении от пользователя
     */
    // Чтобы видеть разницу между lambda и callback
    @SuppressWarnings("ConstantConditions")
    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
        // Если сообщение равно !привязать
        if (event.getMessage().getContentRaw().startsWith(Discord.PREFIX + "привязать")) {
            String message = event.getMessage().getContentRaw();
            MessageChannel channel = event.getChannel();
            String[] args = message.split(" ");
            User sender = event.getAuthor();
            if (event.getMessage().getEmbeds().isEmpty()) {
                Member member = event.getMember();
                // Cooldown manager
                long timeLeft = System.currentTimeMillis() - COOLDOWN_MANAGER.getCooldown(sender.getId());
                long secondsLeft = TimeUnit.MILLISECONDS.toSeconds(timeLeft);
                if (member != null) {
                    if (!hasRole(member, "741639199861506179")) { // РОЛЬ МОЖЕТ МЕНЯТЬСЯ В КОНФИГЕ! TODO: реализовать через конфиг
                        if (secondsLeft < DiscordCooldownManager.DEFAULT_COOLDOWN) {
                            int[] formattedLeft = DiscordCooldownManager.splitTimeArray(DiscordCooldownManager.DEFAULT_COOLDOWN - secondsLeft);
                            channel.sendMessageEmbeds(errorEmbed(LINK_TITLE, String.format(LINK_COOLDOWN, formattedLeft[1], formattedLeft[2]), false, sender)).queue();
                            return;
                        }
                        // Место для добавления кд!
                        COOLDOWN_MANAGER.setCooldown(sender.getId(), System.currentTimeMillis());
                    }
                } else {
                    System.out.println("Не удалось установить кд для пользователя " + sender.getAsTag());
                }
            }

            // Если не 2 аргумента в сообщении
            if (args.length != 2) {
                channel.sendMessageEmbeds(errorEmbed(LINK_TITLE, String.format(LINK_NO_ARGUMENTS, args.length - 1, Discord.PREFIX), true, sender)).queue();
                return;
            }

            Player target = Bukkit.getPlayer(args[1]);
            // Если игрок не присутствует на сервере
            if (target != null && target.isOnline()) {
                // Если игрока нет в вайтлисте сервера
                // TODO: 17.07.2021 режим гостя позволит игрокам находиться на сервере не будучи в вайтлисте
                // FIXME: 17.07.2021 проверить на работоспособность на всякий
                if (Bukkit.getServer().hasWhitelist()) {
                    if (!target.isWhitelisted()) {
                        channel.sendMessageEmbeds(errorEmbed(LINK_TITLE, String.format(LINK_NOT_WHITELISTED, target.getDisplayName()), true, sender)).queue();
                        return;
                    }
                }
            } else {
                channel.sendMessageEmbeds(errorEmbed(LINK_TITLE, String.format(LINK_PLAYER_OFFLINE, args[1]), true, sender)).queue();
                return;
            }
            // Если у игрока нет определённых прав на привязку аккаунта
            String permission = "astralsmp.link";
            if (!target.hasPermission(permission)) {
                channel.sendMessageEmbeds(errorEmbed(LINK_TITLE, String.format(LINK_NO_PERM, permission), true, sender)).queue();
                return;
            }

            // Если в бд уже есть привязанный дискорд аккаунт
            if (Database.containsValue("discord_id = " + sender.getId(), DATABASE_TABLE)) {
                channel.sendMessageEmbeds(errorEmbed(LINK_TITLE, String.format(LINK_ALREADY_LINKED, target.getDisplayName(),
                        Database.getObject("display_name", "discord_id = " + sender.getId(), DATABASE_TABLE)), true, sender)).queue();
                return;
            }
            // Если в бд уже есть майнкрафт аккаунт, к которому попытались привязать дискорд
            if (Database.containsValue(String.format("uuid = '%s'", target.getUniqueId()), DATABASE_TABLE)) {
                channel.sendMessageEmbeds(errorEmbed(LINK_TITLE, String.format(LINK_ALREADY_LINKED, target.getDisplayName()), false, sender)).queue();
                return;
            }
            // Проверка на наличие пользователя в списке спама
            if (MINECRAFT_SPAM_MAP.containsKey(sender.getId()) && MINECRAFT_SPAM_MAP.get(sender.getId()).equals(target.getUniqueId())) {
                channel.sendMessageEmbeds(errorEmbed(LINK_TITLE, LINK_SPAM_REPORTED, true, sender)).queue();
                return;
            }
            UUID targetUUID = target.getUniqueId();
            // Если игрок не завершил предыдущую привязку
            if (UNFINISHED_LINKING.containsKey(targetUUID)) {
                channel.sendMessageEmbeds(errorEmbed(LINK_TITLE, LINK_NOT_FINISHED, false, sender)).queue();
                return;
            }

            // Отправка привязки в лс
            try {
                sender.openPrivateChannel().flatMap(privateChannel -> {
                    EmbedBuilder builder = new EmbedBuilder();
                    builder.setTitle(LINK_TITLE);
                    builder.setDescription(String.format(LINK_SENT, sender.getName(), target.getDisplayName()));
                    builder.setColor(Formatter.hexColorToRGB(AstralThread.GREEN_COLOR));
                    builder.setFooter(sender.getAsTag(), sender.getAvatarUrl());
                    builder.setTimestamp(Instant.now());
                    return privateChannel.sendMessageEmbeds(builder.build());
                }).queue(message1 -> DM_DISABLED.remove(sender.getId()), throwable -> DM_DISABLED.add(sender.getId()));
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                System.out.println("Не удалось открыть ЛС пользователя. Ошибка в коде на строке " + new Throwable().getStackTrace()[0].getLineNumber());
                return;
            }
            if (DM_DISABLED.contains(sender.getId())) {
                channel.sendMessage("Бот не смог отправить вам сообщение в ЛС. Для взаимодействия бота откройте ЛС от участников сервера").queue();
                return;
            }

            // Обозначение незаконченной привязки
            UNFINISHED_LINKING.put(targetUUID, sender.getId());
            // Атомное значение типа логический для проверки на нажатые кнопки
            // Если кнопка была нажата - другие не будут выполнять свой функционал
            AtomicReference<Boolean> clicked = new AtomicReference<>(false);

            // Привязка аккаунта - подтверждение
            target.sendMessage(Formatter.colorize(String.format("&%sПредложение о привязке аккаунта от " + sender.getAsTag() + "." +
                    "\nЕсли это не Ваш Дискорд аккаунт - нажмите на &%s\"⌀ Отмена\"&%s." +
                    "\nПри повторных попытках привязки нажмите &%s\"✎ Спам\"&%s.",
                    AstralThread.GRAY_COLOR,
                    AstralThread.RED_COLOR,
                    AstralThread.GRAY_COLOR,
                    AstralThread.YELLOW_COLOR,
                    AstralThread.GRAY_COLOR)));
            TextComponent link = new TextComponent("✔ Привязать");
            link.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(Formatter.colorize("&7Привязать аккаунт к Дискорду"))));
            link.setColor(ChatColor.of(AstralThread.GREEN_COLOR));
            // Функции при нажатии
            TextComponentCallback.execute(link, p -> {
                // Если игрок уже нажал на кнопку и пытается нажать на другую
                if (clicked.get()) return;

                // Ответ пользователю в случае отсутствия ошибок
                p.sendMessage(Formatter.colorize(String.format("&%sВаш аккаунт был успешно привязан к %s", AstralThread.GREEN_COLOR, sender.getAsTag())));
                EmbedBuilder builder = new EmbedBuilder();
                builder.setTitle(LINK_TITLE);
                builder.setDescription(String.format(LINK_SUCCESS, sender.getName(), target.getDisplayName()));
                builder.setColor(Formatter.hexColorToRGB(AstralThread.GREEN_COLOR));
                builder.setFooter(sender.getAsTag(), sender.getAvatarUrl());
                builder.setTimestamp(Instant.now());
                // присылает эмбед в лс участнику
                sender.openPrivateChannel().flatMap(privateChannel -> privateChannel.sendMessageEmbeds(builder.build())).queue();
                // Данные для вноса в бд
                Object[] userInfo = {target.getUniqueId(), target.getDisplayName(), sender.getId()};
                Database.insertValues(userInfo, DATABASE_TABLE);

                // Обозначение завершения привязки
                UNFINISHED_LINKING.remove(targetUUID);
                clicked.set(true);
            });

            // Отмена привязки
            TextComponent cancel = new TextComponent("⌀ Отмена");
            cancel.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(Formatter.colorize("&7Отменить привязку"))));
            cancel.setColor(ChatColor.of(AstralThread.RED_COLOR));
            // Функции при нажатии
            TextComponentCallback.execute(cancel, p -> {
                // Если игрок уже нажал на кнопку и пытается нажать на другую
                if (clicked.get()) return;

                // Ответ пользователю в случае отсутствия ошибок
                p.sendMessage(Formatter.colorize(String.format("&%sПривязка к аккаунту %s была отменена.", AstralThread.RED_COLOR, sender.getAsTag())));
                sender.openPrivateChannel().flatMap(privateChannel -> privateChannel.sendMessageEmbeds(errorEmbed(LINK_TITLE,
                        String.format(LINK_CANCELED, sender.getName(), target.getDisplayName()), true, sender))).
                        queue();
                // Обозначение завершения привязки
                UNFINISHED_LINKING.remove(targetUUID);
                clicked.set(true);
            });

            // Обозначить спамом
            // Тут нужно дописать отправку жалобы на спам всем модераторам, которые в сети
            TextComponent spam = new TextComponent("✎ Спам");
            spam.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(Formatter.colorize("&7Обозначить попытки привязки спамом"))));
            spam.setColor(ChatColor.of(AstralThread.YELLOW_COLOR));
            // Функции при нажатии
            TextComponentCallback.execute(spam, p -> {
                // Если игрок уже нажал на кнопку и пытается нажать на другую
                if (clicked.get()) return;

                // Ответ пользователю в случае отсутствия ошибок
                p.sendMessage(Formatter.colorize(String.format("&%sПопытки привязки от данного игрока более Вас не потревожат, модераторы получат уведомление о спаме.",
                        AstralThread.YELLOW_COLOR)));
                sender.openPrivateChannel().flatMap(privateChannel -> privateChannel.sendMessageEmbeds(
                        errorEmbed(LINK_TITLE, String.format(LINK_SPAMMED, target.getDisplayName()), true, sender))).queue();
                MINECRAFT_SPAM_MAP.put(sender.getId(), target.getUniqueId());

                // Обозначение завершения привязки
                UNFINISHED_LINKING.remove(targetUUID);
                clicked.set(true);
            });

            TextComponent slash = new TextComponent(" / ");
            slash.setColor(ChatColor.of(AstralThread.GRAY_COLOR));
            target.spigot().sendMessage(link, slash, cancel, slash, spam);
        }
    }

    public MessageEmbed errorEmbed(String title, String description, boolean helpField, User sender) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(title);
        embedBuilder.setDescription(description);
        embedBuilder.setColor(Formatter.hexColorToRGB(AstralThread.RED_COLOR));
        if (sender != null) embedBuilder.setFooter(sender.getAsTag(), sender.getAvatarUrl());
        if (helpField) embedBuilder.addField(COMMAND_HELP_TITLE, COMMAND_HELP_FIELD, true);
        embedBuilder.setTimestamp(Instant.now());
        return embedBuilder.build();
    }

}
