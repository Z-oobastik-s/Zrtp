package org.zoobastiks.zrtp.common;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.zoobastiks.zrtp.Zrtp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Класс для работы с языковыми сообщениями
 */
public class Lang {
    private final Zrtp plugin;
    private File langFile;
    private FileConfiguration config;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Pattern placeholderPattern = Pattern.compile("\\{([^}]+)\\}");
    
    // Сообщения для эффектов телепортации
    private TeleportEffectsMessages teleportEffectsMessages;
    
    /**
     * Перечисление ключей языковых сообщений
     */
    public enum Keys {
        PREFIX("prefix"),
        TELEPORT_STARTED("teleport-started"),
        TELEPORT_SUCCESS("teleport-success"),
        TELEPORT_CANCELLED("teleport-cancelled"),
        PREPARING_COORDS("preparing-coords"),
        PREPARING_TELEPORT("preparing-teleport"),
        NO_PERMISSION("no-permission"),
        COOLDOWN("cooldown"),
        UNSAFE_LOCATION("unsafe-location"),
        WORLD_DISABLED("world-disabled"),
        WORLD_NOT_CONFIGURED("world-not-configured"),
        NOT_ENOUGH_MONEY("not-enough-money"),
        MONEY_WITHDRAWN("money-withdrawn"),
        MONEY_REFUNDED("money-refunded"),
        ALREADY_TELEPORTING("already-teleporting"),
        RELOAD_SUCCESS("reload-success"),
        HELP_MESSAGE("help-message"),
        PERMISSION_DENIED("permission-denied"),
        TELEPORT_COOLDOWN("teleport-cooldown"),
        TELEPORT_DELAY("teleport-delay"),
        WORLD_NOT_FOUND("world-not-found"),
        PLAYER_TELEPORTED("player-teleported"),
        TELEPORT_TO_WORLD("teleport-to-world");
        
        private final String key;
        
        Keys(String key) {
            this.key = key;
        }
        
        @Override
        public String toString() {
            return key;
        }
    }
    
    /**
     * Конструктор языкового менеджера
     * @param plugin Экземпляр плагина
     */
    public Lang(Zrtp plugin) {
        this.plugin = plugin;
        loadLang();
    }
    
    /**
     * Загрузка языкового файла
     */
    private void loadLang() {
        // Получаем код языка из конфигурации плагина
        String langCode = plugin.getPluginConfig().getLanguage();
        
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists() && !langDir.mkdirs()) {
            plugin.log(Level.WARNING, "Не удалось создать директорию для языковых файлов");
        }
        
        langFile = new File(langDir, langCode + ".yml");
        
