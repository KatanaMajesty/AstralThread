package com.astralsmp;

import com.astralsmp.events.TextComponentCallback;
import com.astralsmp.modules.Config;
import com.astralsmp.modules.Database;
import com.astralsmp.modules.Discord;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

/**
 * Главный класс проекта
 * Не создавать новые объекты класса: плагин не запустится
 * Избегать вызовов чего-либо из этого класса (заменять конструкторами)
 */
public class AstralThread extends JavaPlugin {

    public static final char PREFIX = '!';
    private Discord discord;

    public static String GREEN_COLOR;
    public static String RED_COLOR;
    public static String YELLOW_COLOR;
    public static String GRAY_COLOR;

    @Override
    public void onEnable() {
        // Секция для конфигурации (должна быть в первом приоритете)
        Config config = new Config(this);
        Map<String, Object> defaultValues = new HashMap<>();
        defaultValues.put("discord.init.token", "token");
        defaultValues.put("discord.init.activity", "Мяу мяу");
        defaultValues.put("discord.command.title", "Отвязка ⌀");

        defaultValues.put("color_palette.green", "#21db37");
        defaultValues.put("color_palette.red", "#fc3535");
        defaultValues.put("color_palette.yellow", "#f5c720");
        defaultValues.put("color_palette.gray", "#d1d1d1");

        config.setDefaultValues(defaultValues);
        config.initialize();

        // Получение переменных от дефолтных значений
        GREEN_COLOR = getConfig().getString("color_palette.green");
        RED_COLOR = getConfig().getString("color_palette.red");
        YELLOW_COLOR = getConfig().getString("color_palette.yellow");
        GRAY_COLOR = getConfig().getString("color_palette.gray");

        // Дискорд секция (низкий приоритет инициализации)
        discord = new Discord();
        discord.initialize(Config.getConfig().getString("discord.init.token"));

        // БД Секция
        Database database = new Database("astraldatabase.db");
        database.initialize();

        // Майнкрафт ивенты
        getServer().getPluginManager().registerEvents(new TextComponentCallback(), this);
    }

    @Override
    public void onDisable() {
        if (discord != null) {
            discord.unregister();
        }
    }
}
