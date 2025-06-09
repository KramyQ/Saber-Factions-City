package com.massivecraft.factions.war;

import com.massivecraft.factions.*;
import com.massivecraft.factions.scoreboards.FScoreboard;
import com.massivecraft.factions.scoreboards.sidebar.FWarSidebar;
import com.massivecraft.factions.war.struct.ChunkState;
import com.massivecraft.factions.war.struct.WarState;
import lombok.Getter;
import net.coreprotect.CoreProtectAPI;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static com.massivecraft.factions.CoreProtectApi.getCoreProtect;
import static com.massivecraft.factions.cmd.CmdCapture.validSurrounding;
import static com.massivecraft.factions.util.TimeUtil.formatDifference;

public class War {

    String id;
    long warStart;
    long warEnd;
    boolean flagRemove = false;

    public static boolean canPlayerCanBuildDestroyInteractAtLocation(FPlayer me, FLocation loc) {
        // Check if player is in a war where this chunk is captured.
        War war = War.getPlayerWar(me);
        if (war == null) {
            return false;
        }
        Chunk targetChunk = loc.getChunk();
        return war.warState == WarState.WAR_PHASE && war.getCapturedBukkitChunks().contains(targetChunk);
    }

    @Getter
    ArrayList<Faction> attackers = new ArrayList<>();
    @Getter
    ArrayList<Faction> joiningAttackers = new ArrayList<>();
    @Getter
    ArrayList<Faction> invitedAttackers = new ArrayList<>();

    @Getter
    FPlayer vip;


    @Getter
    ArrayList<Faction> defenders = new ArrayList<>();
    @Getter
    ArrayList<Faction> joiningDefenders = new ArrayList<>();
    @Getter
    ArrayList<Faction> invitedDefenders = new ArrayList<>();


    ArrayList<WarChunk> captured = new ArrayList<>();


    public ArrayList<WarChunk> getCapturing() {
        return capturing;
    }

    ArrayList<WarChunk> capturing = new ArrayList<>();
    public int score = 0;
    public WarState warState;
    public int maxCapturing = 2;
    public int attackersNumber = 0;
    public int defendersNumber = 0;


    public War(Faction mainAttacker, Faction mainDefender) {
        id = mainAttacker.getId() + mainDefender.getId();
        warStart = System.currentTimeMillis();
        warEnd = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(Conf.warPeriodTimeMinutes) + TimeUnit.SECONDS.toMillis(Conf.preWarPeriodTimeSeconds);
        attackers.add(mainAttacker);
        defenders.add(mainDefender);
        warState = WarState.PRE_WAR_PHASE;
    }

    public static War getPlayerWar(FPlayer performer) {
        ArrayList<War> warList = FactionsPlugin.getInstance().getWarList();
        for (War war : warList) {
            if (war.getAllPlayers().contains(performer)) {
                return war;
            }
        }
        return null;
    }

