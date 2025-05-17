package org.zoobastiks.zrtp.translator;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import org.bukkit.ChatColor;

/**
 * Класс для обработки текстовых сообщений с поддержкой градиентов и плейсхолдеров
 */
public class Translator {
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^}]+)\\}");
    private static final Pattern GRADIENT_PATTERN = Pattern.compile("<gradient:([^>]+)>([^<]+)</gradient>");
    private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("&([0-9a-fk-orA-FK-OR])");
    
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    
    /**
     * Обработать сообщение с поддержкой градиентов и цветовых кодов
     * @param message Исходное сообщение
     * @return Компонент с форматированием
     */
    public Component parseMessage(String message) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }
        
        // Преобразуем обычные цветовые коды в MiniMessage формат
        message = convertColorCodes(message);
        
        // Конвертируем традиционные плейсхолдеры {name} в MiniMessage формат <name>
        message = convertTraditionalPlaceholders(message);
        
        // Парсим сообщение через MiniMessage
        return miniMessage.deserialize(message);
    }
    
    /**
     * Обработать сообщение с поддержкой градиентов, цветовых кодов и плейсхолдеров
     * @param message Исходное сообщение
     * @param placeholders Пары ключ-значение для замены плейсхолдеров (name1, value1, name2, value2, ...)
     * @return Компонент с форматированием
     */
    public Component parseMessage(String message, String... placeholders) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }
        
        // Заменяем плейсхолдеры в исходном сообщении
        message = replacePlaceholders(message, placeholders);
        
        // Преобразуем обычные цветовые коды в MiniMessage формат
        message = convertColorCodes(message);
        
        // Конвертируем традиционные плейсхолдеры в MiniMessage формат
        message = convertTraditionalPlaceholders(message);
        
        // Создаем резолверы для плейсхолдеров (для обратной совместимости)
        TagResolver.Builder builder = TagResolver.builder();
        if (placeholders != null && placeholders.length > 0 && placeholders.length % 2 == 0) {
            for (int i = 0; i < placeholders.length; i += 2) {
                String placeholder = placeholders[i];
                String value = placeholders[i + 1];
                builder.resolver(Placeholder.parsed(placeholder, value));
            }
        }
        
        // Парсим сообщение через MiniMessage
        return miniMessage.deserialize(message, builder.build());
    }
    
    /**
     * Обработать сообщение с поддержкой градиентов, цветовых кодов и плейсхолдеров
     * @param message Исходное сообщение
     * @param resolvers Резолверы для замены плейсхолдеров
     * @return Компонент с форматированием
     */
    public Component parseMessage(String message, TagResolver... resolvers) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }
        
        // Преобразуем обычные цветовые коды в MiniMessage формат
        message = convertColorCodes(message);
        
        // Конвертируем традиционные плейсхолдеры в MiniMessage формат,
        // но только для тех, которые не будут обработаны TagResolver
        message = convertTraditionalPlaceholders(message, resolvers);
        
        // Парсим сообщение через MiniMessage с использованием резолверов
        return miniMessage.deserialize(message, TagResolver.resolver(resolvers));
    }
    
    /**
     * Обработать сообщение с поддержкой градиентов, цветовых кодов и плейсхолдеров из карты
     * @param message Исходное сообщение
     * @param placeholders Карта с плейсхолдерами и их значениями
     * @return Компонент с форматированием
     */
    public Component parseMessage(String message, Map<String, String> placeholders) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }
        
        // Заменяем плейсхолдеры из карты
        if (placeholders != null && !placeholders.isEmpty()) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        
        // Преобразуем обычные цветовые коды в MiniMessage формат
        message = convertColorCodes(message);
        
        // Конвертируем оставшиеся традиционные плейсхолдеры в MiniMessage формат
        message = convertTraditionalPlaceholders(message);
        
        // Парсим сообщение через MiniMessage
        return miniMessage.deserialize(message);
    }
    
    /**
     * Заменить плейсхолдеры в тексте
     * @param message Исходное сообщение
     * @param placeholders Пары ключ-значение (name1, value1, name2, value2, ...)
     * @return Текст с замененными плейсхолдерами
     */
    public String replacePlaceholders(String message, String... placeholders) {
        if (message == null || placeholders == null || placeholders.length == 0 || placeholders.length % 2 != 0) {
            return message;
        }
        
        for (int i = 0; i < placeholders.length; i += 2) {
            String placeholder = placeholders[i];
            String value = placeholders[i + 1];
            message = message.replace("{" + placeholder + "}", value);
        }
        
        return message;
    }
    
    /**
     * Создать TagResolver для заданных плейсхолдеров
     * @param placeholders Пары ключ-значение (name1, value1, name2, value2, ...)
     * @return TagResolver для MiniMessage
     */
    public TagResolver createResolver(String... placeholders) {
        if (placeholders == null || placeholders.length == 0 || placeholders.length % 2 != 0) {
            return TagResolver.empty();
        }
        
        TagResolver.Builder builder = TagResolver.builder();
        
        for (int i = 0; i < placeholders.length; i += 2) {
            String placeholder = placeholders[i];
            String value = placeholders[i + 1];
            builder.resolver(Placeholder.parsed(placeholder, value));
        }
        
        return builder.build();
    }
    
    /**
     * Создать TagResolver для заданных плейсхолдеров из карты
     * @param placeholders Карта ключ-значение
     * @return TagResolver для MiniMessage
     */
    public TagResolver createResolver(Map<String, String> placeholders) {
        if (placeholders == null || placeholders.isEmpty()) {
            return TagResolver.empty();
        }
        
        TagResolver.Builder builder = TagResolver.builder();
        
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            builder.resolver(Placeholder.parsed(entry.getKey(), entry.getValue()));
        }
        
        return builder.build();
    }
    
    /**
     * Конвертировать обычные цветовые коды (&a, &b, и т.д.) в MiniMessage формат
     * @param message Исходное сообщение
     * @return Сообщение в MiniMessage формате
     */
    private String convertColorCodes(String message) {
        if (message == null) return "";
        
        // Заменяем обычные цветовые коды
        Matcher matcher = COLOR_CODE_PATTERN.matcher(message);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String colorCode = matcher.group(1).toLowerCase();
            String replacement;
            
            switch (colorCode) {
                case "0": replacement = "<black>"; break;
                case "1": replacement = "<dark_blue>"; break;
                case "2": replacement = "<dark_green>"; break;
                case "3": replacement = "<dark_aqua>"; break;
                case "4": replacement = "<dark_red>"; break;
                case "5": replacement = "<dark_purple>"; break;
                case "6": replacement = "<gold>"; break;
                case "7": replacement = "<gray>"; break;
                case "8": replacement = "<dark_gray>"; break;
                case "9": replacement = "<blue>"; break;
                case "a": replacement = "<green>"; break;
                case "b": replacement = "<aqua>"; break;
                case "c": replacement = "<red>"; break;
                case "d": replacement = "<light_purple>"; break;
                case "e": replacement = "<yellow>"; break;
                case "f": replacement = "<white>"; break;
                case "k": replacement = "<obfuscated>"; break;
                case "l": replacement = "<bold>"; break;
                case "m": replacement = "<strikethrough>"; break;
                case "n": replacement = "<underlined>"; break;
                case "o": replacement = "<italic>"; break;
                case "r": replacement = "<reset>"; break;
                default: replacement = "&" + colorCode;
            }
            
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }
    
    /**
     * Конвертировать традиционные плейсхолдеры {name} в MiniMessage формат <name>
     * @param message Исходное сообщение
     * @return Сообщение с преобразованными плейсхолдерами
     */
    private String convertTraditionalPlaceholders(String message) {
        if (message == null) return "";
        
        // Находим все плейсхолдеры типа {name} и преобразуем их в <name>
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(message);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            matcher.appendReplacement(sb, "<" + placeholder + ">");
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }
    
    /**
     * Конвертировать традиционные плейсхолдеры {name} в MiniMessage формат <name>,
     * но только для тех, которые не будут обработаны TagResolver
     * @param message Исходное сообщение
     * @param resolvers TagResolver для проверки на конфликты
     * @return Сообщение с преобразованными плейсхолдерами
     */
    private String convertTraditionalPlaceholders(String message, TagResolver... resolvers) {
        if (message == null) return "";
        if (resolvers == null || resolvers.length == 0) {
            return convertTraditionalPlaceholders(message);
        }
        
        // Находим все плейсхолдеры типа {name} и преобразуем их в <name>,
        // если они не будут обработаны TagResolver
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(message);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            // Здесь можно добавить логику проверки, есть ли этот плейсхолдер в resolvers
            // Сейчас просто конвертируем все
            matcher.appendReplacement(sb, "<" + placeholder + ">");
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }
} 