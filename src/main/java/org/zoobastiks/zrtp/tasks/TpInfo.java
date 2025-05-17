package org.zoobastiks.zrtp.tasks;

import org.bukkit.Location;

/**
 * Класс для хранения информации о телепортации
 */
public class TpInfo {
    private final Location startLocation;
    private final int delay;
    private final long startTime;
    private final double price;
    
    /**
     * Конструктор класса информации о телепортации
     * @param startLocation Начальная локация
     * @param delay Задержка перед телепортацией в секундах
     */
    public TpInfo(Location startLocation, int delay) {
        this(startLocation, delay, 0.0);
    }
    
    /**
     * Конструктор класса информации о телепортации
     * @param startLocation Начальная локация
     * @param delay Задержка перед телепортацией в секундах
     * @param price Стоимость телепортации
     */
    public TpInfo(Location startLocation, int delay, double price) {
        this.startLocation = startLocation;
        this.delay = delay;
        this.startTime = System.currentTimeMillis();
        this.price = price;
    }
    
    /**
     * Получение начальной локации
     * @return Начальная локация
     */
    public Location getStartLocation() {
        return startLocation;
    }
    
    /**
     * Получение задержки перед телепортацией
     * @return Задержка в секундах
     */
    public int getDelay() {
        return delay;
    }
    
    /**
     * Получение времени начала телепортации
     * @return Время начала телепортации в миллисекундах
     */
    public long getStartTime() {
        return startTime;
    }
    
    /**
     * Получение стоимости телепортации
     * @return Стоимость телепортации
     */
    public double getPrice() {
        return price;
    }
    
    /**
     * Проверка, истекло ли время задержки
     * @return true если время задержки истекло
     */
    public boolean isTimeExpired() {
        return System.currentTimeMillis() >= startTime + (delay * 1000L);
    }
} 