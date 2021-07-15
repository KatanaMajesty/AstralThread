package com.astralsmp.modules;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Все методы класса должны быть статическими. Класс не должен иметь конструктор
 * Класс используется исключительно для форматтирования текста на сервере майнкрафта
 * Все методы должны возвращать строку
 */
public class MFormatter {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#[a-fA-F_0-9]{6}"); // паттерн {#??????}

    /**
     * @see MFormatter#HEX_PATTERN
     * Заменяет паттерн цвета на цвет
     *
     * @param message необработанная строка с паттернами
     * @return возвращает обработанную строку без паттернов
     */
    public static String colorize(String message) {
        String result = message;

        Matcher hexMatcher = HEX_PATTERN.matcher(message);
        while (hexMatcher.find()) {
            String matchedStr = hexMatcher.group();
            String formattedStr = matchedStr.replaceAll("&", "");
            result = result.replace(matchedStr, ChatColor.of(formattedStr) + "");
        }

        result = ChatColor.translateAlternateColorCodes('&', result);
        return result;
    }

    /**
     * Выводит строку в консоль
     * @param message необработанная строка
     */
    public static void log(String message) {
        Bukkit.getConsoleSender().sendMessage(colorize(message));
    }

    /**
     * Применяет функцию к строке, позволяя заменить плейсхолдер
     *
     * @param message необработанная строка
     * @param fun функция для обработки строки
     * @return обработанная строка
     */
    public static String placeholderFunction(String message, Function<String, String> fun) {
        return fun.apply(message);
    }
}
