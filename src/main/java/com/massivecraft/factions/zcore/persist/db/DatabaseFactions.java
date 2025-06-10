package com.massivecraft.factions.zcore.persist.db;

import com.massivecraft.factions.Faction;
import com.massivecraft.factions.util.Logger;
import com.massivecraft.factions.zcore.persist.MemoryFactions;
import com.massivecraft.factions.zcore.util.TL;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class DatabaseFactions extends MemoryFactions {

    @Override
    public void load(Consumer<Boolean> success) {
        CompletableFuture.runAsync(() -> {
            try {
                loadFactionsFromDatabase();

                // Ensure default factions exist
                ensureDefaultFactions();

                Logger.print("Loaded " + factions.size() + " factions from database", Logger.PrefixType.DEFAULT);
                success.accept(true);
            } catch (Exception e) {
                Logger.print("Failed to load factions from database: " + e.getMessage(), Logger.PrefixType.FAILED);
                e.printStackTrace();
                success.accept(false);
            }
        });
    }

    private void loadFactionsFromDatabase() throws SQLException {
        String sql = """
            SELECT id, tag, description, is_open, is_peaceful, peaceful_explosions_enabled, 
                   is_permanent, permanent_power, power_boost, money, founded_date, 
                   last_player_logged_off_time, home_world, home_x, home_y, home_z, home_pitch, home_yaw,
                   vault_world, vault_x, vault_y, vault_z, vault_pitch, vault_yaw,
                   checkpoint_world, checkpoint_x, checkpoint_y, checkpoint_z, checkpoint_pitch, checkpoint_yaw,
                   max_vaults, default_role, tnt, tnt_bank_limit, warp_limit, reinforced_armor,
                   paypal, discord, player_focused, last_death, strikes, points,
                   wall_check_minutes, buffer_check_minutes, wee_woo, allowed_spawner_chunks,
                   is_protected, banner_data, chest_data, permissions_data, spawner_chunks_data,
                   completed_missions_data, checks_data, player_wall_check_count_data, 
                   player_buffer_check_count_data
            FROM factions
            """;

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                DatabaseFaction faction = new DatabaseFaction();
                faction.loadFromResultSet(rs);
                factions.put(faction.getId(), faction);
                updateNextIdForId(faction.getId());
            }
        }

        // Load additional data for each faction
        for (Faction faction : factions.values()) {
            if (faction instanceof DatabaseFaction) {
                DatabaseFaction dbFaction = (DatabaseFaction) faction;
                dbFaction.loadRelatedData();
            }
        }
    }

    private void ensureDefaultFactions() {
        // Wilderness
        if (!factions.containsKey("0")) {
            DatabaseFaction wilderness = new DatabaseFaction("0");
            wilderness.setTag(TL.WILDERNESS.toString());
            wilderness.setDescription(TL.WILDERNESS_DESCRIPTION.toString());
            factions.put("0", wilderness);
            wilderness.save();
        }

        // SafeZone
        if (!factions.containsKey("-1")) {
            DatabaseFaction safezone = new DatabaseFaction("-1");
            safezone.setTag(TL.SAFEZONE.toString());
            safezone.setDescription(TL.SAFEZONE_DESCRIPTION.toString());
            factions.put("-1", safezone);
            safezone.save();
        }

        // WarZone
        if (!factions.containsKey("-2")) {
            DatabaseFaction warzone = new DatabaseFaction("-2");
            warzone.setTag(TL.WARZONE.toString());
            warzone.setDescription(TL.WARZONE_DESCRIPTION.toString());
            factions.put("-2", warzone);
            warzone.save();
        }
    }

    @Override
    public Faction createFaction() {
        DatabaseFaction faction = new DatabaseFaction(getNextId());
        factions.put(faction.getId(), faction);
        faction.save();
        return faction;
    }

    @Override
    public void removeFaction(String id) {
        Faction faction = factions.remove(id);
        if (faction instanceof DatabaseFaction) {
            ((DatabaseFaction) faction).delete();
        }
        if (faction != null) {
            faction.remove();
        }
    }

    @Override
    public void forceSave() {
        forceSave(true);
    }

    @Override
    public void forceSave(boolean sync) {
        if (sync) {
            saveAllFactions();
        } else {
            CompletableFuture.runAsync(this::saveAllFactions);
        }
    }

    private void saveAllFactions() {
        for (Faction faction : factions.values()) {
            if (faction instanceof DatabaseFaction) {
                ((DatabaseFaction) faction).save();
            }
        }
    }

    @Override
    public Faction generateFactionObject() {
        String id = getNextId();
        DatabaseFaction faction = new DatabaseFaction(id);
        updateNextIdForId(id);
        return faction;
    }

    @Override
    public Faction generateFactionObject(String id) {
        return new DatabaseFaction(id);
    }

    @Override
    public void convertFrom(MemoryFactions old) {
        // Not needed for database implementation
        throw new UnsupportedOperationException("Database implementation does not support conversion from memory");
    }

    private String getNextId() {
        while (!isIdFree(this.nextId)) {
            this.nextId++;
        }
        return Integer.toString(this.nextId);
    }

    private boolean isIdFree(String id) {
        return !this.factions.containsKey(id);
    }

    private boolean isIdFree(int id) {
        return this.isIdFree(Integer.toString(id));
    }

    private synchronized void updateNextIdForId(int id) {
        if (this.nextId < id) this.nextId = id + 1;
    }

    private void updateNextIdForId(String id) {
        try {
            int idAsInt = Integer.parseInt(id);
            this.updateNextIdForId(idAsInt);
        } catch (NumberFormatException ignored) {}
    }
}