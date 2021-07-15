package com.astralsmp.modules;

import com.astralsmp.commands.DiscordLink;
import com.astralsmp.commands.DiscordUnlink;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;

import javax.security.auth.login.LoginException;

public class Discord {

    public static final char PREFIX = '!';
    private JDA jda;

    public void initialize(String token) {
        try {
            jda = JDABuilder.createDefault(token)
                    .setStatus(OnlineStatus.DO_NOT_DISTURB)
                    .setActivity(Activity.listening("Гран-Куражъ"))
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
