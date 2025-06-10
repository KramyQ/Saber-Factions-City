package com.massivecraft.factions.zcore.persist.db;

import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.util.Logger;
import com.massivecraft.factions.zcore.persist.MemoryBoard;
import org.bukkit.World;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class DatabaseBoard extends MemoryBoard {

    @Override
    public boolean load() {
        try {
            loadBoardFromDatabase();
            Logger.print("Loaded " + flocationIds.size() + " board locations from database", Logger.PrefixType.DEFAULT);
            return true;
        } catch (Exception e) {
            Logger.print("Failed to load board from database: " + e.getMessage(), Logger.PrefixType.FAILED);
            e.printStackTrace();
            return false;
        }
    }

    private void loadBoardFromDatabase() throws SQLException {
        String sql = "SELECT world_name, chunk_x, chunk_z, faction_id FROM board";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            flocationIds.clear();

            while (rs.next()) {
                String worldName = rs.getString("world_name");
                int chunkX = rs.getInt("chunk_x");
                int chunkZ = rs.getInt("chunk_z");
                String factionId = rs.getString("faction_id");

                FLocation flocation = FLocation.wrap(worldName, chunkX, chunkZ);
                flocationIds.put(flocation, factionId);
            }
        }
    }

    @Override
    public void setIdAt(String id, FLocation flocation) {
        String currentId = getIdAt(flocation);
        if (currentId.equals(id)) {
            return;
        }

        clearOwnershipAt(flocation);

        if (id.equals("0")) {
            removeAt(flocation);
            return;
        }

        // Update in memory
        flocationIds.put(flocation, id);

        // Update in database
        saveClaim(flocation, id);
    }

    @Override
    public void removeAt(FLocation flocation) {
        super.removeAt(flocation); // This handles in-memory removal and player notifications

        // Remove from database
        deleteClaim(flocation);
    }

    private void saveClaim(FLocation flocation, String factionId) {
        String sql = """
            INSERT INTO board (world_name, chunk_x, chunk_z, faction_id)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (world_name, chunk_x, chunk_z)
            DO UPDATE SET faction_id = EXCLUDED.faction_id
            """;

        CompletableFuture.runAsync(() -> {
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, flocation.getWorldName());
                stmt.setInt(2, flocation.getIntX());
                stmt.setInt(3, flocation.getIntZ());
                stmt.setString(4, factionId);

                stmt.executeUpdate();

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    private void deleteClaim(FLocation flocation) {
        String sql = "DELETE FROM board WHERE world_name = ? AND chunk_x = ? AND chunk_z = ?";

        CompletableFuture.runAsync(() -> {
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, flocation.getWorldName());
                stmt.setInt(2, flocation.getIntX());
                stmt.setInt(3, flocation.getIntZ());

                stmt.executeUpdate();

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void unclaimAll(String factionId) {
        super.unclaimAll(factionId); // This handles in-memory cleanup

        // Remove from database
        String sql = "DELETE FROM board WHERE faction_id = ?";
        CompletableFuture.runAsync(() -> {
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, factionId);
                stmt.executeUpdate();

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void unclaimAllInWorld(String factionId, World world) {
        super.unclaimAllInWorld(factionId, world); // This handles in-memory cleanup

        // Remove from database
        String sql = "DELETE FROM board WHERE faction_id = ? AND world_name = ?";
        CompletableFuture.runAsync(() -> {
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, factionId);
                stmt.setString(2, world.getName());
                stmt.executeUpdate();

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public Set<FLocation> getAllClaims(String factionId) {
        // For performance, we'll use the in-memory cache for this operation
        // since it's called frequently for territory checks
        return super.getAllClaims(factionId);
    }

    @Override
    public void clean() {
        super.clean(); // This handles in-memory cleanup

        // Clean database of invalid claims
        String sql = """
            DELETE FROM board 
            WHERE faction_id NOT IN (SELECT id FROM factions)
            AND faction_id NOT IN ('0', '-1', '-2')
            """;

        CompletableFuture.runAsync(() -> {
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                int deletedRows = stmt.executeUpdate();
                if (deletedRows > 0) {
                    Logger.print("Board cleaner removed " + deletedRows + " orphaned claims from database", Logger.PrefixType.DEFAULT);
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void forceSave() {
        forceSave(true);
    }

    @Override
    public void forceSave(boolean sync) {
        if (sync) {
            saveAllClaims();
        } else {
            CompletableFuture.runAsync(this::saveAllClaims);
        }
    }

    private void saveAllClaims() {
        // For database implementation, we save claims immediately when they're set
        // So this method is mainly for ensuring consistency
        String deleteSql = "DELETE FROM board";
        String insertSql = "INSERT INTO board (world_name, chunk_x, chunk_z, faction_id) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                deleteStmt.executeUpdate();
            }

            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                for (var entry : flocationIds.entrySet()) {
                    FLocation location = entry.getKey();
                    String factionId = entry.getValue();

                    insertStmt.setString(1, location.getWorldName());
                    insertStmt.setInt(2, location.getIntX());
                    insertStmt.setInt(3, location.getIntZ());
                    insertStmt.setString(4, factionId);
                    insertStmt.addBatch();
                }
                insertStmt.executeBatch();
            }

            conn.commit();
            conn.setAutoCommit(true);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void convertFrom(MemoryBoard old) {
        // Not needed for database implementation
        throw new UnsupportedOperationException("Database implementation does not support conversion from memory");
    }

    // Batch operations for better performance
    public void setClaimsBatch(Set<FLocation> locations, String factionId) {
        String sql = """
            INSERT INTO board (world_name, chunk_x, chunk_z, faction_id)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (world_name, chunk_x, chunk_z)
            DO UPDATE SET faction_id = EXCLUDED.faction_id
            """;

        CompletableFuture.runAsync(() -> {
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                conn.setAutoCommit(false);

                for (FLocation location : locations) {
                    // Update in memory first
                    flocationIds.put(location, factionId);

                    // Add to batch
                    stmt.setString(1, location.getWorldName());
                    stmt.setInt(2, location.getIntX());
                    stmt.setInt(3, location.getIntZ());
                    stmt.setString(4, factionId);
                    stmt.addBatch();
                }

                stmt.executeBatch();
                conn.commit();
                conn.setAutoCommit(true);

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void removeClaimsBatch(Set<FLocation> locations) {
        String sql = "DELETE FROM board WHERE world_name = ? AND chunk_x = ? AND chunk_z = ?";

        CompletableFuture.runAsync(() -> {
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                conn.setAutoCommit(false);

                for (FLocation location : locations) {
                    // Remove from memory first
                    flocationIds.remove(location);

                    // Add to batch
                    stmt.setString(1, location.getWorldName());
                    stmt.setInt(2, location.getIntX());
                    stmt.setInt(3, location.getIntZ());
                    stmt.addBatch();
                }

                stmt.executeBatch();
                conn.commit();
                conn.setAutoCommit(true);

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
}