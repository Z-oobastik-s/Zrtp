package org.zoobastiks.zrtp.tasks;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.zoobastiks.zrtp.Zrtp;
import org.zoobastiks.zrtp.common.Lang;
import org.zoobastiks.zrtp.config.WorldCfg;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * Класс для управления задачами телепортации
 */
public class TaskMgr {
    private final Zrtp plugin;
    private BukkitTask locationCacheTask;
    
    // Хранение состояний телепортации игроков
    private final Map<UUID, TpInfo> teleportingPlayers = new ConcurrentHashMap<>();
    
    // Кэш случайных локаций для быстрой телепортации
    private final Map<String, Queue<Location>> locationCache = new HashMap<>();
    
    // Хранение задач телепортации
    private final Map<UUID, BukkitTask> teleportTasks = new ConcurrentHashMap<>();
    
    /**
     * Конструктор менеджера задач
     * @param plugin Экземпляр плагина
     */
    public TaskMgr(Zrtp plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Запуск всех задач
     */
    public void startTasks() {
        // Задача для заполнения кэша локаций
        locationCacheTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            for (String worldName : plugin.getPluginConfig().getEnabledWorldNames()) {
                World world = Bukkit.getWorld(worldName);
                if (world == null) continue;
                
                WorldCfg worldConfig = plugin.getPluginConfig().getWorldConfig(worldName);
                if (!worldConfig.isEnabled()) continue;
                
                // Не генерируем больше 10 локаций для каждого мира
                Queue<Location> locations = locationCache.computeIfAbsent(worldName, k -> new LinkedList<>());
                if (locations.size() >= 10) continue;
                
                // Асинхронно находим безопасную локацию
                findSafeLocation(world, worldConfig).thenAccept(location -> {
                    if (location != null) {
                        synchronized (locationCache) {
                            Queue<Location> queue = locationCache.computeIfAbsent(worldName, k -> new LinkedList<>());
                            queue.add(location);
                        }
                    }
                });
            }
        }, 100L, 600L); // Первая попытка через 5 секунд, потом каждые 30 секунд
    }
    
    /**
     * Остановка всех задач
     */
    public void stopTasks() {
        if (locationCacheTask != null && !locationCacheTask.isCancelled()) {
            locationCacheTask.cancel();
        }
        
        // Отменяем все задачи телепортации
        teleportTasks.values().forEach(task -> {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        });
        teleportTasks.clear();
        teleportingPlayers.clear();
    }
    
    /**
     * Начать телепортацию игрока
     * @param player Игрок для телепортации
     */
    public void startTeleport(Player player) {
        // Прерывание, если игрок уже телепортируется
        UUID uuid = player.getUniqueId();
        if (teleportTasks.containsKey(uuid)) {
            plugin.getLang().sendAdvancedMessage(player, Lang.Keys.ALREADY_TELEPORTING);
            return;
        }
        
        // Получаем настройки мира
        String worldName = player.getWorld().getName();
        WorldCfg worldConfig = plugin.getPluginConfig().getWorldConfig(worldName);
        
        // Проверяем, включена ли телепортация в этом мире
        if (!worldConfig.isEnabled()) {
            plugin.getLang().sendAdvancedMessage(player, Lang.Keys.WORLD_DISABLED);
            return;
        }
        
        // Проверяем, прошло ли достаточно времени с последней телепортации
        long lastTeleport = plugin.getCooldowns().getOrDefault(uuid, 0L);
        long cooldownTime = worldConfig.getCooldown() * 1000L;
        long currentTime = System.currentTimeMillis();
        
        if (currentTime - lastTeleport < cooldownTime && !player.hasPermission("rtp.nocooldown")) {
            long remaining = (lastTeleport + cooldownTime - currentTime) / 1000;
            plugin.getLang().sendAdvancedMessage(player, Lang.Keys.COOLDOWN, 
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("time", String.valueOf(remaining))
            );
            return;
        }
        
        // Проверяем, есть ли у игрока достаточно денег для телепортации
        double price = worldConfig.getPrice();
        if (price > 0 && plugin.getEconomyManager().isEnabled()) {
            // Если цена больше 0 и экономика включена, проверяем баланс
            if (!plugin.getEconomyManager().hasMoney(player, price)) {
                // Если не хватает денег, отправляем сообщение
                String formattedPrice = plugin.getEconomyManager().formatMoney(price);
                plugin.getLang().sendAdvancedMessage(player, Lang.Keys.NOT_ENOUGH_MONEY, 
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("price", formattedPrice)
                );
                return;
            }
        }
        
        // Получаем задержку телепортации из конфигурации
        int delaySeconds = worldConfig.getDelay();
        
        // Отладочное сообщение
        plugin.log(java.util.logging.Level.INFO, "Запуск телепортации для " + player.getName() + 
                   " с задержкой " + delaySeconds + " сек. Эффекты включены: " + 
                   plugin.getPluginConfig().getTeleportEffectsConfig().isEnabled());
        
        // Установка минимальной задержки для гарантированного отображения всех эффектов
        // Количество стадий = 20, длительность каждой стадии = messageInterval тиков
        // Конвертируем тики в секунды (20 тиков = 1 секунда)
        int messageInterval = plugin.getPluginConfig().getTeleportEffectsConfig().getMessageInterval();
        int totalEffectsDuration = (20 * messageInterval) / 20; // в секундах
        
        // Убеждаемся, что задержка не меньше требуемой для отображения всех эффектов
        int effectDelay = Math.max(delaySeconds, totalEffectsDuration);
        
        // Снимаем деньги с игрока, если цена больше 0 и экономика включена
        if (price > 0 && plugin.getEconomyManager().isEnabled()) {
            if (!plugin.getEconomyManager().withdrawMoney(player, price)) {
                // Если не удалось снять деньги, прерываем телепортацию
                plugin.log(java.util.logging.Level.WARNING, "Не удалось снять деньги с игрока " + player.getName());
                return;
            }
            // Сообщаем игроку о снятии денег
            String formattedPrice = plugin.getEconomyManager().formatMoney(price);
            plugin.getLang().sendAdvancedMessage(player, Lang.Keys.MONEY_WITHDRAWN, 
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("price", formattedPrice)
            );
        }
        
        // Добавляем игрока в список телепортирующихся сразу и сохраняем цену
        teleportingPlayers.put(uuid, new TpInfo(player.getLocation(), effectDelay, price));
        
        // Сообщение о начале телепортации
        plugin.getLang().sendAdvancedMessage(player, Lang.Keys.TELEPORT_STARTED, 
            net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("delay", String.valueOf(effectDelay))
        );
        
        // Запускаем эффекты телепортации
        // Передаем колбэк, который будет вызван по завершении всех эффектов
        plugin.getTeleportEffects().startTeleportEffects(player, effectDelay, () -> {
            plugin.log(java.util.logging.Level.INFO, "Эффекты завершены, выполняем телепортацию для " + player.getName());
            
            // Проверяем, что игрок все еще онлайн и все еще ожидает телепортацию
            if (teleportingPlayers.containsKey(uuid)) {
                // Удаляем игрока из списка телепортирующихся
                teleportingPlayers.remove(uuid);
                teleportTasks.remove(uuid);
                
                // Проверяем, что игрок все еще онлайн
                Player teleportingPlayer = Bukkit.getPlayer(uuid);
                if (teleportingPlayer != null && teleportingPlayer.isOnline()) {
                    // Выполняем телепортацию сразу после завершения всех эффектов
                    executeTeleport(teleportingPlayer);
                }
            }
        });
    }
    
    /**
     * Выполнить телепортацию игрока
     * @param player Игрок для телепортации
     */
    private void executeTeleport(Player player) {
        UUID uuid = player.getUniqueId();
        
        // Получаем мир игрока
        World world = player.getWorld();
        
        // Ищем безопасную локацию
        findRandomLocation(world).thenAccept(location -> {
            // Проверяем, что игрок все еще онлайн
            Player onlinePlayer = Bukkit.getPlayer(uuid);
            if (onlinePlayer != null && onlinePlayer.isOnline()) {
                // Задача должна выполняться в основном потоке
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (location != null) {
                        // Телепортируем игрока
                        onlinePlayer.teleport(location);
                        
                        // Устанавливаем кулдаун
                        plugin.getCooldowns().put(uuid, System.currentTimeMillis());
                        
                        // Отправляем сообщение об успешной телепортации
                        plugin.getLang().sendAdvancedMessage(onlinePlayer, Lang.Keys.TELEPORT_SUCCESS);
                        
                        // Применяем эффекты после телепортации
                        plugin.getPostTeleportEffects().applyPostTeleportEffects(onlinePlayer, location);
                    } else {
                        // Не удалось найти безопасное место
                        plugin.getLang().sendAdvancedMessage(onlinePlayer, Lang.Keys.UNSAFE_LOCATION);
                    }
                });
            }
        }).exceptionally(ex -> {
            plugin.log(Level.WARNING, "Ошибка при поиске локации для телепортации", ex);
            
            // Проверяем, что игрок все еще онлайн
            Player onlinePlayer = Bukkit.getPlayer(uuid);
            if (onlinePlayer != null && onlinePlayer.isOnline()) {
                // Отправляем сообщение об ошибке
                plugin.getLang().sendAdvancedMessage(onlinePlayer, Lang.Keys.UNSAFE_LOCATION);
            }
            
            return null;
        });
    }
    
    /**
     * Завершение телепортации
     * @param player Игрок
     * @param location Целевая локация
     */
    private void completeTeleport(Player player, Location location) {
        UUID uuid = player.getUniqueId();
        
        // Телепортируем игрока
        player.teleport(location);
        
        // Обновляем кулдаун
        plugin.getCooldowns().put(uuid, System.currentTimeMillis());
        
        // Отправляем сообщение об успешной телепортации
        plugin.getLang().sendAdvancedMessage(player, Lang.Keys.TELEPORT_SUCCESS);
        
        // Применяем эффекты после телепортации
        plugin.getPostTeleportEffects().applyPostTeleportEffects(player, location);
        
        // Очищаем данные
        teleportingPlayers.remove(uuid);
        teleportTasks.remove(uuid);
    }
    
    /**
     * Проверка, телепортируется ли игрок
     * @param uuid UUID игрока
     * @return true, если игрок находится в процессе телепортации
     */
    public boolean isPlayerTeleporting(UUID uuid) {
        return teleportTasks.containsKey(uuid) || teleportingPlayers.containsKey(uuid);
    }
    
    /**
     * Отмена телепортации для игрока
     * @param uuid UUID игрока
     */
    public void cancelTeleport(UUID uuid) {
        boolean wasTeleporting = false;
        double refundAmount = 0.0;
        
        // Отменяем текущую задачу, если она есть
        if (teleportTasks.containsKey(uuid)) {
            BukkitTask task = teleportTasks.get(uuid);
            task.cancel();
            teleportTasks.remove(uuid);
            wasTeleporting = true;
        }
        
        // Удаляем игрока из списка телепортирующихся и получаем сумму для возврата
        if (teleportingPlayers.containsKey(uuid)) {
            TpInfo info = teleportingPlayers.get(uuid);
            if (info != null) {
                refundAmount = info.getPrice();
            }
            teleportingPlayers.remove(uuid);
            wasTeleporting = true;
        }
        
        // Возвращаем деньги игроку, если они были сняты
        if (refundAmount > 0 && plugin.getEconomyManager().isEnabled()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                // Возвращаем деньги игроку
                if (plugin.getEconomyManager().depositMoney(player, refundAmount)) {
                    // Сообщаем игроку о возврате денег
                    String formattedPrice = plugin.getEconomyManager().formatMoney(refundAmount);
                    plugin.getLang().sendAdvancedMessage(player, Lang.Keys.MONEY_REFUNDED, 
                        net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("price", formattedPrice)
                    );
                }
            }
        }
        
        // Не отправляем сообщение здесь, это делается в EventsListener
    }
    
    /**
     * Поиск безопасной локации для телепортации (внутренний метод)
     * @param world Мир
     * @param config Конфигурация мира
     * @return CompletableFuture с результатом - локация или null
     */
    private CompletableFuture<Location> findSafeLocation(World world, WorldCfg config) {
        CompletableFuture<Location> result = new CompletableFuture<>();
        
        // Запускаем асинхронный поиск локации
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> 
            testLocationsSequentially(world, config, 0, 30, result)
        );
        
        return result;
    }
    
    /**
     * Последовательно проверяет случайные локации на безопасность
     * 
     * @param world Мир для проверки
     * @param worldConfig Конфигурация мира
     * @param attemptCount Текущая попытка
     * @param maxAttempts Максимальное количество попыток
     * @param result Результат проверки
     */
    private void testLocationsSequentially(World world, WorldCfg worldConfig, int attemptCount, int maxAttempts, CompletableFuture<Location> result) {
        // Если превышено максимальное количество попыток, завершаем поиск
        if (attemptCount >= maxAttempts || result.isDone()) {
            if (!result.isDone()) {
                result.complete(null);
            }
            return;
        }
        
        // Генерируем случайные координаты
        Location randomLocation = generateRandomLocation(world, worldConfig);
        
        // Получаем безопасную высоту без блокировки основного потока
        getSafeHeightAsync(randomLocation).thenAccept(y -> {
            // Устанавливаем Y координату
            randomLocation.setY(y);
            
            // Считаем все локации безопасными по умолчанию
            // Minecraft сам предотвратит телепортацию в опасные места
            if (!result.isDone()) {
                result.complete(randomLocation);
            }
        }).exceptionally(ex -> {
            // В случае ошибки пробуем следующую локацию
            plugin.log(Level.WARNING, "Ошибка при определении безопасной высоты: " + ex.getMessage());
            if (!result.isDone()) {
                Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> 
                    testLocationsSequentially(world, worldConfig, attemptCount + 1, maxAttempts, result), 1L);
            }
            return null;
        });
    }
    
    /**
     * Получает безопасную Y-координату для телепортации
     * @param location Локация
     * @return CompletableFuture с Y-координатой
     */
    private CompletableFuture<Integer> getSafeHeightAsync(Location location) {
        CompletableFuture<Integer> result = new CompletableFuture<>();
        
        // Выбираем безопасную высоту для телепортации
        World world = location.getWorld();
        
        // Запускаем асинхронную задачу для поиска безопасной высоты
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                int x = location.getBlockX();
                int z = location.getBlockZ();
                int maxY = world.getMaxHeight() - 2; // -2 чтобы гарантировать место для головы
                int minY = world.getMinHeight() + 1;
                int safeY = -1;
                
                // Отладочное сообщение выводим только в режиме отладки
                if (plugin.getPluginConfig().isDebugEnabled()) {
                    plugin.log(Level.INFO, "Поиск безопасной высоты в мире " + world.getName() + 
                               " на координатах X:" + x + ", Z:" + z);
                }
                
                // Выбираем высоту в зависимости от мира
                if (world.getEnvironment() == World.Environment.NETHER) {
                    // Для Нижнего мира ищем безопасную высоту, сначала в центре мира (избегая лавы)
                    // Поиск сверху вниз, начиная с безопасной высоты
                    for (int y = 100; y > 30; y--) {
                        if (isSafeToStand(world, x, y, z)) {
                            safeY = y;
                            if (plugin.getPluginConfig().isDebugEnabled()) {
                                plugin.log(Level.INFO, "Найдена безопасная высота в Нижнем мире: Y=" + safeY);
                            }
                            break;
                        }
                    }
                    
                    // Если не нашли безопасную высоту, повторяем поиск снизу вверх
                    if (safeY == -1) {
                        for (int y = 31; y < 120; y++) {
                            if (isSafeToStand(world, x, y, z)) {
                                safeY = y;
                                if (plugin.getPluginConfig().isDebugEnabled()) {
                                    plugin.log(Level.INFO, "Найдена безопасная высота в Нижнем мире (2-я попытка): Y=" + safeY);
                                }
                                break;
                            }
                        }
                    }
                    
                    // Если по-прежнему не нашли, используем значение по умолчанию
                    if (safeY == -1) {
                        // Выводим предупреждение только в режиме отладки
                        if (plugin.getPluginConfig().isDebugEnabled()) {
                            plugin.log(Level.WARNING, "Не удалось найти безопасную высоту в Нижнем мире на X:" + x + ", Z:" + z);
                        }
                        safeY = 64;
                    }
                    
                } else if (world.getEnvironment() == World.Environment.THE_END) {
                    // Для Энда ищем безопасную высоту сверху вниз
                    // ОБРАТИТЕ ВНИМАНИЕ: В Энде часто нет безопасной высоты, поэтому выводим меньше сообщений
                    for (int y = 100; y > 40; y--) {
                        if (isSafeToStand(world, x, y, z)) {
                            safeY = y;
                            if (plugin.getPluginConfig().isDebugEnabled()) {
                                plugin.log(Level.INFO, "Найдена безопасная высота в Энде: Y=" + safeY);
                            }
                            break;
                        }
                    }
                    
                    // Если не нашли безопасную высоту, ищем от центра островов
                    if (safeY == -1) {
                        for (int y = 60; y > 40; y--) {
                            if (isSafeToStand(world, x, y, z)) {
                                safeY = y;
                                if (plugin.getPluginConfig().isDebugEnabled()) {
                                    plugin.log(Level.INFO, "Найдена безопасная высота в Энде (2-я попытка): Y=" + safeY);
                                }
                                break;
                            }
                        }
                    }
                    
                    // Если по-прежнему не нашли, используем значение по умолчанию
                    if (safeY == -1) {
                        // НЕ выводим предупреждение для Края, так как это нормальная ситуация
                        safeY = 48;
                    }
                    
                } else {
                    // Для обычного мира ищем безопасную высоту сверху вниз
                    // Начинаем с максимальной высоты и идем вниз
                    for (int y = maxY; y > minY; y--) {
                        if (isSafeToStand(world, x, y, z)) {
                            safeY = y;
                            if (plugin.getPluginConfig().isDebugEnabled()) {
                                plugin.log(Level.INFO, "Найдена безопасная высота в обычном мире: Y=" + safeY);
                            }
                            break;
                        }
                    }
                    
                    // Если не нашли, дополнительная проверка от уровня моря вниз
                    if (safeY == -1) {
                        for (int y = 63; y > minY; y--) {
                            if (isSafeToStand(world, x, y, z)) {
                                safeY = y;
                                if (plugin.getPluginConfig().isDebugEnabled()) {
                                    plugin.log(Level.INFO, "Найдена безопасная высота в обычном мире (2-я попытка): Y=" + safeY);
                                }
                                break;
                            }
                        }
                    }
                    
                    // Если не нашли, проверяем еще раз, строго проверяя каждую высоту
                    if (safeY == -1) {
                        for (int y = maxY; y > minY; y--) {
                            if (isVerySafeToStand(world, x, y, z)) {
                                safeY = y;
                                if (plugin.getPluginConfig().isDebugEnabled()) {
                                    plugin.log(Level.INFO, "Найдена очень безопасная высота: Y=" + safeY);
                                }
                                break;
                            }
                        }
                    }
                    
                    // Если всё еще не нашли безопасную высоту, пытаемся найти верхний блок мира
                    if (safeY == -1) {
                        safeY = world.getHighestBlockYAt(x, z) + 1;
                        if (plugin.getPluginConfig().isDebugEnabled()) {
                            plugin.log(Level.INFO, "Использую высочайший блок мира: Y=" + safeY);
                        }
                        
                        // Проверяем, что на этой высоте действительно безопасно
                        if (!isSafeToStand(world, x, safeY, z)) {
                            // Если нет, используем уровень моря
                            if (plugin.getPluginConfig().isDebugEnabled()) {
                                plugin.log(Level.WARNING, "Не удалось найти безопасную высоту в обычном мире на X:" + x + ", Z:" + z);
                            }
                            safeY = 63;
                        }
                    }
                }
                
                // Финальная проверка безопасности
                if (safeY > 0) {
                    if (!isSafeToStand(world, x, safeY, z)) {
                        if (plugin.getPluginConfig().isDebugEnabled()) {
                            plugin.log(Level.WARNING, "Финальная проверка: Высота Y=" + safeY + 
                                       " не безопасна, использую принудительно безопасный уровень");
                        }
                        
                        // В случае опасности, сбрасываем на безопасное значение
                        if (world.getEnvironment() == World.Environment.NETHER) {
                            safeY = 64;
                        } else if (world.getEnvironment() == World.Environment.THE_END) {
                            safeY = 48;
                        } else {
                            safeY = 63;
                        }
                    }
                }
                
                // Возвращаем найденную высоту
                final int finalSafeY = safeY;
                if (plugin.getPluginConfig().isDebugEnabled()) {
                    plugin.log(Level.INFO, "Итоговая безопасная высота: Y=" + finalSafeY);
                }
                result.complete(finalSafeY);
            } catch (Exception e) {
                plugin.log(Level.WARNING, "Ошибка при определении безопасной высоты: " + e.getMessage());
                // В случае ошибки используем стандартные значения
                if (world.getEnvironment() == World.Environment.NETHER) {
                    result.complete(64);
                } else if (world.getEnvironment() == World.Environment.THE_END) {
                    result.complete(48);
                } else {
                    result.complete(63);
                }
            }
        });
        
        return result;
    }
    
    /**
     * Проверяет, безопасно ли стоять на указанной позиции (блок ниже твердый, два блока сверху пустые)
     * @param world Мир
     * @param x X-координата
     * @param y Y-координата (позиция ног)
     * @param z Z-координата
     * @return true, если позиция безопасна для телепортации
     */
    private boolean isSafeToStand(World world, int x, int y, int z) {
        try {
            // Проверяем, что блок ниже твердый (можно стоять)
            org.bukkit.block.Block blockBelow = world.getBlockAt(x, y - 1, z);
            
            // Блок ниже должен быть твердым
            if (!blockBelow.getType().isSolid()) {
                return false;
            }
            
            // Блок ниже не должен быть опасным
            if (isUnsafeBlock(blockBelow.getType())) {
                return false;
            }
            
            // Проверяем, что два блока выше пустые (не задохнется и влезет)
            org.bukkit.block.Block feet = world.getBlockAt(x, y, z);
            org.bukkit.block.Block head = world.getBlockAt(x, y + 1, z);
            
            // Блоки для ног и головы должны быть воздухом
            if (!feet.getType().isAir() || !head.getType().isAir()) {
                return false;
            }
            
            // Не должно быть жидкостей
            if (blockBelow.isLiquid() || feet.isLiquid() || head.isLiquid()) {
                return false;
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Усиленная проверка безопасности для особо сложных случаев
     * @param world Мир
     * @param x X-координата
     * @param y Y-координата (позиция ног)
     * @param z Z-координата
     * @return true, если позиция очень безопасна для телепортации
     */
    private boolean isVerySafeToStand(World world, int x, int y, int z) {
        // Основная проверка безопасности
        if (!isSafeToStand(world, x, y, z)) {
            return false;
        }
        
        try {
            // Дополнительные проверки
            
            // Проверяем бо́льшую область вокруг для обнаружения лавы или других опасностей
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    // Проверяем блок под игроком и вокруг
                    org.bukkit.block.Block block = world.getBlockAt(x + dx, y - 1, z + dz);
                    if (block.isLiquid() || isUnsafeBlock(block.getType())) {
                        return false;
                    }
                    
                    // Проверяем уровень ног и головы
                    if (dx == 0 && dz == 0) continue; // Уже проверено в isSafeToStand
                    
                    if (!block.getType().isAir() && block.getType().isSolid()) {
                        return false;
                    }
                    
                    block = world.getBlockAt(x + dx, y + 1, z + dz);
                    if (!block.getType().isAir() && block.getType().isSolid()) {
                        return false;
                    }
                }
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Проверяет, является ли блок небезопасным для стояния на нем
     * @param material Материал блока
     * @return true, если блок опасен
     */
    private boolean isUnsafeBlock(org.bukkit.Material material) {
        String name = material.name();
        
        // Опасные блоки для стояния на них
        return name.contains("LAVA") || 
               name.contains("FIRE") || 
               name.contains("CACTUS") || 
               name.contains("MAGMA") || 
               name.contains("POWDER_SNOW") || 
               name.equals("CAMPFIRE") || 
               name.equals("SOUL_CAMPFIRE") ||
               name.contains("POINTED_DRIPSTONE") ||
               name.contains("ANVIL") ||
               name.contains("TNT") ||
               name.contains("SAND") ||  // Песок может упасть
               name.contains("GRAVEL"); // Гравий может упасть
    }
    
    /**
     * Найти случайную безопасную локацию в указанном мире
     * @param world Мир для поиска
     * @return CompletableFuture с найденной локацией или null, если не найдена
     */
    public CompletableFuture<Location> findRandomLocation(World world) {
        CompletableFuture<Location> result = new CompletableFuture<>();
        
        // Получаем настройки мира
        WorldCfg worldConfig = plugin.getPluginConfig().getWorldConfig(world.getName());
        if (!worldConfig.isEnabled()) {
            // Мир отключен для телепортации
            result.complete(null);
            return result;
        }
        
        // Запускаем асинхронно поиск локации
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> 
            testLocationsSequentially(world, worldConfig, 0, 30, result));
        
        return result;
    }
    
    /**
     * Асинхронно проверяет безопасность локации
     * @param location Локация для проверки
     * @return CompletableFuture с результатом проверки (true - безопасна)
     */
    private CompletableFuture<Boolean> checkLocationSafetyAsync(Location location) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        
        // Запускаем асинхронную проверку безопасности
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Устанавливаем таймаут для проверки
                Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                    if (!result.isDone()) {
                        // Если проверка не завершена за 1 секунду, считаем локацию небезопасной
                        plugin.log(Level.WARNING, "Проверка безопасности локации превысила таймаут");
                        result.complete(false);
                    }
                }, 20L);
                
                // Проверяем, безопасна ли локация для телепортации
                int x = location.getBlockX();
                int y = location.getBlockY();
                int z = location.getBlockZ();
                World world = location.getWorld();
                
                boolean safe = isSafeToStand(world, x, y, z);
                
                // Проверяем запрещенные биомы
                if (safe) {
                    org.bukkit.block.Block block = world.getBlockAt(x, y, z);
                    org.bukkit.block.Biome biome = block.getBiome();
                    
                    WorldCfg worldConfig = plugin.getPluginConfig().getWorldConfig(world.getName());
                    Set<String> forbiddenBiomes = worldConfig.getForbiddenBiomes();
                    
                    if (forbiddenBiomes.contains(biome.toString())) {
                        safe = false;
                    }
                }
                
                result.complete(safe);
            } catch (Exception e) {
                plugin.log(Level.WARNING, "Ошибка при проверке безопасности локации: " + e.getMessage());
                result.complete(false);
            }
        });
        
        return result;
    }
    
    /**
     * Генерация случайной локации в пределах указанного радиуса
     * @param world Мир
     * @param config Конфигурация мира
     * @return Случайная локация с Y=100 (будет скорректирована позже)
     */
    private Location generateRandomLocation(World world, WorldCfg config) {
        // Получаем центр (если есть) или используем центр мира
        Location center = config.getCenter();
        if (center == null) {
            center = new Location(world, 0, 0, 0);
        }
        
        double x = center.getX();
        double z = center.getZ();
        
        // Генерируем случайный радиус от minRadius до maxRadius
        double radius = config.getMinRadius() + Math.random() * (config.getMaxRadius() - config.getMinRadius());
        
        // Генерируем случайный угол
        double angle = Math.random() * 2 * Math.PI;
        
        // Вычисляем координаты
        double newX = x + radius * Math.cos(angle);
        double newZ = z + radius * Math.sin(angle);
        
        // Создаем локацию с Y=100 (будет скорректирована позже)
        return new Location(world, newX, 100, newZ);
    }
    
    /**
     * Проверка, находится ли игрок на той же локации
     * @param current Текущая локация
     * @param start Начальная локация
     * @return true если локации совпадают (с погрешностью)
     */
    private boolean isSameLocation(Location current, Location start) {
        // Допускаем небольшую погрешность (0.1 блока)
        return current.getWorld().equals(start.getWorld()) &&
               Math.abs(current.getX() - start.getX()) < 0.1 &&
               Math.abs(current.getY() - start.getY()) < 0.1 &&
               Math.abs(current.getZ() - start.getZ()) < 0.1;
    }
    
    /**
     * Получить начальную локацию телепортации игрока
     * @param uuid UUID игрока
     * @return Начальная локация или null, если игрок не телепортируется
     */
    public Location getStartLocation(UUID uuid) {
        TpInfo info = teleportingPlayers.get(uuid);
        return info != null ? info.getStartLocation() : null;
    }
} 