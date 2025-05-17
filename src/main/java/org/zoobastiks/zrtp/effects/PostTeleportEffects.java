package org.zoobastiks.zrtp.effects;

import io.papermc.paper.advancement.AdvancementDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.zoobastiks.zrtp.Zrtp;
import org.zoobastiks.zrtp.common.PostTeleportEffectsMessages;
import org.zoobastiks.zrtp.config.PostTeleportEffectsCfg;
import org.zoobastiks.zrtp.config.TeleportEffectsCfg;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

/**
 * Класс для обработки эффектов после телепортации
 */
public class PostTeleportEffects {
    private final Zrtp plugin;
    private final Map<UUID, BukkitTask> rewardTasks = new HashMap<>();
    
    /**
     * Конструктор
     * @param plugin Экземпляр плагина
     */
    public PostTeleportEffects(Zrtp plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Применить эффекты после телепортации
     * @param player Игрок
     * @param location Локация телепортации
     */
    public void applyPostTeleportEffects(Player player, Location location) {
        UUID uuid = player.getUniqueId();
        PostTeleportEffectsCfg config = plugin.getPluginConfig().getPostTeleportEffectsConfig();
        PostTeleportEffectsMessages messages = plugin.getPluginConfig().getPostTeleportEffectsMessages();
        TeleportEffectsCfg effectsCfg = plugin.getPluginConfig().getTeleportEffectsConfig();
        boolean useActionBar = effectsCfg.useActionBar();
        
        // Отменить существующие задачи, если есть
        cancelEffects(uuid);
        
        // Применить эффекты зелий
        for (PotionEffect effect : config.getPotionEffects()) {
            player.addPotionEffect(effect);
        }
        
        // Уменьшить голод
        if (config.isHungerEnabled() && config.getHungerAmount() > 0) {
            int currentFoodLevel = player.getFoodLevel();
            int newFoodLevel = Math.max(1, currentFoodLevel - config.getHungerAmount());
            player.setFoodLevel(newFoodLevel);
            
            // Отправляем сообщение через Translator
            Component hungerMessage = plugin.getTranslator().parseMessage(messages.getEffectsHunger());
            sendEffectMessage(player, hungerMessage, useActionBar);
        }
        
        // Нанести урон
        if (config.isDamageEnabled() && config.getDamageAmount() > 0) {
            double currentHealth = player.getHealth();
            double newHealth = Math.max(0.5, currentHealth - config.getDamageAmount());
            player.setHealth(newHealth);
            
            // Отправляем сообщение через Translator
            Component damageMessage = plugin.getTranslator().parseMessage(messages.getEffectsDamage());
            sendEffectMessage(player, damageMessage, useActionBar);
        }
        
        // Создать эффект молнии
        if (config.isLightningEnabled()) {
            if (config.isLightningDamage()) {
                // Реальная молния с уроном
                player.getWorld().strikeLightning(player.getLocation());
            } else {
                // Визуальный эффект без урона
                player.getWorld().strikeLightningEffect(player.getLocation());
            }
            
            // Отправляем сообщение через Translator
            Component lightningMessage = plugin.getTranslator().parseMessage(messages.getEffectsLightning());
            sendEffectMessage(player, lightningMessage, useActionBar);
        }
        
        // Выполнить команды
        if (config.isCommandsEnabled()) {
            // Команды от имени игрока
            for (String command : config.getPlayerCommands()) {
                try {
                    String parsedCommand = command.replace("{player}", player.getName());
                    player.performCommand(parsedCommand);
                } catch (Exception e) {
                    plugin.log(Level.WARNING, "Ошибка при выполнении команды от имени игрока: " + command, e);
                }
            }
            
            // Команды от имени консоли
            for (String command : config.getConsoleCommands()) {
                try {
                    String parsedCommand = command.replace("{player}", player.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsedCommand);
                } catch (Exception e) {
                    plugin.log(Level.WARNING, "Ошибка при выполнении команды от имени консоли: " + command, e);
                }
            }
            
            // Отправляем сообщение через Translator
            Component commandMessage = plugin.getTranslator().parseMessage(messages.getEffectsCommand());
            sendEffectMessage(player, commandMessage, useActionBar);
        }
        
        // Спавн мобов
        if (config.isSpawnMobsEnabled()) {
            boolean mobsSpawned = false;
            
            for (PostTeleportEffectsCfg.MobSpawnInfo mobInfo : config.getMobSpawnInfos()) {
                // Проверяем шанс спавна
                double random = ThreadLocalRandom.current().nextDouble(100);
                if (random <= mobInfo.getChance()) {
                    try {
                        // Спавним мобов
                        EntityType entityType = EntityType.valueOf(mobInfo.getEntityType());
                        
                        for (int i = 0; i < mobInfo.getAmount(); i++) {
                            // Генерируем случайную позицию в радиусе
                            Location spawnLoc = getRandomLocationAround(player.getLocation(), mobInfo.getRadius());
                            
                            // Спавним моба
                            LivingEntity entity = (LivingEntity) player.getWorld().spawnEntity(spawnLoc, entityType);
                            entity.setCustomName(player.getName() + "'s " + entityType.name());
                            entity.setCustomNameVisible(true);
                        }
                        
                        mobsSpawned = true;
                        
                        // Определяем тип сообщения в зависимости от типа моба
                        boolean isHostile = isHostileMob(entityType);
                        
                        // Отправляем соответствующее сообщение через Translator
                        Component mobMessage;
                        if (isHostile) {
                            mobMessage = plugin.getTranslator().parseMessage(messages.getMobsSpawned());
                        } else {
                            mobMessage = plugin.getTranslator().parseMessage(messages.getMobsFriendly());
                        }
                        sendEffectMessage(player, mobMessage, useActionBar);
                    } catch (Exception e) {
                        plugin.log(Level.WARNING, "Ошибка при спавне моба: " + mobInfo.getEntityType(), e);
                    }
                }
            }
            
            // Если мобы были заспавнены, проигрываем звук
            if (mobsSpawned) {
                player.playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.5f, 1.0f);
            }
        }
        
        // Показать информацию о локации
        if (config.isShowLocationInfo()) {
            sendLocationMessage(player, location);
        }
        
        // Запустить систему наград, если включена
        if (config.isRewardsEnabled()) {
            startRewardSequence(player);
        }
    }
    
