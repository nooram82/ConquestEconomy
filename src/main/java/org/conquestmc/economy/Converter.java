package org.conquestmc.economy;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.conquestmc.Util;
import org.conquestmc.economy.EconomyImplementer;

import java.util.HashMap;
import java.util.Map;

import static dev.lone.itemsadder.api.CustomStack.getInstance;

public class Converter {

    EconomyImplementer eco;

    public Converter(EconomyImplementer economyImplementer) {
        this.eco = economyImplementer;
    }

    private static final Map<Integer, ItemStack> COIN_DENOMINATIONS =  Map.of(
            1, getInstance("gold_coin").getItemStack(),
            10, getInstance("gold_coin_pile").getItemStack(),
            50, getInstance("gold_coin_pouch").getItemStack(),
            100, getInstance("gold_coin_bag").getItemStack(),
            200, getInstance("gold_coin_chest").getItemStack()
    );

    public int getValue(ItemStack item) {
        // The item meta is designed specifically to where the data = the value.
        // I.E a gold coin has the model data value of 1, and is also equal to 1.
        return item.getItemMeta().getCustomModelData();
    }

    public boolean isNotGold(ItemStack item) {
        for (ItemStack denomination: COIN_DENOMINATIONS.values()) {
            if (denomination.isSimilar(item))
                return false;
        }
        return true;
    }

    public int getInventoryValue(Player player){
        int value = 0;

        // calculating the value of all the gold in the inventory to nuggets
        for (ItemStack item : player.getInventory()) {
            if (item == null) continue;

            if (isNotGold(item)) continue;

            value += (getValue(item) * item.getAmount());

        }
        return value;
    }

    public void remove(Player player, int amount){
        int value = 0;

        // calculating the value of all the gold in the inventory to nuggets
        for (ItemStack item : player.getInventory()) {
            if (item == null) continue;
            if (isNotGold(item)) continue;

            value += (getValue(item) * item.getAmount());
        }

        // Checks if the Value of the items is greater than the amount to deposit
        if (value < amount) return;

        // Deletes all gold items
        for (ItemStack item : player.getInventory()) {
            if (item == null) continue;
            if (isNotGold(item)) continue;

            item.setAmount(0);
            item.setType(Material.AIR);
        }

        int newBalance = value - amount;
        give(player, newBalance);
    }

    public void give(Player player, int value){
        boolean warning = false;

        HashMap<Integer, ItemStack> chests = player.getInventory().addItem(getCoinStack(200, value/200));
        for (ItemStack item : chests.values()) {
            if (isDenominationItem(item, 200) && item.getAmount() > 0) {
                player.getWorld().dropItem(player.getLocation(), item);
                warning = true;
            }
        }

        value -= (value/200)*200;

        HashMap<Integer, ItemStack> bags = player.getInventory().addItem(getCoinStack(100, value/100));
        for (ItemStack item : bags.values()) {
            if (isDenominationItem(item, 100) && item.getAmount() > 0) {
                player.getWorld().dropItem(player.getLocation(), item);
                warning = true;
            }
        }

        value -= (value/100)*100;

        HashMap<Integer, ItemStack> pouches = player.getInventory().addItem(getCoinStack(50, value/50));
        for (ItemStack item : pouches.values()) {
            if (isDenominationItem(item, 50) && item.getAmount() > 0) {
                player.getWorld().dropItem(player.getLocation(), item);
                warning = true;
            }
        }
        value -= (value/50)*50;

        HashMap<Integer, ItemStack> piles = player.getInventory().addItem(getCoinStack(10, value/10));
        for (ItemStack item : piles.values()) {
            if (isDenominationItem(item, 10) && item.getAmount() > 0) {
                player.getWorld().dropItem(player.getLocation(), item);
                warning = true;
            }
        }
        value -= (value/10)*10;

        HashMap<Integer, ItemStack> coins = player.getInventory().addItem(getCoinStack(1, value));
        for (ItemStack item : coins.values()) {
            if (isDenominationItem(item, 1) && item.getAmount() > 0) {
                player.getWorld().dropItem(player.getLocation(), item);
                warning = true;
            }
        }

        if (warning)
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

            value = value + (getValue(item) * item.getAmount());
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

    private ItemStack getCoinStack(int denomination, int amount) {
        ItemStack stack = COIN_DENOMINATIONS.get(denomination);
        stack.setAmount(amount);
        return stack;
    }

    private boolean isDenominationItem(ItemStack item, int denomination) {
        return COIN_DENOMINATIONS.get(denomination).isSimilar(item);
    }
}
