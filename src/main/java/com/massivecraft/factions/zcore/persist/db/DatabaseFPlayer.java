package com.massivecraft.factions.zcore.persist.db;

import com.google.gson.reflect.TypeToken;
import com.massivecraft.factions.*;
import com.massivecraft.factions.struct.ChatMode;
import com.massivecraft.factions.struct.Role;
import com.massivecraft.factions.util.WarmUpUtil;
import com.massivecraft.factions.zcore.persist.MemoryFPlayer;

import java.sql.*;
import java.util.HashMap;

public class DatabaseFPlayer extends MemoryFPlayer {
    private boolean needsSave = false;

    public DatabaseFPlayer() {
        super();
    }

    public DatabaseFPlayer(String id) {
        super(id);
        this.needsSave = true;
    }

    // Load player data from ResultSet
    public void loadFromResultSet(ResultSet rs) throws SQLException {
        this.id = rs.getString("id");
        this.name = rs.getString("name");
        this.factionId = rs.getString("faction_id");

        String roleStr = rs.getString("role");
        this.role = roleStr != null ? Role.fromString(roleStr) : Role.NORMAL;

        this.title = rs.getString("title");
        this.power = rs.getDouble("power");
        this.powerBoost = rs.getDouble("power_boost");
        this.lastPowerUpdateTime = rs.getLong("last_power_update_time");
        this.lastLoginTime = rs.getLong("last_login_time");

        String chatModeStr = rs.getString("chat_mode");
        this.chatMode = chatModeStr != null ? ChatMode.valueOf(chatModeStr) : ChatMode.PUBLIC;

        this.ignoreAllianceChat = rs.getBoolean("ignore_alliance_chat");
        this.monitorJoins = rs.getBoolean("monitor_joins");
        this.spyingChat = rs.getBoolean("spying_chat");
        this.showScoreboard = rs.getBoolean("show_scoreboard");
        this.kills = rs.getInt("kills");
        this.deaths = rs.getInt("deaths");
        this.willAutoLeave = rs.getBoolean("will_auto_leave");
        this.mapHeight = rs.getInt("map_height");
        this.isFlying = rs.getBoolean("is_flying");
        this.isAutoFlying = rs.getBoolean("is_auto_flying");
        this.isAlt = rs.getBoolean("is_alt");
        this.enteringPassword = rs.getBoolean("entering_password");
        this.enteringPasswordWarp = rs.getString("entering_password_warp");

        // Load last stood at location
        String lastStoodWorld = rs.getString("last_stood_at_world");
        if (lastStoodWorld != null) {
            int lastStoodX = rs.getInt("last_stood_at_x");
            int lastStoodZ = rs.getInt("last_stood_at_z");
            this.lastStoodAt = FLocation.wrap(lastStoodWorld, lastStoodX, lastStoodZ);
        } else {
            this.lastStoodAt = FLocation.empty();
        }

        this.mapAutoUpdating = rs.getBoolean("map_auto_updating");

        // Load auto claim/unclaim factions
        String autoClaimForId = rs.getString("auto_claim_for");
        if (autoClaimForId != null) {
            this.autoClaimFor = Factions.getInstance().getFactionById(autoClaimForId);
        }

        String autoUnclaimForId = rs.getString("auto_unclaim_for");
        if (autoUnclaimForId != null) {
            this.autoUnclaimFor = Factions.getInstance().getFactionById(autoUnclaimForId);
        }

        this.loginPvpDisabled = rs.getBoolean("login_pvp_disabled");
        this.lastFrostwalkerMessage = rs.getLong("last_frostwalker_message");
        this.shouldTakeFallDamage = rs.getBoolean("should_take_fall_damage");
        this.isStealthEnabled = rs.getBoolean("is_stealth_enabled");
        this.notificationsEnabled = rs.getBoolean("notifications_enabled");
        this.titlesEnabled = rs.getBoolean("titles_enabled");
        this.seeingChunk = rs.getBoolean("seeing_chunk");
        this.inspectMode = rs.getBoolean("inspect_mode");
        this.friendlyFire = rs.getBoolean("friendly_fire");
        this.enemiesNearby = rs.getBoolean("enemies_nearby");
        this.inChest = rs.getBoolean("in_chest");
        this.inVault = rs.getBoolean("in_vault");
        this.isAdminBypassing = rs.getBoolean("is_admin_bypassing");

        // Load warmup data
        String warmupTypeStr = rs.getString("warmup_type");
        if (warmupTypeStr != null) {
            this.warmup = WarmUpUtil.Warmup.valueOf(warmupTypeStr);
            this.warmupTask = rs.getInt("warmup_task_id");
        }

        // Load command cooldowns
        String cooldownsData = rs.getString("command_cooldowns");
        if (cooldownsData != null && !cooldownsData.isEmpty()) {
            try {
                this.commandCooldown = FactionsPlugin.getInstance().getGson()
                    .fromJson(cooldownsData, new TypeToken<HashMap<String, Long>>(){}.getType());
            } catch (Exception e) {
                e.printStackTrace();
                this.commandCooldown = new HashMap<>();
            }
        } else {
            this.commandCooldown = new HashMap<>();
        }

        this.needsSave = false;
    }