    /**
     * Получить случайную локацию вокруг указанной
     * @param center Центральная точка
     * @param radius Радиус
     * @return Случайная локация
     */
    private Location getRandomLocationAround(Location center, int radius) {
        double angle = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI;
        double distance = ThreadLocalRandom.current().nextDouble() * radius;
        
        double x = center.getX() + distance * Math.cos(angle);
        double z = center.getZ() + distance * Math.sin(angle);
        
        return new Location(center.getWorld(), x, center.getY(), z);
    }
    
    /**
     * Отправка сообщения о локации
     * @param player Игрок
     * @param location Локация
     */
    private void sendLocationMessage(Player player, Location location) {
        // Получаем конфигурацию эффектов
        PostTeleportEffectsCfg config = plugin.getPluginConfig().getPostTeleportEffectsConfig();
        
        // Если информация о локации отключена, прерываем выполнение
        if (!config.isShowLocationInfo()) {
            return;
        }
        
        PostTeleportEffectsMessages messages = plugin.getPluginConfig().getPostTeleportEffectsMessages();
        TeleportEffectsCfg effectsCfg = plugin.getPluginConfig().getTeleportEffectsConfig();
        boolean useActionBar = effectsCfg.useActionBar();
        
        // Координаты и информация о мире
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        String worldName = location.getWorld().getName();
        String biomeName = location.getBlock().getBiome().toString();
        
        // Создаем TagResolver для плейсхолдеров
        TagResolver placeholders = TagResolver.builder()
            .resolver(Placeholder.parsed("x", String.valueOf(x)))
            .resolver(Placeholder.parsed("y", String.valueOf(y)))
            .resolver(Placeholder.parsed("z", String.valueOf(z)))
            .resolver(Placeholder.parsed("world", worldName))
            .resolver(Placeholder.parsed("biome", biomeName))
            .build();
        
        // Создаем компоненты для Title и Subtitle
        Component title = plugin.getTranslator().parseMessage(messages.getLocationTitle(), placeholders);
        Component subtitle = plugin.getTranslator().parseMessage(messages.getLocationSubtitle(), placeholders);
        
        // Отображаем title
        player.showTitle(Title.title(
            title,
            subtitle,
            Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(500))
        ));
        
