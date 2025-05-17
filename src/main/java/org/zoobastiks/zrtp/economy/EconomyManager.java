package org.zoobastiks.zrtp.economy;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.zoobastiks.zrtp.Zrtp;

import java.util.logging.Level;

/**
 * Менеджер экономики для интеграции с Vault
 */
public class EconomyManager {
    private final Zrtp plugin;
    private Economy economy;
    private boolean enabled = false;

    /**
     * Конструктор менеджера экономики
     * @param plugin Экземпляр плагина
     */
    public EconomyManager(Zrtp plugin) {
        this.plugin = plugin;
        setupEconomy();
    }

    /**
     * Настройка экономики
     * @return true в случае успеха
     */
    private boolean setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            plugin.log(Level.INFO, "Vault не найден, функции экономики отключены.");
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.log(Level.WARNING, "Провайдер экономики не найден, функции экономики отключены.");
            return false;
        }

        economy = rsp.getProvider();
        enabled = (economy != null);
        
        if (enabled) {
            plugin.log(Level.INFO, "Интеграция с экономикой успешно настроена!");
        } else {
            plugin.log(Level.WARNING, "Не удалось получить провайдер экономики, функции экономики отключены.");
        }
        
        return enabled;
    }

    /**
     * Проверка, включена ли экономика
     * @return true если экономика включена
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Проверка, есть ли у игрока достаточно денег
     * @param player Игрок
     * @param amount Сумма
     * @return true если хватает денег
     */
    public boolean hasMoney(Player player, double amount) {
        if (!enabled || economy == null) return true;
        return economy.has(player, amount);
    }

    /**
     * Снятие денег с игрока
     * @param player Игрок
     * @param amount Сумма
     * @return true если транзакция прошла успешно
     */
    public boolean withdrawMoney(Player player, double amount) {
        if (!enabled || economy == null) return true;
        if (amount <= 0) return true;

        EconomyResponse response = economy.withdrawPlayer(player, amount);
        if (response.transactionSuccess()) {
            plugin.log(Level.INFO, "Снято " + amount + " с игрока " + player.getName() + " за телепортацию.");
            return true;
        } else {
            plugin.log(Level.WARNING, "Ошибка при снятии денег с игрока " + player.getName() + ": " + response.errorMessage);
            return false;
        }
    }

    /**
     * Пополнение баланса игрока
     * @param player Игрок
     * @param amount Сумма
     * @return true если транзакция прошла успешно
     */
    public boolean depositMoney(Player player, double amount) {
        if (!enabled || economy == null) return true;
        if (amount <= 0) return true;

        EconomyResponse response = economy.depositPlayer(player, amount);
        if (response.transactionSuccess()) {
            plugin.log(Level.INFO, "Добавлено " + amount + " игроку " + player.getName() + ".");
            return true;
        } else {
            plugin.log(Level.WARNING, "Ошибка при добавлении денег игроку " + player.getName() + ": " + response.errorMessage);
            return false;
        }
    }

    /**
     * Форматирование суммы в строку
     * @param amount Сумма
     * @return Отформатированная строка с суммой
     */
    public String formatMoney(double amount) {
        if (!enabled || economy == null) return String.valueOf(amount);
        return economy.format(amount);
    }
} 