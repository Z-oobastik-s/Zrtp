package org.zoobastiks.zrtp.config;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.zoobastiks.zrtp.Zrtp;
import org.zoobastiks.zrtp.common.PostTeleportEffectsMessages;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/**
 * Класс управления конфигурацией
 */
public class Cfg {
    private final Zrtp plugin;
    private File configFile;
    private FileConfiguration config;
    
    // Настройки по умолчанию
    private int defaultMinRadius = 100;
    private int defaultMaxRadius = 5000;
    private int defaultDelay = 5;
    private int defaultCooldown = 30;
    private double defaultPrice = 0.0;
    private String language = "ru_RU";
    private boolean debug = false;
    
    // Хранилище для настроек миров
    private final Map<String, WorldCfg> worldConfigs = new HashMap<>();
    
    // Настройки эффектов телепортации
    private TeleportEffectsCfg teleportEffectsConfig;
    
    // Настройки эффектов после телепортации
    private PostTeleportEffectsCfg postTeleportEffectsConfig;
    private PostTeleportEffectsMessages postTeleportEffectsMessages;
    
    /**
     * Конструктор конфигурации
     * @param plugin Экземпляр плагина
     */
    public Cfg(Zrtp plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    /**
     * Загрузка конфигурации
     */
    private void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "config.yml");
        
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        loadDefaults();
        loadWorldConfigurations();
        loadTeleportEffectsConfig();
        
