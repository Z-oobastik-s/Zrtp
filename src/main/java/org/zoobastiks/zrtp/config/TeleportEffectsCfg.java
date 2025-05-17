package org.zoobastiks.zrtp.config;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Класс для хранения настроек эффектов телепортации
 */
public class TeleportEffectsCfg {
    // Включены ли эффекты
    private boolean enabled;
    
    // Использовать ActionBar вместо чата
    private boolean useActionBar;
    
    // Интервалы времени
    private int messageInterval;
    private int titleFadeIn;
    private int titleStay;
    private int titleFadeOut;
    
    // Громкость и тональность звуков
    private float volume;
    private float pitch;
    
    // Звуки для разных стадий
    private Sound stage1Sound;
    private Sound stage2Sound;
    private Sound stage3Sound;
    private Sound stage4Sound;
    private Sound countdownSound;
    private Sound teleportSound;
    private Sound cancelSound;
    
    // Цвета
    private String stageStartColor;
    private String stageEndColor;
    private String countdownStartColor;
    private String countdownEndColor;
    private String teleportStartColor;
    private String teleportEndColor;
    private String cancelColor;
    private String count3Color;
    private String count2Color;
    private String count1Color;
    
    /**
     * Конструктор с параметрами
     */
    public TeleportEffectsCfg(boolean enabled, boolean useActionBar, int messageInterval,
                              int titleFadeIn, int titleStay, int titleFadeOut,
                              float volume, float pitch, 
                              Sound stage1Sound, Sound stage2Sound, Sound stage3Sound, Sound stage4Sound,
                              Sound countdownSound, Sound teleportSound, Sound cancelSound,
                              String stageStartColor, String stageEndColor,
                              String countdownStartColor, String countdownEndColor,
                              String teleportStartColor, String teleportEndColor,
                              String cancelColor, String count3Color, String count2Color, String count1Color) {
        this.enabled = enabled;
        this.useActionBar = useActionBar;
        this.messageInterval = messageInterval;
        this.titleFadeIn = titleFadeIn;
        this.titleStay = titleStay;
        this.titleFadeOut = titleFadeOut;
        this.volume = volume;
        this.pitch = pitch;
        this.stage1Sound = stage1Sound;
        this.stage2Sound = stage2Sound;
        this.stage3Sound = stage3Sound;
        this.stage4Sound = stage4Sound;
        this.countdownSound = countdownSound;
        this.teleportSound = teleportSound;
        this.cancelSound = cancelSound;
        this.stageStartColor = stageStartColor;
        this.stageEndColor = stageEndColor;
        this.countdownStartColor = countdownStartColor;
        this.countdownEndColor = countdownEndColor;
        this.teleportStartColor = teleportStartColor;
        this.teleportEndColor = teleportEndColor;
        this.cancelColor = cancelColor;
        this.count3Color = count3Color;
        this.count2Color = count2Color;
        this.count1Color = count1Color;
    }
    
    /**
     * Конструктор настроек эффектов телепортации по умолчанию
     */
    public TeleportEffectsCfg() {
        this.enabled = true;
        this.useActionBar = true;
        this.messageInterval = 20;
        this.titleFadeIn = 6;
        this.titleStay = 14;
        this.titleFadeOut = 6;
        this.volume = 1.0f;
        this.pitch = 1.0f;
        this.stage1Sound = Sound.ENTITY_VILLAGER_WORK_CLERIC;
        this.stage2Sound = Sound.ENTITY_VILLAGER_YES;
        this.stage3Sound = Sound.ENTITY_VILLAGER_AMBIENT;
        this.stage4Sound = Sound.ENTITY_VILLAGER_CELEBRATE;
        this.countdownSound = Sound.BLOCK_NOTE_BLOCK_GUITAR;
        this.teleportSound = Sound.ENTITY_LIGHTNING_BOLT_IMPACT;
        this.cancelSound = Sound.ENTITY_VILLAGER_NO;
        this.stageStartColor = "#00BFFF";
        this.stageEndColor = "#4169E1";
        this.countdownStartColor = "#FF4500";
        this.countdownEndColor = "#FF8C00";
        this.teleportStartColor = "#00FF00";
        this.teleportEndColor = "#32CD32";
        this.cancelColor = "#FF0000";
        this.count3Color = "#FF0000";
        this.count2Color = "#FFA500";
        this.count1Color = "#FFFF00";
    }
    
