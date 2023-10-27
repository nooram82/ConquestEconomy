package org.conquestmc.towny;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.event.NewDayEvent;
import com.palmergames.bukkit.towny.event.economy.TownPreTransactionEvent;
import com.palmergames.bukkit.towny.event.economy.TownTransactionEvent;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TransactionType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.conquestmc.ConquestEconomy;
import org.conquestmc.Util;
import org.conquestmc.economy.Converter;

public class TownBankListener implements Listener {
    private final TownyAPI towny = TownyAPI.getInstance();
    private final Converter converter = ConquestEconomy.getPlugin().getEco().getConverter();

    @EventHandler
    public void onChestPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (block.getType() != Material.CHEST)
            return;
        if (towny.isWilderness(block))
            return;
        Location location = block.getLocation();
        if (towny.getTownBlock(location).getType() != TownBlockType.BANK)
            return;

        TownBankDatabase.getTownBank(towny.getTown(location)).addChestLocation(location);
        event.getPlayer().sendMessage(Util.getMessage("bank-chest-placed"));
    }

    @EventHandler
    public void onChestBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.CHEST)
            return;
        if (towny.isWilderness(block))
            return;
        Location location = block.getLocation();
        if (towny.getTownBlock(location).getType() != TownBlockType.BANK)
            return;

        TownBank townBank = TownBankDatabase.getTownBank(towny.getTown(location));
        if (townBank == null)
            return;
        Chest chest = (Chest) block.getState();
        townBank.removeMoney(converter.getInventoryValue(chest.getInventory()));
        townBank.removeChestLocation(location);
        event.getPlayer().sendMessage(Util.getMessage("bank-chest-broken"));
    }

    @EventHandler
    public void onTownTransaction(TownPreTransactionEvent event) {
        TownBank townBank = TownBankDatabase.getTownBank(event.getTown());
        TransactionType type = event.getTransaction().getType();
        int amount = (int) Math.floor(event.getTransaction().getAmount());
        Player player = event.getTransaction().getPlayer();
        Bukkit.broadcastMessage(type.toString());
        Bukkit.broadcastMessage(event.isAsynchronous() + "");
        Bukkit.getScheduler().runTask(ConquestEconomy.getPlugin(), () -> {
            switch (type) {
                case ADD -> townBank.add(amount);
                case SUBTRACT -> townBank.remove(amount);
                case DEPOSIT -> townBank.deposit(player, amount);
                case WITHDRAW -> townBank.withdraw(player, amount);
            }
        });
        event.setCancelMessage("");
        event.setCancelled(true);
    }

    @EventHandler
    public void onNewDay(NewDayEvent event) {

    }

    @EventHandler
    public void onBankChestInteract(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (topInventory.getLocation() == null)
            return;
        Block block = event.getClickedInventory().getLocation().getBlock();
        if (block.getType() != Material.CHEST)
            return;
        if (towny.isWilderness(block))
            return;
        Location location = block.getLocation();
        if (towny.getTownBlock(location).getType() != TownBlockType.BANK)
            return;

        TownBank townBank = TownBankDatabase.getTownBank(towny.getTown(location));
        if (townBank == null)
            return;

        event.setCancelled(true);
        event.getWhoClicked().sendMessage(Util.getMessage("cannot-manipulate-chest"));
    }

}
