package love.toad;

import java.util.logging.Logger;
import java.util.HashMap;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.io.File;
import java.io.StringWriter;
import java.io.PrintWriter;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.NamespacedKey;
import org.bukkit.boss.KeyedBossBar;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.enchantments.Enchantment;
import java.time.Instant;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Bukkit APIs:
 *
 * Inventory: https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/inventory/Inventory.html
 * Manipulate something's inventory, could be a player, could be a chest.
 *
 * PlayerInventory: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/inventory/PlayerInventory.html
 * Specific to player with methods for armor/weapon slots.
 */

public class InventoryBuddy extends JavaPlugin implements Listener, CommandExecutor {
    private static String PLUGIN_NAME = "InventoryBuddy";
    private static String VERSION = "1.0.0";
    private static String VERSION_STRING = PLUGIN_NAME + '/' + VERSION;
    private static String HELP_MESSAGE = "Usage: /inventory [ load | save | list ]";
    Logger log = Logger.getLogger("Minecraft");

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        log.info("[InventoryBuddy] Enabled");
    }

    @Override
    public void onDisable() {
        log.info("[InventoryBuddy] Disabled");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("inventory")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Not a console command");
                return false;
            } else {
                Player player = (Player) sender;
                if (args.length == 1) {
                    /*
                     * INVENTORY SAVE
                     */
                    if (args[0].equalsIgnoreCase("save")) {
                        saveInventory(player);
                        player.sendMessage(ChatColor.BLUE + "InventoryBuddy: Equipment layout saved");
                    /*
                     * INVENTORY LOAD
                     */
                    } else if (args[0].equalsIgnoreCase("load")) {
                        JSONObject playerInventory = loadInventory(player);
                        if (playerInventory != null) {
                            ArrayList<String> warnings = applyInventory(player, playerInventory);
                            if (warnings.size() > 0) {
                                Iterator warningsIterator = warnings.iterator();
                                while (warningsIterator.hasNext()) {
                                    player.sendMessage(ChatColor.YELLOW + "InventoryBuddy: [WARNING] " + warningsIterator.next());
                                }
                            }
                            player.sendMessage(ChatColor.BLUE + "InventoryBuddy: Equipment layout loaded");
                        }
                    /*
                     * INVENTORY SAVE
                     */
                    } else if (args[0].equalsIgnoreCase("list")) {
                        ArrayList<String> inventoryList = new ArrayList<String>();
                        JSONObject savedPlayerInventory = loadInventory(player);
                        if (savedPlayerInventory != null) {
                            String version = (String) savedPlayerInventory.get("version");
                            long saved = (long) savedPlayerInventory.get("created");
                            Instant instant = Instant.ofEpochSecond(saved);
                            ZonedDateTime zdt = instant.atZone(ZoneId.of("America/Los_Angeles")); // california über allies
                            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM/dd/uuuu HH:mm:ss", Locale.ENGLISH);
                            player.sendMessage(ChatColor.GREEN + version + " created " + dtf.format(zdt));
                            JSONArray savedInventory = (JSONArray) savedPlayerInventory.get("inventory");
                            int slot = -1;
                            Iterator savedInventoryIterator = savedInventory.iterator();
                            String itemName;
                            while (savedInventoryIterator.hasNext()) {
                                ++slot;
                                itemName = (String) savedInventoryIterator.next();
                                if (itemName != null) {
                                    inventoryList.add(String.format("%s (%d)", (String) itemName, slot));
                                }
                            }
                            player.sendMessage(ChatColor.BLUE + String.join(", ", inventoryList));
                        }
                    } else if (args[0].equalsIgnoreCase("help")) {
                        player.sendMessage(ChatColor.BLUE + HELP_MESSAGE);
                    }
                } else {
                    player.sendMessage(ChatColor.BLUE + HELP_MESSAGE);
                }

            }
        }
        return true;
    }

    /*
     * LOAD INVENTORY: returns null if something fails, handles player notification
     */
    private JSONObject loadInventory(Player player) {
        final String playerId = player.getUniqueId().toString();
        final String playerInventoryFile = String.join("/", this.getDataFolder().getAbsolutePath(), "players", playerId + ".json");

        JSONParser jsonParser = new JSONParser();
        JSONObject savedPlayerInventory;

        try (FileReader reader = new FileReader(playerInventoryFile)) {
            Object obj = jsonParser.parse(reader);
            savedPlayerInventory = (JSONObject) obj;
        } catch (FileNotFoundException e) {
            player.sendMessage(ChatColor.BLUE + "File not found, you must \"/inventory save\" first");
            return null;
        } catch (IOException e) {
            player.sendMessage(ChatColor.BLUE + "I/O exception, somebody fucked up");
            return null;
        } catch (ParseException e) {
            player.sendMessage(ChatColor.BLUE + "Parse exception, somebody really fucked up");
            return null;
        }

        return savedPlayerInventory;
    }

    /*
     * APPLY INVENTORY
     */
    private ArrayList<String> applyInventory(Player player, JSONObject savedPlayerInventory) {
        ArrayList<String> warnings = new ArrayList<String>();

        // check future compatibility here
        String version = (String) savedPlayerInventory.get("version");

        JSONArray savedInventory = (JSONArray) savedPlayerInventory.get("inventory");
        ArrayList<ItemStack> itemStack;

        // map of items loaded: ITEM_NAME => SLOT_NUMBER
        HashMap<String, ArrayList> currentInventoryMap = new HashMap<String, ArrayList>();
        String itemName;
        PlayerInventory playerInventory = player.getInventory();
        for (ItemStack item : playerInventory.getContents()) {
            // skip empty slots
            if (item == null || item.getAmount() <= 0) {
                continue;
            }
            itemName = item.getType().name();

            // add to a list of items indexed by item name
            if (currentInventoryMap.containsKey(itemName)) {
                itemStack = currentInventoryMap.get(itemName);
                itemStack.add(item);
                currentInventoryMap.put(itemName, itemStack);
            } else {
                itemStack = new ArrayList<ItemStack>();
                itemStack.add(item);
                currentInventoryMap.put(itemName, itemStack);
            }
        }

        // remove all items: we have copies in `currentInventoryMap`
        playerInventory.clear();

        // iterate over inventory layout
        int slot = -1;
        Iterator savedInventoryIterator = savedInventory.iterator();
        ArrayList<String> missing = new ArrayList<String>();
        while (savedInventoryIterator.hasNext()) {
            ++slot;
            itemName = (String) savedInventoryIterator.next();

            if (itemName == null) {
                continue;
            } else if (! currentInventoryMap.containsKey(itemName)) {
                // track missing items: those specified in layout but not currently in player's inventory
                missing.add(itemName);
                continue;
            }

            itemStack = currentInventoryMap.get(itemName);

            // this should never happen
            if (itemStack.size() == 0) {
                log.info(String.format(".. Item %s has nothing in inventory map (odd)", itemName));
                continue;
            }

            // find first item by name
            ItemStack item = itemStack.remove(0); // unshift
            if (itemStack.size() == 0) {
                currentInventoryMap.remove(itemName);
            } else {
                currentInventoryMap.put(itemName, itemStack);
            }

            // put item in slot
            playerInventory.setItem(slot, item);
        }

        // deal with leftovers: extra items hanging around in player inventory not in JSON
        int emptySlot;
        ItemStack item;
        boolean done = false;
        Iterator leftoverIterator;
        ArrayList<String> leftovers = new ArrayList<String>();

        for (Map.Entry<String, ArrayList> entry : currentInventoryMap.entrySet()) {
            if (done) {
                break;
            }
            itemStack = currentInventoryMap.get(entry.getKey());
            leftoverIterator = itemStack.iterator();
            while (leftoverIterator.hasNext()) {
                if (done) {
                    break;
                }
                emptySlot = playerInventory.firstEmpty();

                // this should never happen
                if (emptySlot == -1) {
                    warnings.add("Ran out of empty slots");
                    done = true;
                } else {
                    item = (ItemStack) leftoverIterator.next();
                    playerInventory.setItem(emptySlot, item);
                    leftovers.add(item.getType().name());
                }
            }
        }

        if (missing.size() > 0) {
            warnings.add("Items missing from inventory: " + String.join(", ", missing));
        }

        if (leftovers.size() > 0) {
            warnings.add("Unexpected items: " + String.join(", ", leftovers));
        }

        return warnings;
    }

    private void saveInventory(Player player) {
        final String playerId = player.getUniqueId().toString();

        JSONObject obj = new JSONObject();

        // preamble: track create date + add version to file
        obj.put("player", player.getName());
        obj.put("version", VERSION_STRING);
        obj.put("created", Instant.now().toEpochMilli() / 1000);

        String inventoryDir = this.getDataFolder().getAbsolutePath() + "/" + "players";
        new File(inventoryDir).mkdirs();
        String playerInventoryFile = String.join("/", inventoryDir, playerId + ".json");

        JSONArray inventory = new JSONArray();
        int slot = -1;
        for (ItemStack item : player.getInventory().getContents()) {
            ++slot;

            if (item == null || item.getAmount() <= 0) {
                // empty slot
                inventory.add(null);
            } else {
                inventory.add(item.getType().name());
            }
        }

        obj.put("inventory", inventory);

        // write JSON file to disk
        FileWriter file;
        try {
            file = new FileWriter(playerInventoryFile);
            file.write(obj.toJSONString());
            file.flush();
            file.close();
        } catch (IOException ex) {
            StringWriter errors = new StringWriter();
            ex.printStackTrace(new PrintWriter(errors));
            log.info("[InventoryBuddy] " + errors.toString());
        }
    }
}
