package org.zoobastiks.zrtp.common;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * Класс для хранения сообщений эффектов после телепортации
 */
public class PostTeleportEffectsMessages {
    // Сообщения для информации о локации
    private String locationTitle;
    private String locationSubtitle;
    private String locationCoords;
    private String locationWorld;
    private String locationBiome;
    
    // Сообщения для системы наград
    private List<String> rewardSearching;
    private String rewardFound;
    private String rewardContents;
    private String rewardItem;
    private String rewardChance;
    
    // Сообщения о мобах
    private String mobsSpawned;
    private String mobsFriendly;
    
    // Сообщения об эффектах
    private String effectsDamage;
    private String effectsHunger;
    private String effectsLightning;
    private String effectsCommand;
    
    /**
     * Конструктор с параметрами
     */
    public PostTeleportEffectsMessages(
            String locationTitle, String locationSubtitle, String locationCoords,
            String locationWorld, String locationBiome,
            List<String> rewardSearching, String rewardFound, String rewardContents,
            String rewardItem, String rewardChance,
            String mobsSpawned, String mobsFriendly,
            String effectsDamage, String effectsHunger, String effectsLightning, String effectsCommand) {
        this.locationTitle = locationTitle;
        this.locationSubtitle = locationSubtitle;
        this.locationCoords = locationCoords;
        this.locationWorld = locationWorld;
        this.locationBiome = locationBiome;
        this.rewardSearching = rewardSearching;
        this.rewardFound = rewardFound;
        this.rewardContents = rewardContents;
        this.rewardItem = rewardItem;
        this.rewardChance = rewardChance;
        this.mobsSpawned = mobsSpawned;
        this.mobsFriendly = mobsFriendly;
        this.effectsDamage = effectsDamage;
        this.effectsHunger = effectsHunger;
        this.effectsLightning = effectsLightning;
        this.effectsCommand = effectsCommand;
    }
    
    /**
     * Конструктор сообщений по умолчанию
     */
    public PostTeleportEffectsMessages() {
        this.locationTitle = "Спутник дал сбой.";
        this.locationSubtitle = "Вы приземлились:";
        this.locationCoords = "Координаты: X:{x}, Y:{y}, Z:{z}";
        this.locationWorld = "Мир: {world}";
        this.locationBiome = "Биом: {biome}";
        
        this.rewardSearching = Arrays.asList(
            "⚡ Телепортация завершена!",
            "🛸 Ты оказался в новой локации.",
            "🔍 Осмотрим ближайшее окружение..."
        );
        this.rewardFound = "🗣️ О, сундук! Обнаружен!";
        this.rewardContents = "📦 Внутри сундука:";
        this.rewardItem = "📦 {item} x{count}";
        this.rewardChance = "🎉 Шанс: {chance}%";
        
        this.mobsSpawned = "⚠️ Внимание! Рядом появились враги!";
        this.mobsFriendly = "🐾 К вам подошло дружелюбное существо.";
        
        this.effectsDamage = "💔 Телепортация повредила вашему здоровью!";
        this.effectsHunger = "🍗 Вы ощущаете сильный голод после телепортации.";
        this.effectsLightning = "⚡ Молния ударила рядом с вами!";
        this.effectsCommand = "✅ Активировано специальное действие!";
    }
    