    /**
     * Создание настроек эффектов из секции конфигурации
     * @param section Секция конфигурации
     * @return Настройки эффектов телепортации
     */
    public static TeleportEffectsCfg fromConfig(ConfigurationSection section) {
        if (section == null) {
            return new TeleportEffectsCfg();
        }
        
        boolean enabled = section.getBoolean("enabled", true);
        boolean useActionBar = section.getBoolean("use-actionbar", true);
        
        // Загрузка временных интервалов
        ConfigurationSection timings = section.getConfigurationSection("timings");
        int messageInterval = 20;
        int titleFadeIn = 6;
        int titleStay = 14;
        int titleFadeOut = 6;
        
        if (timings != null) {
            messageInterval = timings.getInt("message-interval", 20);
            titleFadeIn = timings.getInt("title-fade-in", 6);
            titleStay = timings.getInt("title-stay", 14);
            titleFadeOut = timings.getInt("title-fade-out", 6);
        }
        
        // Загрузка звуков
        ConfigurationSection sounds = section.getConfigurationSection("sounds");
        float volume = 1.0f;
        float pitch = 1.0f;
        Sound stage1Sound = Sound.ENTITY_VILLAGER_WORK_CLERIC;
        Sound stage2Sound = Sound.ENTITY_VILLAGER_YES;
        Sound stage3Sound = Sound.ENTITY_VILLAGER_AMBIENT;
        Sound stage4Sound = Sound.ENTITY_VILLAGER_CELEBRATE;
        Sound countdownSound = Sound.BLOCK_NOTE_BLOCK_GUITAR;
        Sound teleportSound = Sound.ENTITY_LIGHTNING_BOLT_IMPACT;
        Sound cancelSound = Sound.ENTITY_VILLAGER_NO;
        
        if (sounds != null) {
            volume = (float) sounds.getDouble("volume", 1.0);
            pitch = (float) sounds.getDouble("pitch", 1.0);
            
            try {
                stage1Sound = Sound.valueOf(sounds.getString("stage1", "ENTITY_VILLAGER_WORK_CLERIC"));
                stage2Sound = Sound.valueOf(sounds.getString("stage2", "ENTITY_VILLAGER_YES"));
                stage3Sound = Sound.valueOf(sounds.getString("stage3", "ENTITY_VILLAGER_AMBIENT"));
                stage4Sound = Sound.valueOf(sounds.getString("stage4", "ENTITY_VILLAGER_CELEBRATE"));
                countdownSound = Sound.valueOf(sounds.getString("countdown", "BLOCK_NOTE_BLOCK_GUITAR"));
                teleportSound = Sound.valueOf(sounds.getString("teleport", "ENTITY_LIGHTNING_BOLT_IMPACT"));
                cancelSound = Sound.valueOf(sounds.getString("cancel", "ENTITY_VILLAGER_NO"));
            } catch (IllegalArgumentException e) {
                // Если не удалось найти звук, используем значения по умолчанию
            }
        }
        
        // Загрузка цветов
        ConfigurationSection colors = section.getConfigurationSection("colors");
        String stageStartColor = "#00BFFF";
        String stageEndColor = "#4169E1";
        String countdownStartColor = "#FF4500";
        String countdownEndColor = "#FF8C00";
        String teleportStartColor = "#00FF00";
        String teleportEndColor = "#32CD32";
        String cancelColor = "#FF0000";
        String count3Color = "#FF0000";
        String count2Color = "#FFA500";
        String count1Color = "#FFFF00";
        
        if (colors != null) {
            stageStartColor = colors.getString("stage-start", "#00BFFF");
            stageEndColor = colors.getString("stage-end", "#4169E1");
            countdownStartColor = colors.getString("countdown-start", "#FF4500");
            countdownEndColor = colors.getString("countdown-end", "#FF8C00");
            teleportStartColor = colors.getString("teleport-start", "#00FF00");
            teleportEndColor = colors.getString("teleport-end", "#32CD32");
            cancelColor = colors.getString("cancel", "#FF0000");
            count3Color = colors.getString("count-3", "#FF0000");
            count2Color = colors.getString("count-2", "#FFA500");
            count1Color = colors.getString("count-1", "#FFFF00");
        }
        
        return new TeleportEffectsCfg(
            enabled, useActionBar, messageInterval,
            titleFadeIn, titleStay, titleFadeOut,
            volume, pitch,
            stage1Sound, stage2Sound, stage3Sound, stage4Sound,
            countdownSound, teleportSound, cancelSound,
            stageStartColor, stageEndColor,
            countdownStartColor, countdownEndColor,
            teleportStartColor, teleportEndColor,
            cancelColor, count3Color, count2Color, count1Color
        );
    }
    
    // Геттеры
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public boolean useActionBar() {
        return useActionBar;
    }
    
    public int getMessageInterval() {
        return messageInterval;
    }
    
    public Duration getTitleFadeIn() {
        return Duration.ofMillis(titleFadeIn * 50); // Конвертация тиков в миллисекунды
    }
    
    public Duration getTitleStay() {
        return Duration.ofMillis(titleStay * 50);
    }
    
    public Duration getTitleFadeOut() {
        return Duration.ofMillis(titleFadeOut * 50);
    }
    
    public float getVolume() {
        return volume;
    }
    
    public float getPitch() {
        return pitch;
    }
    
    public Sound getStage1Sound() {
        return stage1Sound;
    }
    
    public Sound getStage2Sound() {
        return stage2Sound;
    }
    
    public Sound getStage3Sound() {
        return stage3Sound;
    }
    
    public Sound getStage4Sound() {
        return stage4Sound;
    }
    
    public Sound getCountdownSound() {
        return countdownSound;
    }
    
    public Sound getTeleportSound() {
        return teleportSound;
    }
    
    public Sound getCancelSound() {
        return cancelSound;
    }
    
    public String getStageStartColor() {
        return stageStartColor;
    }
    
    public String getStageEndColor() {
        return stageEndColor;
    }
    
    public String getCountdownStartColor() {
        return countdownStartColor;
    }
    
    public String getCountdownEndColor() {
        return countdownEndColor;
    }
    
    public String getTeleportStartColor() {
        return teleportStartColor;
    }
    
    public String getTeleportEndColor() {
        return teleportEndColor;
    }
    
    public String getCancelColor() {
        return cancelColor;
    }
    
    public String getCountdownColor(int count) {
        switch (count) {
            case 3: return count3Color;
            case 2: return count2Color;
            case 1: return count1Color;
            default: return "#FFFFFF";
        }
    }
} 