        // Создаем компоненты для сообщений о координатах, мире и биоме
        Component coordsMessage = plugin.getTranslator().parseMessage(messages.getLocationCoords(), placeholders);
        Component worldMessage = plugin.getTranslator().parseMessage(messages.getLocationWorld(), placeholders);
        Component biomeMessage = plugin.getTranslator().parseMessage(messages.getLocationBiome(), placeholders);
        
        // Задержка перед отправкой сообщений о локации (чтобы не перекрывать title)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (useActionBar) {
                // Если включен ActionBar, отправляем сообщения последовательно с интервалом
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.sendActionBar(coordsMessage);
                }, 0L);
                
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.sendActionBar(worldMessage);
                }, 40L);
                
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.sendActionBar(biomeMessage);
                }, 80L);
            } else {
                // Иначе отправляем в чат
                player.sendMessage(coordsMessage);
                player.sendMessage(worldMessage);
                player.sendMessage(biomeMessage);
            }
        }, 20L);
        
        // Проигрываем звук
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
    }
    
    /**
     * Запустить последовательность сообщений о поиске наград
     * @param player Игрок
     */
    private void startRewardSequence(Player player) {
        UUID uuid = player.getUniqueId();
        PostTeleportEffectsMessages messages = plugin.getPluginConfig().getPostTeleportEffectsMessages();
        
        // Отменить существующие задачи, если есть
        if (rewardTasks.containsKey(uuid)) {
            rewardTasks.get(uuid).cancel();
        }
        
        // Запускаем задачу отправки сообщений
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Отправляем первое сообщение
            sendRewardMessage(player, 0);
            
            // Планируем отправку остальных сообщений
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                sendRewardMessage(player, 1);
                
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    sendRewardMessage(player, 2);
                    
                    // Проверяем, нашел ли игрок награду
                    checkAndGiveReward(player);
                    
                    // Удаляем задачу из списка
                    rewardTasks.remove(uuid);
                }, 30L); // 1.5 секунды
            }, 30L); // 1.5 секунды
        }, 10L); // 0.5 секунды
        
        // Сохраняем задачу
        rewardTasks.put(uuid, task);
    }
    
    /**
     * Отправка сообщения о награде
     * @param player Игрок
     * @param index Индекс сообщения
     */
    private void sendRewardMessage(Player player, int index) {
        PostTeleportEffectsMessages messages = plugin.getPluginConfig().getPostTeleportEffectsMessages();
        TeleportEffectsCfg effectsCfg = plugin.getPluginConfig().getTeleportEffectsConfig();
        boolean useActionBar = effectsCfg.useActionBar();
        
        // Проверяем, есть ли сообщение с таким индексом
        String message = messages.getRewardSearchingMessage(index);
        if (message != null) {
            // Отправляем сообщение через Translator
            Component component = plugin.getTranslator().parseMessage(message);
            
            if (useActionBar) {
                player.sendActionBar(component);
            } else {
                player.sendMessage(component);
            }
            
            // Проигрываем звук
            Sound sound = (index == 0) ? Sound.ENTITY_EXPERIENCE_ORB_PICKUP :
                         (index == 1) ? Sound.BLOCK_NOTE_BLOCK_HARP :
                                       Sound.BLOCK_NOTE_BLOCK_BELL;
            if (sound != null) {
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            }
        }
    }
    
    /**
     * Проверить и выдать награду игроку
     * @param player Игрок
     */
    private void checkAndGiveReward(Player player) {
        PostTeleportEffectsCfg config = plugin.getPluginConfig().getPostTeleportEffectsConfig();
        PostTeleportEffectsMessages messages = plugin.getPluginConfig().getPostTeleportEffectsMessages();
        TeleportEffectsCfg effectsCfg = plugin.getPluginConfig().getTeleportEffectsConfig();
        boolean useActionBar = effectsCfg.useActionBar();
        
        // Получаем случайный предмет из конфигурации
        ItemStack reward = config.getRandomRewardItem();
        
        // Если награда была выбрана
        if (reward != null) {
            // Создаем компоненты сообщений
            Component foundMessage = plugin.getTranslator().parseMessage(messages.getRewardFound());
            Component contentsMessage = plugin.getTranslator().parseMessage(messages.getRewardContents());
            
            // Создаем плейсхолдеры для предмета и шанса
            String itemName = reward.getType().name().toLowerCase().replace("_", " ");
            String itemCount = String.valueOf(reward.getAmount());
            
            // Парсим сообщения с плейсхолдерами
            Component itemComponent = plugin.getTranslator().parseMessage(
                messages.getRewardItem(),
                TagResolver.builder()
                    .resolver(Placeholder.parsed("item", itemName))
                    .resolver(Placeholder.parsed("count", itemCount))
                    .build()
            );
            
            Component chanceComponent = plugin.getTranslator().parseMessage(
                messages.getRewardChance(),
                TagResolver.builder()
                    .resolver(Placeholder.parsed("chance", String.valueOf(config.getRewardChance())))
                    .build()
            );
            
            // Отправляем сообщения
            if (useActionBar) {
                // Последовательная отправка в ActionBar с интервалами
                player.sendActionBar(foundMessage);
                
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.sendActionBar(contentsMessage);
                }, 30L);
                
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.sendActionBar(itemComponent);
                }, 60L);
                
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.sendActionBar(chanceComponent);
                }, 90L);
            } else {
                // Обычная отправка в чат
                player.sendMessage(foundMessage);
                player.sendMessage(contentsMessage);
                player.sendMessage(itemComponent);
                player.sendMessage(chanceComponent);
            }
            
            // Выдаем награду игроку
            player.getInventory().addItem(reward);
            
            // Проигрываем звук успеха
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            
            // Добавляем эффект частиц
            player.spawnParticle(org.bukkit.Particle.HEART, 
                player.getLocation().add(0, 1.5, 0), 30, 0.5, 0.5, 0.5, 0.1);
        }
    }
    
    /**
     * Отменить все эффекты для игрока
     * @param uuid UUID игрока
     */
    public void cancelEffects(UUID uuid) {
        if (rewardTasks.containsKey(uuid)) {
            rewardTasks.get(uuid).cancel();
            rewardTasks.remove(uuid);
        }
    }
    
    /**
     * Проверяет, является ли моб враждебным
     * @param entityType Тип моба
     * @return true если моб враждебный
     */
    private boolean isHostileMob(EntityType entityType) {
        switch (entityType) {
            case ZOMBIE:
            case SKELETON:
            case CREEPER:
            case SPIDER:
            case CAVE_SPIDER:
            case ENDERMAN:
            case WITCH:
            case BLAZE:
            case GHAST:
            case MAGMA_CUBE:
            case SILVERFISH:
            case SLIME:
            case GUARDIAN:
            case ELDER_GUARDIAN:
            case EVOKER:
            case VEX:
            case VINDICATOR:
            case PHANTOM:
            case PILLAGER:
            case RAVAGER:
            case HOGLIN:
            case PIGLIN:
            case ZOGLIN:
            case PIGLIN_BRUTE:
            case WARDEN:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Отправляет сообщение об эффекте игроку
     * @param player Игрок
     * @param message Сообщение
     * @param useActionBar Использовать ли ActionBar
     */
    private void sendEffectMessage(Player player, Component message, boolean useActionBar) {
        if (useActionBar) {
            player.sendActionBar(message);
        } else {
            player.sendMessage(message);
        }
    }
} 