package uadev.levelsaver;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NonNull;

public class LSReloadCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, @NonNull Command cmd, @NonNull String label, String @NonNull [] args) {
        if (!sender.hasPermission("levelsaver.admin")) {
            sender.sendMessage(LevelSaver.getInstance().getMessage("no-permission"));
            return true;
        }
        LevelSaver.getInstance().saveData();
        sender.sendMessage(LevelSaver.getInstance().getMessage("reload-success"));
        return true;
    }
}