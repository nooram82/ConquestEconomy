package org.conquestmc.towny;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class TownBankDatabase {
    private static final List<TownBank> TOWN_BANKS = new ArrayList<>();

    public static List<TownBank> getTownBanks() {
        return TOWN_BANKS;
    }

    public static TownBank getTownBank(Town town) {
        if (town == null)
            return null;
        for (TownBank townBank: TOWN_BANKS) {
            if (townBank.getTown().equals(town)) {
                return townBank;
            }
        }
        // Add the town because it SHOULD have a bank, then recursive call.
        addTown(town);
        return getTownBank(town);
    }

    public static TownBank getTownBank(Player player) {
        Town town = TownyAPI.getInstance().getResident(player).getTownOrNull();
        if (town == null)
            return null;
        return getTownBank(town);
    }
    public static void addTown(Town town) {
        TOWN_BANKS.add(new TownBank(town));
    }

    public static void removeTownBank(Town town) {
        for (TownBank townBank: TOWN_BANKS) {
            if (townBank.getTown().equals(town)) {
                townBank.delete();
                TOWN_BANKS.remove(townBank);
            }
        }
    }

    public static void populateTownBanks() {
        var towny = TownyAPI.getInstance();
        for (Town town: towny.getTowns()) {
            TOWN_BANKS.add(new TownBank(town));
        }
    }

    public static void saveTownBanks() {
        TOWN_BANKS.forEach(TownBank::saveData);
    }
}
