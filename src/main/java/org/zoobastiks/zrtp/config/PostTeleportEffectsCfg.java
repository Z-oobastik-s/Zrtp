package org.zoobastiks.zrtp.config;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Класс для хранения настроек эффектов после телепортации
 */
public class PostTeleportEffectsCfg {
    private static final Logger LOGGER = Bukkit.getLogger();
    
    // Показывать информацию о локации
    private boolean showLocationInfo;
    
    // Эффекты зелий
    private List<PotionEffect> potionEffects;
    
    // Настройки голода
    private boolean hungerEnabled;
    private int hungerAmount;
    
    // Настройки урона
    private boolean damageEnabled;
    private double damageAmount;
    
    // Настройки молнии
    private boolean lightningEnabled;
    private boolean lightningDamage;
    
    // Команды
    private boolean commandsEnabled;
    private List<String> consoleCommands;
    private List<String> playerCommands;
    
    // Спавн мобов
    private boolean spawnMobsEnabled;
    private List<MobSpawnInfo> mobSpawnInfos;
    
    // Система наград
    private boolean rewardsEnabled;
    private double rewardChance;
    private Map<String, RewardItem> rewardItems;
    
    /**
     * Конструктор по умолчанию с настройками по умолчанию
     */
    public PostTeleportEffectsCfg() {
        this.showLocationInfo = true;
        
        // Эффекты зелий по умолчанию
        this.potionEffects = new ArrayList<>();
        this.potionEffects.add(new PotionEffect(PotionEffectType.NAUSEA, 30 * 20, 0));
        
        // Настройки голода
        this.hungerEnabled = true;
        this.hungerAmount = 4;
        
        // Настройки урона
        this.damageEnabled = false;
        this.damageAmount = 2.0;
        
        // Настройки молнии
        this.lightningEnabled = false;
        this.lightningDamage = false;
        
        // Команды
        this.commandsEnabled = false;
        this.consoleCommands = new ArrayList<>();
        this.playerCommands = new ArrayList<>();
        
        // Спавн мобов
        this.spawnMobsEnabled = false;
        this.mobSpawnInfos = new ArrayList<>();
        
        // Система наград
        this.rewardsEnabled = true;
        this.rewardChance = 5.0;
        this.rewardItems = new HashMap<>();
        
        // Добавляем несколько наград по умолчанию
        RewardItem diamond = new RewardItem("DIAMOND", 1, null);
        RewardItem emerald = new RewardItem("EMERALD", 2, null);
        
        this.rewardItems.put("DIAMOND", diamond);
        this.rewardItems.put("EMERALD", emerald);
    }
    
    /**
     * Конструктор с параметрами
     */
    public PostTeleportEffectsCfg(boolean showLocationInfo, List<PotionEffect> potionEffects,
                                 boolean hungerEnabled, int hungerAmount,
                                 boolean damageEnabled, double damageAmount,
                                 boolean lightningEnabled, boolean lightningDamage,
                                 boolean commandsEnabled, List<String> consoleCommands, List<String> playerCommands,
                                 boolean spawnMobsEnabled, List<MobSpawnInfo> mobSpawnInfos,
                                 boolean rewardsEnabled, double rewardChance, Map<String, RewardItem> rewardItems) {
        this.showLocationInfo = showLocationInfo;
        this.potionEffects = potionEffects;
        this.hungerEnabled = hungerEnabled;
        this.hungerAmount = hungerAmount;
        this.damageEnabled = damageEnabled;
        this.damageAmount = damageAmount;
        this.lightningEnabled = lightningEnabled;
        this.lightningDamage = lightningDamage;
        this.commandsEnabled = commandsEnabled;
        this.consoleCommands = consoleCommands;
        this.playerCommands = playerCommands;
        this.spawnMobsEnabled = spawnMobsEnabled;
        this.mobSpawnInfos = mobSpawnInfos;
        this.rewardsEnabled = rewardsEnabled;
        this.rewardChance = rewardChance;
        this.rewardItems = rewardItems;
    }
    
