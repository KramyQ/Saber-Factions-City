package com.massivecraft.factions.war;

import com.massivecraft.factions.*;
import com.massivecraft.factions.scoreboards.FScoreboard;
import com.massivecraft.factions.scoreboards.sidebar.FWarSidebar;
import com.massivecraft.factions.war.struct.WarState;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class War {

    String id;
    long warStart;
    long warEnd;
    ArrayList<Faction> attackers = new ArrayList<>();
    ArrayList<Faction> defenders = new ArrayList<>();
    ArrayList<WarChunk> captured = new ArrayList<>();
    ArrayList<WarChunk> capturing = new ArrayList<>();
    int score = 0;
    WarState warState;


    public War(Faction mainAttacker, Faction mainDefender) {
        id = mainAttacker.getId() + mainDefender.getId();
        warStart = System.currentTimeMillis();
        warEnd = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(Conf.warPeriodTimeMinutes) + TimeUnit.MINUTES.toMillis(Conf.preWarPeriodTimeMinutes);
        attackers.add(mainAttacker);
        defenders.add(mainDefender);
        warState = WarState.PRE_WAR_PHASE;
    }

    public ArrayList<Faction> getAllFactions() {
        ArrayList<Faction> allFactions = new ArrayList<>();
        allFactions.addAll(attackers);
        allFactions.addAll(defenders);
        return allFactions;
    }

    public static boolean isFactionAtWar(Faction faction) {
        ArrayList<War> warList = FactionsPlugin.getInstance().getWarList();
        for (War war : warList) {
            if (war.getAllFactions().contains(faction)) {
                return true;
            }
        }
        return false;
    }

    public ArrayList<FPlayer> getAllPlayers() {
        ArrayList<FPlayer> allPlayers = new ArrayList<>();
        for (Faction faction : getAllFactions()) {
            for (Player player : faction.getOnlinePlayers()) {
                FPlayer fplayer = FPlayers.getInstance().getByPlayer(player);
                allPlayers.add(fplayer);
            }
        }
        return allPlayers;
    }


    public void setWarScoreboards() {
        for (FPlayer player : getAllPlayers()) {
            FScoreboard.get(player).setWarSidebar(new FWarSidebar(player.getFaction()));
        }
    }

    // TEMP FOR DEBUG
    private void sendToParticipants(String message){
         for (FPlayer player : getAllPlayers()) {
               player.sendMessage(message);
           }
    }

    public static void manageWars() {
        ArrayList<War> warList = FactionsPlugin.getInstance().getWarList();
        if (!warList.isEmpty()) {
            for (War war : warList) {
                applyStateChangeIfNeeded(war);
                if (war.warState == WarState.WAR_PHASE) {
                    manageChunks(war);
                }
            }
//           for (FPlayer player : warList.get(0).getAllPlayers()) {
//               player.sendMessage("YOU ARE AT WAR LULZ");
//           }
        }
        // Check if wars are happening. X
        // Check if wars are over. If a war is over, change its state, compute rewards and stats
        // If war is not over. Check its chunks being captured and update their %
        // or state (if update state, update score accordingly) depending on the number of people of each sides in the chunk
    }

    private static void manageChunks(War war) {
        ArrayList<FPlayer> players = war.getAllPlayers();
        for (WarChunk warChunk : war.capturing) {
            Pair<Integer, Integer> cardinals = getCardinalsInWarChunk(warChunk, players, war);
            computeWarChunkProgression(warChunk, cardinals, war);
        }
    }

    private static void computeWarChunkProgression(WarChunk warChunk, Pair<Integer, Integer> cardinals, War war) {
        int cardinalDiff = cardinals.getLeft().intValue() - cardinals.getRight().intValue();
        if (cardinalDiff > 0) {
            float progression = cardinalDiff * war.getCaptureRatio();
            warChunk.progression += progression;
            if (warChunk.progression >= 100) {
                warChunk.captured = true;
                war.captured.add(warChunk);
                war.capturing.remove(warChunk);
            }
        }
    }

    private int getCaptureRatio() {
        return 1;
    }

    private static Pair<Integer, Integer> getCardinalsInWarChunk(WarChunk warChunk, ArrayList<FPlayer> players, War war) {
        Integer defenders = 0;
        Integer attackers = 0;
        for (Entity entity : warChunk.chunk.getEntities()) {
            if (entity instanceof Player) {
                FPlayer fplayer = FPlayers.getInstance().getByPlayer((Player) entity);
                if (fplayer != null && players.contains(fplayer)) {
                    if (war.attackers.contains(fplayer.getFaction())) {
                        attackers++;
                    } else {
                        defenders++;
                    }
                }
            }
        }
        return Pair.of(defenders, attackers);
    }

    private static void applyStateChangeIfNeeded(War war) {
        switch (war.warState) {
            case PRE_WAR_PHASE:
                if(war.isPreWarOver()){
                    war.sendToParticipants("Pre war is over.");
                    war.warState = WarState.WAR_PHASE;
                }
                break;
            case WAR_PHASE:
                if(war.isWarOver()){
                    war.computeWarRewards();
                    war.computeWarStats();
                    war.sendToParticipants("War is over.");
                    war.warState = WarState.POST_WAR_PHASE;
                }
                break;
            case POST_WAR_PHASE:
                 if(war.isPostWarOver()){
                    cleanUpScoreboards(war);
                    cleanUpWarData(war);
                    war.sendToParticipants("Post War is over.");
                }
                break;
            default:
                break;
        }

    }

    private static void cleanUpScoreboards(War war) {
    }

    private static void cleanUpWarData(War war) {
    }

    private void computeWarStats() {
    }

    private void computeWarRewards() {
    }

    private boolean isPreWarOver() {
        return System.currentTimeMillis() >= warStart + TimeUnit.MINUTES.toMillis(Conf.preWarPeriodTimeMinutes);
    }

    private boolean isWarOver() {
        return System.currentTimeMillis() >= warStart + TimeUnit.MINUTES.toMillis(Conf.preWarPeriodTimeMinutes) + TimeUnit.MINUTES.toMillis(Conf.warPeriodTimeMinutes);
    }

    private boolean isPostWarOver() {
        return System.currentTimeMillis() >= warStart + TimeUnit.MINUTES.toMillis(Conf.preWarPeriodTimeMinutes) + TimeUnit.MINUTES.toMillis(Conf.warPeriodTimeMinutes) + TimeUnit.MINUTES.toMillis(Conf.postWarPeriodTimeMinutes);
    }

    private static boolean anyWar() {
        return !FactionsPlugin.getInstance().getWarList().isEmpty();
    }

    public static void startWarManagerTask() {
        BukkitTask warManagerTask = new BukkitRunnable() {
            @Override
            public void run() {
                War.manageWars();
            }
        }.runTaskTimer(FactionsPlugin.getInstance(), 20, 20);
        FactionsPlugin.getInstance().setWarManagerTask(warManagerTask);
    }

    public static void stopWarManager() {
        BukkitTask warManagerTask = FactionsPlugin.getInstance().getWarManagerTask();
        if (warManagerTask != null) {
            warManagerTask.cancel();
        }
    }
}
