package com.massivecraft.factions.zcore.persist.db;

import com.google.gson.reflect.TypeToken;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.FactionsPlugin;
import com.massivecraft.factions.missions.Mission;
import com.massivecraft.factions.struct.Relation;
import com.massivecraft.factions.struct.Role;
import com.massivecraft.factions.util.FastChunk;
import com.massivecraft.factions.util.LazyLocation;
import com.massivecraft.factions.zcore.fperms.Access;
import com.massivecraft.factions.zcore.fperms.Permissable;
import com.massivecraft.factions.zcore.fperms.PermissableAction;
import com.massivecraft.factions.zcore.persist.MemoryFaction;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DatabaseFaction extends MemoryFaction {
    private boolean needsSave = false;

    public DatabaseFaction() {
        super();
    }

    public DatabaseFaction(String id) {
        super(id);
        this.needsSave = true;
    }

    // Load faction data from ResultSet
    public void loadFromResultSet(ResultSet rs) throws SQLException {
        this.id = rs.getString("id");
        this.tag = rs.getString("tag");
        this.description = rs.getString("description");
        this.open = rs.getBoolean("is_open");
        this.peaceful = rs.getBoolean("is_peaceful");
        this.peacefulExplosionsEnabled = rs.getBoolean("peaceful_explosions_enabled");
        this.permanent = rs.getBoolean("is_permanent");

        int permanentPowerValue = rs.getInt("permanent_power");
        this.permanentPower = rs.wasNull() ? null : permanentPowerValue;

        this.powerBoost = rs.getDouble("power_boost");
        this.money = rs.getDouble("money");
        this.foundedDate = rs.getLong("founded_date");
        this.lastPlayerLoggedOffTime = rs.getLong("last_player_logged_off_time");

        // Load home location
        String homeWorld = rs.getString("home_world");
        if (homeWorld != null) {
            this.home = new LazyLocation(
                    homeWorld,
                    rs.getDouble("home_x"),
                    rs.getDouble("home_y"),
                    rs.getDouble("home_z"),
                    rs.getFloat("home_pitch"),
                    rs.getFloat("home_yaw")
            );
        }

        // Load vault location
        String vaultWorld = rs.getString("vault_world");
        if (vaultWorld != null) {
            this.vault = new LazyLocation(
                    vaultWorld,
                    rs.getDouble("vault_x"),
                    rs.getDouble("vault_y"),
                    rs.getDouble("vault_z"),
                    rs.getFloat("vault_pitch"),
                    rs.getFloat("vault_yaw")
            );
        }

        // Load checkpoint location
        String checkpointWorld = rs.getString("checkpoint_world");
        if (checkpointWorld != null) {
            this.checkpoint = new Location(
                    Bukkit.getWorld(checkpointWorld),
                    rs.getDouble("checkpoint_x"),
                    rs.getDouble("checkpoint_y"),
                    rs.getDouble("checkpoint_z"),
                    rs.getFloat("checkpoint_pitch"),
                    rs.getFloat("checkpoint_yaw")
            );
        }

        this.maxVaults = rs.getInt("max_vaults");
        this.defaultRole = Role.fromString(rs.getString("default_role"));
        this.tnt = rs.getLong("tnt");
        this.tntBankSize = rs.getLong("tnt_bank_limit");
        this.warpLimit = rs.getInt("warp_limit");
        this.reinforcedArmor = rs.getDouble("reinforced_armor");
        this.paypal = rs.getString("paypal");
        this.discord = rs.getString("discord");
        this.player = rs.getString("player_focused");
        this.lastDeath = rs.getLong("last_death");
        this.strikes = rs.getInt("strikes");
        this.points = rs.getInt("points");
        this.wallCheckMinutes = rs.getInt("wall_check_minutes");
        this.bufferCheckMinutes = rs.getInt("buffer_check_minutes");
        this.weeWoo = rs.getBoolean("wee_woo");
        this.allowedSpawnerChunks = rs.getInt("allowed_spawner_chunks");
        this.protectedfac = rs.getBoolean("is_protected");

        // Load JSON data
        loadJsonData(rs);

        this.needsSave = false;
    }

    private void loadJsonData(ResultSet rs) throws SQLException {
        // Load banner data
        String bannerData = rs.getString("banner_data");
        if (bannerData != null && !bannerData.isEmpty()) {
            try {
                this.bannerSerialized = FactionsPlugin.getInstance().getGson()
                        .fromJson(bannerData, new TypeToken<Map<String, Object>>() {
                        }.getType());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Load permissions data
        String permissionsData = rs.getString("permissions_data");
        if (permissionsData != null && !permissionsData.isEmpty()) {
            try {
                this.permissions = FactionsPlugin.getInstance().getGson()
                        .fromJson(permissionsData, new TypeToken<Map<Permissable, Map<PermissableAction, Access>>>() {
                        }.getType());
            } catch (Exception e) {
                e.printStackTrace();
                resetPerms(); // Fallback to default permissions
            }
        } else {
            resetPerms();
        }

        // Load spawner chunks data
        String spawnerChunksData = rs.getString("spawner_chunks_data");
        if (spawnerChunksData != null && !spawnerChunksData.isEmpty()) {
            try {
                this.spawnerChunks = FactionsPlugin.getInstance().getGson()
                        .fromJson(spawnerChunksData, new TypeToken<Set<FastChunk>>() {
                        }.getType());
            } catch (Exception e) {
                e.printStackTrace();
                this.spawnerChunks = new HashSet<>();
            }
        } else {
            this.spawnerChunks = new HashSet<>();
        }

        // Load completed missions data
        String completedMissionsData = rs.getString("completed_missions_data");
        if (completedMissionsData != null && !completedMissionsData.isEmpty()) {
            try {
                this.completedMissions = FactionsPlugin.getInstance().getGson()
                        .fromJson(completedMissionsData, new TypeToken<List<String>>() {
                        }.getType());
            } catch (Exception e) {
                e.printStackTrace();
                this.completedMissions = new ArrayList<>();
            }
        } else {
            this.completedMissions = new ArrayList<>();
        }

        // Load checks data
        String checksData = rs.getString("checks_data");
        if (checksData != null && !checksData.isEmpty()) {
            try {
                this.checks = FactionsPlugin.getInstance().getGson()
                        .fromJson(checksData, new TypeToken<Map<Long, String>>() {
                        }.getType());
            } catch (Exception e) {
                e.printStackTrace();
                this.checks = new ConcurrentHashMap<>();
            }
        } else {
            this.checks = new ConcurrentHashMap<>();
        }

        // Load player wall check count data
        String playerWallCheckData = rs.getString("player_wall_check_count_data");
        if (playerWallCheckData != null && !playerWallCheckData.isEmpty()) {
            try {
                this.playerWallCheckCount = FactionsPlugin.getInstance().getGson()
                        .fromJson(playerWallCheckData, new TypeToken<Map<UUID, Integer>>() {
                        }.getType());
            } catch (Exception e) {
                e.printStackTrace();
                this.playerWallCheckCount = new ConcurrentHashMap<>();
            }
        } else {
            this.playerWallCheckCount = new ConcurrentHashMap<>();
        }

        // Load player buffer check count data
        String playerBufferCheckData = rs.getString("player_buffer_check_count_data");
        if (playerBufferCheckData != null && !playerBufferCheckData.isEmpty()) {
            try {
                this.playerBufferCheckCount = FactionsPlugin.getInstance().getGson()
                        .fromJson(playerBufferCheckData, new TypeToken<Map<UUID, Integer>>() {
                        }.getType());
            } catch (Exception e) {
                e.printStackTrace();
                this.playerBufferCheckCount = new ConcurrentHashMap<>();
            }
        } else {
            this.playerBufferCheckCount = new ConcurrentHashMap<>();
        }
    }

    // Load related data from other tables
    public void loadRelatedData() {
        loadRelations();
        loadInvites();
        loadWarps();
        loadAnnouncements();
        loadClaimOwnership();
        loadUpgrades();
        loadMissions();
        loadRules();
    }

    private void loadRelations() {
        this.relationWish = new HashMap<>();
        String sql = "SELECT other_faction_id, relation FROM faction_relations WHERE faction_id = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, this.id);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String otherFactionId = rs.getString("other_faction_id");
                    Relation relation = Relation.fromString(rs.getString("relation"));
                    this.relationWish.put(otherFactionId, relation);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        // Insert current missions
        if (missions != null && !missions.isEmpty()) {
            String insertSql = "INSERT INTO faction_missions (faction_id, mission_name, mission_data) VALUES (?, ?, ?)";
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(insertSql)) {

                for (Map.Entry<String, Mission> entry : missions.entrySet()) {
                    stmt.setString(1, id);
                    stmt.setString(2, entry.getKey());
                    stmt.setString(3, FactionsPlugin.getInstance().getGson().toJson(entry.getValue()));
                    stmt.addBatch();
                }
                stmt.executeBatch();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveRules() {
        // Delete existing rules
        String deleteSql = "DELETE FROM faction_rules WHERE faction_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
            stmt.setString(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        // Insert current rules
        if (rules != null && !rules.isEmpty()) {
            String insertSql = "INSERT INTO faction_rules (faction_id, rule_index, rule_text) VALUES (?, ?, ?)";
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(insertSql)) {

                for (Map.Entry<Integer, String> entry : rules.entrySet()) {
                    stmt.setString(1, id);
                    stmt.setInt(2, entry.getKey());
                    stmt.setString(3, entry.getValue());
                    stmt.addBatch();
                }
                stmt.executeBatch();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // Delete faction from database
    public void delete() {
        if (id == null) return;

        String sql = "DELETE FROM factions WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Override methods that should trigger save
    @Override
    public void setTag(String str) {
        super.setTag(str);
        this.needsSave = true;
    }

    @Override
    public void setDescription(String value) {
        super.setDescription(value);
        this.needsSave = true;
    }

    @Override
    public void setOpen(boolean isOpen) {
        super.setOpen(isOpen);
        this.needsSave = true;
    }

    @Override
    public void setPeaceful(boolean isPeaceful) {
        super.setPeaceful(isPeaceful);
        this.needsSave = true;
    }

    @Override
    public void setPermanent(boolean isPermanent) {
        super.setPermanent(isPermanent);
        this.needsSave = true;
    }

    @Override
    public void setPowerBoost(double powerBoost) {
        super.setPowerBoost(powerBoost);
        this.needsSave = true;
    }

    @Override
    public void setFactionBalance(double money) {
        super.setFactionBalance(money);
        this.needsSave = true;
    }

    @Override
    public void setHome(Location home) {
        super.setHome(home);
        this.needsSave = true;
    }

    @Override
    public void deleteHome() {
        super.deleteHome();
        this.needsSave = true;
    }

    @Override
    public void setVault(Location vaultLocation) {
        super.setVault(vaultLocation);
        this.needsSave = true;
    }

    @Override
    public void setCheckpoint(Location location) {
        super.setCheckpoint(location);
        this.needsSave = true;
    }

    @Override
    public void addRule(String rule) {
        super.addRule(rule);
        this.needsSave = true;
    }

    @Override
    public void removeRule(int index) {
        super.removeRule(index);
        this.needsSave = true;
    }

    @Override
    public void clearRules() {
        super.clearRules();
        this.needsSave = true;
    }

    @Override
    public void setUpgrade(String upgrade, int level) {
        super.setUpgrade(upgrade, level);
        this.needsSave = true;
    }

    @Override
    public void setWarp(String name, LazyLocation loc) {
        super.setWarp(name, loc);
        this.needsSave = true;
    }

    @Override
    public boolean removeWarp(String name) {
        boolean result = super.removeWarp(name);
        if (result) {
            this.needsSave = true;
        }
        return result;
    }

    @Override
    public void setWarpPassword(String warp, String password) {
        super.setWarpPassword(warp, password);
        this.needsSave = true;
    }

    @Override
    public void invite(FPlayer fplayer) {
        super.invite(fplayer);
        this.needsSave = true;
    }

    @Override
    public void deinvite(FPlayer fplayer) {
        super.deinvite(fplayer);
        this.needsSave = true;
    }

    @Override
    public void altInvite(FPlayer fplayer) {
        super.altInvite(fplayer);
        this.needsSave = true;
    }

    @Override
    public void deinviteAlt(FPlayer fplayer) {
        super.deinviteAlt(fplayer);
        this.needsSave = true;
    }

    @Override
    public void setRelationWish(Faction otherFaction, Relation relation) {
        super.setRelationWish(otherFaction, relation);
        this.needsSave = true;
    }

    @Override
    public void addAnnouncement(FPlayer fPlayer, String msg) {
        super.addAnnouncement(fPlayer, msg);
        this.needsSave = true;
    }

    @Override
    public void removeAnnouncements(FPlayer fPlayer) {
        super.removeAnnouncements(fPlayer);
        this.needsSave = true;
    }

    @Override
    public void setPlayerAsOwner(FPlayer player, FLocation loc) {
        super.setPlayerAsOwner(player, loc);
        this.needsSave = true;
    }

    @Override
    public void removePlayerAsOwner(FPlayer player, FLocation loc) {
        super.removePlayerAsOwner(player, loc);
        this.needsSave = true;
    }

    @Override
    public void clearClaimOwnership(FLocation loc) {
        super.clearClaimOwnership(loc);
        this.needsSave = true;
    }

    @Override
    public void clearAllClaimOwnership() {
        super.clearAllClaimOwnership();
        this.needsSave = true;
    }

    private void loadInvites() {
        // Clear existing invites
        super.invites.clear();
        super.altinvites.clear();
        String sql = "SELECT player_id, is_alt_invite FROM faction_invites WHERE faction_id = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, this.id);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String playerId = rs.getString("player_id");
                    boolean isAltInvite = rs.getBoolean("is_alt_invite");

                    if (isAltInvite) {
                        this.altinvites.add(playerId);
                    } else {
                        this.invites.add(playerId);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadWarps() {
        this.warps = new ConcurrentHashMap<>();
        this.warpPasswords = new ConcurrentHashMap<>();
        String sql = "SELECT warp_name, world_name, x, y, z, pitch, yaw, password FROM faction_warps WHERE faction_id = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, this.id);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String warpName = rs.getString("warp_name");
                    String worldName = rs.getString("world_name");

                    if (worldName != null) {
                        LazyLocation location = new LazyLocation(
                                worldName,
                                rs.getDouble("x"),
                                rs.getDouble("y"),
                                rs.getDouble("z"),
                                rs.getFloat("pitch"),
                                rs.getFloat("yaw")
                        );
                        this.warps.put(warpName, location);
                    }

                    String password = rs.getString("password");
                    if (password != null) {
                        this.warpPasswords.put(warpName.toLowerCase(), password);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadAnnouncements() {
        this.announcements = new HashMap<>();
        String sql = "SELECT player_id, message FROM faction_announcements WHERE faction_id = ? ORDER BY created_at";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, this.id);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String playerId = rs.getString("player_id");
                    String message = rs.getString("message");

                    this.announcements.computeIfAbsent(playerId, k -> new ArrayList<>()).add(message);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadClaimOwnership() {
        this.claimOwnership = new ConcurrentHashMap<>();
        String sql = "SELECT world_name, chunk_x, chunk_z, owner_id FROM faction_claim_ownership WHERE faction_id = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, this.id);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    FLocation location = FLocation.wrap(
                            rs.getString("world_name"),
                            rs.getInt("chunk_x"),
                            rs.getInt("chunk_z")
                    );
                    String ownerId = rs.getString("owner_id");

                    this.claimOwnership.computeIfAbsent(location, k -> new HashSet<>()).add(ownerId);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadUpgrades() {
        this.upgrades = new HashMap<>();
        String sql = "SELECT upgrade_name, level FROM faction_upgrades WHERE faction_id = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, this.id);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String upgradeName = rs.getString("upgrade_name");
                    int level = rs.getInt("level");
                    this.upgrades.put(upgradeName, level);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadMissions() {
        this.missions = new ConcurrentHashMap<>();
        String sql = "SELECT mission_name, mission_data FROM faction_missions WHERE faction_id = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, this.id);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String missionName = rs.getString("mission_name");
                    String missionData = rs.getString("mission_data");

                    try {
                        Mission mission = FactionsPlugin.getInstance().getGson()
                                .fromJson(missionData, Mission.class);
                        this.missions.put(missionName, mission);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadRules() {
        this.rules = new HashMap<>();
        String sql = "SELECT rule_index, rule_text FROM faction_rules WHERE faction_id = ? ORDER BY rule_index";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, this.id);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int ruleIndex = rs.getInt("rule_index");
                    String ruleText = rs.getString("rule_text");
                    this.rules.put(ruleIndex, ruleText);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Save faction to database
    public void save() {
        if (!needsSave && id != null) {
            return; // No changes to save
        }

        String sql = """
            
                INSERT INTO factions (
                id, tag, description, is_open, is_peaceful, peaceful_explosions_enabled,
                is_permanent, permanent_power, power_boost, money, founded_date,
                last_player_logged_off_time, home_world, home_x, home_y, home_z, home_pitch, home_yaw,
                vault_world, vault_x, vault_y, vault_z, vault_pitch, vault_yaw,
                checkpoint_world, checkpoint_x, checkpoint_y, checkpoint_z, checkpoint_pitch, checkpoint_yaw,
                max_vaults, default_role, tnt, tnt_bank_limit, warp_limit, reinforced_armor,
                paypal, discord, player_focused, last_death, strikes, points,
                wall_check_minutes, buffer_check_minutes, wee_woo, allowed_spawner_chunks,
                is_protected, banner_data, permissions_data, spawner_chunks_data,
                completed_missions_data, checks_data, player_wall_check_count_data,
                player_buffer_check_count_data, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            ON CONFLICT (id) DO UPDATE SET
                tag = EXCLUDED.tag,
                description = EXCLUDED.description,
                is_open = EXCLUDED.is_open,
                is_peaceful = EXCLUDED.is_peaceful,
                peaceful_explosions_enabled = EXCLUDED.peaceful_explosions_enabled,
                is_permanent = EXCLUDED.is_permanent,
                permanent_power = EXCLUDED.permanent_power,
                power_boost = EXCLUDED.power_boost,
                money = EXCLUDED.money,
                founded_date = EXCLUDED.founded_date,
                last_player_logged_off_time = EXCLUDED.last_player_logged_off_time,
                home_world = EXCLUDED.home_world,
                home_x = EXCLUDED.home_x,
                home_y = EXCLUDED.home_y,
                home_z = EXCLUDED.home_z,
                home_pitch = EXCLUDED.home_pitch,
                home_yaw = EXCLUDED.home_yaw,
                vault_world = EXCLUDED.vault_world,
                vault_x = EXCLUDED.vault_x,
                vault_y = EXCLUDED.vault_y,
                vault_z = EXCLUDED.vault_z,
                vault_pitch = EXCLUDED.vault_pitch,
                vault_yaw = EXCLUDED.vault_yaw,
                checkpoint_world = EXCLUDED.checkpoint_world,
                checkpoint_x = EXCLUDED.checkpoint_x,
                checkpoint_y = EXCLUDED.checkpoint_y,
                checkpoint_z = EXCLUDED.checkpoint_z,
                checkpoint_pitch = EXCLUDED.checkpoint_pitch,
                checkpoint_yaw = EXCLUDED.checkpoint_yaw,
                max_vaults = EXCLUDED.max_vaults,
                default_role = EXCLUDED.default_role,
                tnt = EXCLUDED.tnt,
                tnt_bank_limit = EXCLUDED.tnt_bank_limit,
                warp_limit = EXCLUDED.warp_limit,
                reinforced_armor = EXCLUDED.reinforced_armor,
                paypal = EXCLUDED.paypal,
                discord = EXCLUDED.discord,
                player_focused = EXCLUDED.player_focused,
                last_death = EXCLUDED.last_death,
                strikes = EXCLUDED.strikes,
                points = EXCLUDED.points,
                wall_check_minutes = EXCLUDED.wall_check_minutes,
                buffer_check_minutes = EXCLUDED.buffer_check_minutes,
                wee_woo = EXCLUDED.wee_woo,
                allowed_spawner_chunks = EXCLUDED.allowed_spawner_chunks,
                is_protected = EXCLUDED.is_protected,
                banner_data = EXCLUDED.banner_data,
                permissions_data = EXCLUDED.permissions_data,
                spawner_chunks_data = EXCLUDED.spawner_chunks_data,
                completed_missions_data = EXCLUDED.completed_missions_data,
                checks_data = EXCLUDED.checks_data,
                player_wall_check_count_data = EXCLUDED.player_wall_check_count_data,
                player_buffer_check_count_data = EXCLUDED.player_buffer_check_count_data,
                updated_at = CURRENT_TIMESTAMP
            """;

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            int i = 1;
            stmt.setString(i++, id);
            stmt.setString(i++, tag);
            stmt.setString(i++, description);
            stmt.setBoolean(i++, open);
            stmt.setBoolean(i++, peaceful);
            stmt.setBoolean(i++, peacefulExplosionsEnabled);
            stmt.setBoolean(i++, permanent);

            if (permanentPower != null) {
                stmt.setInt(i++, permanentPower);
            } else {
                stmt.setNull(i++, Types.INTEGER);
            }

            stmt.setDouble(i++, powerBoost);
            stmt.setDouble(i++, money);
            stmt.setLong(i++, foundedDate);
            stmt.setLong(i++, lastPlayerLoggedOffTime);

            // Home location
            if (home != null && home.getLocation() != null) {
                Location homeLoc = home.getLocation();
                stmt.setString(i++, homeLoc.getWorld().getName());
                stmt.setDouble(i++, homeLoc.getX());
                stmt.setDouble(i++, homeLoc.getY());
                stmt.setDouble(i++, homeLoc.getZ());
                stmt.setFloat(i++, homeLoc.getPitch());
                stmt.setFloat(i++, homeLoc.getYaw());
            } else {
                stmt.setNull(i++, Types.VARCHAR); // world
                stmt.setNull(i++, Types.DOUBLE);  // x
                stmt.setNull(i++, Types.DOUBLE);  // y
                stmt.setNull(i++, Types.DOUBLE);  // z
                stmt.setNull(i++, Types.REAL);    // pitch
                stmt.setNull(i++, Types.REAL);    // yaw
            }

            // Vault location
            if (vault != null && vault.getLocation() != null) {
                Location vaultLoc = vault.getLocation();
                stmt.setString(i++, vaultLoc.getWorld().getName());
                stmt.setDouble(i++, vaultLoc.getX());
                stmt.setDouble(i++, vaultLoc.getY());
                stmt.setDouble(i++, vaultLoc.getZ());
                stmt.setFloat(i++, vaultLoc.getPitch());
                stmt.setFloat(i++, vaultLoc.getYaw());
            } else {
                stmt.setNull(i++, Types.VARCHAR); // world
                stmt.setNull(i++, Types.DOUBLE);  // x
                stmt.setNull(i++, Types.DOUBLE);  // y
                stmt.setNull(i++, Types.DOUBLE);  // z
                stmt.setNull(i++, Types.REAL);    // pitch
                stmt.setNull(i++, Types.REAL);    // yaw
            }

            // Checkpoint location
            if (checkpoint != null) {
                stmt.setString(i++, checkpoint.getWorld().getName());
                stmt.setDouble(i++, checkpoint.getX());
                stmt.setDouble(i++, checkpoint.getY());
                stmt.setDouble(i++, checkpoint.getZ());
                stmt.setFloat(i++, checkpoint.getPitch());
                stmt.setFloat(i++, checkpoint.getYaw());
            } else {
                stmt.setNull(i++, Types.VARCHAR); // world
                stmt.setNull(i++, Types.DOUBLE);  // x
                stmt.setNull(i++, Types.DOUBLE);  // y
                stmt.setNull(i++, Types.DOUBLE);  // z
                stmt.setNull(i++, Types.REAL);    // pitch
                stmt.setNull(i++, Types.REAL);    // yaw
            }

            stmt.setInt(i++, maxVaults);
            stmt.setString(i++, defaultRole != null ? defaultRole.name() : Role.RECRUIT.name());
            stmt.setLong(i++, tnt);
            stmt.setLong(i++, tntBankSize);
            stmt.setInt(i++, warpLimit);
            stmt.setDouble(i++, reinforcedArmor);
            stmt.setString(i++, paypal);
            stmt.setString(i++, discord);
            stmt.setString(i++, player);
            stmt.setLong(i++, lastDeath);
            stmt.setInt(i++, strikes);
            stmt.setInt(i++, points);
            stmt.setInt(i++, wallCheckMinutes);
            stmt.setInt(i++, bufferCheckMinutes);
            stmt.setBoolean(i++, weeWoo);
            stmt.setInt(i++, allowedSpawnerChunks);
            stmt.setBoolean(i++, protectedfac);

            // JSON data
            stmt.setString(i++, bannerSerialized != null ?
                FactionsPlugin.getInstance().getGson().toJson(bannerSerialized) : null);
            stmt.setString(i++, permissions != null ?
                FactionsPlugin.getInstance().getGson().toJson(permissions) : null);
            stmt.setString(i++, spawnerChunks != null ?
                FactionsPlugin.getInstance().getGson().toJson(spawnerChunks) : null);
            stmt.setString(i++, completedMissions != null ?
                FactionsPlugin.getInstance().getGson().toJson(completedMissions) : null);
            stmt.setString(i++, checks != null ?
                FactionsPlugin.getInstance().getGson().toJson(checks) : null);
            stmt.setString(i++, playerWallCheckCount != null ?
                FactionsPlugin.getInstance().getGson().toJson(playerWallCheckCount) : null);
            stmt.setString(i, playerBufferCheckCount != null ?
                FactionsPlugin.getInstance().getGson().toJson(playerBufferCheckCount) : null);

            stmt.executeUpdate();

            // Save related data
            saveRelatedData();

            this.needsSave = false;

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void saveRelatedData() {
        saveRelations();
        saveInvites();
        saveWarps();
        saveAnnouncements();
        saveClaimOwnership();
        saveUpgrades();
        saveMissions();
        saveRules();
    }

    private void saveRelations() {
        // Delete existing relations
        String deleteSql = "DELETE FROM faction_relations WHERE faction_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
            stmt.setString(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        // Insert current relations
        if (relationWish != null && !relationWish.isEmpty()) {
            String insertSql = "INSERT INTO faction_relations (faction_id, other_faction_id, relation) VALUES (?, ?, ?)";
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(insertSql)) {

                for (Map.Entry<String, Relation> entry : relationWish.entrySet()) {
                    stmt.setString(1, id);
                    stmt.setString(2, entry.getKey());
                    stmt.setString(3, entry.getValue().name());
                    stmt.addBatch();
                }
                stmt.executeBatch();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveInvites() {
        // Delete existing invites
        String deleteSql = "DELETE FROM faction_invites WHERE faction_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
            stmt.setString(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        // Insert current invites
        String insertSql = "INSERT INTO faction_invites (faction_id, player_id, is_alt_invite) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertSql)) {

            if (invites != null) {
                for (String playerId : invites) {
                    stmt.setString(1, id);
                    stmt.setString(2, playerId);
                    stmt.setBoolean(3, false);
                    stmt.addBatch();
                }
            }

            if (altinvites != null) {
                for (String playerId : altinvites) {
                    stmt.setString(1, id);
                    stmt.setString(2, playerId);
                    stmt.setBoolean(3, true);
                    stmt.addBatch();
                }
            }

            stmt.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void saveWarps() {
        // Delete existing warps
        String deleteSql = "DELETE FROM faction_warps WHERE faction_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
            stmt.setString(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        // Insert current warps
        if (warps != null && !warps.isEmpty()) {
            String insertSql = "INSERT INTO faction_warps (faction_id, warp_name, world_name, x, y, z, pitch, yaw, password) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(insertSql)) {

                for (Map.Entry<String, LazyLocation> entry : warps.entrySet()) {
                    String warpName = entry.getKey();
                    LazyLocation location = entry.getValue();

                    if (location != null && location.getLocation() != null) {
                        Location loc = location.getLocation();

                        stmt.setString(1, id);
                        stmt.setString(2, warpName);
                        stmt.setString(3, loc.getWorld().getName());
                        stmt.setDouble(4, loc.getX());
                        stmt.setDouble(5, loc.getY());
                        stmt.setDouble(6, loc.getZ());
                        stmt.setFloat(7, loc.getPitch());
                        stmt.setFloat(8, loc.getYaw());

                        String password = warpPasswords != null ? warpPasswords.get(warpName.toLowerCase()) : null;
                        stmt.setString(9, password);

                        stmt.addBatch();
                    }
                }
                stmt.executeBatch();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveAnnouncements() {
        // Delete existing announcements
        String deleteSql = "DELETE FROM faction_announcements WHERE faction_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
            stmt.setString(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        // Insert current announcements
        if (announcements != null && !announcements.isEmpty()) {
            String insertSql = "INSERT INTO faction_announcements (faction_id, player_id, message) VALUES (?, ?, ?)";
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(insertSql)) {

                for (Map.Entry<String, List<String>> entry : announcements.entrySet()) {
                    String playerId = entry.getKey();
                    List<String> messages = entry.getValue();

                    if (messages != null) {
                        for (String message : messages) {
                            stmt.setString(1, id);
                            stmt.setString(2, playerId);
                            stmt.setString(3, message);
                            stmt.addBatch();
                        }
                    }
                }
                stmt.executeBatch();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveClaimOwnership() {
        // Delete existing claim ownership
        String deleteSql = "DELETE FROM faction_claim_ownership WHERE faction_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
            stmt.setString(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        // Insert current claim ownership
        if (claimOwnership != null && !claimOwnership.isEmpty()) {
            String insertSql = "INSERT INTO faction_claim_ownership (faction_id, world_name, chunk_x, chunk_z, owner_id) VALUES (?, ?, ?, ?, ?)";
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(insertSql)) {

                for (Map.Entry<FLocation, Set<String>> entry : claimOwnership.entrySet()) {
                    FLocation location = entry.getKey();
                    Set<String> owners = entry.getValue();

                    if (owners != null) {
                        for (String ownerId : owners) {
                            stmt.setString(1, id);
                            stmt.setString(2, location.getWorldName());
                            stmt.setInt(3, location.getIntX());
                            stmt.setInt(4, location.getIntZ());
                            stmt.setString(5, ownerId);
                            stmt.addBatch();
                        }
                    }
                }
                stmt.executeBatch();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveUpgrades() {
        // Delete existing upgrades
        String deleteSql = "DELETE FROM faction_upgrades WHERE faction_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
            stmt.setString(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        // Insert current upgrades
        if (upgrades != null && !upgrades.isEmpty()) {
            String insertSql = "INSERT INTO faction_upgrades (faction_id, upgrade_name, level) VALUES (?, ?, ?)";
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(insertSql)) {

                for (Map.Entry<String, Integer> entry : upgrades.entrySet()) {
                    stmt.setString(1, id);
                    stmt.setString(2, entry.getKey());
                    stmt.setInt(3, entry.getValue());
                    stmt.addBatch();
                }
                stmt.executeBatch();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveMissions() {
        // Delete existing missions
        String deleteSql = "DELETE FROM faction_missions WHERE faction_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
            stmt.setString(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    }