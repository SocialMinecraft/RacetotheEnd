package club.somc.endrace;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages player data for the plugin.
 */
public class PlayerDataManager {
    private final JavaPlugin plugin;
    private final File dataFile;
    private final File finishDataFile;

    private final HashMap<UUID, PlayerData> playerData = new HashMap<>();

    /**
     * Constructor for the PlayerDataManager.
     * @param plugin The main plugin class
     */
    public PlayerDataManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "playerData.csv");
        this.finishDataFile = new File(plugin.getDataFolder(), "finishedPlayerData.csv");
    }

    /**
     * Get the player data for a specific player.
     * @param playerId The UUID of the player
     * @return The player data
     */
    public PlayerData getPlayerData(UUID playerId) {
        
        return playerData.getOrDefault(playerId, new PlayerDataManager.PlayerData());
    }

    /**
     * Get the player data map.
     * @return The player data map
     */
    public  HashMap<UUID, PlayerData> getPlayerData() {
        return playerData;
    }

    /**
     * Load player data from the data file.
     */
    public void loadPlayerData() {
        if (!dataFile.exists()) {
            plugin.getLogger().info("No player data file found.");
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(dataFile.toURI()))) {
            String line;
            reader.readLine();  // Skip header
            while ((line = reader.readLine()) != null) {
                plugin.getLogger().info("Reading line.");
                String[] parts = line.split(",");
                UUID playerId = UUID.fromString(parts[0]);
                PlayerData data = new PlayerData();
                data.totalPlayTime = Long.parseLong(parts[1]);
                data.finished = Boolean.parseBoolean(parts[2]);
                playerData.put(playerId, data);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Save player data to the data file.
     */
    public void savePlayerData() {
        if (!dataFile.getParentFile().exists()) {
            dataFile.getParentFile().mkdirs();
        }

        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(dataFile.toURI()))) {

            writer.write("playerId,totalPlayTime,finished\n");
            
            for (Map.Entry<UUID, PlayerData> entry : playerData.entrySet()) {
                UUID playerId = entry.getKey();
                PlayerData data = entry.getValue();
                writer.write(String.format("%s,%d,%b\n", playerId, data.totalPlayTime, data.finished));
            }
            plugin.getLogger().info("Player data saved to " + dataFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Save player data to the data file.
     * @param playerId The UUID of the player
     * @param totalTime The total play time of the player in milliseconds
     * @param finished Whether the player has finished the game
     */
    public void savePlayerData(UUID playerId, long totalTime, boolean finished) {
        if (!dataFile.getParentFile().exists()) {
            dataFile.getParentFile().mkdirs();
        }
        
        // Load existing data
        Map<UUID, PlayerData> existingData = new HashMap<>();
        if (dataFile.exists()) {
            try (BufferedReader reader = Files.newBufferedReader(Paths.get(dataFile.toURI()))) {
                String line;
                reader.readLine(); // Skip header
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",");
                    UUID id = UUID.fromString(parts[0]);
                    PlayerData data = new PlayerData();
                    data.totalPlayTime = Long.parseLong(parts[1]);
                    data.finished = Boolean.parseBoolean(parts[2]);
                    existingData.put(id, data);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Update the player's data
        PlayerData playerData = existingData.getOrDefault(playerId, new PlayerData());
        playerData.totalPlayTime = totalTime;
        playerData.finished = finished;
        existingData.put(playerId, playerData);

        // Write updated data back to the file
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(dataFile.toURI()))) {
            writer.write("playerId,totalPlayTime,finished\n");
            for (Map.Entry<UUID, PlayerData> entry : existingData.entrySet()) {
                UUID id = entry.getKey();
                PlayerData data = entry.getValue();
                writer.write(String.format("%s,%d,%b\n", id, data.totalPlayTime, data.finished));
            }
            plugin.getLogger().info("Player data updated for " + playerId);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Save player finish data to the finish data file.
     * @param playerId The UUID of the player
     * @param playerName The name of the player
     * @param totalTime The total play time of the player in milliseconds
     */
    public void savePlayerFinishData(UUID playerId, String playerName, long totalTime, long playtime) {
        if (!finishDataFile.getParentFile().exists()) {
            finishDataFile.getParentFile().mkdirs();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(finishDataFile, true))) {
            writer.write(String.format("%s,%s,%d,%d\n", playerId, playerName, totalTime, playtime));
            plugin.getLogger().info("Player finish data saved for " + playerName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Update the total play time for all players.
     */
    public void updatePlayerTotalPlayTime() {
        long currentTime = System.currentTimeMillis();
        for (Map.Entry<UUID, PlayerData> entry : getPlayerData().entrySet()) {
            PlayerData data = entry.getValue();
            if (data.finished) {
                continue;
            }
            if (data.lastLoginTime <= 0) continue;
            long sessionTime = currentTime - data.lastLoginTime;
            data.totalPlayTime += sessionTime;
            data.lastLoginTime = currentTime;
        }
    }


    /**
     * Data structure to store player data.
     */
    public static class PlayerData {
        long lastLoginTime = 0;
        long totalPlayTime = 0;
        boolean finished = false;
    }
}