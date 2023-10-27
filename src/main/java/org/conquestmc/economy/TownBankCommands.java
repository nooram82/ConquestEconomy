package org.conquestmc.economy;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlockType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.conquestmc.towny.TownBank;
import org.conquestmc.towny.TownBankDatabase;
import org.jetbrains.annotations.Nullable;
import redempt.redlib.commandmanager.CommandHook;

import java.util.Objects;
import java.util.Optional;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component;
import static org.conquestmc.Util.getMessage;

public class TownBankCommands {
    EconomyImplementer eco;
    Converter converter;

    public TownBankCommands(EconomyImplementer eco) {
        this.eco = eco;
        converter = eco.getConverter();
    }

    public boolean isInBankPlot(Player player) {
        if (TownyAPI.getInstance().getTownBlock(player.getLocation()) == null || !Objects.requireNonNull(TownyAPI.getInstance().getTownBlock(player.getLocation())).getType().equals(TownBlockType.BANK)) {
            player.sendMessage(getMessage("error-bankplot"));
            return true;
        }
        return false;
    }

    @CommandHook("towndeposit")
    public void deposit(CommandSender commandSender, String coins) {
        if (!(commandSender instanceof Player player))
            return;

        if (isInBankPlot(player)) return;
        if (coins == null) {
            player.sendMessage(getMessage("help-deposit"));
            return;
        }
        var amount = extractCoinValue(player, coins, converter.getInventoryValue(player));
        if (amount.isEmpty())
            return;
        TownBank townBank = TownBankDatabase.getTownBank(player);
        if (townBank == null)
            return;
        townBank.deposit(player, amount.get());
    }

    @CommandHook("townwithdraw")
    public void withdraw(CommandSender commandSender, String coins) {
        if (!(commandSender instanceof Player player))
            return;

        if (isInBankPlot(player)) return;
        if (coins == null) {
            player.sendMessage(getMessage("help-withdraw"));
            return;
        }
        TownBank townBank = TownBankDatabase.getTownBank(player);
        if (townBank == null)
            return;
        var amount = extractCoinValue(player, coins, townBank.getBalance());
        if (amount.isEmpty())
            return;
        townBank.withdraw(player, amount.get());
    }

    private Optional<Integer> extractCoinValue(Player player, String coins, int max) {
        if (coins.equals("all")) {
            player.sendMessage(getMessage("info-deposit", component("amount", text(max))));
            return Optional.of(max);
        }
        int amount = Integer.parseInt(coins);
        if (amount == 0) {
            player.sendMessage(getMessage("error-zero"));
        }
        else if (amount < 0) {
            player.sendMessage(getMessage("error-negative"));
        } else if (amount > max) {
            player.sendMessage(getMessage("error-notenough"));
        } else {
            player.sendMessage(getMessage("info-deposit",
                    component("amount", text(coins)))
            );
            return Optional.of(amount);
        }
        return Optional.empty();
    }

}
