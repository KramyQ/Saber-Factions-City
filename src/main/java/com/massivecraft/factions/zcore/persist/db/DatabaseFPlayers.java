package com.massivecraft.factions.zcore.persist.db;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.util.Logger;
import com.massivecraft.factions.zcore.persist.MemoryFPlayers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class DatabaseFPlayers extends MemoryFPlayers {

    @Override
    public void load(Consumer<Boolean> finish) {
        CompletableFuture.runAsync(() -> {
            try {
                loadFPlayersFromDatabase();
                Logger.print("Loaded " + fPlayers.size() + " players from database", Logger.PrefixType.DEFAULT);
                finish.accept(true);
            } catch (Exception e) {
                Logger.print("Failed to load players from database: " + e.getMessage(), Logger.PrefixType.FAILED);
                e.printStackTrace();
                finish.accept(false);
            }
        });
    }

    private void loadFPlayersFromDatabase() throws SQLException {
        String sql = """
            SELECT id, name, faction_id, role, title, power, power_boost, last_power_update_time,
                   last_login_time, chat_mode, ignore_alliance_chat, monitor_joins, spying_chat,
                   show_scoreboard, kills, deaths, will_auto_leave, map_height, is_flying,
                   is_auto_flying, is_alt, entering_password, entering_password_warp,
                   last_stood_at_world, last_stood_at_x, last_stood_at_z, map_auto_updating,
                   auto_claim_for, auto_unclaim_for, login_pvp_disabled, last_frostwalker_message,
                   should_take_fall_damage, is_stealth_enabled, notifications_enabled,
                   titles_enabled, seeing_chunk, inspect_mode, friendly_fire, enemies_nearby,
                   in_chest, in_vault, is_admin_bypassing, warmup_type, warmup_task_id,
                   command_cooldowns
            FROM fplayers
            """;

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                DatabaseFPlayer fPlayer = new DatabaseFPlayer();
                fPlayer.loadFromResultSet(rs);
                fPlayers.put(fPlayer.getId(), fPlayer);
            }
        }
    }

    @Override
    public void forceSave() {
        forceSave(true);
    }

    @Override
    public void forceSave(boolean sync) {
        if (sync) {
            saveAllFPlayers();
        } else {
            CompletableFuture.runAsync(this::saveAllFPlayers);
        }
    }

    private void saveAllFPlayers() {
        for (FPlayer fPlayer : fPlayers.values()) {
            if (fPlayer instanceof DatabaseFPlayer) {
                DatabaseFPlayer dbFPlayer = (DatabaseFPlayer) fPlayer;
                if (dbFPlayer.shouldBeSaved()) {
                    dbFPlayer.save();
                }
            }
        }
    }

    @Override
    public DatabaseFPlayer generateFPlayer(String id) {
        DatabaseFPlayer player = new DatabaseFPlayer(id);
        this.fPlayers.put(player.getId(), player);
        return player;
    }

    @Override
    public void convertFrom(MemoryFPlayers old) {
        // Not needed for database implementation
        throw new UnsupportedOperationException("Database implementation does not support conversion from memory");
    }
}