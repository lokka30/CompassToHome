package me.lokka30.compasstohome;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class CompassToHome extends JavaPlugin implements Listener {

    // Stores the system time for each player of when they use the compass.
    // Used for cooldowns.
    final HashMap<UUID, Long> cooldownMap = new HashMap<>();

    @Override
    public void onEnable() {
        // Make sure the config is saved to the data folder.
        saveDefaultConfig();

        // Register all event handlers.
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onInteract(final PlayerInteractEvent event) {
        // Ensure they're using the correct items.
        if(!event.hasItem()) return;
        if(event.getItem().getType() != Material.valueOf(getConfig().getString("material", "COMPASS"))) return;
        if(!event.getAction().toString().startsWith("RIGHT_CLICK")) return;

        // Variables we'll use later.
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();

        // Check the cooldown map.
        final double cooldown = getConfig().getDouble("cooldown", 0.0d);

        // if the cooldown is 0 seconds then just ignore that system entirely.
        if(cooldown != 0.0) {
            if(cooldownMap.containsKey(uuid)) {
                final long timeSinceLastTp = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - cooldownMap.get(uuid));
                final double timeRemaining = cooldown - timeSinceLastTp;
                if(timeRemaining != 0.0d) {
                    player.sendMessage(colorize(
                            getConfig().getString("cooldown-message", "Please wait %time%s.")
                                    .replace("%time%", Double.toString(timeRemaining))
                    ));
                    return;
                }
            }
            cooldownMap.put(uuid, System.currentTimeMillis());
        }

        // Teleport the player.
        Location to = player.getBedSpawnLocation();
        if(to == null) { to = player.getWorld().getSpawnLocation(); }
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
        if(getConfig().getBoolean("teleport-sound.enabled", true)) {
            try {
                player.playSound(
                        to,
                        Sound.valueOf(getConfig().getString("teleport-sound.id", "ENTITY_ENDERMAN_TELEPORT")),
                        (float) getConfig().getDouble("teleport-sound.volume"),
                        (float) getConfig().getDouble("teleport-sound.pitch")
                );
            } catch(IllegalArgumentException ex) {
                getLogger().info("Please change the teleport sound ID in config.yml. The sound you are currently using is not available in your Minecraft version. You can alternatively disable the teleport sound if you wish.");
            }
        }
    }

    // Converts the color codes in a message.
    private String colorize(final String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}
