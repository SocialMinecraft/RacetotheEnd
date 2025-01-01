package club.somc.endrace;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Bukkit;

import java.util.Set;
import java.util.UUID;

/**
 * Handles the logic for when a player touches the end portal.
 */
public class EndPortalHandler {
    private final Set<UUID> playersTouchedEndPortal;
    private final PlayerDataManager playerDataManager;
    private final App plugin;

    /**
     * Constructor for the EndPortalHandler.
     * @param plugin The main plugin class
     * @param playersTouchedEndPortal The set of players that have touched the end portal
     * @param playerDataManager The PlayerDataManager instance
     */
    public EndPortalHandler(App plugin, Set<UUID> playersTouchedEndPortal, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.playersTouchedEndPortal = playersTouchedEndPortal;
        this.playerDataManager = playerDataManager;
    }

    /**
     * Handle the player move event when a player touches the end portal.
     * @param event The PlayerMoveEvent
     */
    public void handlePlayerMove(PlayerMoveEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        // Check if the player is touching an end portal
        if (event.getTo().getBlock().getType() == Material.END_PORTAL) {
            System.out.println("Hello");
            if (!playersTouchedEndPortal.contains(playerId)) {
                playersTouchedEndPortal.add(playerId);
                PlayerDataManager.PlayerData data = playerDataManager.getPlayerData(playerId);
                if (data != null && !data.finished) {
                    int requiredEyes = 8;
                    int playerEyes = 0;
                    for (ItemStack itemStack : event.getPlayer().getInventory().all(Material.ENDER_EYE).values()) {
                        playerEyes += itemStack.getAmount();
                    }
                    if (playerEyes >= requiredEyes) {
                        data.finished = true;
                        long sessionTime = System.currentTimeMillis() - data.lastLoginTime;
                        data.totalPlayTime += sessionTime;
                        String formattedTime = plugin.formatTime(data.totalPlayTime);
                        data.lastLoginTime = System.currentTimeMillis();
                        plugin.getServer().broadcastMessage(event.getPlayer().getName() + " has reached the end, and it took them\n" + formattedTime + " of playtime.");
                        
                        // Update the scoreboard to show the final time in green
                        Player player = event.getPlayer();
                        Objective objective = player.getScoreboard().getObjective(DisplaySlot.SIDEBAR);
                        if (objective != null) {
                            objective.setDisplayName("§a" + formattedTime); // Set the display name with green color
                        }

                        // Save player finish data
                        playerDataManager.savePlayerData(playerId, data.totalPlayTime, true);
                        playerDataManager.savePlayerFinishData(playerId, player.getName(), data.totalPlayTime);
                    } else {
                        int neededEyes = requiredEyes - playerEyes;
                        event.getPlayer().sendMessage("You have " + playerEyes + " eyes of ender\nYou need " + neededEyes + " more eyes of ender to finish.");
                    }
                }
            }
        } else {
            playersTouchedEndPortal.remove(playerId);
        }
    }

    /**
     * Start the timer task to update the scoreboard every second.
     */
    public void startTimerTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID playerId : playerDataManager.getPlayerData().keySet()) {
                    PlayerDataManager.PlayerData data = playerDataManager.getPlayerData().get(playerId);
                    if (data != null && !data.finished) {
                        long currentTime = System.currentTimeMillis();
                        long sessionTime = currentTime - data.lastLoginTime;
                        long totalTime = data.totalPlayTime + sessionTime;
                        String formattedTime = plugin.formatTime(totalTime);

                        // Update the scoreboard for the player
                        Player player = Bukkit.getPlayer(playerId);
                        if (player != null) {
                            Objective objective = player.getScoreboard().getObjective(DisplaySlot.SIDEBAR);
                            if (objective != null) {
                                objective.setDisplayName(formattedTime);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Update every second (20 ticks)
    }
}