    /**
     * Загрузка настроек из секции конфигурации
     * @param section Секция конфигурации
     * @return Экземпляр настроек
     */
    public static PostTeleportEffectsCfg fromConfig(ConfigurationSection section) {
        if (section == null) {
            return new PostTeleportEffectsCfg();
        }
        
        // Показывать информацию о локации
        boolean showLocationInfo = section.getBoolean("show-location-info", true);
        
        // Эффекты зелий
        List<PotionEffect> potionEffects = new ArrayList<>();
        ConfigurationSection effectsSection = section.getConfigurationSection("effects");
        
        if (effectsSection != null) {
            // Парсим эффекты зелий
            List<String> potionEffectsStr = effectsSection.getStringList("potions");
            for (String effectStr : potionEffectsStr) {
                try {
                    String[] parts = effectStr.split(":");
                    if (parts.length >= 2) {
                        PotionEffectType type = PotionEffectType.getByName(parts[0]);
                        if (type != null) {
                            int duration = Integer.parseInt(parts[1]) * 20; // секунды в тики
                            int amplifier = (parts.length > 2) ? Integer.parseInt(parts[2]) - 1 : 0;
                            potionEffects.add(new PotionEffect(type, duration, amplifier));
                        } else {
                            LOGGER.warning("Неизвестный тип эффекта: " + parts[0]);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Ошибка при парсинге эффекта: " + effectStr, e);
                }
            }
        }
        
        // Настройки голода
        boolean hungerEnabled = false;
        int hungerAmount = 4;
        
        if (effectsSection != null) {
            ConfigurationSection hungerSection = effectsSection.getConfigurationSection("hunger");
            if (hungerSection != null) {
                hungerEnabled = hungerSection.getBoolean("enabled", false);
                hungerAmount = hungerSection.getInt("amount", 4);
            }
        }
        
        // Настройки урона
        boolean damageEnabled = false;
        double damageAmount = 2.0;
        
        if (effectsSection != null) {
            ConfigurationSection damageSection = effectsSection.getConfigurationSection("damage");
            if (damageSection != null) {
                damageEnabled = damageSection.getBoolean("enabled", false);
                damageAmount = damageSection.getDouble("amount", 2.0);
            }
        }
        
        // Настройки молнии
        boolean lightningEnabled = false;
        boolean lightningDamage = false;
        
        if (effectsSection != null) {
            ConfigurationSection lightningSection = effectsSection.getConfigurationSection("lightning");
            if (lightningSection != null) {
                lightningEnabled = lightningSection.getBoolean("enabled", false);
                lightningDamage = lightningSection.getBoolean("damage", false);
            }
        }
        
        // Команды
        boolean commandsEnabled = false;
        List<String> consoleCommands = new ArrayList<>();
        List<String> playerCommands = new ArrayList<>();
        
        if (effectsSection != null) {
            ConfigurationSection commandsSection = effectsSection.getConfigurationSection("commands");
            if (commandsSection != null) {
                commandsEnabled = commandsSection.getBoolean("enabled", false);
                consoleCommands = commandsSection.getStringList("console");
                playerCommands = commandsSection.getStringList("player");
            }
        }
        
        // Спавн мобов
        boolean spawnMobsEnabled = false;
        List<MobSpawnInfo> mobSpawnInfos = new ArrayList<>();
        
        if (effectsSection != null) {
            ConfigurationSection mobsSection = effectsSection.getConfigurationSection("spawn-mobs");
            if (mobsSection != null) {
                spawnMobsEnabled = mobsSection.getBoolean("enabled", false);
                List<String> mobsConfig = mobsSection.getStringList("mobs");
                
                for (String mobConfig : mobsConfig) {
                    try {
                        String[] parts = mobConfig.split(":");
                        if (parts.length >= 4) {
                            String type = parts[0];
                            int amount = Integer.parseInt(parts[1]);
                            int radius = Integer.parseInt(parts[2]);
                            double chance = Double.parseDouble(parts[3]);
                            
                            mobSpawnInfos.add(new MobSpawnInfo(type, amount, radius, chance));
                        }
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Ошибка при парсинге настроек моба: " + mobConfig, e);
                    }
                }
            }
        }
        
        // Система наград
        boolean rewardsEnabled = false;
        double rewardChance = 5.0;
        Map<String, RewardItem> rewardItems = new HashMap<>();
        
        ConfigurationSection rewardsSection = section.getConfigurationSection("rewards");
        if (rewardsSection != null) {
            rewardsEnabled = rewardsSection.getBoolean("enabled", false);
            rewardChance = rewardsSection.getDouble("chance", 5.0);
            
            ConfigurationSection itemsSection = rewardsSection.getConfigurationSection("items");
            if (itemsSection != null) {
                for (String key : itemsSection.getKeys(false)) {
                    try {
                        // Проверяем, является ли значение простым числом или секцией
                        if (itemsSection.isConfigurationSection(key)) {
                            // Сложный формат с дополнительными данными
                            ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
                            int amount = itemSection.getInt("amount", 1);
                            
                            // Получаем данные для предмета
                            Map<String, String> data = null;
                            ConfigurationSection dataSection = itemSection.getConfigurationSection("data");
                            if (dataSection != null) {
                                data = new HashMap<>();
                                for (String dataKey : dataSection.getKeys(false)) {
                                    data.put(dataKey, dataSection.getString(dataKey));
                                }
                            }
                            
                            rewardItems.put(key, new RewardItem(key, amount, data));
                        } else {
                            // Простой формат (предмет: количество)
                            int amount = itemsSection.getInt(key);
                            rewardItems.put(key, new RewardItem(key, amount, null));
                        }
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Ошибка при парсинге предмета награды: " + key, e);
                    }
                }
            }
        }
        
        return new PostTeleportEffectsCfg(
            showLocationInfo, potionEffects,
            hungerEnabled, hungerAmount,
            damageEnabled, damageAmount,
            lightningEnabled, lightningDamage,
            commandsEnabled, consoleCommands, playerCommands,
            spawnMobsEnabled, mobSpawnInfos,
            rewardsEnabled, rewardChance, rewardItems
        );
    }
    
    /**
     * Получение случайного предмета награды
     * @return Предмет или null, если награда не выпала
     */
    @Nullable
    public ItemStack getRandomRewardItem() {
        if (!rewardsEnabled || rewardItems.isEmpty()) {
            return null;
        }
        
        // Проверяем шанс выпадения награды
        double random = ThreadLocalRandom.current().nextDouble(100);
        if (random > rewardChance) {
            return null;
        }
        
        // Выбираем случайный предмет из доступных
        List<String> keys = new ArrayList<>(rewardItems.keySet());
        String selectedKey = keys.get(ThreadLocalRandom.current().nextInt(keys.size()));
        RewardItem rewardItem = rewardItems.get(selectedKey);
        
        return createItemStack(rewardItem);
    }
    
    /**
     * Создание ItemStack из настроек награды
     * @param rewardItem Настройки награды
     * @return ItemStack или null в случае ошибки
     */
    @Nullable
    private ItemStack createItemStack(RewardItem rewardItem) {
        if (rewardItem == null) {
            return null;
        }
        
        // Обработка специального случая для денег
        if ("MONEY".equalsIgnoreCase(rewardItem.getMaterial())) {
            // В реальной реализации здесь должна быть интеграция с Vault
            // Для теста просто возвращаем золотой слиток с описанием
            ItemStack moneyItem = new ItemStack(Material.GOLD_INGOT, 1);
            ItemMeta meta = moneyItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§6Деньги");
                
                // Форматируем сумму
                String format = "{amount} монет";
                if (rewardItem.getData() != null && rewardItem.getData().containsKey("format")) {
                    format = rewardItem.getData().get("format");
                }
                format = format.replace("{amount}", String.valueOf(rewardItem.getAmount()));
                
                meta.setLore(Collections.singletonList("§e" + format));
                moneyItem.setItemMeta(meta);
            }
            return moneyItem;
        }
        
        // Обычные предметы
        try {
            Material material = Material.valueOf(rewardItem.getMaterial().toUpperCase());
            ItemStack item = new ItemStack(material, rewardItem.getAmount());
            
            // Если есть дополнительные данные, обрабатываем их
            if (rewardItem.getData() != null) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    // Устанавливаем имя, если указано
                    if (rewardItem.getData().containsKey("name")) {
                        meta.setDisplayName("§f" + rewardItem.getData().get("name"));
                    }
                    
                    // Добавляем зачарования, если указаны
                    if (rewardItem.getData().containsKey("value")) {
                        String enchantmentsStr = rewardItem.getData().get("value");
                        String[] enchantmentList = enchantmentsStr.split(",");
                        
                        for (String enchStr : enchantmentList) {
                            String[] enchParts = enchStr.split("::");
                            if (enchParts.length >= 2) {
                                String enchName = enchParts[0].toUpperCase();
                                int level = Integer.parseInt(enchParts[1]);
                                
                                try {
                                    Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(enchName.toLowerCase()));
                                    if (enchantment != null) {
                                        // Для зачарованных книг используем EnchantmentStorageMeta
                                        if (material == Material.ENCHANTED_BOOK) {
                                            EnchantmentStorageMeta storageMeta = (EnchantmentStorageMeta) meta;
                                            storageMeta.addStoredEnchant(enchantment, level, true);
                                        } else {
                                            meta.addEnchant(enchantment, level, true);
                                        }
                                    }
                                } catch (Exception e) {
                                    LOGGER.log(Level.WARNING, "Ошибка при добавлении зачарования: " + enchStr, e);
                                }
                            }
                        }
                    }
                    
                    item.setItemMeta(meta);
                }
            }
            
