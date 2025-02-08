package me.lokka30.compasstohome;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class CompassToHome extends JavaPlugin implements Listener {

    // Stores the system time for each player of when they use the compass.
    // Used for cooldowns.
    final HashMap<UUID, Long> cooldownMap = new HashMap<>();
    final boolean isOneNine = isOneNine();

    @Override
    public void onEnable() {
        // Make sure the config is saved to the data folder.
        saveDefaultConfig();

        // Register all event handlers.
        getServer().getPluginManager().registerEvents(this, this);
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInteract(final PlayerInteractEvent event) {
        // Variables we'll use later.
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();

        // Ensure it's a right click
        switch (event.getAction()) {
            case RIGHT_CLICK_AIR:
            case RIGHT_CLICK_BLOCK:
                break;
            default:
                return;
        }

        // Ensure they're using the correct item type.
        ItemStack itemStack = event.getItem();
        if (!event.hasItem() || itemStack == null) {
            if (isOneNine) {
                itemStack = player.getInventory().getItemInMainHand();
                if (isItemStackAir(itemStack)) {
                    itemStack = player.getInventory().getItemInOffHand();
                }
            } else {
                itemStack = player.getItemInHand();
            }
        }
        if (isItemStackAir(itemStack)) {
            return;
        }
        if (itemStack.getType() != Material.valueOf(getConfig().getString("material", "COMPASS"))) {
            return;
        }

        // Check the cooldown map.
        final double cooldown = getConfig().getDouble("cooldown", 0L);

        // if the cooldown is 0 seconds then just ignore that system entirely.
        if (cooldown != 0.0) {
            if (cooldownMap.containsKey(uuid)) {
                final double timeRemaining = cooldown - TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - cooldownMap.get(uuid)
                );
                if (timeRemaining > 0.0d) {
                    player.sendMessage(colorize(
                            getConfig()
                                    .getString("cooldown-message", "Please wait %time%s.")
                                    .replace("%time%", Double.toString(timeRemaining))
                    ));
                    return;
                }
            }
            cooldownMap.put(uuid, System.currentTimeMillis());
        }

        // Teleport the player.
        Location to = player.getBedSpawnLocation();
        if (to == null) {
            to = player.getWorld().getSpawnLocation();
        }
        player.teleport(to);

        // Send a message!
        player.sendMessage(colorize(
                getConfig().getString("teleport-message", "Whoosh!")
                        .replace("%x%", Integer.toString(player.getLocation().getBlockX()))
                        .replace("%y%", Integer.toString(player.getLocation().getBlockY()))
                        .replace("%z%", Integer.toString(player.getLocation().getBlockZ()))
                        .replace("%world%", player.getLocation().getWorld().getName())
        ));

        // Play a sound!
        if (getConfig().getBoolean("teleport-sound.enabled", true)) {
            try {
                player.playSound(
                        to,
                        Sound.valueOf(getConfig().getString("teleport-sound.id", "ENTITY_ENDERMAN_TELEPORT")),
                        (float) getConfig().getDouble("teleport-sound.volume"),
                        (float) getConfig().getDouble("teleport-sound.pitch")
                );
            } catch (IllegalArgumentException ex) {
                getLogger().info("Please change the teleport sound ID in config.yml. The sound you are currently using is not available in your Minecraft version. You can alternatively disable the teleport sound if you wish.");
            }
        }

        // Run commands
        final List<String> playerCommandsToRun = getConfig().getStringList("player-commands-to-run");
        playerCommandsToRun
            .stream()
            .map(str -> str.substring(1)) // ignore starting slash in config
            .map(str -> str.replace("%player-name%", player.getName()))
            .forEach(player::performCommand);

        final List<String> serverCommandsToRun = getConfig().getStringList("server-commands-to-run");
        serverCommandsToRun
            .stream()
            .map(str -> str.substring(1)) // ignore starting slash in config
            .map(str -> str.replace("%player-name%", player.getName()))
            .forEach(cmd -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd));
    }

    // Converts the color codes in a message.
    private String colorize(final String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    // Check if an itemstack is air or nothing
    private boolean isItemStackAir(ItemStack is) {
        return is == null || is.getType() == Material.AIR;
    }

    private boolean isOneNine() {
        try {
            Material.valueOf("SHIELD");
        } catch (IllegalArgumentException ex) {
            return false;
        }
        return true;
    }
}
