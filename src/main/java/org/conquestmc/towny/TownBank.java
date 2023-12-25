package org.conquestmc.towny;

import com.palmergames.bukkit.towny.object.Town;
import de.leonhard.storage.Json;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.conquestmc.ConquestEconomy;
import org.conquestmc.Util;
import org.conquestmc.economy.Converter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class TownBank {
    private final Town town;
    private int balance;
    private final HashSet<Location> chestLocations;
    private final Json bankFile;
    private final Converter converter = ConquestEconomy.getPlugin().getEco().getConverter();

    public TownBank(Town town) {
        this.town = town;
        bankFile = new Json(town.getUUID() + ".json", ConquestEconomy.getPlugin().getDataFolder() + "/data/towns/");
        chestLocations = loadChestLocations(bankFile.getStringList("chest-locations"));
        balance = bankFile.getInt("balance");
    }


    public Town getTown() {
        return town;
    }

    public int getBalance() {
        return balance;
    }

    public HashSet<Location> getChestLocations() {
        return chestLocations;
    }

    public void setBalance(int balance) {
        this.balance = balance;
    }

    public void addMoney(int amount) {
        this.balance = balance + amount;
    }

    public void removeMoney(int amount) {
        this.balance = Math.max(balance - amount, 0);
    }

    public void removeMoneyThenItems(int amount) {
        removeMoney(amount);
        Bukkit.getScheduler().runTask(ConquestEconomy.getPlugin(), () -> remove(amount));
    }

    public boolean isBankChest(Block block) {
        return chestLocations.contains(block.getLocation());
    }
    public void addChestLocation(Location loc) {
        chestLocations.add(loc);
    }


    public void removeChestLocation(Location loc) {
        chestLocations.remove(loc);
    }

    void delete() {
        bankFile.getFile().delete();
    }

    public List<Inventory> getPhysicalChestInventories() {
        List<Inventory> inventories = new ArrayList<>();
        for (Location loc: chestLocations) {
            Block block = loc.getBlock();
            if (block.getType() != Material.CHEST)
                continue;

            if (block.getState() instanceof Container container)
                inventories.add(container.getInventory());
        }
        return inventories;
    }

    // This takes the locations saved in the file as strings, and transforms them into actual location objects.
    private HashSet<Location> loadChestLocations(List<String> stringList) {
        HashSet<Location> chests = new HashSet<>();
        for (String locationString: stringList) {
            // world;x;y;z
            chests.add(convertStringToLocation(locationString));
        }
        return chests;
    }

     void saveData() {
        List<String> chests = new ArrayList<>();
        for (Location loc: chestLocations) {
            chests.add(convertLocationToString(loc));
        }
        bankFile.set("chest-locations", chests);
        bankFile.set("balance", balance);
    }

    public void withdraw(Player player, int amount) {
        int remainingGold = amount;
        int cannotStore = 0;
        if (getBalance() < amount) {
            player.sendMessage(Util.getMessage("error-notenough"));
            return;
        }

        if (getChestLocations().isEmpty()) {
            player.sendMessage(Util.getMessage("error-no-bank-chests"));
            return;
        }

        for (Inventory chestInventory : getPhysicalChestInventories()) {

            if (remainingGold <= 0) {
                break; // No more gold to withdraw
            }
            if (chestInventory.isEmpty())
                continue;

            // Calculate how much gold is available in the chest
            int availableGold = converter.getInventoryValue(chestInventory);

            if (availableGold > 0) {
                // Withdraw as much gold as possible from this chest
                int goldToWithdraw = Math.min(remainingGold, availableGold);
                converter.remove(chestInventory, goldToWithdraw);
                remainingGold -= goldToWithdraw;
                cannotStore += converter.giveAndReturnExcess(player.getInventory(), goldToWithdraw);
            }

            if (remainingGold <= 0)
                break;
        }

        removeMoney(amount - remainingGold);

        // Handle any remaining excess gold
        if (cannotStore > 0) {
            var excessItems = converter.compress(remainingGold);
            for (ItemStack goldItem : excessItems) {
                player.getWorld().dropItem(player.getLocation(), goldItem);
            }
            player.sendMessage(Util.getMessage("warning-drops-bank"));
        }

        player.sendMessage(Util.getMessage("bank-withdrawn", Placeholder.component("amount", Component.text(amount - remainingGold))));
    }

    public void remove(int amount) {
        int remainingGold = amount;
        if (getBalance() < amount) {
            return;
        }

        if (getChestLocations().isEmpty()) {
            return;
        }

        for (Inventory chestInventory : getPhysicalChestInventories()) {

            if (remainingGold <= 0) {
                break; // No more gold to withdraw
            }
            if (chestInventory.isEmpty())
                continue;

            // Calculate how much gold is available in the chest
            int availableGold = converter.getInventoryValue(chestInventory);

            if (availableGold > 0) {
                // Withdraw as much gold as possible from this chest
                int goldToWithdraw = Math.min(remainingGold, availableGold);
                converter.remove(chestInventory, goldToWithdraw);
                remainingGold -= goldToWithdraw;
            }

            if (remainingGold <= 0)
                break;
        }

    }

    public void deposit(Player player, int amount) {
        int remainingGold = amount;

        if (!canDeposit(player, amount))
            return;

        for (Inventory chestInventory : getPhysicalChestInventories()) {
            if (remainingGold <= 0) {
                break; // No more gold to deposit
            }

            // Chest is full, go to next chest.
            if (chestInventory.firstEmpty() == -1)
                continue;

            // remove the gold from player
            converter.remove(player, amount);
            // add it into the chest
            remainingGold = converter.giveAndReturnExcess(chestInventory, amount);
            // compress the chest automatically for efficiency
            var compressed = converter.compress(converter.getInventoryValue(chestInventory));
            chestInventory.clear();
            chestInventory.addItem(compressed);
            if (remainingGold <= 0)
                break;
        }

        if (amount == remainingGold) {
            player.sendMessage(Util.getMessage("bank-no-space"));
            return;
        }

        addMoney(amount - remainingGold);
        // Handle any remaining excess gold
        if (remainingGold > 0) {
            var excessItems = converter.compress(remainingGold);
            for (ItemStack goldItem : excessItems) {
                player.getWorld().dropItem(player.getLocation(), goldItem);
            }
            player.sendMessage(Util.getMessage("warning-drops-bank"));
        }

        player.sendMessage(Util.getMessage("bank-deposited", Placeholder.component("amount", Component.text(amount - remainingGold))));
    }

    public boolean canDeposit(Player player, int amount) {
        if (converter.getInventoryValue(player) < amount) {
            player.sendMessage(Util.getMessage("error-notenough"));
            return true;
        }
        if (getChestLocations().isEmpty()) {
            player.sendMessage(Util.getMessage("error-no-bank-chests"));
            return true;
        }
        return false;
    }

    public void add(int amount) {
        int remainingGold = amount;

        if (getChestLocations().isEmpty()) {
            return;
        }
        for (Inventory chestInventory : getPhysicalChestInventories()) {
            if (remainingGold <= 0) {
                break; // No more gold to deposit
            }

            // Chest is full, go to next chest.
            if (chestInventory.firstEmpty() == -1)
                continue;

            // add it into the chest
            remainingGold = converter.giveAndReturnExcess(chestInventory, amount);
            // compress the chest automatically for efficiency
            var compressed = converter.compress(converter.getInventoryValue(chestInventory));
            chestInventory.clear();
            chestInventory.addItem(compressed);
            if (remainingGold <= 0)
                break;
        }

        if (amount == remainingGold) {
            return;
        }

        addMoney(amount - remainingGold);
    }


    private String convertLocationToString(Location loc) {
        return String.join(";", loc.getWorld().getName(), String.valueOf(loc.getBlockX()), String.valueOf(loc.getBlockY()), String.valueOf(loc.getBlockZ()));
    }

    private Location convertStringToLocation(String string) {
        String[] split = string.split(";");
        return new Location(Bukkit.getWorld(split[0]), Double.parseDouble(split[1]), Double.parseDouble(split[2]), Double.parseDouble(split[3]));
    }

}
