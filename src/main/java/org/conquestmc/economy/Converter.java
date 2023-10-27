package org.conquestmc.economy;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.conquestmc.ConquestEconomy;
import org.conquestmc.Util;

import java.util.*;
import java.util.logging.Level;

public class Converter {

    EconomyImplementer eco;

    public Converter(EconomyImplementer economyImplementer) {
        this.eco = economyImplementer;
    }

    private static final List<Denomination> COIN_DENOMINATIONS = loadDenominations();


    public int getValue(ItemStack item) {
        // The item meta is designed specifically to where the data = the value.
        // I.E a gold coin has the model data value of 1, and is also equal to 1.
        return item.getItemMeta().getCustomModelData() * item.getAmount();
    }

    public boolean isNotGold(ItemStack item) {
        for (Denomination denomination: COIN_DENOMINATIONS) {
            if (denomination.getItem().isSimilar(item))
                return false;
        }
        return true;
    }

    public int getInventoryValue(Inventory inv){
        int value = 0;

        // calculating the value of all the gold in the inventory to nuggets
        for (ItemStack item : inv) {
            if (item == null) continue;

            if (isNotGold(item)) continue;

            value += getValue(item);

        }
        return value;
    }

    public int getInventoryValue(Player player){
        return getInventoryValue(player.getInventory());
    }
    public void remove(Inventory inv, int amount){
        int value = getInventoryValue(inv);

        // Checks if the Value of the items is greater than the amount to deposit
        if (value < amount) return;

        // Deletes all gold items
        for (ItemStack item : inv) {
            if (item == null) continue;
            if (isNotGold(item)) continue;

            item.setAmount(0);
            item.setType(Material.AIR);
        }

        int newBalance = value - amount;
        giveAndReturnExcess(inv, newBalance);
    }

    public int giveAndReturnExcess(Inventory inv, int value) {
        int excess = 0;

        for (Denomination denomination : COIN_DENOMINATIONS) {
            int coinsToGive = value / denomination.getValue();
            value -= coinsToGive * denomination.getValue();

            HashMap<Integer, ItemStack> coinMap = inv.addItem(getCoinStack(denomination, coinsToGive));

            for (ItemStack item : coinMap.values()) {
                if (isDenominationItem(item, denomination) && item.getAmount() > 0) {
                    inv.getLocation().getWorld().dropItem(inv.getLocation(), item);
                    excess += getValue(item);
                }
            }
        }
        return excess;
    }

    public ItemStack[] compress(int value) {

        List<ItemStack> items = new ArrayList<>();
        for (Denomination denomination : COIN_DENOMINATIONS) {
            int coinsToGive = value / denomination.getValue();
            value -= coinsToGive * denomination.getValue();

            items.add(getCoinStack(denomination, coinsToGive));
        }
        return items.toArray(new ItemStack[0]);
    }

    public void remove(Player player, int amount){
        remove(player.getInventory(), amount);
    }

    public void give(Player player, int value) {
        // This gives everything, but if it is true then it means it could not
        // fill up the entire inventory, therefore it drops.
        if (giveAndReturnExcess(player.getInventory(), value) > 0)
            player.sendMessage(Util.getMessage("warning-drops"));
    }


    public void withdrawAll(Player player){
        String uuid = player.getUniqueId().toString();

        // searches in the Hashmap for the balance, so that a player can't withdraw gold from his Inventory
        int value = eco.getBank().getAccountBalance(player.getUniqueId().toString());
        eco.getBank().setBalance(uuid, (0));

        give(player, value);
    }

    public void withdraw(Player player, int nuggets){
        String uuid = player.getUniqueId().toString();
        int oldbalance = eco.getBank().getAccountBalance(player.getUniqueId().toString());

        // Checks balance in HashMap
        if (nuggets > eco.getBank().getPlayerBank().get(player.getUniqueId().toString())) {
            player.sendMessage(Util.getMessage("error-notenoughmoneywithdraw"));
            return;
        }
        eco.getBank().setBalance(uuid, (oldbalance - nuggets));

        give(player, nuggets);

    }

    public void depositAll(Player player){
        OfflinePlayer op = Bukkit.getOfflinePlayer(player.getUniqueId());
        int value = 0;

        for (ItemStack item : player.getInventory()) {
            if (item == null) continue;

            if (isNotGold(item)) continue;

            value += getValue(item);
            item.setAmount(0);
            item.setType(Material.AIR);
        }

        eco.depositPlayer(op, value);

    }

    public void deposit(Player player, int nuggets){
        OfflinePlayer op = Bukkit.getOfflinePlayer(player.getUniqueId());

        remove(player, nuggets);
        eco.depositPlayer(op, nuggets);
    }

    private ItemStack getCoinStack(Denomination denomination, int amount) {
        ItemStack stack = denomination.getItem();
        stack.setAmount(amount);
        return stack;
    }

    private boolean isDenominationItem(ItemStack item, Denomination denomination) {
        return item.isSimilar(denomination.getItem());
    }

    private static List<Denomination> loadDenominations() {
        var denominationsSection = ConquestEconomy.getPlugin().getConfig().getConfigurationSection("denominations");
        List<Denomination> denominations = new ArrayList<>();
        for (String key: denominationsSection.getKeys(false)) {
            denominations.add(new Denomination(key));
        }
        // Sort the denominations based on their values
        denominations.sort(Collections.reverseOrder(Comparator.comparingInt(Denomination::getValue)));

        ConquestEconomy.getPlugin().getLogger().log(Level.INFO, "Loaded " + denominations.size());
        return denominations;
    }
}