        // Загрузка postTeleportEffectsConfig будет выполнена позже,
        // после инициализации Lang в Zrtp.onEnable()
    }
    
    /**
     * Загрузка настроек по умолчанию
     */
    private void loadDefaults() {
        defaultMinRadius = config.getInt("defaults.min-radius", 100);
        defaultMaxRadius = config.getInt("defaults.max-radius", 5000);
        defaultDelay = config.getInt("defaults.delay", 5);
        defaultCooldown = config.getInt("defaults.cooldown", 30);
        defaultPrice = config.getDouble("defaults.price", 0.0);
        language = config.getString("language", "ru_RU");
        debug = config.getBoolean("debug", false);
    }
    
    /**
     * Загрузка настроек для миров
     */
    private void loadWorldConfigurations() {
        worldConfigs.clear();
        
        ConfigurationSection worldsSection = config.getConfigurationSection("worlds");
        if (worldsSection != null) {
            for (String worldName : worldsSection.getKeys(false)) {
                ConfigurationSection worldSection = worldsSection.getConfigurationSection(worldName);
                if (worldSection != null) {
                    WorldCfg worldCfg = new WorldCfg(
                        worldSection.getInt("min-radius", defaultMinRadius),
                        worldSection.getInt("max-radius", defaultMaxRadius),
                        worldSection.getInt("delay", defaultDelay),
                        worldSection.getInt("cooldown", defaultCooldown),
                        worldSection.getDouble("price", defaultPrice),
                        worldSection.getBoolean("enabled", true),
                        createCenterLocation(worldName, worldSection)
                    );
                    
                    // Загрузка запрещенных биомов
                    List<String> forbiddenBiomes = worldSection.getStringList("forbidden-biomes");
                    worldCfg.setForbiddenBiomes(new HashSet<>(forbiddenBiomes));
                    
                    worldConfigs.put(worldName, worldCfg);
                }
            }
        }
    }
    
    /**
     * Загрузка настроек эффектов телепортации
     */
    private void loadTeleportEffectsConfig() {
        ConfigurationSection teleportEffectsSection = config.getConfigurationSection("teleport-effects");
        teleportEffectsConfig = TeleportEffectsCfg.fromConfig(teleportEffectsSection);
    }
    
    /**
     * Загрузка настроек эффектов после телепортации
     */
    public void loadPostTeleportEffectsConfig() {
        // Загрузка настроек эффектов после телепортации
        ConfigurationSection postTeleportSection = config.getConfigurationSection("post-teleport");
        postTeleportEffectsConfig = PostTeleportEffectsCfg.fromConfig(postTeleportSection);
        
        // Загрузка сообщений для эффектов после телепортации
        if (plugin.getLang() != null && plugin.getLang().getMessagesConfig() != null) {
            ConfigurationSection messagesSection = plugin.getLang().getMessagesConfig().getConfigurationSection("post-teleport");
            postTeleportEffectsMessages = PostTeleportEffectsMessages.fromConfig(messagesSection);
        } else {
            // Создаем пустые сообщения для предотвращения ошибок
            postTeleportEffectsMessages = new PostTeleportEffectsMessages();
            plugin.log(Level.WARNING, "Не удалось загрузить сообщения для эффектов после телепортации. Lang не инициализирован.");
        }
    }
    
    /**
     * Создание центральной локации для мира
     * @param worldName Имя мира
     * @param section Секция конфигурации
     * @return Локация центра или null, если не указана
     */
    private Location createCenterLocation(String worldName, ConfigurationSection section) {
        if (section.contains("center")) {
            World world = plugin.getServer().getWorld(worldName);
            if (world != null) {
                int x = section.getInt("center.x", 0);
                int z = section.getInt("center.z", 0);
                return new Location(world, x, 0, z);
            }
        }
        return null;
    }
    
    /**
     * Перезагрузка конфигурации
     */
    public void reload() {
        config = YamlConfiguration.loadConfiguration(configFile);
        loadDefaults();
        loadWorldConfigurations();
        loadTeleportEffectsConfig();
        
        // Сначала нужно перезагрузить Lang
        if (plugin.getLang() != null) {
            plugin.getLang().reload();
        }
        
        // Теперь загружаем эффекты после телепортации
        loadPostTeleportEffectsConfig();
    }
    
    /**
     * Получение настроек для конкретного мира
     * @param worldName Имя мира
     * @return Конфигурация мира или настройки по умолчанию
     */
    public WorldCfg getWorldConfig(String worldName) {
        return worldConfigs.getOrDefault(worldName, 
            new WorldCfg(defaultMinRadius, defaultMaxRadius, defaultDelay, defaultCooldown, defaultPrice, true, null));
    }
    
    /**
     * Получение настроек эффектов телепортации
     * @return Конфигурация эффектов телепортации
     */
    public TeleportEffectsCfg getTeleportEffectsConfig() {
        return teleportEffectsConfig;
    }
    
    /**
     * Получение настроек эффектов после телепортации
     * @return Конфигурация эффектов после телепортации
     */
    public PostTeleportEffectsCfg getPostTeleportEffectsConfig() {
        return postTeleportEffectsConfig;
    }
    
    /**
     * Получение сообщений для эффектов после телепортации
     * @return Сообщения для эффектов после телепортации
     */
    public PostTeleportEffectsMessages getPostTeleportEffectsMessages() {
        return postTeleportEffectsMessages;
    }
    
    /**
     * Получение доступных миров для телепортации
     * @return Список названий доступных миров
     */
    public List<String> getEnabledWorldNames() {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, WorldCfg> entry : worldConfigs.entrySet()) {
            if (entry.getValue().isEnabled()) {
                result.add(entry.getKey());
            }
        }
        return result;
    }
    
    /**
     * Получение минимального радиуса по умолчанию
     * @return Минимальный радиус
     */
    public int getDefaultMinRadius() {
        return defaultMinRadius;
    }
    
    /**
     * Получение максимального радиуса по умолчанию
     * @return Максимальный радиус
     */
    public int getDefaultMaxRadius() {
        return defaultMaxRadius;
    }
    
    /**
     * Получение задержки по умолчанию
     * @return Задержка в секундах
     */
    public int getDefaultDelay() {
        return defaultDelay;
    }
    
    /**
     * Получение кулдауна по умолчанию
     * @return Кулдаун в секундах
     */
    public int getDefaultCooldown() {
        return defaultCooldown;
    }
    
    /**
     * Получение цены телепортации по умолчанию
     * @return Цена
     */
    public double getDefaultPrice() {
        return defaultPrice;
    }
    
    /**
     * Получение кода языка из конфигурации
     * @return Код языка (например, ru_RU)
     */
    public String getLanguage() {
        return language;
    }
    
    /**
     * Получение значения параметра debug
     * @return true если режим отладки включен
     */
    public boolean isDebugEnabled() {
        return debug;
    }
    
    /**
     * Сохранение конфигурации
     */
    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.log(Level.SEVERE, "Не удалось сохранить конфигурацию", e);
        }
    }
    
    /**
     * Завершение загрузки конфигурации после инициализации Lang
     * Должен вызываться из Zrtp.onEnable() после инициализации Lang
     */
    public void finishLoading() {
        loadPostTeleportEffectsConfig();
    }
    
    /**
     * Проверяет, настроен ли мир явно в конфигурации
     * @param worldName Имя мира для проверки
     * @return true, если мир указан в секции worlds конфигурации
     */
    public boolean isWorldConfigured(String worldName) {
        return worldConfigs.containsKey(worldName);
    }
} 