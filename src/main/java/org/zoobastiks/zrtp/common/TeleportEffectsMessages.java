package org.zoobastiks.zrtp.common;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Класс для хранения сообщений эффектов телепортации
 */
public class TeleportEffectsMessages {
    // Заголовки
    private String titleCancel;
    private String titleTeleport;
    private String actionbarCountdown;
    
    // Сообщения этапов
    private List<String> stage1Messages;
    private List<String> stage2Messages;
    private List<String> stage3Messages;
    private List<String> stage4Messages;
    private List<String> countdownMessages;
    private String teleportMessage;
    private String cancelMessage;
    
    /**
     * Конструктор сообщений эффектов телепортации
     */
    public TeleportEffectsMessages(
            String titleCancel, String titleTeleport, String actionbarCountdown,
            List<String> stage1Messages, List<String> stage2Messages, 
            List<String> stage3Messages, List<String> stage4Messages,
            List<String> countdownMessages, String teleportMessage, String cancelMessage) {
        this.titleCancel = titleCancel;
        this.titleTeleport = titleTeleport;
        this.actionbarCountdown = actionbarCountdown;
        this.stage1Messages = stage1Messages;
        this.stage2Messages = stage2Messages;
        this.stage3Messages = stage3Messages;
        this.stage4Messages = stage4Messages;
        this.countdownMessages = countdownMessages;
        this.teleportMessage = teleportMessage;
        this.cancelMessage = cancelMessage;
    }
    
    /**
     * Конструктор сообщений по умолчанию
     */
    public TeleportEffectsMessages() {
        this.titleCancel = "Телепортация отменена!";
        this.titleTeleport = "Телепортация!";
        this.actionbarCountdown = "Телепортация через {count}";
        
        this.stage1Messages = Arrays.asList(
            "⚡ Лаборатория активирована!",
            "🧪 Поиск локации начат.",
            "🌍 Ищем безопасную точку..."
        );
        
        this.stage2Messages = Arrays.asList(
            "📡 Связь установлена.",
            "🛰️ Сканируем координаты...",
            "📍 Подходящее место найдено!"
        );
        
        this.stage3Messages = Arrays.asList(
            "🔐 Проверка завершена.",
            "🧬 Пространство стабильно.",
            "💥 Ошибок не обнаружено."
        );
        
        this.stage4Messages = Arrays.asList(
            "📊 Команда \"/rtp\" принята!",
            "🧠 Подготовка завершена.",
            "🛸 Телепорт почти готов..."
        );
        
        this.countdownMessages = Arrays.asList(
            "⏳ Начинаем отсчёт:",
            "3...",
            "2...",
            "1..."
        );
        
        this.teleportMessage = "🚀 Телепортация началась!";
        this.cancelMessage = "⛔ Телепортация отменена: {reason}";
    }
    
    /**
     * Загрузка сообщений из секции конфигурации
     * @param section Секция конфигурации
     * @return Объект с сообщениями
     */
    public static TeleportEffectsMessages fromConfig(ConfigurationSection section) {
        if (section == null) {
            return new TeleportEffectsMessages();
        }
        
        String titleCancel = section.getString("title-cancel", "Телепортация отменена!");
        String titleTeleport = section.getString("title-teleport", "Телепортация!");
        String actionbarCountdown = section.getString("actionbar-countdown", "Телепортация через {count}");
        
        List<String> stage1Messages = getMessagesFromSection(section, "stage1", 
            Arrays.asList("⚡ Лаборатория активирована!", "🧪 Поиск локации начат.", "🌍 Ищем безопасную точку..."));
        
        List<String> stage2Messages = getMessagesFromSection(section, "stage2", 
            Arrays.asList("📡 Связь установлена.", "🛰️ Сканируем координаты...", "📍 Подходящее место найдено!"));
        
        List<String> stage3Messages = getMessagesFromSection(section, "stage3", 
            Arrays.asList("🔐 Проверка завершена.", "🧬 Пространство стабильно.", "💥 Ошибок не обнаружено."));
        
        List<String> stage4Messages = getMessagesFromSection(section, "stage4", 
            Arrays.asList("📊 Команда \"/rtp\" принята!", "🧠 Подготовка завершена.", "🛸 Телепорт почти готов..."));
        
        List<String> countdownMessages = getMessagesFromSection(section, "countdown", 
            Arrays.asList("⏳ Начинаем отсчёт:", "3...", "2...", "1..."));
        
        String teleportMessage = section.getString("teleport", "🚀 Телепортация началась!");
        String cancelMessage = section.getString("cancel-message", "⛔ Телепортация отменена: {reason}");
        
        return new TeleportEffectsMessages(
            titleCancel, titleTeleport, actionbarCountdown,
            stage1Messages, stage2Messages, stage3Messages, stage4Messages,
            countdownMessages, teleportMessage, cancelMessage
        );
    }
    
