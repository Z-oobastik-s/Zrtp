package org.zoobastiks.zrtp.commands;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.zoobastiks.zrtp.Zrtp;
import org.zoobastiks.zrtp.common.Lang;
import org.zoobastiks.zrtp.config.WorldCfg;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Обработчик команд плагина
 */
public class RtpCmd implements CommandExecutor, TabCompleter {
    private final Zrtp plugin;
    
    /**
     * Конструктор обработчика команд
     * @param plugin Экземпляр плагина
     */
    public RtpCmd(Zrtp plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            // Простая телепортация игрока
            if (!(sender instanceof Player)) {
                sender.sendMessage("Из консоли необходимо указать имя игрока: /rtp <игрок> [мир]");
                return true;
            }
            
            handleTeleport((Player) sender);
            return true;
        }
        
        // Обработка команд с параметрами
        switch (args[0].toLowerCase()) {
            case "reload":
                // Перезагрузка плагина
                if (sender instanceof Player && !sender.hasPermission("rtp.reload")) {
                    plugin.getLang().sendAdvancedMessage((Player) sender, Lang.Keys.NO_PERMISSION);
                    return true;
                }
                
                plugin.reload();
                
                if (sender instanceof Player) {
                    plugin.getLang().sendAdvancedMessage((Player) sender, Lang.Keys.RELOAD_SUCCESS);
                } else {
                    sender.sendMessage("Плагин успешно перезагружен");
                }
                break;
                
            case "help":
                // Показать помощь по плагину
                if (sender instanceof Player) {
                    plugin.getLang().sendAdvancedMessage((Player) sender, Lang.Keys.HELP_MESSAGE);
                } else {
                    sender.sendMessage("Команды: /rtp <игрок> [мир] - Телепортация игрока, /rtp reload - Перезагрузка плагина");
                }
                break;
                
            case "player":
                // Телепортация другого игрока (устаревший формат)
                if (args.length < 2) {
                    sender.sendMessage("Использование: /rtp player <имя_игрока> [мир]");
                    return true;
                }
                
                if (sender instanceof Player && !sender.hasPermission("rtp.other")) {
                    plugin.getLang().sendAdvancedMessage((Player) sender, Lang.Keys.NO_PERMISSION);
                    return true;
                }
                
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null || !target.isOnline()) {
                    sender.sendMessage("Игрок не найден или не в сети");
                    return true;
                }
                
                // Проверка защиты от телепортации
                if (target.hasPermission("rtp.notme") && (!(sender.isOp() || !(sender instanceof Player)))) {
                    sender.sendMessage("Этот игрок не может быть телепортирован");
                    return true;
                }
                
                // Телепортация в указанный мир, если указан
                if (args.length > 2) {
                    teleportPlayerToWorld(target, args[2], sender);
                } else {
                    handleTeleport(target);
                }
                
                // Сообщаем о телепортации
                if (sender instanceof Player) {
                    Player playerSender = (Player) sender;
                    String message = plugin.getLang().gradient("Вы телепортировали игрока " + target.getName(), "#00FFCC", "#33CCFF");
                    playerSender.sendMessage(plugin.getLang().parseComponent(plugin.getLang().getMessage(Lang.Keys.PREFIX) + message));
                } else {
                    sender.sendMessage("Игрок " + target.getName() + " телепортирован");
                }
                break;
                
