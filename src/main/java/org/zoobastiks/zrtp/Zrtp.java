package org.zoobastiks.zrtp;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.zoobastiks.zrtp.api.RtpApi;
import org.zoobastiks.zrtp.commands.RtpCmd;
import org.zoobastiks.zrtp.common.Lang;
import org.zoobastiks.zrtp.config.Cfg;
import org.zoobastiks.zrtp.economy.EconomyManager;
import org.zoobastiks.zrtp.effects.PostTeleportEffects;
import org.zoobastiks.zrtp.effects.TeleportEffects;
import org.zoobastiks.zrtp.events.EventsListener;
import org.zoobastiks.zrtp.tasks.TaskMgr;
import org.zoobastiks.zrtp.translator.Translator;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Основной класс плагина случайной телепортации
 * @author Zoobastiks
 */
public final class Zrtp extends JavaPlugin {
    private static Zrtp instance;
    private Cfg config;
    private Lang lang;
    private TaskMgr taskManager;
    private TeleportEffects teleportEffects;
    private PostTeleportEffects postTeleportEffects;
    private Translator translator;
    private EconomyManager economyManager;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    /**
     * @return Экземпляр плагина
     */
    public static Zrtp getInstance() {
        return instance;
    }

    /**
     * Получение конфигурации плагина
     * @return Конфигурация
     */
    public Cfg getPluginConfig() {
        return config;
    }

    /**
     * Получение менеджера языка
     * @return Языковой менеджер
     */
    public Lang getLang() {
        return lang;
    }

    /**
     * Получение менеджера задач
     * @return Менеджер задач
     */
    public TaskMgr getTaskManager() {
        return taskManager;
    }
    
    /**
     * Получение менеджера эффектов телепортации
     * @return Менеджер эффектов
     */
    public TeleportEffects getTeleportEffects() {
        return teleportEffects;
    }
    
    /**
     * Получение менеджера эффектов после телепортации
     * @return Менеджер эффектов после телепортации
     */
    public PostTeleportEffects getPostTeleportEffects() {
        return postTeleportEffects;
    }

    /**
     * Получение переводчика сообщений
     * @return Экземпляр переводчика
     */
    public Translator getTranslator() {
        return translator;
    }

    /**
     * Получение карты кулдаунов игроков
     * @return Карта UUID игрока -> время последнего использования
     */
    public Map<UUID, Long> getCooldowns() {
        return cooldowns;
    }

    /**
     * Получение менеджера экономики
     * @return Менеджер экономики
     */
    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    @Override
    public void onEnable() {
        instance = this;
        
        // Инициализация Translator перед Lang
        this.translator = new Translator();
        
        // Загрузка конфигурации
        this.config = new Cfg(this);
        this.lang = new Lang(this);
        
        // Обязательная перезагрузка lang для инициализации языковых файлов
        this.lang.reload();
        
        // Завершение загрузки конфигурации (после инициализации Lang)
        this.config.finishLoading();
        
        // Инициализация остальных компонентов
        this.taskManager = new TaskMgr(this);
        this.teleportEffects = new TeleportEffects(this);
        this.postTeleportEffects = new PostTeleportEffects(this);
        
        // Инициализация экономики
        this.economyManager = new EconomyManager(this);

        // Инициализация API
        RtpApi.init(this);
        
        // Регистрация команд
        RtpCmd mainCommand = new RtpCmd(this);
        PluginCommand rtpCommand = getCommand("rtp");
        if (rtpCommand != null) {
            rtpCommand.setExecutor(mainCommand);
            rtpCommand.setTabCompleter(mainCommand);
        }
        
        PluginCommand wildCommand = getCommand("wild");
        if (wildCommand != null) {
            wildCommand.setExecutor(mainCommand);
            wildCommand.setTabCompleter(mainCommand);
        }
        
        // Регистрация обработчиков событий
        Bukkit.getPluginManager().registerEvents(new EventsListener(this), this);
        
        // Запуск асинхронных задач
        taskManager.startTasks();
        
        // Вывод информации о запуске плагина
        getLogger().info("====================================");
        getLogger().info("  Плагин Zrtp v" + getDescription().getVersion() + " успешно запущен!");
        getLogger().info("  Автор: " + String.join(", ", getDescription().getAuthors()));
        if (config.isDebugEnabled()) {
            getLogger().info("  Режим отладки: ВКЛЮЧЕН");
        }
        getLogger().info("====================================");
    }

    @Override
    public void onDisable() {
        // Остановка всех задач
        if (taskManager != null) {
            taskManager.stopTasks();
        }
        
        // Отмена всех телепортаций и эффектов
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            if (taskManager != null && taskManager.isPlayerTeleporting(uuid)) {
                taskManager.cancelTeleport(uuid);
            }
            if (teleportEffects != null) {
                teleportEffects.cancelEffects(uuid);
            }
            if (postTeleportEffects != null) {
                postTeleportEffects.cancelEffects(uuid);
            }
        }
        
        getLogger().info("Плагин успешно выключен!");
    }
    
    /**
     * Логирование сообщений в консоль
     * @param level Уровень логирования
     * @param message Сообщение
     */
    public void log(Level level, String message) {
        // Если уровень не WARNING или SEVERE, и debug выключен, не выводим сообщение
        if (level.intValue() < Level.WARNING.intValue() && config != null && !config.isDebugEnabled()) {
            return;
        }
        getLogger().log(level, message);
    }
    
    /**
     * Логирование сообщений с исключением
     * @param level Уровень логирования
     * @param message Сообщение
     * @param throwable Исключение
     */
    public void log(Level level, String message, Throwable throwable) {
        // Если уровень не WARNING или SEVERE, и debug выключен, не выводим сообщение
        if (level.intValue() < Level.WARNING.intValue() && config != null && !config.isDebugEnabled()) {
            return;
        }
        getLogger().log(level, message, throwable);
    }
    
    /**
     * Перезагрузка плагина
     */
    public void reload() {
        // Отмена всех задач
        taskManager.stopTasks();
        
        // Перезагрузка конфигурации
        config.reload();
        lang.reload();
        
        // Инициализация экономики заново
        this.economyManager = new EconomyManager(this);
        
        // Перезапуск задач
        taskManager.startTasks();
        
        getLogger().info("Плагин успешно перезагружен!");
    }
} 