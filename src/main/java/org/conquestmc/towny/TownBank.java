package org.conquestmc.towny;

import com.palmergames.bukkit.towny.object.Town;
import de.leonhard.storage.Json;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.conquestmc.ConquestEconomy;
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
        town.getAccount().setBalance(balance, "gold deposited");
    }

    public void removeMoney(int amount) {
        this.balance = Math.max(balance - amount, 0);
        town.getAccount().setBalance(balance, "gold withdrawn");
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

    public boolean withdraw(Player player, int amount) {
        int remainingGold = amount;
        int cannotStore = 0;
        if (getBalance() < amount) {
            Bukkit.broadcastMessage("Not enough gold!");
            return false;
        }

        if (getPhysicalChestInventories().isEmpty()) {
            Bukkit.broadcastMessage("No chests found!");
            return false;
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
        }
        return true;
    }

    public boolean remove(int amount) {
        int remainingGold = amount;
        if (getBalance() < amount) {
            remainingGold = amount = getBalance();
        }

        if (getPhysicalChestInventories().isEmpty()) {
            Bukkit.broadcastMessage("No chests found!");
            return false;
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

        removeMoney(amount - remainingGold);
        return true;
    }

    public boolean deposit(Player player, int amountToDeposit) {
        Bukkit.broadcastMessage("deposit method active");
        int remainingGold = amountToDeposit;


        if (getPhysicalChestInventories().isEmpty()) {
            Bukkit.broadcastMessage("No chests found!");
            return false;
        }
        for (Inventory chestInventory : getPhysicalChestInventories()) {
            if (remainingGold <= 0) {
                break; // No more gold to deposit
            }

            // Chest is full, go to next chest.
            if (chestInventory.firstEmpty() == -1)
                continue;

            // remove the gold from player
            converter.remove(player, amountToDeposit);
            // add it into the chest
            remainingGold = converter.giveAndReturnExcess(chestInventory, amountToDeposit);
            // compress the chest automatically for efficiency
            var compressed = converter.compress(converter.getInventoryValue(chestInventory));
            chestInventory.clear();
            chestInventory.addItem(compressed);
            if (remainingGold <= 0)
                break;
        }

        if (amountToDeposit == remainingGold) {
            Bukkit.broadcastMessage("No space found! Nothing deposited...");
            return false;
        }

        addMoney(amountToDeposit - remainingGold);
        // Handle any remaining excess gold
        if (remainingGold > 0) {
            var excessItems = converter.compress(remainingGold);
            for (ItemStack goldItem : excessItems) {
                player.getWorld().dropItem(player.getLocation(), goldItem);
            }
        }
        return true;
    }

    public boolean add(int amountToDeposit) {
        int remainingGold = amountToDeposit;


        if (getPhysicalChestInventories().isEmpty()) {
            return false;
        }
        for (Inventory chestInventory : getPhysicalChestInventories()) {
            if (remainingGold <= 0) {
                break; // No more gold to deposit
            }

            // Chest is full, go to next chest.
            if (chestInventory.firstEmpty() == -1)
                continue;

            // add it into the chest
            remainingGold = converter.giveAndReturnExcess(chestInventory, amountToDeposit);
            // compress the chest automatically for efficiency
            var compressed = converter.compress(converter.getInventoryValue(chestInventory));
            chestInventory.clear();
            chestInventory.addItem(compressed);
            if (remainingGold <= 0)
                break;
        }

        if (amountToDeposit == remainingGold) {
            return false;
        }

        addMoney(amountToDeposit - remainingGold);
        return true;
    }


    private String convertLocationToString(Location loc) {
        return String.join(";", loc.getWorld().getName(), String.valueOf(loc.getBlockX()), String.valueOf(loc.getBlockY()), String.valueOf(loc.getBlockZ()));
    }

    private Location convertStringToLocation(String string) {
        String[] split = string.split(";");
        return new Location(Bukkit.getWorld(split[0]), Double.parseDouble(split[1]), Double.parseDouble(split[2]), Double.parseDouble(split[3]));
    }
}
