package me.elsiff.inventoryholder;

import com.google.common.base.Preconditions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
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

        if (args.length < 2 ) {
            list.add("list");

            if (sender.hasPermission("inventoryholder.save")) {
                list.add("save");
            }

            if (sender.hasPermission("inventoryholder.load")) {
                list.add("load");
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

        config.set("storage", inv.getStorageContents());
        config.set("armor", inv.getArmorContents());
        config.set("extra", inv.getExtraContents());

        config.save(file);
    }

    public void load(PlayerInventory inv, String name) {
        File file = new File(getDataFolder(), name + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        ItemStack[] storage = null, armor = null, extra = null;

        if (config.contains("storage"))
            storage = (ItemStack[]) config.get("storage");

        if (config.contains("armor"))
            armor = (ItemStack[]) config.get("armor");

        if (config.contains("extra"))
            extra = (ItemStack[]) config.get("extra");

        inv.setStorageContents(storage != null ? storage : new ItemStack[inv.getStorageContents().length]);
        inv.setArmorContents(armor != null ? armor : new ItemStack[inv.getArmorContents().length]);
        inv.setExtraContents(extra != null ? extra : new ItemStack[inv.getExtraContents().length]);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length < 1 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(prefix + "§3> ===== §b§lInventoryHolder §bv" + getDescription().getVersion() + "§3 ===== <");
            sender.sendMessage(prefix + "/" + label + " help");
            sender.sendMessage(prefix + "/" + label + " list");
            sender.sendMessage(prefix + "/" + label + " save <kitName>");
            sender.sendMessage(prefix + "/" + label + " load <kitName>");
            sender.sendMessage(prefix + "/" + label + " force <player> <kitName>");

            return true;
        } else if (args[0].equalsIgnoreCase("list")) {
            File[] files = getDataFolder().listFiles();
            Preconditions.checkNotNull(files);

            if (files.length == 0) {
                sender.sendMessage(prefix + "There's no kit yet. Try '/invh save <name>'");
                return true;
            }

            StringBuilder builder = new StringBuilder();

            for (int i = 0; i < files.length; i ++) {
                String name = files[i].getName();
                builder.append(name);

                if (i + 1 < files.length) {
                    builder.append(", ");
                }
            }

            sender.sendMessage(prefix + "There're " + files.length + " kits: §e" + builder.toString());

            return true;
        } else if (args[0].equalsIgnoreCase("save") && args.length == 2) {

            if (!(sender instanceof Player)) {
                sender.sendMessage(prefix + "It's the command for online players.");
                return true;
            }

            Player player = (Player) sender;

            if (!player.hasPermission("inventoryholder.save")) {
                player.sendMessage(prefix + "You don't have the permission.");
                return true;
            }

            String name = args[1];

            if (exist(name)) {
                player.sendMessage(prefix + name + " is already taken name, please try other one.");
                return true;
            }

            try {
                save(player.getInventory(), name);

                player.sendMessage(prefix + "Your inventory has been saved successfully as the kit §e" + name + "§r!");
            } catch (IOException e) {
                e.printStackTrace();
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

            player.sendMessage(prefix + "The kit " + name + " has been loaded to your inventory.");

            return true;
        } else if (args[0].equalsIgnoreCase("force") && args.length == 3) {

            if (!sender.hasPermission("inventoryholder.force")) {
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

            sender.sendMessage(prefix + "The kit " + name + " has been loaded to the inventory of " + target.getName() + ".");

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
