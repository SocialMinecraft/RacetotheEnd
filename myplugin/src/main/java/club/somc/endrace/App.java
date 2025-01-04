package club.somc.endrace;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;



public class App extends JavaPlugin implements Listener {

   private PlayerDataManager playerDataManager;
   private EndPortalHandler endPortalHandler;
    
   /**
    * Called when the plugin is enabled.
    */
    @Override
    public void onEnable() {
        getLogger().info("Hello World! 2.0");
        this.playerDataManager = new PlayerDataManager(this);
        this.endPortalHandler = new EndPortalHandler(this, new HashSet<UUID>(), playerDataManager);
        getServer().getPluginManager().registerEvents(this, this);
        playerDataManager.loadPlayerData();
        endPortalHandler.startTimerTask();
        getLogger().info("Done loading player data!");
    }

    /**
     * Called when the plugin is disabled.
     */
    @Override
    public void onDisable() {
        getLogger().info("Race to the End has been disabled!");
        playerDataManager.updatePlayerTotalPlayTime();
        playerDataManager.savePlayerData();
    }

    /**
     * Called when the world is saved.
     * @param event The WorldSaveEvent
     */
    @EventHandler
    public void onWorldSave(WorldSaveEvent event) {
        getLogger().info("World is saving, saving player data...");
        playerDataManager.updatePlayerTotalPlayTime();
        playerDataManager.savePlayerData();
    }

    /**
     * Called when a player joins the server.
     * @param event The PlayerJoinEvent
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        PlayerDataManager.PlayerData data = playerDataManager.getPlayerData(playerId);
        data.lastLoginTime = System.currentTimeMillis();
        playerDataManager.getPlayerData().put(playerId, data);
        setupScoreboard(event.getPlayer());
    }

    /**
     * Called when a player quits the server.
     * @param event The PlayerQuitEvent
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        PlayerDataManager.PlayerData data = playerDataManager.getPlayerData(playerId);
        if (data != null && !data.finished) {
            long sessionTime = System.currentTimeMillis() - data.lastLoginTime;
            data.totalPlayTime += sessionTime;
            data.lastLoginTime = 0;
            playerDataManager.getPlayerData().put(playerId, data);
        }
    }

    /**
     * Check if the player is on the ground or in the air.
     * @param event The PlayerMoveEvent
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        endPortalHandler.handlePlayerMove(event);
    }


    /**
     * Setup the timer for a player.
     * @param player The player to setup the timer for.
     */
    private void setupScoreboard(Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard board = manager.getNewScoreboard();
        Objective objective = board.registerNewObjective("timer", "dummy", "Playtime");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        player.setScoreboard(board);
    
        // Update the scoreboard with the player's total playtime
        UUID playerId = player.getUniqueId();
        PlayerDataManager.PlayerData data = playerDataManager.getPlayerData(playerId);
        if (data != null) {
            String formattedTime = formatTime(data.totalPlayTime);
            if (data.finished) {
                objective.setDisplayName("§a" + formattedTime); // Green text for finished players
            } else {
                objective.setDisplayName(formattedTime);
            }
        }
    }

    /**
     * Format the time in milliseconds to a human-readable format.
     * @param millis The time in milliseconds.
     * @return The formatted time string.
     */
    public String formatTime(long millis) {
        long days = TimeUnit.MILLISECONDS.toDays(millis);
        long hours = TimeUnit.MILLISECONDS.toHours(millis) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;

        if (days > 0) {
            return String.format("%dd %dh %dm", days, hours, minutes);
            /*return String.format("%d day%s, %d hour%s, %d minute%s",
                                 days, days > 1 ? "s" : "",
                                 hours, hours > 1 ? "s" : "",
                                 minutes, minutes > 1 ? "s" : "");*/
        }
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
            /*return String.format("%d hour%s, %d minute%s, %d second%s",
                                 hours, hours > 1 ? "s" : "",
                                 minutes, minutes > 1 ? "s" : "",
                                 seconds, seconds > 1 ? "s" : "");*/
        }
        if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
            /*return String.format("%d minute%s, %d second%s",
                                 minutes, minutes > 1 ? "s" : "",
                                 seconds, seconds > 1 ? "s" : "");*/
        }
        return String.format("%d second%s", seconds, seconds > 1 ? "s" : "");
    }

    
}
