package org.zoobastiks.zrtp.effects;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.zoobastiks.zrtp.Zrtp;
import org.zoobastiks.zrtp.common.Lang;
import org.zoobastiks.zrtp.common.TeleportEffectsMessages;
import org.zoobastiks.zrtp.config.TeleportEffectsCfg;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Класс для создания эффектов телепортации
 */
public class TeleportEffects {
    private final Zrtp plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<UUID, BukkitTask> effectTasks = new HashMap<>();
    
    /**
     * Конструктор класса эффектов
     * @param plugin Экземпляр плагина
     */
    public TeleportEffects(Zrtp plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Запуск эффектов телепортации с колбэком завершения
     * @param player Игрок
     * @param delayInSeconds Задержка телепортации в секундах
     * @param completionCallback Колбэк, который будет вызван после завершения всех эффектов
     */
    public void startTeleportEffects(Player player, int delayInSeconds, Runnable completionCallback) {
        // Отладочное сообщение для трассировки вызова
        plugin.log(java.util.logging.Level.INFO, "Вызов startTeleportEffects с колбэком для " + player.getName() + 
                   ", задержка: " + delayInSeconds + " сек.");
        
        // Проверяем, включены ли эффекты телепортации
        TeleportEffectsCfg config = plugin.getPluginConfig().getTeleportEffectsConfig();
        
        // Отладочное сообщение о состоянии конфигурации
        plugin.log(java.util.logging.Level.INFO, "Эффекты включены: " + config.isEnabled() + 
                   ", интервал: " + config.getMessageInterval() + 
                   ", звук стадии 1: " + config.getStage1Sound().toString());
        
        if (!config.isEnabled()) {
            plugin.log(java.util.logging.Level.INFO, "Эффекты отключены в конфигурации, вызываем колбэк сразу.");
            // Если эффекты отключены, вызываем колбэк сразу
            completionCallback.run();
            return;
        }
        
        UUID uuid = player.getUniqueId();
        
        // Отменяем текущие эффекты, если они есть
        cancelEffects(uuid);
        
        // Запускаем последовательность эффектов
        AtomicInteger stageCounter = new AtomicInteger(0);
        
        plugin.log(java.util.logging.Level.INFO, "Запуск задачи эффектов с интервалом " + 
                   config.getMessageInterval() + " тиков.");
        
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            int currentStage = stageCounter.getAndIncrement();
            
            plugin.log(java.util.logging.Level.INFO, "Воспроизведение эффекта для стадии " + currentStage);
            
            if (currentStage < 20) { // Запускаем эффекты только для 4-х стадий + отсчет
                playEffectsForStage(player, currentStage);
            } else {
                plugin.log(java.util.logging.Level.INFO, "Завершение последовательности эффектов.");
                cancelEffects(uuid);
                
                // Вызываем колбэк после завершения всех эффектов
                plugin.log(java.util.logging.Level.INFO, "Вызываем колбэк завершения эффектов.");
                completionCallback.run();
            }
            
        }, 0L, config.getMessageInterval()); // Выполняем с интервалом из конфигурации
        