            default:
                // Проверка, является ли первый аргумент именем игрока
                Player targetPlayer = Bukkit.getPlayer(args[0]);
                if (targetPlayer != null && targetPlayer.isOnline()) {
                    // Проверка прав на телепортацию других игроков
                    if (sender instanceof Player && !sender.hasPermission("rtp.other")) {
                        plugin.getLang().sendAdvancedMessage((Player) sender, Lang.Keys.NO_PERMISSION);
                        return true;
                    }
                    
                    // Проверка защиты от телепортации
                    if (targetPlayer.hasPermission("rtp.notme") && (!(sender.isOp() || !(sender instanceof Player)))) {
                        sender.sendMessage("Этот игрок не может быть телепортирован");
                        return true;
                    }
                    
                    // Если указан мир, телепортируем в него
                    if (args.length > 1) {
                        teleportPlayerToWorld(targetPlayer, args[1], sender);
                    } else {
                        handleTeleport(targetPlayer);
                    }
                    
                    // Сообщаем о телепортации
                    if (sender instanceof Player) {
                        Player playerSender = (Player) sender;
                        
                        plugin.getLang().sendAdvancedMessage(playerSender, Lang.Keys.PLAYER_TELEPORTED, 
                            Placeholder.parsed("player", targetPlayer.getName()),
                            Placeholder.parsed("world", targetPlayer.getWorld().getName())
                        );
                    } else {
                        sender.sendMessage("Игрок " + targetPlayer.getName() + " телепортирован");
                    }
                } else {
                    // Неизвестная команда или игрок не найден
                    if (sender instanceof Player) {
                        plugin.getLang().sendAdvancedMessage((Player) sender, Lang.Keys.HELP_MESSAGE);
                    } else {
                        sender.sendMessage("Неизвестная команда или игрок не найден. Используйте /rtp <игрок> [мир]");
                    }
                }
                break;
        }
        
        return true;
    }
    
    /**
     * Телепортация игрока в указанный мир
     * @param player Игрок для телепортации
     * @param worldName Имя мира
     * @param sender Отправитель команды для сообщений
     */
    private void teleportPlayerToWorld(Player player, String worldName, CommandSender sender) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            sender.sendMessage("Мир '" + worldName + "' не найден");
            return;
        }
        
        // Проверяем, настроен ли мир в конфигурации
        if (!plugin.getPluginConfig().isWorldConfigured(worldName)) {
            // Сообщаем о невозможности телепортации в ненастроенный мир
            if (sender instanceof Player) {
                plugin.getLang().sendAdvancedMessage((Player) sender, Lang.Keys.WORLD_NOT_CONFIGURED);
            } else {
                sender.sendMessage("Телепортация в мире '" + worldName + "' не настроена. Добавьте его в config.yml.");
            }
            return;
        }
        
        WorldCfg worldConfig = plugin.getPluginConfig().getWorldConfig(worldName);
        if (!worldConfig.isEnabled()) {
            sender.sendMessage("Телепортация в мир '" + worldName + "' отключена");
            return;
        }
        
        // Отправляем сообщение о подготовке телепортации
        if (sender instanceof Player) {
            Player senderPlayer = (Player) sender;
            String message = plugin.getLang().gradient("Поиск места для телепортации в мире " + world.getName() + "...", "#00BFFF", "#87CEFA");
            senderPlayer.sendMessage(plugin.getLang().parseComponent(plugin.getLang().getMessage(Lang.Keys.PREFIX) + message));
        } else {
            sender.sendMessage("Поиск места для телепортации в мире " + world.getName() + "...");
        }
        
        // Поиск безопасной локации в указанном мире
        plugin.getTaskManager().findRandomLocation(world).thenAccept(location -> {
            if (location != null) {
                // Выполняем телепортацию в основном потоке
                Bukkit.getScheduler().runTask(plugin, () -> {
                    // Телепортируем игрока
                    player.teleport(location);
                    plugin.getCooldowns().put(player.getUniqueId(), System.currentTimeMillis());
                    
                    // Отправляем сообщения
                    plugin.getLang().sendAdvancedMessage(player, Lang.Keys.TELEPORT_SUCCESS);
                    
                    if (sender != player && sender instanceof Player) {
                        Player senderPlayer = (Player) sender;
                        
                        plugin.getLang().sendAdvancedMessage(senderPlayer, Lang.Keys.PLAYER_TELEPORTED, 
                            Placeholder.parsed("player", player.getName()),
                            Placeholder.parsed("world", player.getWorld().getName())
                        );
                    } else if (sender != player) {
                        sender.sendMessage("Игрок " + player.getName() + " телепортирован");
                    }
                });
            } else {
                // Не удалось найти безопасное место
                plugin.getLang().sendAdvancedMessage(player, Lang.Keys.UNSAFE_LOCATION);
            }
        }).exceptionally(ex -> {
            plugin.log(Level.WARNING, "Ошибка при телепортации игрока " + player.getName() + " в мир " + worldName, ex);
            if (sender instanceof Player) {
                Player senderPlayer = (Player) sender;
                senderPlayer.sendMessage(plugin.getLang().parseComponent(plugin.getLang().getMessage(Lang.Keys.PREFIX) + 
                                         plugin.getLang().gradient("Произошла ошибка при телепортации", "#FF5555", "#FF0000")));
            } else {
                sender.sendMessage("Произошла ошибка при телепортации игрока " + player.getName());
            }
            return null;
        });
    }
    
    /**
     * Обработка команды телепортации
     * @param player Игрок для телепортации
     */
    private void handleTeleport(Player player) {
        // Проверка прав на использование
        if (!player.hasPermission("rtp.use")) {
            plugin.getLang().sendAdvancedMessage(player, Lang.Keys.NO_PERMISSION);
            return;
        }
        
        World world = player.getWorld();
        String worldName = world.getName();
        
        // Проверяем, настроен ли мир в конфигурации
        if (!plugin.getPluginConfig().isWorldConfigured(worldName)) {
            // Сообщаем игроку, что в этом мире телепортация не настроена
            plugin.getLang().sendAdvancedMessage(player, Lang.Keys.WORLD_NOT_CONFIGURED);
            return;
        }
        
        // Получаем настройки для мира игрока
        WorldCfg worldConfig = plugin.getPluginConfig().getWorldConfig(worldName);
        
        // Проверяем, что телепортация включена в данном мире
        if (!worldConfig.isEnabled()) {
            plugin.getLang().sendAdvancedMessage(player, Lang.Keys.WORLD_DISABLED);
            return;
        }
        
        // Проверка на кулдаун, если игрок не имеет права его игнорировать
        if (!player.hasPermission("rtp.nocooldown") && !player.hasPermission("rtp.bypass")) {
            long lastUsage = plugin.getCooldowns().getOrDefault(player.getUniqueId(), 0L);
            long currentTime = System.currentTimeMillis();
            long cooldownTime = worldConfig.getCooldown() * 1000L;
            
            if (lastUsage + cooldownTime > currentTime) {
                long remainingTime = (lastUsage + cooldownTime - currentTime) / 1000;
                
                plugin.getLang().sendAdvancedMessage(player, Lang.Keys.COOLDOWN, 
                    Placeholder.parsed("time", String.valueOf(remainingTime))
                );
                return;
            }
        }
        
        // Проверка экономики, если цена больше 0
        if (worldConfig.getPrice() > 0 && plugin.getEconomyManager().isEnabled()) {
            if (!plugin.getEconomyManager().hasMoney(player, worldConfig.getPrice())) {
                plugin.getLang().sendAdvancedMessage(player, Lang.Keys.NOT_ENOUGH_MONEY,
                    Placeholder.parsed("price", String.format("%.2f", worldConfig.getPrice()))
                );
                return;
            }
        }
        
        // Проверка, не телепортируется ли игрок уже
        if (plugin.getTaskManager().isPlayerTeleporting(player.getUniqueId())) {
            plugin.getLang().sendAdvancedMessage(player, Lang.Keys.ALREADY_TELEPORTING);
            return;
        }
        
        // Инициируем телепортацию
        plugin.getTaskManager().startTeleport(player);
    }
    
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        
        // Базовые команды и первый аргумент
        if (args.length == 1) {
            if (sender.hasPermission("rtp.reload")) {
                completions.add("reload");
            }
            completions.add("help");
            
            // Добавляем имена игроков
            if (sender.hasPermission("rtp.other")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                        completions.add(player.getName());
                    }
                }
            }
        }
        
        // Список миров для указанного игрока
        if (args.length == 2) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target != null && sender.hasPermission("rtp.other")) {
                for (World world : Bukkit.getWorlds()) {
                    WorldCfg worldConfig = plugin.getPluginConfig().getWorldConfig(world.getName());
                    if (worldConfig.isEnabled() && world.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(world.getName());
                    }
                }
            }
        }
        
        return completions;
    }
} 