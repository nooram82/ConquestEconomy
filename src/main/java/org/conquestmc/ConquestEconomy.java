package org.conquestmc;

import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.conquestmc.economy.Commands;
import org.conquestmc.economy.EconomyImplementer;
import org.conquestmc.vault.VaultHook;
import redempt.redlib.commandmanager.ArgType;
import redempt.redlib.commandmanager.CommandParser;

import java.util.Map;

public final class ConquestEconomy extends JavaPlugin implements Listener{

    EconomyImplementer eco;
    private VaultHook vaultHook;
    private static ConquestEconomy PLUGIN;

    public ConquestEconomy() {
        PLUGIN = this;
    }

    public static Plugin getPlugin() {
        return PLUGIN;
    }

    @Override
    public void onEnable() {
        // Register the listener for the IA load event.
        getServer().getPluginManager().registerEvents(this, this);
        eco = new EconomyImplementer(this);
        vaultHook = new VaultHook(this, eco);
        vaultHook.hook();

        // Commands from RedLib
        ArgType<OfflinePlayer> offlinePlayer = new ArgType<>("offlinePlayer", Bukkit::getOfflinePlayer)
                .tabStream(c -> Bukkit.getOnlinePlayers().stream().map(Player::getName));
        new CommandParser(this.getResource("commands.rdcml"))
                .setArgTypes(offlinePlayer)
                .parse()
                .register("ConquestEconomy",
                        new Commands(eco));

        // Event class registering
        Bukkit.getPluginManager().registerEvents(new Events (this, eco.getBank()), this);
        saveDefaultConfig();
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

        vaultHook.unhook();

        getLogger().info("ConquestEconomy disabled.");
    }

    @EventHandler
    void onIALoad(ItemsAdderLoadDataEvent event) {
        // Initialize the converter which depends on IA item data to work.
        eco.initConverter();
    }
}