    /**
     * Получение списка сообщений из секции конфигурации
     * @param section Секция конфигурации
     * @param key Ключ для списка сообщений
     * @param defaultMessages Сообщения по умолчанию
     * @return Список сообщений
     */
    private static List<String> getMessagesFromSection(ConfigurationSection section, String key, List<String> defaultMessages) {
        if (section.isList(key)) {
            List<String> messages = section.getStringList(key);
            return messages.isEmpty() ? defaultMessages : messages;
        }
        return defaultMessages;
    }
    
    // Геттеры
    
    /**
     * Получение сообщения для заголовка отмены
     * @return Заголовок отмены
     */
    public String getTitleCancel() {
        return titleCancel;
    }
    
    /**
     * Получение сообщения для заголовка телепортации
     * @return Заголовок телепортации
     */
    public String getTitleTeleport() {
        return titleTeleport;
    }
    
    /**
     * Получение шаблона для ActionBar отсчета
     * @return Шаблон с плейсхолдером {count}
     */
    public String getActionbarCountdown() {
        return actionbarCountdown;
    }
    
    /**
     * Получение сообщения для указанной стадии и индекса
     * @param stage Номер стадии (1-4)
     * @param index Индекс сообщения в стадии
     * @return Сообщение или null, если не найдено
     */
    @Nullable
    public String getStageMessage(int stage, int index) {
        List<String> messages;
        
        switch (stage) {
            case 1:
                messages = stage1Messages;
                break;
            case 2:
                messages = stage2Messages;
                break;
            case 3:
                messages = stage3Messages;
                break;
            case 4:
                messages = stage4Messages;
                break;
            default:
                return null;
        }
        
        if (index >= 0 && index < messages.size()) {
            return messages.get(index);
        }
        
        return null;
    }
    
    /**
     * Получение сообщения отсчета по индексу
     * @param index Индекс сообщения (0 = начало, 1-3 = цифры)
     * @return Сообщение или null, если не найдено
     */
    @Nullable
    public String getCountdownMessage(int index) {
        if (index >= 0 && index < countdownMessages.size()) {
            return countdownMessages.get(index);
        }
        return null;
    }
    
    /**
     * Получение сообщения о телепортации
     * @return Сообщение о телепортации
     */
    public String getTeleportMessage() {
        return teleportMessage;
    }
    
    /**
     * Получение сообщения об отмене телепортации
     * @return Сообщение об отмене телепортации
     */
    public String getCancelMessage() {
        return cancelMessage;
    }
    
    /**
     * Получение количества сообщений в стадии
     * @param stage Номер стадии (1-4)
     * @return Количество сообщений
     */
    public int getStageMessageCount(int stage) {
        switch (stage) {
            case 1:
                return stage1Messages.size();
            case 2:
                return stage2Messages.size();
            case 3:
                return stage3Messages.size();
            case 4:
                return stage4Messages.size();
            default:
                return 0;
        }
    }
    
    /**
     * Получение количества сообщений в отсчете
     * @return Количество сообщений
     */
    public int getCountdownMessageCount() {
        return countdownMessages.size();
    }
} 