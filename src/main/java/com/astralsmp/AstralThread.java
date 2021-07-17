package com.astralsmp;

import com.astralsmp.events.LinkingPlayerEvents;
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
        config.addDefaults(defaultValues);
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
        getServer().getPluginManager().registerEvents(new LinkingPlayerEvents(), this);
    }

    @Override
    public void onDisable() {
        if (discord != null) {
            discord.unregister();
        }
    }
}