    // Save player to database
    public void save() {
        if (!needsSave && id != null) {
            return; // No changes to save
        }

        String sql = """
            INSERT INTO fplayers (
                id, name, faction_id, role, title, power, power_boost, last_power_update_time,
                last_login_time, chat_mode, ignore_alliance_chat, monitor_joins, spying_chat,
                show_scoreboard, kills, deaths, will_auto_leave, map_height, is_flying,
                is_auto_flying, is_alt, entering_password, entering_password_warp,
                last_stood_at_world, last_stood_at_x, last_stood_at_z, map_auto_updating,
                auto_claim_for, auto_unclaim_for, login_pvp_disabled, last_frostwalker_message,
                should_take_fall_damage, is_stealth_enabled, notifications_enabled,
                titles_enabled, seeing_chunk, inspect_mode, friendly_fire, enemies_nearby,
                in_chest, in_vault, is_admin_bypassing, warmup_type, warmup_task_id,
                command_cooldowns, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            ON CONFLICT (id) DO UPDATE SET
                name = EXCLUDED.name,
                faction_id = EXCLUDED.faction_id,
                role = EXCLUDED.role,
                title = EXCLUDED.title,
                power = EXCLUDED.power,
                power_boost = EXCLUDED.power_boost,
                last_power_update_time = EXCLUDED.last_power_update_time,
                last_login_time = EXCLUDED.last_login_time,
                chat_mode = EXCLUDED.chat_mode,
                ignore_alliance_chat = EXCLUDED.ignore_alliance_chat,
                monitor_joins = EXCLUDED.monitor_joins,
                spying_chat = EXCLUDED.spying_chat,
                show_scoreboard = EXCLUDED.show_scoreboard,
                kills = EXCLUDED.kills,
                deaths = EXCLUDED.deaths,
                will_auto_leave = EXCLUDED.will_auto_leave,
                map_height = EXCLUDED.map_height,
                is_flying = EXCLUDED.is_flying,
                is_auto_flying = EXCLUDED.is_auto_flying,
                is_alt = EXCLUDED.is_alt,
                entering_password = EXCLUDED.entering_password,
                entering_password_warp = EXCLUDED.entering_password_warp,
                last_stood_at_world = EXCLUDED.last_stood_at_world,
                last_stood_at_x = EXCLUDED.last_stood_at_x,
                last_stood_at_z = EXCLUDED.last_stood_at_z,
                map_auto_updating = EXCLUDED.map_auto_updating,
                auto_claim_for = EXCLUDED.auto_claim_for,
                auto_unclaim_for = EXCLUDED.auto_unclaim_for,
                login_pvp_disabled = EXCLUDED.login_pvp_disabled,
                last_frostwalker_message = EXCLUDED.last_frostwalker_message,
                should_take_fall_damage = EXCLUDED.should_take_fall_damage,
                is_stealth_enabled = EXCLUDED.is_stealth_enabled,
                notifications_enabled = EXCLUDED.notifications_enabled,
                titles_enabled = EXCLUDED.titles_enabled,
                seeing_chunk = EXCLUDED.seeing_chunk,
                inspect_mode = EXCLUDED.inspect_mode,
                friendly_fire = EXCLUDED.friendly_fire,
                enemies_nearby = EXCLUDED.enemies_nearby,
                in_chest = EXCLUDED.in_chest,
                in_vault = EXCLUDED.in_vault,
                is_admin_bypassing = EXCLUDED.is_admin_bypassing,
                warmup_type = EXCLUDED.warmup_type,
                warmup_task_id = EXCLUDED.warmup_task_id,
                command_cooldowns = EXCLUDED.command_cooldowns,
                updated_at = CURRENT_TIMESTAMP
            """;

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            int i = 1;
            stmt.setString(i++, id);
            stmt.setString(i++, name);
            stmt.setString(i++, factionId);
            stmt.setString(i++, role != null ? role.name() : Role.NORMAL.name());
            stmt.setString(i++, title);
            stmt.setDouble(i++, power);
            stmt.setDouble(i++, powerBoost);
            stmt.setLong(i++, lastPowerUpdateTime);
            stmt.setLong(i++, lastLoginTime);
            stmt.setString(i++, chatMode != null ? chatMode.name() : ChatMode.PUBLIC.name());
            stmt.setBoolean(i++, ignoreAllianceChat);
            stmt.setBoolean(i++, monitorJoins);
            stmt.setBoolean(i++, spyingChat);
            stmt.setBoolean(i++, showScoreboard);
            stmt.setInt(i++, kills);
            stmt.setInt(i++, deaths);
            stmt.setBoolean(i++, willAutoLeave);
            stmt.setInt(i++, mapHeight);
            stmt.setBoolean(i++, isFlying);
            stmt.setBoolean(i++, isAutoFlying);
            stmt.setBoolean(i++, isAlt);
            stmt.setBoolean(i++, enteringPassword);
            stmt.setString(i++, enteringPasswordWarp);

            // Last stood at location
            if (lastStoodAt != null && !lastStoodAt.equals(FLocation.empty())) {
                stmt.setString(i++, lastStoodAt.getWorldName());
                stmt.setInt(i++, lastStoodAt.getIntX());
                stmt.setInt(i++, lastStoodAt.getIntZ());
            } else {
                stmt.setNull(i++, Types.VARCHAR); // world
                stmt.setNull(i++, Types.INTEGER); // x
                stmt.setNull(i++, Types.INTEGER); // z
            }

            stmt.setBoolean(i++, mapAutoUpdating);
            stmt.setString(i++, autoClaimFor != null ? autoClaimFor.getId() : null);
            stmt.setString(i++, autoUnclaimFor != null ? autoUnclaimFor.getId() : null);
            stmt.setBoolean(i++, loginPvpDisabled);
            stmt.setLong(i++, lastFrostwalkerMessage);
            stmt.setBoolean(i++, shouldTakeFallDamage);
            stmt.setBoolean(i++, isStealthEnabled);
            stmt.setBoolean(i++, notificationsEnabled);
            stmt.setBoolean(i++, titlesEnabled);
            stmt.setBoolean(i++, seeingChunk);
            stmt.setBoolean(i++, inspectMode);
            stmt.setBoolean(i++, friendlyFire);
            stmt.setBoolean(i++, enemiesNearby);
            stmt.setBoolean(i++, inChest);
            stmt.setBoolean(i++, inVault);
            stmt.setBoolean(i++, isAdminBypassing);
            stmt.setString(i++, warmup != null ? warmup.name() : null);
            stmt.setInt(i++, warmupTask);
            stmt.setString(i, commandCooldown != null ?
                FactionsPlugin.getInstance().getGson().toJson(commandCooldown) : null);

            stmt.executeUpdate();
            this.needsSave = false;

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Delete player from database
    public void delete() {
        if (id == null) return;

        String sql = "DELETE FROM fplayers WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void remove() {
        delete();
        ((DatabaseFPlayers) FPlayers.getInstance()).fPlayers.remove(getId());
    }

    // Override methods that should trigger save
    @Override
    public void setFaction(Faction faction, boolean alt) {
        super.setFaction(faction, alt);
        this.needsSave = true;
    }

    @Override
    public void setRole(Role role) {
        super.setRole(role);
        this.needsSave = true;
    }

    @Override
    public void setTitle(org.bukkit.command.CommandSender sender, String title) {
        super.setTitle(sender, title);
        this.needsSave = true;
    }

    @Override
    public void alterPower(double delta) {
        super.alterPower(delta);
        this.needsSave = true;
    }

    @Override
    public void setPowerBoost(double powerBoost) {
        super.setPowerBoost(powerBoost);
        this.needsSave = true;
    }

    @Override
    public void setLastLoginTime(long lastLoginTime) {
        super.setLastLoginTime(lastLoginTime);
        this.needsSave = true;
    }

    @Override
    public void setChatMode(ChatMode chatMode) {
        super.setChatMode(chatMode);
        this.needsSave = true;
    }

    @Override
    public void setIgnoreAllianceChat(boolean ignore) {
        super.setIgnoreAllianceChat(ignore);
        this.needsSave = true;
    }

    @Override
    public void setSpyingChat(boolean chatSpying) {
        super.setSpyingChat(chatSpying);
        this.needsSave = true;
    }

    @Override
    public void setShowScoreboard(boolean show) {
        super.setShowScoreboard(show);
        this.needsSave = true;
    }

    @Override
    public void setAutoLeave(boolean willLeave) {
        super.setAutoLeave(willLeave);
        this.needsSave = true;
    }

    @Override
    public void setMapHeight(int height) {
        super.setMapHeight(height);
        this.needsSave = true;
    }

    @Override
    public void setFlying(boolean fly) {
        super.setFlying(fly);
        this.needsSave = true;
    }

    @Override
    public void setAutoFlying(boolean autoFly) {
        super.setAutoFlying(autoFly);
        this.needsSave = true;
    }

    @Override
    public void setAlt(boolean alt) {
        super.setAlt(alt);
        this.needsSave = true;
    }

    @Override
    public void setLastStoodAt(FLocation flocation) {
        super.setLastStoodAt(flocation);
        this.needsSave = true;
    }

    @Override
    public void setAutoClaimFor(Faction faction) {
        super.setAutoClaimFor(faction);
        this.needsSave = true;
    }

    @Override
    public void setAutoUnclaimFor(Faction faction) {
        super.setAutoUnclaimFor(faction);
        this.needsSave = true;
    }

    @Override
    public void setIsAdminBypassing(boolean val) {
        super.setIsAdminBypassing(val);
        this.needsSave = true;
    }

    @Override
    public void setStealth(boolean stealthToggle) {
        super.setStealth(stealthToggle);
        this.needsSave = true;
    }

    @Override
    public void setNotificationsEnabled(boolean enabled) {
        super.setNotificationsEnabled(enabled);
        this.needsSave = true;
    }

    @Override
    public void setTitlesEnabled(Boolean b) {
        super.setTitlesEnabled(b);
        this.needsSave = true;
    }

    @Override
    public void setSeeingChunk(boolean seeingChunk) {
        super.setSeeingChunk(seeingChunk);
        this.needsSave = true;
    }

    @Override
    public void setInspectMode(boolean status) {
        super.setInspectMode(status);
        this.needsSave = true;
    }

    @Override
    public void setFriendlyFire(boolean status) {
        super.setFriendlyFire(status);
        this.needsSave = true;
    }

    @Override
    public void setEnemiesNearby(Boolean b) {
        super.setEnemiesNearby(b);
        this.needsSave = true;
    }

    @Override
    public void setInFactionsChest(boolean b) {
        super.setInFactionsChest(b);
        this.needsSave = true;
    }

    @Override
    public void setInVault(boolean status) {
        super.setInVault(status);
        this.needsSave = true;
    }

    @Override
    public void setCooldown(String cmd, long cooldown) {
        super.setCooldown(cmd, cooldown);
        this.needsSave = true;
    }

    @Override
    public void addWarmup(WarmUpUtil.Warmup warmup, int taskId) {
        super.addWarmup(warmup, taskId);
        this.needsSave = true;
    }

    @Override
    public void stopWarmup() {
        super.stopWarmup();
        this.needsSave = true;
    }

    @Override
    public void clearWarmup() {
        super.clearWarmup();
        this.needsSave = true;
    }
}