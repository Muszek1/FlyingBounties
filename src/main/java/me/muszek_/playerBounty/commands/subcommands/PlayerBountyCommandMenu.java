package me.muszek_.playerBounty.commands.subcommands;

import me.muszek_.playerBounty.PlayerBounty;
import me.muszek_.playerBounty.commands.SubCommand;
import me.muszek_.playerBounty.menusystem.BountyMenu;
import org.bukkit.entity.Player;

import java.util.List;

public class PlayerBountyCommandMenu extends SubCommand {
    private final BountyMenu menu;

    public PlayerBountyCommandMenu(PlayerBounty plugin) {
        this.menu = new BountyMenu(plugin);
    }

    @Override
    public String getName() {
        return "menu";
    }

    @Override
    public String getDescription() {
        return "Otwiera menu zleceniowe";
    }

    @Override
    public String getSyntax() {
        return "/bounty menu";
    }

    @Override
    public String getPermission() {
        return "zlecenia.menu";
    }

    @Override
    public void perform(Player player, String[] args) {

        menu.open(player);
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        return List.of();
    }
}
