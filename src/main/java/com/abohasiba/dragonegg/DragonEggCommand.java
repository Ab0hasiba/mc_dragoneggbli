package com.abohasiba.dragonegg;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Command executor and tab completer for /dragonegg.
 */
public class DragonEggCommand implements CommandExecutor, TabCompleter {

    private final DragonEgg plugin;

    public DragonEggCommand(DragonEgg plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("dragonegg.admin")) {
            String noPerm = plugin.getConfiguration().getMessageNoPermission();
            if (noPerm != null && !noPerm.isEmpty()) {
                sender.sendMessage(noPerm);
            }
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadPluginConfig();
            String reloaded = plugin.getConfiguration().getMessageReloaded();
            if (reloaded != null && !reloaded.isEmpty()) {
                sender.sendMessage(reloaded);
            }
            return true;
        }

        sender.sendMessage(DragonEggConfig.color("&eUsage: /" + label + " reload"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("dragonegg.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            if ("reload".startsWith(args[0].toLowerCase())) {
                completions.add("reload");
            }
            return completions;
        }

        return Collections.emptyList();
    }
}
