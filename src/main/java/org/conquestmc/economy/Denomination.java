package org.conquestmc.economy;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.conquestmc.ConquestEconomy;
import org.conquestmc.Util;

import java.util.ArrayList;
import java.util.List;

public class Denomination {
    private int value;
    private final ItemStack item;

    public Denomination(String name) {
        item = generateItem(name);
    }

    public ItemStack getItem() {
        return item;
    }

    public int getValue() {
        return value;
    }
    private ItemStack generateItem(String name) {
        ConfigurationSection itemSection = ConquestEconomy.getPlugin().getConfig().getConfigurationSection("denominations." + name);

        Material itemMaterial = Material.getMaterial(itemSection.getString("material"));
        if (itemMaterial == null) {
            return null;
        }
        ItemStack item = new ItemStack(itemMaterial, 1);

        addName(itemSection, item);
        addModelData(itemSection, item);
        addLore(itemSection, item);
        // Set the value
        this.value = itemSection.getInt("value");

        return item;
    }


    private void addLore(ConfigurationSection itemSection, ItemStack item) {
        List<String> lore = itemSection.getStringList("lore");
        if (lore.isEmpty()) {
            return;
        }
        List<Component> deserializedLore = new ArrayList<>();
        for (String line : lore) {
            deserializedLore.add(Util.getMiniMessage().deserialize(line));
        }
        ItemMeta meta = item.getItemMeta();
        meta.lore(deserializedLore);
        item.setItemMeta(meta);
    }

    private void addModelData(ConfigurationSection itemSection, ItemStack item) {
        int customModelData = itemSection.getInt("custom-model-data");
        if (customModelData != 0) {
            ItemMeta meta = item.getItemMeta();
            meta.setCustomModelData(customModelData);
            item.setItemMeta(meta);
        }
    }

    private void addName(ConfigurationSection itemSection, ItemStack item) {
        String configuredName = itemSection.getString("name");
        if (configuredName != null) {
            Component name = Util.getMiniMessage().deserialize(configuredName);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(name);
            item.setItemMeta(meta);
        }
    }

}
