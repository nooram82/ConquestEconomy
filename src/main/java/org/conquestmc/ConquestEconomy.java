package org.conquestmc;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.conquestmc.economy.Bank;
import org.conquestmc.economy.BankCommands;
import org.conquestmc.economy.EconomyImplementer;
import org.conquestmc.economy.TownBankCommands;
import org.conquestmc.towny.TownBankDatabase;
import org.conquestmc.towny.TownBankListener;
import org.conquestmc.vault.VaultHook;
import redempt.redlib.commandmanager.ArgType;
import redempt.redlib.commandmanager.CommandParser;

import java.util.Map;

public final class ConquestEconomy extends JavaPlugin{

    private EconomyImplementer eco;
    private VaultHook vaultHook;
    private static ConquestEconomy PLUGIN;

    public ConquestEconomy() {
        PLUGIN = this;
    }

    public static ConquestEconomy getPlugin() {
        return PLUGIN;
    }

    @Override
    public void onEnable() {
        eco = new EconomyImplementer(this);
        vaultHook = new VaultHook(this, eco);
        vaultHook.hook();

        registerEvents();
        saveDefaultConfig();
        TownBankDatabase.populateTownBanks();
        // Commands from RedLib
        registerCommands();
        populateOnlinePlayerBanks();
    }

    private void populateOnlinePlayerBanks() {
        Bank bank = eco.getBank();
        for (Player player: getServer().getOnlinePlayers()) {
            String uuid = player.getUniqueId().toString();
            bank.getPlayerBank().put(uuid, bank.getBalanceFile().getInt(uuid));
        }
    }

    private void registerEvents() {
        Bukkit.getPluginManager().registerEvents(new Events (this, eco.getBank()), this);
        Bukkit.getPluginManager().registerEvents(new TownBankListener(), this);
    }

    @Override
    public void onDisable() {
        // Save player HashMap to File
        for(Map.Entry<String, Integer> entry : eco.getBank().getPlayerBank().entrySet()) {
            String key = entry.getKey();
            int value = entry.getValue();

            eco.getBank().getBalanceFile().getFileData().insert(key, value);
        }
        eco.getBank().getBalanceFile().write();

        // Save FakeAccount HashMap to File
        for(Map.Entry<String, Integer> entry : eco.getBank().getFakeAccounts().entrySet()) {
            String key = entry.getKey();
            int value = entry.getValue();

            eco.getBank().getFakeAccountsFile().getFileData().insert(key, value);
        }
        eco.getBank().getFakeAccountsFile().write();
        TownBankDatabase.saveTownBanks();

        vaultHook.unhook();

        getLogger().info("ConquestEconomy disabled.");
    }

    private void registerCommands() {
        ArgType<OfflinePlayer> offlinePlayer = new ArgType<>("offlinePlayer", Bukkit::getOfflinePlayer)
                .tabStream(c -> Bukkit.getOnlinePlayers().stream().map(Player::getName));
        new CommandParser(this.getResource("commands.rdcml"))
                .setArgTypes(offlinePlayer)
                .parse()
                .register("ConquestEconomy",
                        new BankCommands(eco));
        new CommandParser(this.getResource("townbankcommands.rdcml"))
                .parse()
                .register("ConquestEconomy",
                        new TownBankCommands(eco));
    }

    public EconomyImplementer getEco() {
        return eco;
    }
}
