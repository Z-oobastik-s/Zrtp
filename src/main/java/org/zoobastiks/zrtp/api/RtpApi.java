package org.zoobastiks.zrtp.api;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.zoobastiks.zrtp.Zrtp;

import java.util.concurrent.CompletableFuture;

/**
 * API для интеграции с другими плагинами
 */
public class RtpApi {
    private static Zrtp plugin;
    
    /**
     * Инициализация API
     * @param instance Экземпляр плагина
     */
    public static void init(Zrtp instance) {
        plugin = instance;
    }
    
    /**
     * Получение экземпляра API
     * @return Экземпляр API или null, если плагин не загружен
     */
    public static RtpApi getInstance() {
        if (plugin == null) {
            return null;
        }
        return new RtpApi();
    }
    
    /**
     * Выполнить телепортацию игрока случайным образом
     * @param player Игрок для телепортации
     * @return true, если игрок соответствует всем требованиям для телепортации
     */
    public boolean teleportPlayer(Player player) {
        if (plugin == null) return false;
        
        // Проверка, находится ли игрок уже в процессе телепортации
        if (plugin.getTaskManager().isPlayerTeleporting(player.getUniqueId())) {
            return false;
        }
        
        // Проверка, включена ли телепортация в мире игрока
        String worldName = player.getWorld().getName();
        if (!plugin.getPluginConfig().getWorldConfig(worldName).isEnabled()) {
            return false;
        }
        
        // Проверка кулдауна
        long lastTeleport = plugin.getCooldowns().getOrDefault(player.getUniqueId(), 0L);
        long cooldownTime = plugin.getPluginConfig().getWorldConfig(worldName).getCooldown() * 1000L;
        long currentTime = System.currentTimeMillis();
        
        if (currentTime - lastTeleport < cooldownTime && !player.hasPermission("rtp.nocooldown")) {
            return false;
        }
        
        // Если все проверки пройдены, начинаем телепортацию
        plugin.getTaskManager().startTeleport(player);
        return true;
    }
    
    /**
     * Проверка, находится ли игрок в процессе телепортации
     * @param player Игрок для проверки
     * @return true, если игрок в процессе телепортации
     */
    public boolean isPlayerTeleporting(Player player) {
        if (plugin == null) return false;
        return plugin.getTaskManager().isPlayerTeleporting(player.getUniqueId());
    }
    
    /**
     * Отмена текущей телепортации игрока
     * @param player Игрок для отмены телепортации
     */
    public void cancelTeleport(Player player) {
        if (plugin == null) return;
        plugin.getTaskManager().cancelTeleport(player.getUniqueId());
    }
    
    /**
     * Получить случайную безопасную локацию в указанном мире
     * @param world Мир для поиска локации
     * @return CompletableFuture с результатом - локация или null, если не найдена
     */
    public CompletableFuture<Location> getRandomLocation(World world) {
        CompletableFuture<Location> future = new CompletableFuture<>();
        
        if (plugin == null) {
            future.complete(null);
            return future;
        }
        
        // Используем внутренний метод для поиска локации
        plugin.getTaskManager().findRandomLocation(world).thenAccept(future::complete);
        
        return future;
    }
} 