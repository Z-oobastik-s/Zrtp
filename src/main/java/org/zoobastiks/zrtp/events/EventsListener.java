package org.zoobastiks.zrtp.events;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.zoobastiks.zrtp.Zrtp;
import org.zoobastiks.zrtp.common.Lang;
import org.zoobastiks.zrtp.common.TeleportEffectsMessages;
import org.zoobastiks.zrtp.config.TeleportEffectsCfg;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;

import java.time.Duration;
import java.util.UUID;

/**
 * Обработчик событий для плагина
 */
public class EventsListener implements Listener {
    private final Zrtp plugin;
    
    /**
     * Конструктор обработчика событий
     * @param plugin Экземпляр плагина
     */
    public EventsListener(Zrtp plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Обработка события перемещения игрока
     * @param event Событие перемещения
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Проверяем, находится ли игрок в процессе телепортации
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Если игрок не телепортируется, нет смысла продолжать проверку
        if (!plugin.getTaskManager().isPlayerTeleporting(uuid)) {
            return;
        }
        
        // Получим начальную локацию телепортации
        Location startLocation = plugin.getTaskManager().getStartLocation(uuid);
        
        // Проверяем, было ли существенное перемещение (не просто поворот головы)
        // Отменяем телепортацию только если игрок значительно переместился
        // Проверяем изменение координат или если startLocation недоступна
        if (startLocation != null) {
            // Более точная проверка с использованием стартовой позиции
            double distanceSquared = event.getTo().distanceSquared(startLocation);
            if (distanceSquared > 0.1) { // Минимальное перемещение для отмены телепортации
                cancelTeleportWithEffects(player, uuid, event);
            }
        } else if (event.getFrom().getBlockX() != event.getTo().getBlockX() ||
                  event.getFrom().getBlockZ() != event.getTo().getBlockZ() ||
                  event.getFrom().getBlockY() != event.getTo().getBlockY()) {
            // Стандартная проверка, если нет стартовой позиции
            cancelTeleportWithEffects(player, uuid, event);
        }
    }
    
    /**
     * Отменяет телепортацию с эффектами
     * @param player Игрок
     * @param uuid UUID игрока
     * @param event Событие перемещения (для логирования)
     */
    private void cancelTeleportWithEffects(Player player, UUID uuid, PlayerMoveEvent event) {
        // Логируем информацию о перемещении для отладки
        plugin.log(java.util.logging.Level.INFO, "Телепортация отменена из-за движения игрока: " + 
                   player.getName() + " от " + formatLocation(event.getFrom()) + 
                   " к " + formatLocation(event.getTo()));
        
        // Останавливаем эффекты телепортации
        plugin.getTeleportEffects().cancelEffects(uuid);
        
        // Отменяем телепортацию
        plugin.getTaskManager().cancelTeleport(uuid);
        
        // Отправляем стандартное сообщение об отмене
        plugin.getLang().sendAdvancedMessage(player, Lang.Keys.TELEPORT_CANCELLED);
        
        // Получаем настройки эффектов
        TeleportEffectsCfg config = plugin.getPluginConfig().getTeleportEffectsConfig();
        TeleportEffectsMessages messages = plugin.getLang().getTeleportEffectsMessages();
        
        // Если эффекты отключены, не показываем дополнительные эффекты
        if (!config.isEnabled()) return;
        
        // Воспроизводим звук отмены
        player.playSound(player.getLocation(), config.getCancelSound(), config.getVolume(), config.getPitch());
        
        // Показываем заголовок с сообщением об отмене
        Component title = Component.text(messages.getTitleCancel())
            .color(TextColor.fromHexString(config.getCancelColor()));
        
        Title titleObj = Title.title(
            title, 
            Component.empty(), 
            Title.Times.of(config.getTitleFadeIn(), config.getTitleStay(), config.getTitleFadeOut())
        );
        
        player.showTitle(titleObj);
        
        // Если включен ActionBar, отправляем сообщение об отмене в ActionBar
        if (config.useActionBar()) {
            Component cancelMessage = plugin.getTranslator().parseMessage(
                messages.getCancelMessage(),
                "reason", "движение"
            );
            player.sendActionBar(cancelMessage);
        }
    }
    
    /**
     * Форматирование локации для логирования
     * @param location Локация
     * @return Отформатированная строка
     */
    private String formatLocation(Location location) {
        return "(" + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + ")";
    }
    
    /**
     * Обработка события выхода игрока с сервера
     * @param event Событие выхода
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Отменяем телепортацию, если игрок выходит с сервера
        UUID uuid = event.getPlayer().getUniqueId();
        if (plugin.getTaskManager().isPlayerTeleporting(uuid)) {
            // Останавливаем эффекты телепортации
            plugin.getTeleportEffects().cancelEffects(uuid);
            // Отменяем телепортацию
            plugin.getTaskManager().cancelTeleport(uuid);
        }
    }
    
    /**
     * Обработка события смерти игрока
     * @param event Событие смерти
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        // Отменяем телепортацию, если игрок умер
        UUID uuid = event.getEntity().getUniqueId();
        if (plugin.getTaskManager().isPlayerTeleporting(uuid)) {
            // Останавливаем эффекты телепортации
            plugin.getTeleportEffects().cancelEffects(uuid);
            // Отменяем телепортацию
            plugin.getTaskManager().cancelTeleport(uuid);
        }
    }
    
    /**
     * Обработка события возрождения игрока
     * @param event Событие возрождения
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        // Больше не телепортируем игрока при возрождении
    }
    
    /**
     * Обработка события входа игрока на сервер
     * @param event Событие входа
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Больше не телепортируем игрока при входе на сервер
    }
} 