    /**
     * Загрузка сообщений из секции конфигурации
     * @param section Секция конфигурации
     * @return Объект с сообщениями
     */
    public static PostTeleportEffectsMessages fromConfig(ConfigurationSection section) {
        if (section == null) {
            return new PostTeleportEffectsMessages();
        }
        
        // Загрузка сообщений для информации о локации
        ConfigurationSection locationSection = section.getConfigurationSection("location-info");
        String locationTitle = "Спутник дал сбой.";
        String locationSubtitle = "Вы приземлились:";
        String locationCoords = "Координаты: X:{x}, Y:{y}, Z:{z}";
        String locationWorld = "Мир: {world}";
        String locationBiome = "Биом: {biome}";
        
        if (locationSection != null) {
            locationTitle = locationSection.getString("title", "Спутник дал сбой.");
            locationSubtitle = locationSection.getString("subtitle", "Вы приземлились:");
            locationCoords = locationSection.getString("coords", "Координаты: X:{x}, Y:{y}, Z:{z}");
            locationWorld = locationSection.getString("world", "Мир: {world}");
            locationBiome = locationSection.getString("biome", "Биом: {biome}");
        }
        
        // Загрузка сообщений для системы наград
        ConfigurationSection rewardSection = section.getConfigurationSection("reward");
        List<String> rewardSearching = Arrays.asList(
            "⚡ Телепортация завершена!",
            "🛸 Ты оказался в новой локации.",
            "🔍 Осмотрим ближайшее окружение..."
        );
        String rewardFound = "🗣️ О, сундук! Обнаружен!";
        String rewardContents = "📦 Внутри сундука:";
        String rewardItem = "📦 {item} x{count}";
        String rewardChance = "🎉 Шанс: {chance}%";
        
        if (rewardSection != null) {
            rewardSearching = rewardSection.getStringList("searching");
            rewardFound = rewardSection.getString("found", "🗣️ О, сундук! Обнаружен!");
            rewardContents = rewardSection.getString("contents", "📦 Внутри сундука:");
            rewardItem = rewardSection.getString("item", "📦 {item} x{count}");
            rewardChance = rewardSection.getString("chance", "🎉 Шанс: {chance}%");
        }
        
        // Загрузка сообщений о мобах
        ConfigurationSection mobsSection = section.getConfigurationSection("mobs");
        String mobsSpawned = "⚠️ Внимание! Рядом появились враги!";
        String mobsFriendly = "🐾 К вам подошло дружелюбное существо.";
        
        if (mobsSection != null) {
            mobsSpawned = mobsSection.getString("spawned", "⚠️ Внимание! Рядом появились враги!");
            mobsFriendly = mobsSection.getString("friendly", "🐾 К вам подошло дружелюбное существо.");
        }
        
        // Загрузка сообщений об эффектах
        ConfigurationSection effectsSection = section.getConfigurationSection("effects");
        String effectsDamage = "💔 Телепортация повредила вашему здоровью!";
        String effectsHunger = "🍗 Вы ощущаете сильный голод после телепортации.";
        String effectsLightning = "⚡ Молния ударила рядом с вами!";
        String effectsCommand = "✅ Активировано специальное действие!";
        
        if (effectsSection != null) {
            effectsDamage = effectsSection.getString("damage", "💔 Телепортация повредила вашему здоровью!");
            effectsHunger = effectsSection.getString("hunger", "🍗 Вы ощущаете сильный голод после телепортации.");
            effectsLightning = effectsSection.getString("lightning", "⚡ Молния ударила рядом с вами!");
            effectsCommand = effectsSection.getString("command", "✅ Активировано специальное действие!");
        }
        
        return new PostTeleportEffectsMessages(
            locationTitle, locationSubtitle, locationCoords, locationWorld, locationBiome,
            rewardSearching, rewardFound, rewardContents, rewardItem, rewardChance,
            mobsSpawned, mobsFriendly,
            effectsDamage, effectsHunger, effectsLightning, effectsCommand
        );
    }
    
    // Геттеры
    
    public String getLocationTitle() {
        return locationTitle;
    }
    
    public String getLocationSubtitle() {
        return locationSubtitle;
    }
    
    public String getLocationCoords() {
        return locationCoords;
    }
    
    public String getLocationWorld() {
        return locationWorld;
    }
    
    public String getLocationBiome() {
        return locationBiome;
    }
    
    /**
     * Получение сообщения поиска по индексу
     * @param index Индекс сообщения
     * @return Сообщение или null, если не найдено
     */
    @Nullable
    public String getRewardSearchingMessage(int index) {
        if (index >= 0 && index < rewardSearching.size()) {
            return rewardSearching.get(index);
        }
        return null;
    }
    
    public int getRewardSearchingMessagesCount() {
        return rewardSearching.size();
    }
    
    public String getRewardFound() {
        return rewardFound;
    }
    
    public String getRewardContents() {
        return rewardContents;
    }
    
    public String getRewardItem() {
        return rewardItem;
    }
    
    public String getRewardChance() {
        return rewardChance;
    }
    
    public String getMobsSpawned() {
        return mobsSpawned;
    }
    
    public String getMobsFriendly() {
        return mobsFriendly;
    }
    
    public String getEffectsDamage() {
        return effectsDamage;
    }
    
    public String getEffectsHunger() {
        return effectsHunger;
    }
    
    public String getEffectsLightning() {
        return effectsLightning;
    }
    
    public String getEffectsCommand() {
        return effectsCommand;
    }
} 