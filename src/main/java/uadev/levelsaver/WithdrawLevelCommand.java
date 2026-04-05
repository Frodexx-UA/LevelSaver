package uadev.levelsaver;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;

public class WithdrawLevelCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command cmd, @NonNull String label, String @NonNull [] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(LevelSaver.getInstance().getMessage("player-only"));
            return true;
        }
        if (!p.hasPermission("levelsaver.player")) {
            p.sendMessage(LevelSaver.getInstance().getMessage("no-permission"));
            return true;
        }
        if (args.length == 0) {
            p.sendMessage(LevelSaver.getInstance().getMessage("help.withdrawlevel"));
            return true;
        }

        ItemStack hand = p.getInventory().getItemInMainHand();
        if (!LevelSaver.getInstance().isLevelBook(hand)) {
            p.sendMessage(LevelSaver.getInstance().getMessage("not-holding-book"));
            return true;
        }

        int count;
        try {
            count = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            p.sendMessage(LevelSaver.getInstance().getMessage("invalid-number"));
            return true;
        }

        if (count < 1) {
            p.sendMessage(LevelSaver.getInstance().getMessage("amount-too-low"));
            return true;
        }

        int stored = LevelSaver.getInstance().getStoredLevels(hand);
        if (stored < count) {
            p.sendMessage(LevelSaver.getInstance().getMessage("not-enough-levels-in-book", "stored", String.valueOf(stored)));
            return true;
        }

        LevelSaver.getInstance().setPlayerStoredLevels(p.getUniqueId(), stored - count);
        LevelSaver.getInstance().setStoredLevels(hand, stored - count);
        LevelSaver.getInstance().updateBookLore(hand);

        ItemStack banknote = LevelSaver.getInstance().createBanknote(count, p.getName());
        p.getInventory().addItem(banknote);

        p.sendMessage(LevelSaver.getInstance().getMessage("withdraw-success", "amount", String.valueOf(count)));
        LevelSaver.getInstance().saveData();
        return true;
    }
}