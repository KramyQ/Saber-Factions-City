package com.massivecraft.factions.zcore.persist.db;

import com.massivecraft.factions.FactionsPlugin;
import com.massivecraft.factions.util.Logger;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static DatabaseManager instance;
    private HikariDataSource dataSource;

    private DatabaseManager() {
        setupDatabase();
        createTables();
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private void setupDatabase() {
        FactionsPlugin plugin = FactionsPlugin.getInstance();

        String host = plugin.getConfig().getString("database.host", "localhost");
        int port = plugin.getConfig().getInt("database.port", 5432);
        String database = plugin.getConfig().getString("database.database", "factions");
        String username = plugin.getConfig().getString("database.username", "factions");
        String password = plugin.getConfig().getString("database.password", "password");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + database);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("org.postgresql.Driver");

        // Connection pool settings
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        this.dataSource = new HikariDataSource(config);

        Logger.print("Database connection pool initialized", Logger.PrefixType.DEFAULT);
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    private void createTables() {
        try (Connection conn = getConnection()) {
            createFactionsTable(conn);
            createFPlayersTable(conn);
            createBoardTable(conn);
            createFactionsRelationsTable(conn);
            createFactionsInvitesTable(conn);
            createFactionsWarpsTable(conn);
            createFactionsAnnouncementsTable(conn);
            createFactionsClaimOwnershipTable(conn);
            createFactionsUpgradesTable(conn);
            createFactionsMissionsTable(conn);
            createFactionsRulesTable(conn);
            Logger.print("Database tables created/verified", Logger.PrefixType.DEFAULT);
        } catch (SQLException e) {
            Logger.print("Failed to create database tables: " + e.getMessage(), Logger.PrefixType.FAILED);
            e.printStackTrace();
        }
    }

    private void createFactionsTable(Connection conn) throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS factions (
                id VARCHAR(255) PRIMARY KEY,
                tag VARCHAR(255) NOT NULL,
                description TEXT,
                is_open BOOLEAN DEFAULT false,
                is_peaceful BOOLEAN DEFAULT false,
                peaceful_explosions_enabled BOOLEAN DEFAULT false,
                is_permanent BOOLEAN DEFAULT false,
                permanent_power INTEGER,
                power_boost DOUBLE PRECISION DEFAULT 0.0,
                money DOUBLE PRECISION DEFAULT 0.0,
                founded_date BIGINT,
                last_player_logged_off_time BIGINT DEFAULT 0,
                home_world VARCHAR(255),
                home_x DOUBLE PRECISION,
                home_y DOUBLE PRECISION,
                home_z DOUBLE PRECISION,
                home_pitch REAL,
                home_yaw REAL,
                vault_world VARCHAR(255),
                vault_x DOUBLE PRECISION,
                vault_y DOUBLE PRECISION,
                vault_z DOUBLE PRECISION,
                vault_pitch REAL,
                vault_yaw REAL,
                checkpoint_world VARCHAR(255),
                checkpoint_x DOUBLE PRECISION,
                checkpoint_y DOUBLE PRECISION,
                checkpoint_z DOUBLE PRECISION,
                checkpoint_pitch REAL,
                checkpoint_yaw REAL,
                max_vaults INTEGER DEFAULT 0,
                default_role VARCHAR(50) DEFAULT 'RECRUIT',
                tnt BIGINT DEFAULT 0,
                tnt_bank_limit BIGINT DEFAULT 0,
                warp_limit INTEGER DEFAULT 0,
                reinforced_armor DOUBLE PRECISION DEFAULT 0.0,
                paypal VARCHAR(255),
                discord VARCHAR(255),
                player_focused VARCHAR(255),
                last_death BIGINT DEFAULT 0,
                strikes INTEGER DEFAULT 0,
                points INTEGER DEFAULT 0,
                wall_check_minutes INTEGER DEFAULT 0,
                buffer_check_minutes INTEGER DEFAULT 0,
                wee_woo BOOLEAN DEFAULT false,
                allowed_spawner_chunks INTEGER DEFAULT 0,
                is_protected BOOLEAN DEFAULT true,
                banner_data TEXT,
                chest_data TEXT,
                permissions_data TEXT,
                spawner_chunks_data TEXT,
                completed_missions_data TEXT,
                checks_data TEXT,
                player_wall_check_count_data TEXT,
                player_buffer_check_count_data TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }

        // Create indexes
        String[] indexes = {
            "CREATE INDEX IF NOT EXISTS idx_factions_tag ON factions(tag)",
            "CREATE INDEX IF NOT EXISTS idx_factions_is_open ON factions(is_open)",
            "CREATE INDEX IF NOT EXISTS idx_factions_is_peaceful ON factions(is_peaceful)"
        };

        try (Statement stmt = conn.createStatement()) {
            for (String index : indexes) {
                stmt.execute(index);
            }
        }
    }

    private void createFPlayersTable(Connection conn) throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS fplayers (
                id VARCHAR(255) PRIMARY KEY,
                name VARCHAR(255),
                faction_id VARCHAR(255),
                role VARCHAR(50) DEFAULT 'NORMAL',
                title VARCHAR(255),
                power DOUBLE PRECISION DEFAULT 0.0,
                power_boost DOUBLE PRECISION DEFAULT 0.0,
                last_power_update_time BIGINT,
                last_login_time BIGINT,
                chat_mode VARCHAR(50) DEFAULT 'PUBLIC',
                ignore_alliance_chat BOOLEAN DEFAULT false,
                monitor_joins BOOLEAN DEFAULT false,
                spying_chat BOOLEAN DEFAULT false,
                show_scoreboard BOOLEAN DEFAULT true,
                kills INTEGER DEFAULT 0,
                deaths INTEGER DEFAULT 0,
                will_auto_leave BOOLEAN DEFAULT true,
                map_height INTEGER DEFAULT 8,
                is_flying BOOLEAN DEFAULT false,
                is_auto_flying BOOLEAN DEFAULT false,
                is_alt BOOLEAN DEFAULT false,
                entering_password BOOLEAN DEFAULT false,
                entering_password_warp VARCHAR(255),
                last_stood_at_world VARCHAR(255),
                last_stood_at_x INTEGER,
                last_stood_at_z INTEGER,
                map_auto_updating BOOLEAN DEFAULT false,
                auto_claim_for VARCHAR(255),
                auto_unclaim_for VARCHAR(255),
                login_pvp_disabled BOOLEAN DEFAULT false,
                last_frostwalker_message BIGINT DEFAULT 0,
                should_take_fall_damage BOOLEAN DEFAULT true,
                is_stealth_enabled BOOLEAN DEFAULT false,
                notifications_enabled BOOLEAN DEFAULT true,
                titles_enabled BOOLEAN DEFAULT true,
                seeing_chunk BOOLEAN DEFAULT false,
                inspect_mode BOOLEAN DEFAULT false,
                friendly_fire BOOLEAN DEFAULT false,
                enemies_nearby BOOLEAN DEFAULT false,
                in_chest BOOLEAN DEFAULT false,
                in_vault BOOLEAN DEFAULT false,
                is_admin_bypassing BOOLEAN DEFAULT false,
                warmup_type VARCHAR(50),
                warmup_task_id INTEGER,
                command_cooldowns TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (faction_id) REFERENCES factions(id) ON DELETE SET NULL
            )
            """;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }

        // Create indexes
        String[] indexes = {
            "CREATE INDEX IF NOT EXISTS idx_fplayers_faction_id ON fplayers(faction_id)",
            "CREATE INDEX IF NOT EXISTS idx_fplayers_name ON fplayers(name)",
            "CREATE INDEX IF NOT EXISTS idx_fplayers_role ON fplayers(role)",
            "CREATE INDEX IF NOT EXISTS idx_fplayers_is_alt ON fplayers(is_alt)"
        };

        try (Statement stmt = conn.createStatement()) {
            for (String index : indexes) {
                stmt.execute(index);
            }
        }
    }

    private void createBoardTable(Connection conn) throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS board (
                world_name VARCHAR(255) NOT NULL,
                chunk_x INTEGER NOT NULL,
                chunk_z INTEGER NOT NULL,
                faction_id VARCHAR(255) NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (world_name, chunk_x, chunk_z),
                FOREIGN KEY (faction_id) REFERENCES factions(id) ON DELETE CASCADE
            )
            """;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }

        // Create indexes for faster lookups
        String[] indexes = {
            "CREATE INDEX IF NOT EXISTS idx_board_faction_id ON board(faction_id)",
            "CREATE INDEX IF NOT EXISTS idx_board_world_name ON board(world_name)",
            "CREATE INDEX IF NOT EXISTS idx_board_coords ON board(chunk_x, chunk_z)"
        };

        try (Statement stmt = conn.createStatement()) {
            for (String index : indexes) {
                stmt.execute(index);
            }
        }
    }

    private void createFactionsRelationsTable(Connection conn) throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS faction_relations (
                faction_id VARCHAR(255) NOT NULL,
                other_faction_id VARCHAR(255) NOT NULL,
                relation VARCHAR(50) NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (faction_id, other_faction_id),
                FOREIGN KEY (faction_id) REFERENCES factions(id) ON DELETE CASCADE,
                FOREIGN KEY (other_faction_id) REFERENCES factions(id) ON DELETE CASCADE
            )
            """;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    private void createFactionsInvitesTable(Connection conn) throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS faction_invites (
                faction_id VARCHAR(255) NOT NULL,
                player_id VARCHAR(255) NOT NULL,
                is_alt_invite BOOLEAN DEFAULT false,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (faction_id, player_id),
                FOREIGN KEY (faction_id) REFERENCES factions(id) ON DELETE CASCADE
            )
            """;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    private void createFactionsWarpsTable(Connection conn) throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS faction_warps (
                faction_id VARCHAR(255) NOT NULL,
                warp_name VARCHAR(255) NOT NULL,
                world_name VARCHAR(255),
                x DOUBLE PRECISION,
                y DOUBLE PRECISION,
                z DOUBLE PRECISION,
                pitch REAL,
                yaw REAL,
                password VARCHAR(255),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (faction_id, warp_name),
                FOREIGN KEY (faction_id) REFERENCES factions(id) ON DELETE CASCADE
            )
            """;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    private void createFactionsAnnouncementsTable(Connection conn) throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS faction_announcements (
                id SERIAL PRIMARY KEY,
                faction_id VARCHAR(255) NOT NULL,
                player_id VARCHAR(255) NOT NULL,
                message TEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (faction_id) REFERENCES factions(id) ON DELETE CASCADE
            )
            """;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }

        String index = "CREATE INDEX IF NOT EXISTS idx_faction_announcements_faction_player ON faction_announcements(faction_id, player_id)";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(index);
        }
    }

    private void createFactionsClaimOwnershipTable(Connection conn) throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS faction_claim_ownership (
                faction_id VARCHAR(255) NOT NULL,
                world_name VARCHAR(255) NOT NULL,
                chunk_x INTEGER NOT NULL,
                chunk_z INTEGER NOT NULL,
                owner_id VARCHAR(255) NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (faction_id, world_name, chunk_x, chunk_z, owner_id),
                FOREIGN KEY (faction_id) REFERENCES factions(id) ON DELETE CASCADE
            )
            """;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    private void createFactionsUpgradesTable(Connection conn) throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS faction_upgrades (
                faction_id VARCHAR(255) NOT NULL,
                upgrade_name VARCHAR(255) NOT NULL,
                level INTEGER DEFAULT 0,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (faction_id, upgrade_name),
                FOREIGN KEY (faction_id) REFERENCES factions(id) ON DELETE CASCADE
            )
            """;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    private void createFactionsMissionsTable(Connection conn) throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS faction_missions (
                faction_id VARCHAR(255) NOT NULL,
                mission_name VARCHAR(255) NOT NULL,
                mission_data TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (faction_id, mission_name),
                FOREIGN KEY (faction_id) REFERENCES factions(id) ON DELETE CASCADE
            )
            """;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    private void createFactionsRulesTable(Connection conn) throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS faction_rules (
                faction_id VARCHAR(255) NOT NULL,
                rule_index INTEGER NOT NULL,
                rule_text TEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (faction_id, rule_index),
                FOREIGN KEY (faction_id) REFERENCES factions(id) ON DELETE CASCADE
            )
            """;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            Logger.print("Database connection pool closed", Logger.PrefixType.DEFAULT);
        }
    }
}