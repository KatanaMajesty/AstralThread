package com.astralsmp.modules;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Класс для работы с серверным конфигом
 * Все статические методы рекомендуется использовать перед инициализацией конфига
 */
public class Config {

    private Map<String, Object> defaultValues;
    private final Plugin plugin;
    private final FileConfiguration config;

    public Config(Plugin plugin) {
        this.plugin = plugin;
        config = this.plugin.getConfig();
    }

    /**
     * Инициализация конфига
     * При отсутствии конфига - создаётся новый в соответствующей папке
     * Наличие конфига в папке resources не обязательно при использовании этого метода
     */
    public void initialize() {
        // Проверка на наличие конфига в папке плагина
        File configFile = new File(plugin.getDataFolder() + File.separator + "config.yml");
        try {
            if (plugin.getDataFolder().mkdir() && configFile.createNewFile()) {
                System.out.println("Конфиг был успешно создан");
            } else {
                System.out.println("Конфиг был успешно загружен");
            }
        } catch (IOException exception) {
            exception.printStackTrace();
            System.out.println("Конфиг не был создан");
        }

        // инициализация дефолтных значений
        initDefaults();

        try {
            config.load(configFile);
        } catch (IOException | InvalidConfigurationException exception) {
            exception.printStackTrace();
            System.out.println("Не удалось загрузить конфиг");
        }
    }

    public void initDefaults() {
        if (defaultValues != null) {
            config.addDefaults(defaultValues);
            plugin.getConfig().options().copyDefaults(true);
            plugin.saveConfig();
        }
    }

    /**
     * Позволяет добавлять дефолтные значения в конфиг.
     * Вызывается перед инициализацией конфига
     * @see Config#initialize()
     *
     * @param defaultValues - коллекция интерфейса Map, которая хранит в себе ключ пути к объекту
     */
    public void setDefaultValues(Map<String, Object> defaultValues) {
        this.defaultValues = defaultValues;
    }

}
