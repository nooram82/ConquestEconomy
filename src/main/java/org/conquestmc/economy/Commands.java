package org.conquestmc.economy;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.TownBlockType;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.conquestmc.ConquestEconomy;
import org.conquestmc.Util;
import redempt.redlib.commandmanager.CommandHook;

import java.util.Objects;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component;

public class Commands {
    EconomyImplementer eco;

    public Commands(EconomyImplementer eco) {
        this.eco = eco;
    }

    public boolean isBankingRestrictedToPlot(Player player){
        if (ConquestEconomy.getPlugin().getConfig().getBoolean("restrictToBankPlot")) {
            if (TownyAPI.getInstance().getTownBlock(player.getLocation()) == null || !Objects.requireNonNull(TownyAPI.getInstance().getTownBlock(player.getLocation())).getType().equals(TownBlockType.BANK)){
                player.sendMessage(Util.getMessage("error-bankplot"));
                return true;
            }
        }
        return false;
    }

    @CommandHook("balance")
    public void balance(CommandSender commandSender) {
        Player player = (Player) commandSender;
        String uuid = player.getUniqueId().toString();
        player.sendMessage(Util.getMessage("info-balance",
                component("total", text(eco.getBalance(uuid))),
                component("bank", text(eco.getBank().getPlayerBank().get(uuid))),
                component("inv", text(eco.getConverter().getInventoryValue(player))))
        );
    }

    @CommandHook("pay")
    public void pay(CommandSender commandSender, OfflinePlayer target, int amount) {
        Player sender = (Player) commandSender;
        String senderuuid = sender.getUniqueId().toString();
        String targetuuid = target.getUniqueId().toString();

        if (isBankingRestrictedToPlot(sender)) return;

        if (amount == 0) {
            sender.sendMessage(Util.getMessage("error-zero"));
            return;
        }

        if (amount < 0) {
            sender.sendMessage(Util.getMessage("error-negative"));
            return;
        }

        if (amount > eco.getBank().getTotalPlayerBalance(senderuuid)) {
            sender.sendMessage(Util.getMessage("error-notenough"));
            return;
        } else if (senderuuid.equals(targetuuid)){
            sender.sendMessage(Util.getMessage("error-payyourself"));
            return;
        } else if (Util.isOfflinePlayer(target.getName()) == null) {
            sender.sendMessage(Util.getMessage("error-noplayer"));
            return;
        }

        eco.withdrawPlayer(sender, amount);
        sender.sendMessage(Util.getMessage("info-sendmoneyto",
                component("amount", text(amount)),
                component("target", text(target.getName()))
        ));
        if (target.isOnline()) {
            target.getPlayer().sendMessage(Util.getMessage("info-moneyreceived",
                    component("amount", text(amount)),
                    component("sender", text(sender.getName()))
            ));
            eco.getBank().setBalance(target.getUniqueId().toString(), eco.getBank().getTotalPlayerBalance(targetuuid) + amount);
        } else {
            eco.depositPlayer(target, eco.getBank().getTotalPlayerBalance(targetuuid) + amount);
        }
    }

    @CommandHook("deposit")
    public void deposit(CommandSender commandSender, String coins){
        Player player = (Player) commandSender;

        if (isBankingRestrictedToPlot(player)) return;
        if (coins == null) {
            player.sendMessage(Util.getMessage("help-deposit"));
            return;
        }

        if (coins.equals("all")) {
            player.sendMessage(Util.getMessage("info-deposit",
                    component("amount", text(eco.getConverter().getInventoryValue(player))))
            );
            eco.getConverter().depositAll((Player) commandSender);
        } else if (Integer.parseInt(coins) == 0) {
            player.sendMessage(Util.getMessage("error-zero"));
        } else if (Integer.parseInt(coins) < 0) {
            player.sendMessage(Util.getMessage("error-negative"));
        } else if (Integer.parseInt(coins) > eco.getConverter().getInventoryValue(player)) {
            player.sendMessage(Util.getMessage("error-notenough"));
        } else {
            player.sendMessage(Util.getMessage("info-deposit",
                    component("amount", text(coins)))
            );
            eco.getConverter().deposit((Player) commandSender, Integer.parseInt(coins));
        }

    }

    @CommandHook("withdraw")
    public void withdraw(CommandSender commandSender, String coins){
        Player player = (Player) commandSender;

        if (isBankingRestrictedToPlot(player)) {
            return;
        }

        if (coins == null) {
            player.sendMessage(Util.getMessage("help-withdraw"));
        } else if (coins.equals("all")) {
            player.sendMessage(Util.getMessage("info-withdraw",
                    component("amount", text(eco.getBank().getAccountBalance(player.getUniqueId().toString()))))
            );
            eco.getConverter().withdrawAll((Player) commandSender);
        } else if (Integer.parseInt(coins) == 0) {
            player.sendMessage(Util.getMessage("error-zero"));
        } else if (Integer.parseInt(coins) < 0) {
            player.sendMessage(Util.getMessage("error-negative"));
        } else if (Integer.parseInt(coins) > eco.getBank().getAccountBalance(player.getUniqueId().toString())) {
            player.sendMessage(Util.getMessage("error-notenough"));
        } else {
            player.sendMessage(Util.getMessage("info-withdraw",
                    component("amount", text(coins)))
            );
            eco.getConverter().withdraw((Player) commandSender, Integer.parseInt(coins));
        }

    }

    @CommandHook("set")
    public void set(CommandSender commandSender, OfflinePlayer target, int gold){
        if (commandSender instanceof Player) {
            Player player = (Player) commandSender;
            player.sendMessage(Util.getMessage("info-sender-moneyset",
                    component("amount", text(gold)),
                    component("target", text(target.getName()))
            ));
        }

        eco.getBank().setBalance(target.getUniqueId().toString(), gold);
        target.getPlayer().sendMessage(Util.getMessage("info-target-moneyset",
                component("amount", text(gold))
        ));
    }

    @CommandHook("add")
    public void add(CommandSender commandSender, OfflinePlayer target, int gold){
        if (commandSender instanceof Player) {
            Player player = (Player) commandSender;
            player.sendMessage(Util.getMessage("info-sender-addmoney",
                    component("amount", text(gold)),
                    component("target", text(target.getName()))
            ));
        }

        eco.depositPlayer(target, gold);
        target.getPlayer().sendMessage(Util.getMessage("info-target-addmoney",
                component("amount", text(gold))
        ));
    }

    @CommandHook("remove")
    public void remove(CommandSender commandSender, OfflinePlayer target, int gold) {
        if (commandSender instanceof Player) {
            Player player = (Player) commandSender;
            player.sendMessage(Util.getMessage("info-sender-remove",
                    component("amount", text(gold)),
                    component("target", text(target.getName()))
            ));
        }

        eco.withdrawPlayer(target, gold);
        target.getPlayer().sendMessage(Util.getMessage("info-target-remove",
                component("amount", text(gold))
        ));
    }
}


