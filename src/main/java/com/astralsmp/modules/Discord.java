package com.astralsmp.modules;

import com.astralsmp.commands.DiscordLink;
import com.astralsmp.commands.DiscordUnlink;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;

import javax.security.auth.login.LoginException;

public class Discord {

    private static final String ACTIVITY = Config.getConfig().getString("discord.init.activity");

    public static final char PREFIX = '!';
    public static JDA jda;

    public void initialize(String token) {
        try {
            assert ACTIVITY != null;
            jda = JDABuilder.createDefault(token)
                    .setStatus(OnlineStatus.DO_NOT_DISTURB)
                    .setActivity(Activity.listening(ACTIVITY))
                    .build();
            jda.addEventListener(new DiscordLink());
            jda.addEventListener(new DiscordUnlink());
            jda.awaitReady();
            System.out.println("Дискорд бот запущен");
        } catch (LoginException | InterruptedException exception) {
            exception.printStackTrace();
            System.out.println("Не удалось инициализировать бота");
        }
    }

    public void unregister() {
        if (jda != null) {
            jda.shutdown();
            System.out.println("Дискорд бот выключен");
        }
    }

}
