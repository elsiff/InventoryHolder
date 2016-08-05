package me.elsiff.inventoryholder;

import com.google.common.base.Preconditions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class InventoryHolder extends JavaPlugin {
    private final String prefix = "§e[InvHolder]§r ";

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            boolean created = getDataFolder().mkdir();

            if (!created) {
                getLogger().warning("Failed to create the directory!");
            }
        }

        getLogger().info("Plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Plugin has been disabled!");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> list = new ArrayList<String>();

        if (args.length < 2) {
            list.add("list");

            if (sender.hasPermission("inventoryholder.save")) {
                list.add("save");
            }

            if (sender.hasPermission("inventoryholder.load")) {
                list.add("load");
            }
        } else if ((args.length == 2 && args[0].equalsIgnoreCase("load")) || (args.length == 3 && args[0].equalsIgnoreCase("force"))) {
            for (String kit : getKits()) {
                list.add(kit);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("force")) {
            for (Player player : getServer().getOnlinePlayers()) {
                list.add(player.getName());
            }
        }

        String finalArg = args[args.length - 1];
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            if (!it.next().startsWith(finalArg)) {
                it.remove();
            }
        }

        return list;
    }

    public List<String> getKits() {
        List<String> list = new ArrayList<String>();

        File[] files = getDataFolder().listFiles();
        Preconditions.checkNotNull(files);

        for (File file : files) {
            String name = file.getName();
            list.add(name.substring(0, name.length() - 4));
        }

        return list;
    }

    public boolean exist(String name) {
        File file = new File(getDataFolder(), name + ".yml");

        return file.exists();
    }

    public void save(PlayerInventory inv, String name) throws IOException {
        File file = new File(getDataFolder(), name + ".yml");

        boolean created = file.createNewFile();
        if (!created) {
            getLogger().warning("Failed to create the file: " + name + ".yml");
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        setContents(config, "storage", inv.getStorageContents());
        setContents(config, "armor", inv.getArmorContents());
        setContents(config, "extra", inv.getExtraContents());

        config.save(file);
    }

    public void load(PlayerInventory inv, String name) {
        File file = new File(getDataFolder(), name + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        ItemStack[] storage = null, armor = null, extra = null;

        int lengthStorage = inv.getStorageContents().length;
        int lengthArmor = inv.getArmorContents().length;
        int lengthExtra = inv.getExtraContents().length;

        if (config.contains("storage"))
            storage = getContents(config, "storage", lengthStorage);

        if (config.contains("armor"))
            armor = getContents(config, "armor", lengthArmor);

        if (config.contains("extra"))
            extra = getContents(config, "extra", lengthExtra);

        inv.setStorageContents(storage != null ? storage : new ItemStack[lengthStorage]);
        inv.setArmorContents(armor != null ? armor : new ItemStack[lengthArmor]);
        inv.setExtraContents(extra != null ? extra : new ItemStack[lengthExtra]);
    }

    private void setContents(FileConfiguration config, String path, ItemStack[] contents) {
        for (int i = 0; i < contents.length; i ++) {
            config.set(path + ".num_" + i, contents[i]);
        }
    }

    private ItemStack[] getContents(FileConfiguration config, String path, int length) {
        ConfigurationSection section = config.getConfigurationSection(path);
        ItemStack[] contents = new ItemStack[length];

        for (String id : section.getKeys(false)) {
            ItemStack item = section.getItemStack(id);
            int i = Integer.parseInt(id.substring(4));

            contents[i] = item;
        }

        return contents;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length < 1 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(prefix + "§3> ===== §b§lInventoryHolder §3 ===== <");
            sender.sendMessage(prefix + "/" + label + " help");
            sender.sendMessage(prefix + "/" + label + " list");
            sender.sendMessage(prefix + "/" + label + " save <kitName>");
            sender.sendMessage(prefix + "/" + label + " delete <kitName>");
            sender.sendMessage(prefix + "/" + label + " load <kitName>");
            sender.sendMessage(prefix + "/" + label + " force <player> <kitName>");

            return true;
        } else if (args[0].equalsIgnoreCase("list")) {
            List<String> list = getKits();

            if (list.size() == 0) {
                sender.sendMessage(prefix + "There're no kits yet. Try '/invh save <name>'");
                return true;
            }

            StringBuilder builder = new StringBuilder();

            for (int i = 0; i < list.size(); i ++) {
                String name = list.get(i);
                builder.append(name);

                if (i + 1 < list.size()) {
                    builder.append(", ");
                }
            }

            sender.sendMessage(prefix + "There're " + list.size() + " kits: §e" + builder.toString());

            return true;
        } else if (args[0].equalsIgnoreCase("save") && args.length == 2) {

            if (!(sender instanceof Player)) {
                sender.sendMessage(prefix + "It's the command for online players.");
                return true;
            }

            Player player = (Player) sender;

            if (!player.hasPermission("inventoryholder.admin")) {
                player.sendMessage(prefix + "You don't have the permission.");
                return true;
            }

            String name = args[1];

            if (exist(name)) {
                player.sendMessage(prefix + name + " is already taken name, please try another one.");
                return true;
            }

            try {
                save(player.getInventory(), name);

                player.sendMessage(prefix + "The kit has been saved successfully!");
            } catch (IOException e) {
                e.printStackTrace();
            }

            return true;
        } else if (args[0].equalsIgnoreCase("delete") && args.length == 2) {

            if (!sender.hasPermission("inventoryholder.admin")) {
                sender.sendMessage(prefix + "You don't have the permission.");
                return true;
            }

            String name = args[1];

            if (!exist(name)) {
                sender.sendMessage(prefix + "There's no kit named " + name + ".");
                return true;
            }

            File file = new File(getDataFolder(), name + ".yml");
            boolean isDeleted = file.delete();

            if (isDeleted) {
                sender.sendMessage(prefix + "The kit has been deleted successfully.");
            } else {
                sender.sendMessage(prefix + "Failed to delete the file. Please check if other program is using it now.");
            }

            return true;
        } else if (args[0].equalsIgnoreCase("load") && args.length == 2) {

            if (!(sender instanceof Player)) {
                sender.sendMessage(prefix + "It's the command for online players.");
                return true;
            }

            Player player = (Player) sender;

            if (!player.hasPermission("inventoryholder.load")) {
                player.sendMessage(prefix + "You don't have the permission.");
                return true;
            }

            String name = args[1];

            if (!exist(name)) {
                player.sendMessage(prefix + "There's no kit named " + name + ".");
                return true;
            }

            load(player.getInventory(), name);

            player.sendMessage(prefix + "The kit §e" + name + "§r has been loaded to your inventory.");

            return true;
        } else if (args[0].equalsIgnoreCase("force") && args.length == 3) {

            if (!sender.hasPermission("inventoryholder.admin")) {
                sender.sendMessage(prefix + "You don't have the permission.");
                return true;
            }

            Player target = getPlayer(args[1]);

            if (target == null) {
                sender.sendMessage(prefix + args[1] + " is not online now.");
                return true;
            }

            String name = args[2];

            if (!exist(name)) {
                sender.sendMessage(prefix + "There's no kit named " + name + ".");
                return true;
            }

            load(target.getInventory(), name);

            sender.sendMessage(prefix + "The kit §e" + name + "§r has been forced to §e" + target.getName() + "§r.");

            return true;
        } else {
            sender.sendMessage(prefix + "That's an invalid command.");

            return true;
        }
    }

    private Player getPlayer(String name) {
        for (Player player : getServer().getOnlinePlayers()) {
            if (player.getName().equals(name)) {
                return player;
            }
        }

        return null;
    }
}
