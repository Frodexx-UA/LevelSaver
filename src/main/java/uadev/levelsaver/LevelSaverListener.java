package uadev.levelsaver;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class LevelSaverListener implements Listener {

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        Player p = e.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();

        if (LevelSaver.getInstance().isLevelBook(item)) {
            handleLevelBook(p, item, e.getAction(), p.isSneaking());
            e.setCancelled(true);
        } else if (LevelSaver.getInstance().isBanknote(item)) {
            if (e.getAction().isRightClick()) {
                int levels = LevelSaver.getInstance().getStoredLevels(item);
                p.giveExpLevels(levels);
                item.setAmount(item.getAmount() - 1);
                p.sendMessage(LevelSaver.getInstance().getMessage("received-levels", "levels", String.valueOf(levels)));
                e.setCancelled(true);
            }
        }
    }

    private void handleLevelBook(Player p, ItemStack book, Action action, boolean sneaking) {
        int stored = LevelSaver.getInstance().getStoredLevels(book);

        if (action.isLeftClick()) {
            if (sneaking) {
                p.sendMessage(LevelSaver.getInstance().getMessage("chat-withdraw-prompt"));
                LevelSaver.getInstance().addAwaitingInput(p, "withdraw");
                LevelSaver.getInstance().showSelectedAmount(p, book);
            } else {
                if (stored > 0) {
                    p.giveExpLevels(stored);
                    LevelSaver.getInstance().setPlayerStoredLevels(p.getUniqueId(), 0);
                    LevelSaver.getInstance().setStoredLevels(book, 0);
                    LevelSaver.getInstance().updateBookLore(book);
                    p.sendMessage(LevelSaver.getInstance().getMessage("withdraw-all-success", "amount", String.valueOf(stored)));
                    LevelSaver.getInstance().saveData();
                }
            }
        } else if (action.isRightClick()) {
            if (sneaking) {
                p.sendMessage(LevelSaver.getInstance().getMessage("chat-deposit-prompt"));
                LevelSaver.getInstance().addAwaitingInput(p, "deposit");
                LevelSaver.getInstance().showSelectedAmount(p, book);
            } else {
                int playerLevels = p.getLevel();
                if (playerLevels > 0) {
                    LevelSaver.getInstance().setPlayerStoredLevels(p.getUniqueId(), stored + playerLevels);
                    p.giveExpLevels(-playerLevels);
                    LevelSaver.getInstance().setStoredLevels(book, stored + playerLevels);
                    LevelSaver.getInstance().updateBookLore(book);
                    p.sendMessage(LevelSaver.getInstance().getMessage("deposit-all-success", "amount", String.valueOf(playerLevels)));
                    LevelSaver.getInstance().saveData();
                }
            }
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        String mode = LevelSaver.getInstance().getAwaitingInput().remove(p.getUniqueId());
        if (mode == null) return;

        e.setCancelled(true);

        int amount;
        try {
            amount = Integer.parseInt(e.getMessage().trim());
        } catch (NumberFormatException ex) {
            Bukkit.getScheduler().runTask(LevelSaver.getInstance(), () -> {
                p.sendMessage(LevelSaver.getInstance().getMessage("invalid-number"));
                ItemStack hand = p.getInventory().getItemInMainHand();
                if (LevelSaver.getInstance().isLevelBook(hand)) LevelSaver.getInstance().updateBookLore(hand);
            });
            return;
        }

        if (amount < 1) {
            Bukkit.getScheduler().runTask(LevelSaver.getInstance(), () -> {
                p.sendMessage(LevelSaver.getInstance().getMessage("amount-too-low"));
                ItemStack hand = p.getInventory().getItemInMainHand();
                if (LevelSaver.getInstance().isLevelBook(hand)) LevelSaver.getInstance().updateBookLore(hand);
            });
            return;
        }

        Bukkit.getScheduler().runTask(LevelSaver.getInstance(), () -> {
            ItemStack hand = p.getInventory().getItemInMainHand();
            if (!LevelSaver.getInstance().isLevelBook(hand)) {
                p.sendMessage(LevelSaver.getInstance().getMessage("not-holding-book"));
                return;
            }

            int stored = LevelSaver.getInstance().getStoredLevels(hand);

            if (mode.equals("withdraw")) {
                if (amount > stored) {
                    p.sendMessage(LevelSaver.getInstance().getMessage("not-enough-levels-in-book", "stored", String.valueOf(stored)));
                    LevelSaver.getInstance().updateBookLore(hand);
                    return;
                }
                p.giveExpLevels(amount);
                LevelSaver.getInstance().setPlayerStoredLevels(p.getUniqueId(), stored - amount);
                LevelSaver.getInstance().setStoredLevels(hand, stored - amount);
                LevelSaver.getInstance().updateBookLore(hand);
                p.sendMessage(LevelSaver.getInstance().getMessage("withdraw-all-success", "amount", String.valueOf(amount)));
                LevelSaver.getInstance().saveData();
            } else if (mode.equals("deposit")) {
                int playerLvl = p.getLevel();
                if (amount > playerLvl) {
                    p.sendMessage(LevelSaver.getInstance().getMessage("not-enough-player-levels", "player_levels", String.valueOf(playerLvl)));
                    LevelSaver.getInstance().updateBookLore(hand);
                    return;
                }
                p.giveExpLevels(-amount);
                LevelSaver.getInstance().setPlayerStoredLevels(p.getUniqueId(), stored + amount);
                LevelSaver.getInstance().setStoredLevels(hand, stored + amount);
                LevelSaver.getInstance().updateBookLore(hand);
                p.sendMessage(LevelSaver.getInstance().getMessage("deposit-all-success", "amount", String.valueOf(amount)));
                LevelSaver.getInstance().saveData();
            }
        });
    }
}