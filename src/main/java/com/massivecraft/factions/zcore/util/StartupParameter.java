// Update your StartupParameter.java or add this to your startup logic in FactionsPlugin

package com.massivecraft.factions.zcore.util;

import com.massivecraft.factions.*;
import com.massivecraft.factions.zcore.persist.db.*;
import com.massivecraft.factions.zcore.persist.json.*;
import com.massivecraft.factions.util.Logger;

public class StartupParameter {
    
    public static void initData(FactionsPlugin plugin, Runnable callback) {
        // Initialize persistence layer based on backend configuration
        initializePersistenceLayer();
        
        // Load data from chosen backend
        loadDataFromBackend(success -> {
            if (success) {
                Logger.print("Data loaded successfully from " + Conf.backEnd.name() + " backend", Logger.PrefixType.DEFAULT);
                callback.run();
            } else {
                Logger.print("Failed to load data from " + Conf.backEnd.name() + " backend", Logger.PrefixType.FAILED);
                plugin.getServer().getPluginManager().disablePlugin(plugin);
            }
        });
    }
    
    private static void initializePersistenceLayer() {
        switch (Conf.backEnd) {
            case POSTGRESQL:
                // Initialize database connection
                DatabaseManager.getInstance();
                
                // Set database implementations
                Board.instance = new DatabaseBoard();
                FPlayers.instance = new DatabaseFPlayers();
                Factions.instance = new DatabaseFactions();
                
                Logger.print("Initialized PostgreSQL persistence layer", Logger.PrefixType.DEFAULT);
                break;
                
            case JSON:
            default:
                // Set JSON implementations (existing)
                Board.instance = new JSONBoard();
                FPlayers.instance = new JSONFPlayers();
                Factions.instance = new JSONFactions();
                
                Logger.print("Initialized JSON persistence layer", Logger.PrefixType.DEFAULT);
                break;
        }
    }
    
    private static void loadDataFromBackend(java.util.function.Consumer<Boolean> callback) {
        // Load Board data
        boolean boardLoaded = Board.getInstance().load();
        if (!boardLoaded) {
            callback.accept(false);
            return;
        }
        
        // Load Factions data
        Factions.getInstance().load(factionsSuccess -> {
            if (!factionsSuccess) {
                callback.accept(false);
                return;
            }
            
            // Load FPlayers data
            FPlayers.getInstance().load(fplayersSuccess -> {
                if (!fplayersSuccess) {
                    callback.accept(false);
                    return;
                }
                
                // Refresh faction member lists
                for (Faction faction : Factions.getInstance().getAllFactions()) {
                    faction.refreshFPlayers();
                }
                
                callback.accept(true);
            });
        });
    }
    
    public static void initShutdown(FactionsPlugin plugin) {
        // Save all data before shutdown
        if (Factions.getInstance() != null) {
            Factions.getInstance().forceSave(true);
        }
        if (FPlayers.getInstance() != null) {
            FPlayers.getInstance().forceSave(true);
        }
        if (Board.getInstance() != null) {
            Board.getInstance().forceSave(true);
        }
        
        // Close database connections if using database backend
        if (Conf.backEnd == Conf.Backend.POSTGRESQL) {
            DatabaseManager.getInstance().shutdown();
        }
        
        Logger.print("Shutdown completed for " + Conf.backEnd.name() + " backend", Logger.PrefixType.DEFAULT);
    }
}