package uadev.levelsaver;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

public class LevelBookCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, @NonNull Command cmd, @NonNull String label, String @NonNull [] args) {
        if (!sender.hasPermission("levelsaver.admin")) {
            sender.sendMessage(LevelSaver.getInstance().getMessage("no-permission"));
            return true;
        }

        Player target = args.length > 0 ? Bukkit.getPlayer(args[0]) : (sender instanceof Player p ? p : null);
        if (target == null) {
            sender.sendMessage(LevelSaver.getInstance().getMessage("player-not-found"));
            return true;
        }

        target.getInventory().addItem(LevelSaver.getInstance().createLevelBook(target));
        sender.sendMessage(LevelSaver.getInstance().getMessage("book-given", "player", target.getName()));
        return true;
    }
}