            return item;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Ошибка при создании предмета: " + rewardItem.getMaterial(), e);
            return null;
        }
    }
    
    /**
     * Класс для хранения информации о предмете награды
     */
    public static class RewardItem {
        private final String material;
        private final int amount;
        private final Map<String, String> data;
        
        public RewardItem(String material, int amount, Map<String, String> data) {
            this.material = material;
            this.amount = amount;
            this.data = data;
        }
        
        public String getMaterial() {
            return material;
        }
        
        public int getAmount() {
            return amount;
        }
        
        public Map<String, String> getData() {
            return data;
        }
    }
    
    /**
     * Класс для хранения информации о спавне мобов
     */
    public static class MobSpawnInfo {
        private final String entityType;
        private final int amount;
        private final int radius;
        private final double chance;
        
        public MobSpawnInfo(String entityType, int amount, int radius, double chance) {
            this.entityType = entityType;
            this.amount = amount;
            this.radius = radius;
            this.chance = chance;
        }
        
        public String getEntityType() {
            return entityType;
        }
        
        public int getAmount() {
            return amount;
        }
        
        public int getRadius() {
            return radius;
        }
        
        public double getChance() {
            return chance;
        }
    }
    
    // Геттеры
    
    public boolean isShowLocationInfo() {
        return showLocationInfo;
    }
    
    public List<PotionEffect> getPotionEffects() {
        return potionEffects;
    }
    
    public boolean isHungerEnabled() {
        return hungerEnabled;
    }
    
    public int getHungerAmount() {
        return hungerAmount;
    }
    
    public boolean isDamageEnabled() {
        return damageEnabled;
    }
    
    public double getDamageAmount() {
        return damageAmount;
    }
    
    public boolean isLightningEnabled() {
        return lightningEnabled;
    }
    
    public boolean isLightningDamage() {
        return lightningDamage;
    }
    
    public boolean isCommandsEnabled() {
        return commandsEnabled;
    }
    
    public List<String> getConsoleCommands() {
        return consoleCommands;
    }
    
    public List<String> getPlayerCommands() {
        return playerCommands;
    }
    
    public boolean isSpawnMobsEnabled() {
        return spawnMobsEnabled;
    }
    
    public List<MobSpawnInfo> getMobSpawnInfos() {
        return mobSpawnInfos;
    }
    
    public boolean isRewardsEnabled() {
        return rewardsEnabled;
    }
    
    public double getRewardChance() {
        return rewardChance;
    }
    
    public Map<String, RewardItem> getRewardItems() {
        return rewardItems;
    }
} 