package org.zoobastiks.zrtp.config;

import org.bukkit.Location;

import java.util.HashSet;
import java.util.Set;

/**
 * Класс конфигурации мира для телепортации
 */
public class WorldCfg {
    private final int minRadius;
    private final int maxRadius;
    private final int delay;
    private final int cooldown;
    private final double price;
    private final boolean enabled;
    private final Location center;
    private Set<String> forbiddenBiomes = new HashSet<>();
    
    /**
     * Конструктор конфигурации мира
     * 
     * @param minRadius Минимальный радиус телепортации
     * @param maxRadius Максимальный радиус телепортации
     * @param delay Задержка перед телепортацией в секундах
     * @param cooldown Кулдаун между телепортациями в секундах
     * @param price Цена телепортации
     * @param enabled Включен ли мир для телепортации
     * @param center Центральная позиция для телепортации
     */
    public WorldCfg(int minRadius, int maxRadius, int delay, int cooldown, double price, boolean enabled, Location center) {
        this.minRadius = minRadius;
        this.maxRadius = maxRadius;
        this.delay = delay;
        this.cooldown = cooldown;
        this.price = price;
        this.enabled = enabled;
        this.center = center;
    }
    
    /**
     * Получение минимального радиуса телепортации
     * @return Минимальный радиус
     */
    public int getMinRadius() {
        return minRadius;
    }
    
    /**
     * Получение максимального радиуса телепортации
     * @return Максимальный радиус
     */
    public int getMaxRadius() {
        return maxRadius;
    }
    
    /**
     * Получение задержки перед телепортацией
     * @return Задержка в секундах
     */
    public int getDelay() {
        return delay;
    }
    
    /**
     * Получение времени между телепортациями
     * @return Кулдаун в секундах
     */
    public int getCooldown() {
        return cooldown;
    }
    
    /**
     * Получение стоимости телепортации
     * @return Цена
     */
    public double getPrice() {
        return price;
    }
    
    /**
     * Проверка доступности мира для телепортации
     * @return Доступен ли мир
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Получение центральной позиции для телепортации
     * @return Локация центра или null если не задана
     */
    public Location getCenter() {
        return center;
    }
    
    /**
     * Получение списка запрещенных биомов
     * @return Множество имен запрещенных биомов
     */
    public Set<String> getForbiddenBiomes() {
        return forbiddenBiomes;
    }
    
    /**
     * Установка списка запрещенных биомов
     * @param forbiddenBiomes Множество имен запрещенных биомов
     */
    public void setForbiddenBiomes(Set<String> forbiddenBiomes) {
        this.forbiddenBiomes = forbiddenBiomes;
    }
    
    /**
     * Проверка, запрещен ли биом в данном мире
     * @param biomeName Имя биома
     * @return true, если биом запрещен
     */
    public boolean isBiomeForbidden(String biomeName) {
        return forbiddenBiomes.contains(biomeName);
    }
} 