        if (!langFile.exists()) {
            try (InputStream is = plugin.getResource("lang/" + langCode + ".yml")) {
                if (is == null) {
                    plugin.log(Level.WARNING, "Языковой файл " + langCode + " не найден, использую ru_RU.yml");
                    langCode = "ru_RU";
                    langFile = new File(langDir, langCode + ".yml");
                    
                    if (!langFile.exists()) {
                        try (InputStream defaultIs = plugin.getResource("lang/ru_RU.yml")) {
                            if (defaultIs != null) {
                                Files.copy(defaultIs, langFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            } else {
                                plugin.log(Level.WARNING, "Не найден стандартный языковой файл ru_RU.yml");
                                return;
                            }
                        }
                    }
                } else {
                    Files.copy(is, langFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                plugin.log(Level.WARNING, "Ошибка при копировании языкового файла", e);
                return;
            }
        }
        
        config = YamlConfiguration.loadConfiguration(langFile);
        ensureDefaults();
        loadTeleportEffectsMessages();
    }
    
    /**
     * Загрузка сообщений для эффектов телепортации
     */
    private void loadTeleportEffectsMessages() {
        teleportEffectsMessages = TeleportEffectsMessages.fromConfig(config.getConfigurationSection("teleport-effects"));
    }
    
    /**
     * Установка значений по умолчанию
     */
    private void ensureDefaults() {
        Map<Keys, String> defaults = new HashMap<>();
        defaults.put(Keys.PREFIX, "<dark_aqua>[RTP]</dark_aqua> ");
        defaults.put(Keys.TELEPORT_STARTED, "<gradient:#00FFFF:#0099FF>Телепортация начнется через {delay} секунд. Не двигайтесь!</gradient>");
        defaults.put(Keys.TELEPORT_SUCCESS, "<gradient:#FEFEFE:#00FF56>Вы были телепортированы в случайное место!</gradient>");
        defaults.put(Keys.TELEPORT_CANCELLED, "<gradient:#FF5555:#FF0000>Телепортация отменена! Вы двигались.</gradient>");
        defaults.put(Keys.PREPARING_COORDS, "<gradient:#00BFFF:#87CEFA>Подготовка координат для {player} в мире {world}</gradient>");
        defaults.put(Keys.PREPARING_TELEPORT, "<gradient:#00BFFF:#87CEFA>Подготовка к телепортации</gradient>");
        defaults.put(Keys.NO_PERMISSION, "<gradient:#FF5555:#FF0000>У вас нет разрешения на использование этой команды.</gradient>");
        defaults.put(Keys.COOLDOWN, "<gradient:#FFA500:#FF8C00>Подождите {time} секунд перед следующей телепортацией.</gradient>");
        defaults.put(Keys.UNSAFE_LOCATION, "<gradient:#FF5555:#FF0000>Не удалось найти безопасное место для телепортации.</gradient>");
        defaults.put(Keys.WORLD_DISABLED, "<gradient:#FF5555:#FF0000>Телепортация в этом мире отключена.</gradient>");
        defaults.put(Keys.WORLD_NOT_CONFIGURED, "<gradient:#FF5555:#FF0000>Телепортация в этом мире не настроена.</gradient>");
        defaults.put(Keys.NOT_ENOUGH_MONEY, "<gradient:#FF5555:#FF0000>Недостаточно денег для телепортации. Требуется: {price}</gradient>");
        defaults.put(Keys.MONEY_WITHDRAWN, "<gradient:#32CD32:#00FA9A>С вас снято {price} за телепортацию.</gradient>");
        defaults.put(Keys.MONEY_REFUNDED, "<gradient:#FFA500:#FF8C00>Вам возвращено {price} за отмененную телепортацию.</gradient>");
        defaults.put(Keys.RELOAD_SUCCESS, "<gradient:#00FF00:#00CC00>Конфигурация плагина перезагружена.</gradient>");
        defaults.put(Keys.ALREADY_TELEPORTING, "<gradient:#FFA500:#FF8C00>Вы уже находитесь в процессе телепортации.</gradient>");
        defaults.put(Keys.HELP_MESSAGE, "<gradient:#00BFFF:#1E90FF>========== <bold>RTP ПОМОЩЬ</bold> ==========</gradient>\n<gradient:#FAFAFA:#F0F0F0>/rtp</gradient> - Случайная телепортация\n<gradient:#FAFAFA:#F0F0F0>/wild</gradient> - То же, что и /rtp\n<gradient:#FAFAFA:#F0F0F0>/rtp reload</gradient> - Перезагрузить плагин\n<gradient:#FAFAFA:#F0F0F0>/rtp player <имя></gradient> - Телепортировать другого игрока");
        
        boolean changed = false;
        for (Map.Entry<Keys, String> entry : defaults.entrySet()) {
            String path = entry.getKey().toString();
            if (!config.contains(path)) {
                config.set(path, entry.getValue());
                changed = true;
            }
        }
        
        if (changed) {
            try {
                config.save(langFile);
            } catch (IOException e) {
                plugin.log(Level.SEVERE, "Не удалось сохранить языковой файл", e);
            }
        }
    }
    
    /**
     * Получение сообщения по ключу
     * @param key Ключ сообщения
     * @return Текст сообщения
     */
    public String getMessage(Keys key) {
        String path = key.toString();
        return config.getString(path, "Missing message: " + path);
    }
    
    /**
     * Получение сообщения с подстановкой плейсхолдеров
     * @param key Ключ сообщения
     * @param placeholders Массив плейсхолдеров в формате {имя, значение, имя, значение, ...}
     * @return Текст сообщения с подставленными значениями
     */
    public String getMessage(Keys key, String... placeholders) {
        String message = getMessage(key);
        
        // Если нет плейсхолдеров, просто возвращаем сообщение
        if (placeholders == null || placeholders.length == 0 || placeholders.length % 2 != 0) {
            return message;
        }
        
        // Подготавливаем строку для MiniMessage
        for (int i = 0; i < placeholders.length; i += 2) {
            String placeholder = placeholders[i];
            String value = placeholders[i + 1];
            message = message.replace("{" + placeholder + "}", value);
        }
        
        return message;
    }
    
    /**
     * Отправка сообщения игроку
     * @param player Игрок
     * @param key Ключ сообщения
     */
    public void sendMessage(Player player, Keys key) {
        String message = getMessage(key);
        player.sendMessage(miniMessage.deserialize(getMessage(Keys.PREFIX) + message));
    }
    
    /**
     * Отправка сообщения игроку с подстановкой плейсхолдеров
     * @param player Игрок
     * @param key Ключ сообщения
     * @param placeholders Массив плейсхолдеров в формате {имя, значение, имя, значение, ...}
     */
    public void sendMessage(Player player, Keys key, String... placeholders) {
        String message = getMessage(key, placeholders);
        player.sendMessage(miniMessage.deserialize(getMessage(Keys.PREFIX) + message));
    }
    
    /**
     * Отправка сообщения игроку с использованием расширенных TagResolver
     * @param player Игрок
     * @param key Ключ сообщения
     * @param placeholders Массив плейсхолдеров для TagResolver
     */
    public void sendAdvancedMessage(Player player, Keys key, TagResolver... placeholders) {
        String message = getMessage(key);
        String prefix = getMessage(Keys.PREFIX);
        
        // Создаем объединенный TagResolver
        TagResolver resolver = TagResolver.resolver(placeholders);
        
        // Отправляем сообщение с префиксом и плейсхолдерами
        player.sendMessage(miniMessage.deserialize(prefix + message, resolver));
    }
    
    /**
     * Создание градиентного текста
     * @param text Текст для градиента
     * @param startColor Начальный цвет (в формате HEX: #RRGGBB)
     * @param endColor Конечный цвет (в формате HEX: #RRGGBB)
     * @return Отформатированный текст с градиентом
     */
    public String gradient(String text, String startColor, String endColor) {
        return "<gradient:" + startColor + ":" + endColor + ">" + text + "</gradient>";
    }
    
    /**
     * Создание градиентного текста с несколькими цветами
     * @param text Текст для градиента
     * @param colors Массив цветов в формате HEX (#RRGGBB)
     * @return Отформатированный текст с градиентом
     */
    public String multiGradient(String text, String... colors) {
        if (colors.length < 2) {
            return text;
        }
        
        StringBuilder gradientTag = new StringBuilder("<gradient:");
        for (int i = 0; i < colors.length; i++) {
            gradientTag.append(colors[i]);
            if (i < colors.length - 1) {
                gradientTag.append(":");
            }
        }
        gradientTag.append(">");
        
        return gradientTag.toString() + text + "</gradient>";
    }
    
    /**
     * Парсинг сообщения с расширенными плейсхолдерами
     * @param message Сообщение для парсинга
     * @param placeholders Массив плейсхолдеров в формате {имя, значение, имя, значение, ...}
     * @return Компонент с форматированием
     */
    public Component parseAdvancedMessage(String message, String... placeholders) {
        List<TagResolver> resolvers = new ArrayList<>();
        
        if (placeholders != null && placeholders.length > 0 && placeholders.length % 2 == 0) {
            for (int i = 0; i < placeholders.length; i += 2) {
                String placeholder = placeholders[i];
                String value = placeholders[i + 1];
                resolvers.add(Placeholder.parsed(placeholder, value));
            }
        }
        
        if (resolvers.isEmpty()) {
            return miniMessage.deserialize(message);
        } else {
            return miniMessage.deserialize(message, TagResolver.resolver(resolvers));
        }
    }
    
    /**
     * Парсинг текста в компонент
     * @param message Текст для парсинга
     * @return Компонент с форматированием
     */
    public Component parseComponent(String message) {
        return miniMessage.deserialize(message);
    }
    
    /**
     * Парсинг текста с тегами и плейсхолдерами
     * @param message Текст для парсинга
     * @param tagResolver Резолвер тегов
     * @return Компонент с форматированием
     */
    public Component parseComponent(String message, TagResolver tagResolver) {
        return miniMessage.deserialize(message, tagResolver);
    }
    
    /**
     * Перезагрузка языкового файла
     */
    public void reload() {
        try {
            if (langFile != null && langFile.exists()) {
                config = YamlConfiguration.loadConfiguration(langFile);
                loadTeleportEffectsMessages();
                
                // Уведомляем конфигурацию о перезагрузке сообщений для эффектов после телепортации
                if (plugin.getPluginConfig() != null) {
                    plugin.getPluginConfig().loadPostTeleportEffectsConfig();
                }
            } else {
                loadLang();
            }
        } catch (Exception e) {
            plugin.log(Level.SEVERE, "Ошибка при перезагрузке языкового файла", e);
        }
    }
    
    /**
     * Получение сообщений для эффектов телепортации
     * @return Сообщения эффектов телепортации
     */
    public TeleportEffectsMessages getTeleportEffectsMessages() {
        // Отладочное сообщение
        plugin.log(java.util.logging.Level.INFO, "Запрос сообщений для эффектов телепортации");
        
        // Если сообщения не были загружены, пробуем загрузить их сейчас
        if (teleportEffectsMessages == null) {
            plugin.log(java.util.logging.Level.INFO, "Сообщения для эффектов не были загружены, пробуем загрузить");
            loadTeleportEffectsMessages();
        }
        
        // Проверка на null после загрузки
        if (teleportEffectsMessages == null) {
            plugin.log(java.util.logging.Level.WARNING, "Не удалось загрузить сообщения для эффектов, создаем сообщения по умолчанию");
            teleportEffectsMessages = new TeleportEffectsMessages();
        } else {
            // Проверка содержимого (можно убрать в продакшне)
            plugin.log(java.util.logging.Level.INFO, "Пример сообщения стадии 1: " + 
                      teleportEffectsMessages.getStageMessage(1, 0));
        }
        
        return teleportEffectsMessages;
    }
    
    /**
     * Установка сообщения по ключу
     * @param key Ключ сообщения
     * @param value Новое значение
     * @return true если операция успешна
     */
    public boolean setMessage(@NotNull Keys key, @NotNull String value) {
        String path = key.toString();
        config.set(path, value);
        try {
            config.save(langFile);
            return true;
        } catch (IOException e) {
            plugin.log(Level.SEVERE, "Не удалось сохранить языковой файл", e);
            return false;
        }
    }
    
    /**
     * Получение объекта конфигурации
     * @return Файл конфигурации
     */
    public FileConfiguration getMessagesConfig() {
        return config;
    }
} 