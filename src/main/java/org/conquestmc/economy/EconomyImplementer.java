package org.conquestmc.economy;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.conquestmc.ConquestEconomy;
import org.conquestmc.Util;
import org.conquestmc.towny.TownBank;
import org.conquestmc.towny.TownBankDatabase;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class EconomyImplementer implements Economy {
    ConquestEconomy plugin;
    private final Bank bank;

    private final Converter converter;

    public EconomyImplementer(ConquestEconomy plugin) {
        this.plugin = plugin;
        converter = new Converter(this);
        bank = new Bank(plugin, this);
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getName() {
        return "ConquestEconomy";
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public int fractionalDigits() {
        return 0;
    }

    @Override
    public String format(double v) {
        return "⛃ " + v;
    }

    @Override
    public String currencyNamePlural() {
        return "Gold Coins";
    }

    @Override
    public String currencyNameSingular() {
        return "Gold Coin";
    }

    @Override
    public boolean hasAccount(String s) {
        if (Util.isOfflinePlayer(s) != null) return true;
        if (bank.getFakeAccounts().containsKey(s)) return true;
        else if (bank.fakeAccountsFile.contains(s)) return true;

        bank.fakeAccountsFile.set(s, 0);
        bank.getFakeAccounts().put(s, 0);
        return true;
    }

    @Override
    public boolean hasAccount(OfflinePlayer offlinePlayer) {
        return true;
    }

    @Override
    public boolean hasAccount(String s, String s1) {
        if (Util.isOfflinePlayer(s) != null) return true;
        if (bank.getFakeAccounts().containsKey(s)) return true;
        else if (bank.fakeAccountsFile.contains(s)) return true;

        bank.fakeAccountsFile.set(s, 0);
        bank.getFakeAccounts().put(s, 0);
        return true;
    }

    @Override
    public boolean hasAccount(OfflinePlayer offlinePlayer, String s) {
        return true;
    }

    @Override
    public double getBalance(String s) {
        try {
            if (Bukkit.getPlayer(UUID.fromString(s)) != null) return bank.getTotalPlayerBalance(s);
        } catch (IllegalArgumentException e) {
            // String is not UUID
        }
        if (Util.isOfflinePlayer(s) != null) return bank.getTotalPlayerBalance(Bukkit.getOfflinePlayer(s).getUniqueId().toString());
        return bank.getFakeBalance(s);
    }

    @Override
    public double getBalance(OfflinePlayer offlinePlayer) {
        if (offlinePlayer != null) return bank.getTotalPlayerBalance(offlinePlayer.getUniqueId().toString());
        return 0;
    }

    @Override
    public double getBalance(String s, String s1) {
        try {
            if (Bukkit.getPlayer(UUID.fromString(s)) != null) return bank.getTotalPlayerBalance(s);
        } catch (IllegalArgumentException e) {
            // String is not UUID
        }
        if (Util.isOfflinePlayer(s) != null) return bank.getTotalPlayerBalance(Bukkit.getOfflinePlayer(s).getUniqueId().toString());
        return bank.getFakeBalance(s);
    }

    @Override
    public double getBalance(OfflinePlayer offlinePlayer, String s) {
        if (offlinePlayer != null) return bank.getTotalPlayerBalance(offlinePlayer.getUniqueId().toString());
        return 0;
    }

    @Override
    public boolean has(String s, double v) {
        if (Util.isOfflinePlayer(s) != null) return v < bank.getTotalPlayerBalance(Bukkit.getOfflinePlayer(s).getUniqueId().toString());
        else return v < bank.getFakeBalance(s);
    }

    @Override
    public boolean has(OfflinePlayer offlinePlayer, double v) {
        return v < bank.getTotalPlayerBalance(offlinePlayer.getUniqueId().toString());
    }

    @Override
    public boolean has(String s, String s1, double v) {
        if (Util.isOfflinePlayer(s) != null) return v < bank.getTotalPlayerBalance(Bukkit.getOfflinePlayer(s).getUniqueId().toString());
        else return v < bank.getFakeBalance(s);
    }

    @Override
    public boolean has(OfflinePlayer offlinePlayer, String s, double v) {
        return v < bank.getTotalPlayerBalance(offlinePlayer.getUniqueId().toString());
    }

    public EconomyResponse withdrawPlayer(UUID uuid, double amount) {
        int oldBalance = 0;

        // if amount is negative return
        if (amount < 0)
            return new EconomyResponse(amount, oldBalance, EconomyResponse.ResponseType.FAILURE, "error");

        if (Util.isOfflinePlayer(uuid)) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);

            // if player is online
            if (offlinePlayer.isOnline()) {
                Player player = offlinePlayer.getPlayer();

                if (player == null)
                    return new EconomyResponse(amount, oldBalance, EconomyResponse.ResponseType.FAILURE, "error");

                // get balance and InventoryValue from Player
                int oldBankBalance = bank.getAccountBalance(uuid.toString());
                int oldInventoryBalance = converter.getInventoryValue(player);


                // If balance + InventoryValue is < amount, return
                if (amount > oldBankBalance + oldInventoryBalance)
                    return new EconomyResponse(amount, oldBalance, EconomyResponse.ResponseType.FAILURE, "Not enough Money!");
                // Withdraw from inventory first then bank
                if (oldInventoryBalance > amount) {
                    converter.remove(player, (int) amount);
                    return new EconomyResponse(amount, (oldInventoryBalance - amount), EconomyResponse.ResponseType.SUCCESS, "");
                } else {
                    // Withdraw from bank
                    int diff = (int) (amount - oldInventoryBalance);
                    converter.remove(player, oldInventoryBalance);
                    bank.setBalance(uuid.toString(), oldBankBalance - diff);
                    return new EconomyResponse(amount, (oldBankBalance - diff), EconomyResponse.ResponseType.SUCCESS, "");
                }
            } else {
                // When player is offline
                oldBalance = bank.getTotalPlayerBalance(uuid.toString());
                int newBalance = (int) (oldBalance - amount);
                bank.setBalance(uuid.toString(), newBalance);
                return new EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, "");
            }
        }
        return new EconomyResponse(amount, oldBalance, EconomyResponse.ResponseType.FAILURE, "Player not found!");
    }


    @Override
    public EconomyResponse withdrawPlayer(String accountName, double amount) {
        int oldBalance = 0;

        // if amount is negative return
        if (amount < 0) return new EconomyResponse(amount, oldBalance, EconomyResponse.ResponseType.FAILURE, "error");

        if (Util.isOfflinePlayer(accountName) != null) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(accountName);
            return withdrawPlayer(offlinePlayer.getUniqueId(), amount);
        } else {
            return withdrawFromFakeAccount(accountName, amount);
        }
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer offlinePlayer, double amount) {
        int oldBalance = 0;

        // if amount is negative return
        if (amount < 0)
            return new EconomyResponse(amount, oldBalance, EconomyResponse.ResponseType.FAILURE, "error");

        return withdrawPlayer(offlinePlayer.getUniqueId(), amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(String accountName, String s1, double amount) {
        int oldBalance = 0;

        // if amount is negative return
        if (amount < 0)
            return new EconomyResponse(amount, oldBalance, EconomyResponse.ResponseType.FAILURE, "error");

        if (Util.isOfflinePlayer(accountName) != null) {
            return withdrawPlayer(Bukkit.getOfflinePlayer(accountName).getUniqueId(), amount);
        } else {
            return withdrawFromFakeAccount(accountName, amount);
        }
    }

    @NotNull
    private EconomyResponse withdrawFromFakeAccount(String accountName, double amount) {
        int oldBalance = bank.getFakeBalance(accountName);
        int newBalance = (int) (oldBalance - amount);
        TownBank townBank = getTownBank(accountName);
        if (townBank != null) {
            if (townBank.getBalance() >= amount) {
                bank.setBalance(accountName, newBalance);
                Bukkit.getScheduler().runTask(ConquestEconomy.getPlugin(), () -> {
                    townBank.remove((int) amount);
                    townBank.removeMoney((int) amount);
                });
                return new EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, "");
            } else {
                return new EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.FAILURE, "failed to remove!");
            }
        }
        bank.setBalance(accountName, newBalance);
        // Means it wasn't a potential town bank anyway, so maybe some NPC account or something. Still, withdrawn and successful.
        return new EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, "");
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer offlinePlayer, String s, double amount){
        int oldBalance = 0;

        // if amount is negative return
        if (amount < 0) return new EconomyResponse(amount, oldBalance, EconomyResponse.ResponseType.FAILURE, "error");

        return withdrawPlayer(offlinePlayer.getUniqueId(), amount);
    }

    private void withdrawPlayerPhysical(Player player, int amount) {
        String uuid = player.getUniqueId().toString();
        int oldBalance = bank.getAccountBalance(uuid);

        // If amount is negative -> return
        if (amount < 0) return;

        converter.withdraw(player, amount);
        int newBalance = oldBalance - amount;
        bank.setBalance(uuid, newBalance);
    }

    @Override
    public EconomyResponse depositPlayer(String accountName, double amount) {
        int oldBalance = 0;

        // If amount is negative -> return
        if (amount < 0)
            return new EconomyResponse(amount, oldBalance, EconomyResponse.ResponseType.FAILURE, "error");

        if (Util.isOfflinePlayer(accountName) != null) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(accountName);
            String uuid = player.getUniqueId().toString();
            if (player.isOnline()) {
                depositPlayerPhysical(player.getPlayer(), (int) amount);
                return new EconomyResponse(amount, bank.getAccountBalance(player.getUniqueId().toString()), EconomyResponse.ResponseType.SUCCESS, "");
            }
            // Getting balance and calculating new Balance
            oldBalance = bank.getAccountBalance(uuid);
            int newBalance = (int) (oldBalance + amount);
            bank.setBalance(uuid, Math.min(5000, newBalance));
            return new EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, "");
        } else {
            return depositNonPlayer(accountName, amount);
        }
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer offlinePlayer, double amount) {
        String uuid = offlinePlayer.getUniqueId().toString();
        int oldBalance = bank.getAccountBalance(uuid);
        int newBalance = (int) (amount + oldBalance);

        // If amount is negative -> return
        if (amount < 0) return new EconomyResponse(amount, oldBalance, EconomyResponse.ResponseType.FAILURE, "error");

        if (offlinePlayer.isOnline()) {
            depositPlayerPhysical(offlinePlayer.getPlayer(), (int) amount);
            return new EconomyResponse(amount, bank.getAccountBalance(offlinePlayer.getUniqueId().toString()), EconomyResponse.ResponseType.SUCCESS, "");
        }

        bank.setBalance(uuid, Math.min(5000, newBalance));
        return new EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, "");
    }

    @Override
    public EconomyResponse depositPlayer(String accountName, String worldName, double amount) {
        int oldBalance = 0;

        // If amount is negative -> return
        if (amount < 0) return new EconomyResponse(amount, oldBalance, EconomyResponse.ResponseType.FAILURE, "error");

        if (Util.isOfflinePlayer(accountName) != null) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(accountName);
            if (player.isOnline()) {
                depositPlayerPhysical(player.getPlayer(), (int) amount);
                return new EconomyResponse(amount, bank.getAccountBalance(player.getUniqueId().toString()), EconomyResponse.ResponseType.SUCCESS, "");
            }
            String uuid = player.getUniqueId().toString();

            // Getting balance and calculating new Balance
            oldBalance = bank.getAccountBalance(uuid);
            int newBalance = (int) (oldBalance + amount);
            bank.setBalance(uuid, newBalance);
            return new EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, "");
        } else {
            return depositNonPlayer(accountName, amount);
        }
    }

    private void depositPlayerPhysical(Player player, int amount) {
        String uuid = player.getUniqueId().toString();
        int oldBalance = bank.getAccountBalance(uuid);

        // If amount is negative -> return
        if (amount < 0) return;

        converter.remove(player.getInventory(), amount);
        int newBalance = oldBalance + amount;
        bank.setBalance(uuid, newBalance);
    }
    @NotNull
    private EconomyResponse depositNonPlayer(String accountName, double amount) {
        TownBank townBank = getTownBank(accountName);
        if (townBank != null) {
            if (!townBank.getChestLocations().isEmpty()) {
                Bukkit.getScheduler().runTask(ConquestEconomy.getPlugin(), () ->  {
                    townBank.add((int) amount);
                    bank.setBalance(accountName, townBank.getBalance());
                });
                return new EconomyResponse(amount, townBank.getBalance(), EconomyResponse.ResponseType.SUCCESS, "");
            } else {
                return new EconomyResponse(amount, townBank.getBalance(), EconomyResponse.ResponseType.FAILURE, "failed to deposit!");
            }
        } else {
            int oldBalance = bank.getFakeBalance(accountName);
            int newBalance = (int) (oldBalance + amount);
            bank.setBalance(accountName, newBalance);
            return new EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, "");
        }
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer offlinePlayer, String s, double amount) {
        String uuid = offlinePlayer.getUniqueId().toString();
        int oldBalance = bank.getAccountBalance(uuid);
        int newBalance = (int) (amount + oldBalance);

        // If amount is negative -> return
        if (amount < 0) return new EconomyResponse(amount, oldBalance, EconomyResponse.ResponseType.FAILURE, "error");
        if (offlinePlayer.isOnline()) {
            depositPlayerPhysical(offlinePlayer.getPlayer(), (int) amount);
            return new EconomyResponse(amount, bank.getAccountBalance(offlinePlayer.getUniqueId().toString()), EconomyResponse.ResponseType.SUCCESS, "");
        }
        bank.setBalance(uuid, newBalance);
        return new EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, "");
    }

    @Override
    public EconomyResponse createBank(String s, String s1) {
        return null;
    }

    @Override
    public EconomyResponse createBank(String s, OfflinePlayer offlinePlayer) {
        return null;
    }

    @Override
    public EconomyResponse deleteBank(String s) {
        return null;
    }

    @Override
    public EconomyResponse bankBalance(String s) {
        return null;
    }

    @Override
    public EconomyResponse bankHas(String s, double v) {
        return null;
    }

    @Override
    public EconomyResponse bankWithdraw(String s, double v) {
        return null;
    }

    @Override
    public EconomyResponse bankDeposit(String s, double v) {
        return null;
    }

    @Override
    public EconomyResponse isBankOwner(String s, String s1) {
        return null;
    }

    @Override
    public EconomyResponse isBankOwner(String s, OfflinePlayer offlinePlayer) {
        return null;
    }

    @Override
    public EconomyResponse isBankMember(String s, String s1) {
        return null;
    }

    @Override
    public EconomyResponse isBankMember(String s, OfflinePlayer offlinePlayer) {
        return null;
    }

    @Override
    public List<String> getBanks() {
        return null;
    }

    @Override
    public boolean createPlayerAccount(String s) {
        return false;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer offlinePlayer) {
        return false;
    }

    @Override
    public boolean createPlayerAccount(String s, String s1) {
        return false;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer offlinePlayer, String s) {
        return false;
    }

    public Bank getBank() {
        return bank;
    }

    public Converter getConverter() {
        return converter;
    }

    private TownBank getTownBank(String accountName) {
        TownyAPI townyAPI = TownyAPI.getInstance();
        for (Town town: townyAPI.getTowns()) {
            if (town.getAccount().getName().equals(accountName))
                return TownBankDatabase.getTownBank(town);
        }
        for (Nation nation: townyAPI.getNations()) {
            if (nation.getAccount().getName().equals(accountName))
                return TownBankDatabase.getTownBank(nation.getCapital());
        }
        return null;
    }
}
