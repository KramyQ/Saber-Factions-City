package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.struct.Role;
import com.massivecraft.factions.war.War;
import com.massivecraft.factions.war.WarChunk;
import com.massivecraft.factions.war.struct.WarState;
import com.massivecraft.factions.zcore.util.TL;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;

public class CmdCapture extends FCommand {

    /**
     * @author Kram
     */

    public CmdCapture() {
        super();
        this.aliases.addAll(Aliases.capture);

        this.requirements = new CommandRequirements.Builder(Permission.CAPTURE)
                .withRole(Role.NORMAL)
                .playerOnly()
                .memberOnly()
                .build();
    }

    @Override
    public void perform(CommandContext context) {
        FPlayer capturer = context.fPlayer;
        Chunk targetChunk = FLocation.wrap(capturer).getChunk();
        Faction capturerFaction = capturer.getFaction();
        if (capturerFaction == null) {
            context.msg(TL.COMMAND_WAR_CANT_CAPTURE_NO_FACTION);
            return;
        }
        War war = War.getPlayerWar(capturer);
        if (war == null || war.getAttackers().get(0) != capturerFaction || war.warState != WarState.WAR_PHASE) {
            context.msg(TL.COMMAND_WAR_CANT_CAPTURE_NO_WAR);
            return;
        }
        FLocation targetChunkLocation = FLocation.wrap(new Location(targetChunk.getWorld(), targetChunk.getX() * 16, 0, targetChunk.getZ() * 16));
        Faction factionAtChunk = Board.getInstance().getFactionAt(targetChunkLocation);
        Faction mainDefender = war.getDefenders().get(0);
        // check if chunks belongs to main defending faction
        if (factionAtChunk != mainDefender) {
            context.msg(TL.COMMAND_WAR_CANT_CAPTURE_NOT_DEFENDER_CHUNK);
            return;
        }
        int maxCapture = war.getMaxCapturing();
        // check if you can capture more cardinal in war
        if (war.getCapturing().size() >= maxCapture) {
            context.msg(TL.COMMAND_WAR_CANT_CAPTURE_NOT_ENOUGH_CARDINAL);
            return;
        }
        // check if chunk already captured or capturing
        if (war.getCapturedBukkitChunks().contains(targetChunk) || war.getCapturingBukkitChunks().contains(targetChunk)) {
            context.msg(TL.COMMAND_WAR_CANT_CAPTURE_ALREADY_CAPTUR);
            return;
        }
        // check neighbouring chunks for capture rule
        if (!validSurrounding(mainDefender, targetChunk, war)) {
            context.msg(TL.COMMAND_WAR_CANT_CAPTURE_SURROUND_RULE);
            return;
        }
        context.msg("You passed all capture tests now capturing.");
        WarChunk newWarChunk = new WarChunk(targetChunk);
        war.getCapturing().add(newWarChunk);
    }

    public static boolean validSurrounding(Faction mainDefender, Chunk targetChunk, War war) {
        int x = targetChunk.getX();
        int z = targetChunk.getZ();
        World world = targetChunk.getWorld();
        Board board = Board.getInstance();
        ArrayList<Chunk> capturedBukkitChunks = war.getCapturedBukkitChunks();
        for (int i = -1; i <= 1; ++i) {
            for (int j = -1; j <= 1; ++j) {
                FLocation neighbourChunkLocation = FLocation.wrap(new Location(world, (x + i) * 16, 0, (z + j) * 16));
                Faction factionAtChunk = board.getFactionAt(neighbourChunkLocation);
                // Here we check if of all 9 chunks (including the one captured) there is one that is not from the main defending faction or one that is from it but captured.
                if (factionAtChunk != mainDefender || capturedBukkitChunks.contains(neighbourChunkLocation.getChunk())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public TL getUsageTranslation() {
        return TL.COMMAND_CAPTURE_DESCRIPTION;
    }


}
