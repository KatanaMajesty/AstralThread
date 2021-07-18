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
    private static FileConfiguration config;

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

    /**
     * Метод инициализирует дефолтные значения в конфиге
     * Данный метод не должен использоваться в каких-либо инстансах класса
     */
    private void initDefaults() {
        if (defaultValues != null) {
            config.addDefaults(defaultValues);
            plugin.getConfig().options().copyDefaults(true);
            plugin.saveConfig();
        }
    }

    /**
     * Добавляет дефолтные значения для конфига через карту
     * @param defaultValues карта K,V со всеми значениями для конфига
      */
    public void addDefaults(Map<String, Object> defaultValues) {
        // Дискорд
        defaultValues.put("discord.init.token", "token");
        defaultValues.put("discord.init.activity", "Мяу мяу");

        // Отвязка аккаунтов
        defaultValues.put("discord.command.unlink.title", "Отвязка аккаунтов");
        defaultValues.put("discord.command.unlink.no_account", "%sender%, мне не удалось найти привязанный к Вашему дискорду аккаунт.");
        defaultValues.put("discord.command.unlink.not_linked", "Данный игрок не привязан к вашему дискорд аккаунту.");
        defaultValues.put("discord.command.unlink.previous", "Вы не завершили предыдущую отвязку аккаунта");
        defaultValues.put("discord.command.unlink.success", "Успешно отвязано!");
        defaultValues.put("discord.command.unlink.canceled", "%sender%, отвязка аккаунтов была отменена.");
        defaultValues.put("discord.command.unlink.minecraft.success", "Ваш аккаунт более не привязан к %sender%");
        defaultValues.put("discord.command.unlink.minecraft.canceled", "Отвязка от аккаунта %sender% была отменена.");

        // Привязка аккаунтов
        defaultValues.put("discord.command.link.title", "Привязка аккаунтов");
        defaultValues.put("discord.command.link.cooldown", "Подождите %d минут(ы) %d секунд(ы) перед использованием привязки снова");
        defaultValues.put("discord.command.link.no_arguments", "Для выполнения команды нужен 1 аргумент. Было обработано: %d\nСинтаксис команды: `%sпривязать [ник в Майнкрафт]`");
        defaultValues.put("discord.command.link.already_linked", "Не удалось привязать аккаунт %s к вашему Дискорд аккаунту, так как последний уже привязан к %s. " +
                "\nВы можете отвязать его используя команду `!отвязать`");
        defaultValues.put("discord.command.link.not_whitelisted", "Игрок %s не в вайтлисте сервера. Доступ к привязке аккаунтов имеют только игроки проекта");
        defaultValues.put("discord.command.link.player_offline", "Игрок %s не был найден на сервере. Для привязки аккаунта Вы должны находиться на нашем Майнкрафт сервере");
        defaultValues.put("discord.command.link.spam_reported", "Невозможно выполнить привязку к данному аккаунту, так как его владелец обозначил Ваши попытки привязки спамом");
        defaultValues.put("discord.command.link.not_finished", "Владелец этого майнкрафт аккаунта не завершил предыдущую привязку");
        defaultValues.put("discord.command.link.minecraft.already_linked", "Невозможно привязать дискорд к аккаунту %s, так как он уже имеет привязку");

        // Общие для привязки/отвязки
        defaultValues.put("discord.command.not_found", "Вы должны быть на сервере в момент отвязки аккаунтов");
        defaultValues.put("discord.command.help_title", "!помощь");
        defaultValues.put("discord.command.help_field", "для просмотра списка всех доступных команд");

        // Цветовая палитра
        defaultValues.put("color_palette.green", "#21db37");
        defaultValues.put("color_palette.red", "#fc3535");
        defaultValues.put("color_palette.yellow", "#f5c720");
        defaultValues.put("color_palette.gray", "#d1d1d1");
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

    /**
     * Вызывать только при инициализированном конфиге!
     *
     * @return возвращает класс FileConfiguration для получения значений из конфига
     * @throws NullPointerException при config == null
     */
    public static FileConfiguration getConfig() {
        if (config != null) return config;
        else throw new NullPointerException();
    }

}
