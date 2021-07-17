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

public class DiscordLink extends ListenerAdapter {

    /**
     * UNFINISHED_LINKING хранит UUID игрока, который начал привязку аккаунтов, но не закончил её.
     * ОБЯЗАТЕЛЬНО ВЫНОСИТЬ ПОЛЬЗОВАТЕЛЯ ИЗ СПИСКА ПРИ ВЫХОДЕ С СЕРВЕРА!
     * @see com.astralsmp.events.LinkingPlayerEvents#onLinkingLeave(PlayerQuitEvent) 
     */
    public static final Map<UUID, String> UNFINISHED_LINKING = new HashMap<>();
    private static final Map<String, UUID> MINECRAFT_SPAM_MAP = new HashMap<>();
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
                    if (!hasRole(member, "741639199861506179")) {
                        if (secondsLeft < DiscordCooldownManager.DEFAULT_COOLDOWN) {
                            int[] formattedLeft = DiscordCooldownManager.splitTimeArray(DiscordCooldownManager.DEFAULT_COOLDOWN - secondsLeft);
                            channel.sendMessage(String.format(
//                        "Подождите %02d минут(ы) %02d секунд(ы) перед использованием привязки снова", на всякий пусть будет
                                    "Подождите %d минут(ы) %d секунд(ы) перед использованием привязки снова",
                                    formattedLeft[1],
                                    formattedLeft[2]
                            )).queue();
                            return;
                        }
                        // Место для добавления кд!
                        COOLDOWN_MANAGER.setCooldown(sender.getId(), System.currentTimeMillis());
                    }
                } else {
                    System.out.println("Member is null...");
                }
            }

            // Если не 2 аргумента в сообщении
            if (args.length != 2) {
                channel.sendMessageEmbeds(errorEmbed("Неверное количество аргументов", "Для выполнения команды нужно 2 аргумента. Было обработано: " + args.length +
                        "\nСинтаксис команды: `" + Discord.PREFIX + "привязать [ник в Майнкрафт]`", sender)).queue();
                return;
            }
            Player target = Bukkit.getPlayer(args[1]);
            // Если игрок не присутствует на сервере
            if (target != null) {
                // Если игрока нет в вайтлисте сервера
                // TODO: 17.07.2021 режим гостя позволит игрокам находиться на сервере не будучи в вайтлисте
                // FIXME: 17.07.2021 проверить на работоспособность на всякий
                if (Bukkit.getServer().hasWhitelist()) {
                    if (!target.isWhitelisted()) {
                        channel.sendMessage("Этот игрок не в вайтлисте сервера").queue();
                        return;
                    }
                }
            } else {
                channel.sendMessage("Нет такого игрока на сервере").queue();
                return;
            }
            // Если у игрока нет определённых прав на привязку аккаунта
            String permission = "astralsmp.link";
            if (!target.hasPermission(permission)) {
                channel.sendMessage(String.format("У вас нет прав для привязки своего аккаунта." +
                        " Обратитесь к модерации за помощью. *Отсутствуют права:* `%s`", permission)).queue();
            }

            // Если в бд уже есть привязанный дискорд аккаунт
            if (Database.containsValue("discord_id = " + sender.getId(), DATABASE_TABLE)) {
                channel.sendMessageEmbeds(errorEmbed("Ошибка привязки", "Не удалось привязать аккаунт " + target.getDisplayName() +
                        " к Вашему Дискорд аккаунту, так как к вам уже привязан аккаунт " +
                        Database.getObject("display_name", "discord_id = " + sender.getId(), DATABASE_TABLE) +
                        ". Вы можете отвязать его используя команду `!отвязать`", sender)).queue();
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
            if (UNFINISHED_LINKING.containsKey(targetUUID)) {
                channel.sendMessage("Владелец этого майнкрафт аккаунта не завершил предыдущую привязку").queue();
                return;
            }
            // Обозначение незаконченной привязки
            UNFINISHED_LINKING.put(targetUUID, sender.getId());
            // Атомное значение типа логический для проверки на нажатые кнопки
            // Если кнопка была нажата - другие не будут выполнять свой функционал
            AtomicReference<Boolean> clicked = new AtomicReference<>(false);

            // Привязка аккаунта - подтверждение
            TextComponent link = new TextComponent("✔ Привязать");
            link.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(Formatter.colorize("&7Привязать аккаунт к Дискорду"))));
            link.setColor(ChatColor.of(AstralThread.GREEN_COLOR));
            // Функции при нажатии
            TextComponentCallback.execute(link, p -> {
                // Если игрок уже нажал на кнопку и пытается нажать на другую
                if (clicked.get()) return;

                // Ответ пользователю в случае отсутствия ошибок
                p.sendMessage("Ваш аккаунт был успешно привязан к " + sender.getAsTag());
                channel.sendMessage(sender.getAsTag() + " привязан к аккаунту " + target.getDisplayName()).queue();
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
                p.sendMessage("Привязка к аккаунту " + sender.getAsTag() + " была отменена.");
                channel.sendMessage(sender.getName() + ", привязка аккаунтов была отменена.").queue();

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
                p.sendMessage("Попытки привязки от данного игрока более Вас не потревожат, модераторы получат уведомление о спаме.");
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

    public MessageEmbed errorEmbed(String title, String description, User sender) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(title);
        embedBuilder.setDescription(description);
        embedBuilder.setColor(Formatter.hexColorToRGB(AstralThread.RED_COLOR));
        embedBuilder.setFooter(sender.getAsTag(), sender.getAvatarUrl());
        embedBuilder.addField(Discord.PREFIX + "помощь","Просмотреть список всех доступных команд", true);
        embedBuilder.setTimestamp(Instant.now());
        return embedBuilder.build();
    }

}
