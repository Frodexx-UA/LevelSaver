package uadev.levelsaver;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LevelSaver extends JavaPlugin {

    private static LevelSaver instance;
    private FileConfiguration config;
    private File dataFile;
    private YamlConfiguration data;

    private final ConcurrentHashMap<UUID, String> awaitingInput = new ConcurrentHashMap<>();
    private final NamespacedKey LEVELS_KEY = new NamespacedKey(this, "stored_levels");

    public static LevelSaver getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        config = getConfig();
        loadDataFile();

        Objects.requireNonNull(getCommand("levelbook")).setExecutor(new LevelBookCommand());
        Objects.requireNonNull(getCommand("lsreload")).setExecutor(new LSReloadCommand());
        Objects.requireNonNull(getCommand("withdrawlevel")).setExecutor(new WithdrawLevelCommand());

        getServer().getPluginManager().registerEvents(new LevelSaverListener(), this);

        getLogger().info("LevelSaver увімкнено!");
    }

    @Override
    public void onDisable() {
        saveData();
    }

    private void loadDataFile() {
        dataFile = new File(getDataFolder(), "data.yml");

        if (!dataFile.exists()) {
            try {
                File parent = dataFile.getParentFile();
                if (parent != null && !parent.exists()) {
                    if (!parent.mkdirs()) {
                        getLogger().severe("Не вдалося створити директорію для data.yml");
                        return;
                    }
                }
                if (!dataFile.createNewFile()) {
                    getLogger().warning("data.yml вже існує або не вдалося створити");
                }
            } catch (IOException e) {
                getLogger().severe("Не вдалося створити data.yml: " + e.getMessage());
            }
        }

        data = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void saveData() {
        try {
            data.save(dataFile);
        } catch (IOException e) {
            getLogger().severe("Не вдалося зберегти data.yml");
        }
    }

    public int getPlayerStoredLevels(UUID uuid) {
        return data.getInt("players." + uuid + ".stored_levels", 0);
    }

    public void setPlayerStoredLevels(UUID uuid, int levels) {
        data.set("players." + uuid + ".stored_levels", levels);
    }

    public String getMessage(String path, String... placeholders) {
        String msg = config.getString("messages." + path, "&cПовідомлення не знайдено: " + path);
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                msg = msg.replace("%" + placeholders[i] + "%", placeholders[i + 1]);
            }
        }
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    public ItemStack createLevelBook(Player player) {
        int levels = getPlayerStoredLevels(player.getUniqueId());
        Material material = Material.valueOf(config.getString("level_book.material", "ENCHANTED_BOOK").toUpperCase());
        ItemStack book = new ItemStack(material);
        ItemMeta meta = book.getItemMeta();

        String name = config.getString("level_book.name");
        if (name != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        }

        meta.setLore(buildBookLore(levels));

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(LEVELS_KEY, PersistentDataType.INTEGER, levels);

        book.setItemMeta(meta);
        return book;
    }

    public ItemStack createBanknote(int levels, String ownerName) {
        Material material = Material.valueOf(config.getString("withdraw_level.material", "PAPER").toUpperCase());
        ItemStack paper = new ItemStack(material);
        ItemMeta meta = paper.getItemMeta();

        String name = config.getString("withdraw_level.name");
        if (name != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        }

        List<String> loreTemplate = config.getStringList("withdraw_level.lore");
        List<String> finalLore = new ArrayList<>();
        for (String line : loreTemplate) {
            line = line.replace("%levels%", String.valueOf(levels));
            line = line.replace("%owner%", ownerName);
            finalLore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        meta.setLore(finalLore);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(LEVELS_KEY, PersistentDataType.INTEGER, levels);

        paper.setItemMeta(meta);
        return paper;
    }

    private List<String> buildBookLore(int storedLevels) {
        List<String> template = config.getStringList("level_book.lore");
        List<String> finalLore = new ArrayList<>();

        for (String line : template) {
            line = line.replace("%levels%", String.valueOf(storedLevels));
            finalLore.add(ChatColor.translateAlternateColorCodes('&', "&r" + line));
        }
        return finalLore;
    }

    public boolean isLevelBook(ItemStack item) {
        if (item == null || item.getType() != Material.ENCHANTED_BOOK) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;

        String expectedName = ChatColor.translateAlternateColorCodes('&', config.getString("level_book.name", ""));
        return meta.getDisplayName().equals(expectedName) &&
                meta.getPersistentDataContainer().has(LEVELS_KEY, PersistentDataType.INTEGER);
    }

    public boolean isBanknote(ItemStack item) {
        if (item == null || item.getType() != Material.PAPER) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;

        String expectedName = ChatColor.translateAlternateColorCodes('&', config.getString("withdraw_level.name", ""));
        return meta.getDisplayName().equals(expectedName) &&
                meta.getPersistentDataContainer().has(LEVELS_KEY, PersistentDataType.INTEGER);
    }

    public int getStoredLevels(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        Integer value = item.getItemMeta().getPersistentDataContainer().get(LEVELS_KEY, PersistentDataType.INTEGER);
        return value != null ? value : 0;
    }

    public void setStoredLevels(ItemStack item, int levels) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(LEVELS_KEY, PersistentDataType.INTEGER, levels);
        item.setItemMeta(meta);
    }

    public void updateBookLore(ItemStack item) {
        if (!isLevelBook(item)) return;
        int stored = getStoredLevels(item);
        ItemMeta meta = item.getItemMeta();
        meta.setLore(buildBookLore(stored));
        item.setItemMeta(meta);
    }

    public void showSelectedAmount(Player p, ItemStack book) {
        if (!isLevelBook(book)) return;
        int stored = getStoredLevels(book);
        ItemMeta meta = book.getItemMeta();
        meta.setLore(buildBookLore(stored));
        book.setItemMeta(meta);
        p.updateInventory();
    }

    public void addAwaitingInput(Player player, String mode) {
        awaitingInput.put(player.getUniqueId(), mode);
    }

    public ConcurrentHashMap<UUID, String> getAwaitingInput() {
        return awaitingInput;
    }
}