        effectTasks.put(uuid, task);
    }
    
    /**
     * Запуск эффектов телепортации
     * @param player Игрок
     * @param delayInSeconds Задержка телепортации в секундах
     */
    public void startTeleportEffects(Player player, int delayInSeconds) {
        // Отладочное сообщение для трассировки вызова
        plugin.log(java.util.logging.Level.INFO, "Вызов startTeleportEffects без колбэка для " + player.getName() + 
                   ", задержка: " + delayInSeconds + " сек.");
        
        // Вызываем версию с колбэком, но с пустым действием
        startTeleportEffects(player, delayInSeconds, () -> {
            plugin.log(java.util.logging.Level.INFO, "Эффекты завершены, но колбэк не предоставлен.");
        });
    }
    
    /**
     * Воспроизвести эффекты для определенной стадии
     * @param player Игрок
     * @param stage Номер стадии
     */
    private void playEffectsForStage(Player player, int stage) {
        TeleportEffectsCfg config = plugin.getPluginConfig().getTeleportEffectsConfig();
        TeleportEffectsMessages messages = plugin.getLang().getTeleportEffectsMessages();
        
        plugin.log(java.util.logging.Level.INFO, "playEffectsForStage: стадия " + stage + 
                   " для игрока " + player.getName());
        
        if (stage < 3) {
            // Стадия 1: Лаборатория и поиск
            String message = messages.getStageMessage(1, stage % 3);
            if (message != null) {
                plugin.log(java.util.logging.Level.INFO, "Стадия 1, сообщение: " + message);
                playStageEffect(player, message, config.getStage1Sound(), 
                    config.getStageStartColor(), config.getStageEndColor());
            } else {
                plugin.log(java.util.logging.Level.WARNING, "Не найдено сообщение для стадии 1, индекс " + (stage % 3));
            }
        } else if (stage < 6) {
            // Стадия 2: Связь и сканирование
            String message = messages.getStageMessage(2, (stage - 3) % 3);
            if (message != null) {
                plugin.log(java.util.logging.Level.INFO, "Стадия 2, сообщение: " + message);
                playStageEffect(player, message, config.getStage2Sound(), 
                    config.getStageStartColor(), config.getStageEndColor());
            } else {
                plugin.log(java.util.logging.Level.WARNING, "Не найдено сообщение для стадии 2, индекс " + ((stage - 3) % 3));
            }
        } else if (stage < 9) {
            // Стадия 3: Проверка и стабильность
            String message = messages.getStageMessage(3, (stage - 6) % 3);
            if (message != null) {
                plugin.log(java.util.logging.Level.INFO, "Стадия 3, сообщение: " + message);
                playStageEffect(player, message, config.getStage3Sound(), 
                    config.getStageStartColor(), config.getStageEndColor());
            } else {
                plugin.log(java.util.logging.Level.WARNING, "Не найдено сообщение для стадии 3, индекс " + ((stage - 6) % 3));
            }
        } else if (stage < 12) {
            // Стадия 4: Подготовка к телепортации
            String message = messages.getStageMessage(4, (stage - 9) % 3);
            if (message != null) {
                plugin.log(java.util.logging.Level.INFO, "Стадия 4, сообщение: " + message);
                playStageEffect(player, message, config.getStage4Sound(), 
                    config.getStageStartColor(), config.getStageEndColor());
            } else {
                plugin.log(java.util.logging.Level.WARNING, "Не найдено сообщение для стадии 4, индекс " + ((stage - 9) % 3));
            }
        } else if (stage < 16) {
            // Отсчет
            int countdownIndex = stage - 12;
            String message = messages.getCountdownMessage(countdownIndex);
            if (message != null) {
                plugin.log(java.util.logging.Level.INFO, "Отсчет, сообщение: " + message);
                playCountdownEffect(player, message, config.getCountdownSound(), countdownIndex,
                    config.getCountdownStartColor(), config.getCountdownEndColor());
            } else {
                plugin.log(java.util.logging.Level.WARNING, "Не найдено сообщение для отсчета, индекс " + countdownIndex);
            }
        } else if (stage == 16) {
            // Финальное сообщение о телепортации
            plugin.log(java.util.logging.Level.INFO, "Воспроизведение финального эффекта телепортации");
            playTeleportEffect(player);
        }
    }
    
    /**
     * Воспроизвести эффект для стадии
     * @param player Игрок
     * @param message Сообщение
     * @param sound Звук
     * @param startColor Начальный цвет градиента
     * @param endColor Конечный цвет градиента
     */
    private void playStageEffect(Player player, String message, Sound sound, String startColor, String endColor) {
        TeleportEffectsCfg config = plugin.getPluginConfig().getTeleportEffectsConfig();
        
        plugin.log(java.util.logging.Level.INFO, "playStageEffect: воспроизведение для " + player.getName() + 
                   ", сообщение: " + message + ", звук: " + sound.toString());
        
        try {
            // Создаем компонент сообщения без префикса для ActionBar
            Component messageComponent;
            
            if (config.useActionBar()) {
                // Для ActionBar используем только текст сообщения с градиентом, без префикса
                messageComponent = miniMessage.deserialize(plugin.getLang().gradient(message, startColor, endColor));
                player.sendActionBar(messageComponent);
                plugin.log(java.util.logging.Level.INFO, "Сообщение отправлено в ActionBar");
            } else {
                // Для чата используем префикс и сообщение
                messageComponent = miniMessage.deserialize(plugin.getLang().getMessage(Lang.Keys.PREFIX) + 
                                                          plugin.getLang().gradient(message, startColor, endColor));
                player.sendMessage(messageComponent);
                plugin.log(java.util.logging.Level.INFO, "Сообщение отправлено в чат");
            }
            
            // Воспроизводим звук
            player.playSound(player.getLocation(), sound, config.getVolume(), config.getPitch());
            
            plugin.log(java.util.logging.Level.INFO, "Эффект успешно воспроизведен");
        } catch (Exception e) {
            plugin.log(java.util.logging.Level.WARNING, "Ошибка при воспроизведении эффекта: " + e.getMessage());
        }
    }
    
    /**
     * Воспроизвести эффект отсчета
     * @param player Игрок
     * @param message Сообщение
     * @param sound Звук
     * @param stage Этап отсчета (0 = начало, 1-3 = цифры)
     * @param startColor Начальный цвет градиента
     * @param endColor Конечный цвет градиента
     */
    private void playCountdownEffect(Player player, String message, Sound sound, int stage, 
                                    String startColor, String endColor) {
        TeleportEffectsCfg config = plugin.getPluginConfig().getTeleportEffectsConfig();
        TeleportEffectsMessages messages = plugin.getLang().getTeleportEffectsMessages();
        
        plugin.log(java.util.logging.Level.INFO, "playCountdownEffect: воспроизведение для " + player.getName() + 
                   ", стадия отсчета: " + stage + ", сообщение: " + message);
        
        try {
            // Создаем компонент сообщения
            Component messageComponent;
            
            if (config.useActionBar() && stage == 0) {
                // Только первое сообщение отсчета отправляем в ActionBar, если включено
                messageComponent = miniMessage.deserialize(plugin.getLang().gradient(message, startColor, endColor));
                player.sendActionBar(messageComponent);
                plugin.log(java.util.logging.Level.INFO, "Сообщение отправлено в ActionBar");
            } else if (!config.useActionBar()) {
                // Если ActionBar отключен, отправляем в чат
                messageComponent = miniMessage.deserialize(plugin.getLang().getMessage(Lang.Keys.PREFIX) + 
                                                          plugin.getLang().gradient(message, startColor, endColor));
                player.sendMessage(messageComponent);
                plugin.log(java.util.logging.Level.INFO, "Сообщение отправлено в чат");
            }
            
            // Отправляем title и actionbar для цифр отсчета
            if (stage > 0) { // Если это цифры 3, 2, 1
                String number = message.replace("...", "");
                
                // Создаем title
                Component title = Component.text(number)
                        .color(TextColor.fromHexString(config.getCountdownColor(stage == 1 ? 3 : stage == 2 ? 2 : 1)));
                
                Title titleObj = Title.title(title, Component.empty(), 
                                Title.Times.of(config.getTitleFadeIn(), config.getTitleStay(), config.getTitleFadeOut()));
                player.showTitle(titleObj);
                
                // Создаем actionbar с таймером
                String actionbarText = messages.getActionbarCountdown().replace("{count}", number);
                Component actionBar = Component.text(actionbarText)
                        .color(TextColor.fromHexString("#FFFFFF"));
                player.sendActionBar(actionBar);
                
                plugin.log(java.util.logging.Level.INFO, "Отображен title и actionbar: " + number);
            }
            
            // Воспроизводим звук
            player.playSound(player.getLocation(), sound, config.getVolume(), config.getPitch());
            
            plugin.log(java.util.logging.Level.INFO, "Эффект отсчета успешно воспроизведен");
        } catch (Exception e) {
            plugin.log(java.util.logging.Level.WARNING, "Ошибка при воспроизведении эффекта отсчета: " + e.getMessage());
        }
    }
    
    /**
     * Воспроизвести эффект телепортации
     * @param player Игрок
     */
    private void playTeleportEffect(Player player) {
        TeleportEffectsCfg config = plugin.getPluginConfig().getTeleportEffectsConfig();
        TeleportEffectsMessages messages = plugin.getLang().getTeleportEffectsMessages();
        
        plugin.log(java.util.logging.Level.INFO, "playTeleportEffect: воспроизведение финального эффекта для " + 
                   player.getName() + ", сообщение: " + messages.getTeleportMessage());
        
        try {
            // Создаем компонент сообщения
            Component messageComponent;
            
            if (config.useActionBar()) {
                // Для ActionBar используем только текст сообщения с градиентом, без префикса
                messageComponent = miniMessage.deserialize(plugin.getLang().gradient(messages.getTeleportMessage(), 
                                                                                     config.getTeleportStartColor(), 
                                                                                     config.getTeleportEndColor()));
                player.sendActionBar(messageComponent);
                plugin.log(java.util.logging.Level.INFO, "Финальное сообщение отправлено в ActionBar");
            } else {
                // Для чата используем префикс и сообщение
                messageComponent = miniMessage.deserialize(plugin.getLang().getMessage(Lang.Keys.PREFIX) + 
                                                          plugin.getLang().gradient(messages.getTeleportMessage(), 
                                                                                   config.getTeleportStartColor(), 
                                                                                   config.getTeleportEndColor()));
                player.sendMessage(messageComponent);
                plugin.log(java.util.logging.Level.INFO, "Финальное сообщение отправлено в чат");
            }
            
            // Создаем title
            Component title = Component.text(messages.getTitleTeleport())
                    .color(TextColor.fromHexString(config.getTeleportStartColor()));
            Title titleObj = Title.title(title, Component.empty(), 
                           Title.Times.of(config.getTitleFadeIn(), config.getTitleStay(), config.getTitleFadeOut()));
            player.showTitle(titleObj);
            
            // Воспроизводим звук
            player.playSound(player.getLocation(), config.getTeleportSound(), config.getVolume(), config.getPitch());
            
            plugin.log(java.util.logging.Level.INFO, "Финальный эффект телепортации успешно воспроизведен");
        } catch (Exception e) {
            plugin.log(java.util.logging.Level.WARNING, "Ошибка при воспроизведении финального эффекта: " + e.getMessage());
        }
    }
    
    /**
     * Отмена всех эффектов для игрока
     * @param uuid UUID игрока
     */
    public void cancelEffects(UUID uuid) {
        BukkitTask task = effectTasks.remove(uuid);
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }
} 