    public static War getFactionWar(Faction faction) {
        ArrayList<War> warList = FactionsPlugin.getInstance().getWarList();
        for (War war : warList) {
            if (war.attackers.contains(faction) || war.defenders.contains(faction)) {
                return war;
            }
        }
        return null;
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

    public ArrayList<FPlayer> getAllDefenders() {
        ArrayList<FPlayer> allDefenders = new ArrayList<>();
        for (Faction faction : defenders) {
            for (Player player : faction.getOnlinePlayers()) {
                FPlayer fplayer = FPlayers.getInstance().getByPlayer(player);
                allDefenders.add(fplayer);
            }
        }
        return allDefenders;
    }

    public ArrayList<FPlayer> getAllAttackers() {
        ArrayList<FPlayer> allAttackers = new ArrayList<>();
        for (Faction faction : attackers) {
            for (Player player : faction.getOnlinePlayers()) {
                FPlayer fplayer = FPlayers.getInstance().getByPlayer(player);
                allAttackers.add(fplayer);
            }
        }
        return allAttackers;
    }

    public int getAttackersNumber() {
        return getAllPlayers().size() - getAllDefenders().size();
    }

    public int getDefendersNumber() {
        return getAllDefenders().size();
    }


    public void setWarScoreboards() {
        for (FPlayer player : getAllPlayers()) {
            FScoreboard scoreBoard = FScoreboard.get(player);
            if (scoreBoard != null) {
                scoreBoard.setWarSidebar(new FWarSidebar(player.getFaction()));
                scoreBoard.setSidebarVisibility(true);
            }
        }
    }

    // TEMP FOR DEBUG
    private void sendToParticipants(String message) {
        for (FPlayer player : getAllPlayers()) {
            player.sendMessage(message);
        }
    }

    private void sendToMainAttacker(String message) {
        for (Player player : getMainAttacker().getOnlinePlayers()) {
            player.sendMessage(message);
        }
    }

    private void sendToMainDefender(String message) {
        for (Player player : getMainDefender().getOnlinePlayers()) {
            player.sendMessage(message);
        }
    }

    private void sentToFaction(String message, Faction receiver) {
        for (Player player : receiver.getOnlinePlayers()) {
            player.sendMessage(message);
        }
    }

    public static void manageWars() {
        ArrayList<War> warList = FactionsPlugin.getInstance().getWarList();
        ArrayList<War> warsToRemove = new ArrayList<>();
        if (!warList.isEmpty()) {
            for (War war : warList) {
                applyStateChangeIfNeeded(war);
                if (war.warState == WarState.WAR_PHASE) {
                    manageChunks(war);
                }
                if (war.flagRemove) {
                    warsToRemove.add(war);
                }
            }
            for (War war : warsToRemove) {
                warList.remove(war);
            }
        }
    }

    private static void manageChunks(War war) {
        ArrayList<FPlayer> players = war.getAllPlayers();
        ArrayList<WarChunk> chunksJustCaptured = new ArrayList<>();
        for (WarChunk warChunk : war.capturing) {
            Pair<Integer, Integer> cardinals = getCardinalsInWarChunk(warChunk, players, war);
            computeWarChunkProgression(warChunk, cardinals, war);
            if (warChunk.captured) {
                chunksJustCaptured.add(warChunk);
            }
        }
        for (WarChunk warChunk : chunksJustCaptured) {
            scoreChunkCapturePoints(war);
            war.captured.add(warChunk);
            war.capturing.remove(warChunk);
        }
    }

    private static void scoreChunkCapturePoints(War war) {
        war.score += 100;
    }

    private static void computeWarChunkProgression(WarChunk warChunk, Pair<Integer, Integer> cardinals, War war) {
        int cardinalDiff = cardinals.getLeft() - cardinals.getRight();
        if (cardinalDiff > 0) {
            float progression = cardinalDiff * war.getCaptureRatio();
            warChunk.progression += progression + 2;
            if (warChunk.progression >= 100) {
                warChunk.captured = true;
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
        return Pair.of(attackers, defenders);
    }

    private static void applyStateChangeIfNeeded(War war) {
        switch (war.warState) {
            case PRE_WAR_PHASE:
                if (war.isPreWarOver()) {
                    war.sendToParticipants("Pre war is over.");
                    war.setInitialRatios();
//                    war.saveChunks();
                    war.warState = WarState.WAR_PHASE;
                }
                break;
            case WAR_PHASE:
                if (war.isWarOver()) {
                    war.computeWarRewards();
                    war.computeWarStats();
                    war.sendToParticipants("War is over.");
                    war.regenChunks();
                    war.warState = WarState.POST_WAR_PHASE;
                }
                break;
            case POST_WAR_PHASE:
                if (war.isPostWarOver()) {
                    cleanUpScoreboards(war);
                    war.sendToParticipants("Post War is over.");
                    cleanUpWarData(war);
                }
                break;
            default:
                break;
        }

    }

    private void regenChunks() {
        CoreProtectAPI api = getCoreProtect();
        if (api != null) {
            for (Chunk chunkLocation : getCapturedBukkitChunks()) {
                int x = chunkLocation.getX() * 16;
                int z = chunkLocation.getZ() * 16;
                for (int i = 7; i <= 8; i++) {
                    for (int j = 7; j <= 8; j++) {
                        Location radius_center = new Location(chunkLocation.getWorld(), x + i, 0, z + j);
                        FactionsPlugin.getInstance().getServer().getScheduler().runTaskAsynchronously(FactionsPlugin.instance, () -> {
                            api.performRollback(Conf.warPeriodTimeMinutes * 60, null, null, null, null, null, 7, radius_center);
                        });
                    }
                }
            }
        }
    }

    private void setInitialRatios() {
        maxCapturing = getMaxCapturing();
        attackersNumber = getAttackersNumber();
        defendersNumber = getDefendersNumber();
    }

    private static void cleanUpScoreboards(War war) {
        for (FPlayer player : war.getAllPlayers()) {
            player.sendMessage("Cleaning your scoreboard");
            FScoreboard scoreBoard = FScoreboard.get(player);
            if (scoreBoard != null) {
                scoreBoard.setWarSidebar(null);
                scoreBoard.setSidebarVisibility(player.showScoreboard());
            }
        }
    }

    private static void cleanUpWarData(War war) {
        war.flagRemove = true;
    }

    private void computeWarStats() {
    }

    private void computeWarRewards() {
    }

    private boolean isPreWarOver() {
        return System.currentTimeMillis() >= warStart + TimeUnit.SECONDS.toMillis(Conf.preWarPeriodTimeSeconds);
    }

    private boolean isWarOver() {
        return System.currentTimeMillis() >= warStart + TimeUnit.SECONDS.toMillis(Conf.preWarPeriodTimeSeconds) + TimeUnit.MINUTES.toMillis(Conf.warPeriodTimeMinutes);
    }

    private boolean isPostWarOver() {
        return System.currentTimeMillis() >= warStart + TimeUnit.SECONDS.toMillis(Conf.preWarPeriodTimeSeconds) + TimeUnit.MINUTES.toMillis(Conf.warPeriodTimeMinutes) + TimeUnit.MINUTES.toMillis(Conf.postWarPeriodTimeMinutes);
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

    public int getMaxCapturing() {
        return 2 + (getAllDefenders().size() / 5);
    }

    public ArrayList<Chunk> getCapturedBukkitChunks() {
        ArrayList<Chunk> capturedBukkitChunks = new ArrayList<>();
        for (WarChunk warChunk : captured) {
            capturedBukkitChunks.add(warChunk.chunk);
        }
        return capturedBukkitChunks;
    }

    public ArrayList<Chunk> getCapturingBukkitChunks() {
        ArrayList<Chunk> capturingBukkitChunks = new ArrayList<>();
        for (WarChunk warChunk : capturing) {
            capturingBukkitChunks.add(warChunk.chunk);
        }
        return capturingBukkitChunks;
    }

    public String getHumanizedWarState() {
        switch (warState) {
            case PRE_WAR_PHASE:
                return "Prèparation";
            case WAR_PHASE:
                return "Guerre";
            case POST_WAR_PHASE:
                return "Après-Guerre";
            default:
                return "Unknown";
        }
    }

    public String getHumanizedTimeToNextPhase() {
        switch (warState) {
            case PRE_WAR_PHASE:
                return formatDifference((warStart + TimeUnit.SECONDS.toMillis(Conf.preWarPeriodTimeSeconds) - System.currentTimeMillis()) / 1000);
            case WAR_PHASE:
                return formatDifference((warStart + TimeUnit.SECONDS.toMillis(Conf.preWarPeriodTimeSeconds) + TimeUnit.MINUTES.toMillis(Conf.warPeriodTimeMinutes) - System.currentTimeMillis()) / 1000);
            case POST_WAR_PHASE:
                return formatDifference((warStart + TimeUnit.SECONDS.toMillis(Conf.preWarPeriodTimeSeconds) + TimeUnit.MINUTES.toMillis(Conf.warPeriodTimeMinutes) + TimeUnit.MINUTES.toMillis(Conf.postWarPeriodTimeMinutes) - System.currentTimeMillis()) / 1000);
            default:
                return "Unknown";
        }
    }

    public String getHumanizedDefenderList() {
        String defenderList = "-";
        for (Faction defender : defenders) {
            if (defender != defenders.get(0)) {
                defenderList = defenderList + defender.getTag() + "-";
            }
        }
        return defenderList;
    }

    public String getMainDefenderTag() {
        return defenders.get(0).getTag();
    }

    public Faction getMainDefender() {
        return defenders.get(0);
    }

    public Faction getMainAttacker() {
        return attackers.get(0);
    }

    public String getMainAttackerTag() {
        return attackers.get(0).getTag();
    }

    public String getHumanizedAttackerList() {
        String attackerList = "-";
        for (Faction attacker : attackers) {
            if (attacker != attackers.get(0)) {
                attackerList = attackerList + attacker.getTag() + "-";
            }
        }
        return attackerList;
    }


    public ChunkState getCapturePossibilitiesOnChunk(FPlayer fplayer) {
        FLocation playerLocation = FLocation.wrap(fplayer);
        Chunk playerChunk = playerLocation.getChunk();
        Faction factionAtChunk = Board.getInstance().getFactionAt(playerLocation);
        Faction mainDefender = defenders.get(0);
        if (getCapturingBukkitChunks().contains(playerChunk)) {
            return ChunkState.CAPTURING;
        }
        if (getCapturedBukkitChunks().contains(playerChunk)) {
            return ChunkState.CAPTURED;
        }
        if (validSurrounding(mainDefender, playerChunk, this) && maxCapturing > getCurrentCapturingCardinal() && factionAtChunk == mainDefender) {
            return ChunkState.CAN_BE_CAPTURED;
        }
        return ChunkState.CANT_BE_CAPTURED;
    }

    public int getCurrentCapturingCardinal() {
        return capturing.size();
    }

    public String getHumanizedCapturingPostList() {
        String capturePostList = "";
        for (WarChunk warChunk : capturing) {
            capturePostList.concat(warChunk.chunk.getX() * 16 + "-" + warChunk.chunk.getZ() * 16 + " | ");
        }
        return capturePostList;
    }

    public int getPercentProgressionOnChunk(FPlayer fplayer) {
        FLocation playerLocation = FLocation.wrap(fplayer);
        Chunk playerChunk = playerLocation.getChunk();
        WarChunk warChunk = getWarChunkFromChunkCapturing(playerChunk);
        return Math.round(warChunk.progression);

    }

    public WarChunk getWarChunkFromChunkCapturing(Chunk chunk) {
        for (WarChunk capturingChunk : capturing) {
            if (capturingChunk.chunk.equals(chunk)) {
                return capturingChunk;
            }
        }
        return null;
    }


    public void processJoinAttempt(Faction joiner, Faction joined) {
        // Defense Side
        if (getMainDefender() == joined) {
            // Joiner has been invited
            if (invitedDefenders.contains(joiner)) {
                invitedDefenders.remove(joiner);
                defenders.add(joiner);
                notifyParticipantsWarAdd(joiner, "defender");
                this.setWarScoreboards();
            } else {
                joiningDefenders.add(joiner);
                notifyWarJoin(joiner);
            }
        } else if (getMainAttacker() == joined) {
            // Joiner has been invited
            if (invitedAttackers.contains(joiner)) {
                invitedAttackers.remove(joiner);
                attackers.add(joiner);
                notifyParticipantsWarAdd(joiner, "attacker");
                this.setWarScoreboards();
            } else {
                joiningAttackers.add(joiner);
                notifyWarJoin(joiner);
            }

        }
    }

    private void notifyParticipantsWarAdd(Faction joiner, String role) {
        if (role == "defender") {
            sendToParticipants(joiner.getTag() + " has joined the war as a defender with " + joiner.getOnlinePlayers().size() + " warriors.");
        }
        if (role == "attacker") {
            sendToParticipants(joiner.getTag() + " has joined the war as an attacker with " + joiner.getOnlinePlayers().size() + " warriors.");
        }
    }

    private void notifyWarJoin(Faction joiner) {
        sendToMainDefender(joiner.getTag() + " would like to join your war, type: /f warinvite " + joiner.getTag() + " to accept them.");
    }

    private void notifyInvitedToWar(Faction inviter, Faction invitee) {
        sentToFaction(inviter.getTag() + " would like you to join their war, type: /f warjoin " + inviter.getTag() + " to join them.", invitee);
    }

    public void processInviteAttempt(Faction you, Faction them) {
        if (getMainDefender() == you) {
            // Joiner has been invited
            if (joiningDefenders.contains(them)) {
                joiningDefenders.remove(them);
                defenders.add(them);
                notifyParticipantsWarAdd(them, "defender");
            } else {
                invitedDefenders.add(them);
                notifyInvitedToWar(you, them);
            }
        } else if (getMainAttacker() == you) {
            // Joiner has been invited
            if (joiningAttackers.contains(them)) {
                joiningAttackers.remove(them);
                attackers.add(them);
                notifyParticipantsWarAdd(them, "attacker");
            } else {
                invitedAttackers.add(them);
                notifyInvitedToWar(you, them);
            }
        }
    }

    public void interruptWar(String s) {
        cleanUpScoreboards(this);
        cleanUpWarData(this);
        this.sendToParticipants("War has been ended, no cooldown will be applied: " + s);

    }


    public String getAttackersFactionTags() {
        String attackerFactionsTags = "";
        for (Faction attacker : attackers) {
            attackerFactionsTags += attacker.getTag() + ", ";
        }
        return attackerFactionsTags;
    }

    public String getDefendersFactionTags() {
        String defenderFactionsTags = "";
        for (Faction defender : defenders) {
            defenderFactionsTags += defender.getTag() + ", ";
        }
        return defenderFactionsTags;
    }

    public String getDefendersNames() {
        String defendersNames = "";
        for (FPlayer defender : getAllDefenders()) {
            defendersNames += defender.getName() + ", ";
        }
        return defendersNames;
    }

    public String getAttackersNames() {
        String attackersNames = "";
        for (FPlayer attacker : getAllAttackers()) {
            attackersNames += attacker.getName() + ", ";
        }
        return attackersNames;
    }

    public void setDefVip(FPlayer him) {
        if(him != null && this.warState == WarState.PRE_WAR_PHASE){
            vip = him;
            sendToParticipants("Main defender VIP is now : "+ him.getName());
        }
    }
}
