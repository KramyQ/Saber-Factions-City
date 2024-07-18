package com.massivecraft.factions.scoreboards.sidebar;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.scoreboards.FSidebarProvider;
import com.massivecraft.factions.war.War;
import com.massivecraft.factions.war.struct.ChunkState;
import com.massivecraft.factions.war.struct.WarState;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class FWarSidebar extends FSidebarProvider {

    /**
     * @author FactionsUUID Team - Modified By CmdrKittens
     */

    private final Faction faction;

    public FWarSidebar(Faction faction) {
        this.faction = faction;
    }

    @Override
    public String getTitle(FPlayer fplayer) {
        War war = War.getPlayerWar(fplayer);
        Boolean isAttacker = war.getAttackers().contains(fplayer.getFaction());
        if (isAttacker) {
            return ChatColor.RED + ChatColor.BOLD.toString() + "⏹⏹" + ChatColor.RESET + ChatColor.RED + " ⚔ " + ChatColor.BOLD + "⏹⏹ WAR PANEL ⏹⏹" + ChatColor.RESET + ChatColor.RED + " ⚔ " + ChatColor.BOLD + "⏹⏹";
        }
        return ChatColor.RED + "Defend your city!";
    }

    @Override
    public List<String> getLines(FPlayer fplayer) {
        War war = War.getPlayerWar(fplayer);
        Boolean isAttacker = war.getAttackers().contains(fplayer.getFaction());
        List<String> lines = new ArrayList<>();
        // Phase Line + Time Remaining
        lines.add(ChatColor.RED + "⏹" + ChatColor.WHITE + " Phase : " + ChatColor.GOLD + war.getHumanizedWarState() + ChatColor.WHITE + " | " + ChatColor.WHITE + "⌛:" + ChatColor.GREEN + war.getHumanizedTimeToNextPhase());
        // Score Line
        lines.add(ChatColor.RED + "⏹" + ChatColor.WHITE + " Score : " + ChatColor.GOLD + war.score + ChatColor.GRAY + "/1000");
        // Defenders Line x + x allies
        if (war.warState == WarState.PRE_WAR_PHASE) {
            lines.add(ChatColor.RED + "⏹" + ChatColor.WHITE + " ⛨ Defense : " + ChatColor.BLUE + war.getMainDefenderTag());
            lines.add(ChatColor.RED + "⏹" + ChatColor.WHITE + " ⛨ Allies :" + ChatColor.BLUE + war.getHumanizedDefenderList());
            // Attackers Line x + x allies
            lines.add(ChatColor.RED + "⏹" + ChatColor.WHITE + " ⚔ Attack : " + ChatColor.RED + war.getMainAttackerTag());
            lines.add(ChatColor.RED + "⏹" + ChatColor.WHITE + " ⚔ Allies :" + ChatColor.RED + war.getHumanizedAttackerList());
        }
        // Chunk Line

        if (war.warState == WarState.WAR_PHASE) {
            lines.add(ChatColor.RED + "⏹" + ChatColor.WHITE + " ⛨ Defense numbers : " + ChatColor.BLUE + war.defendersNumber);
            lines.add(ChatColor.RED + "⏹" + ChatColor.WHITE + " ⚔ Attack numbers : " + ChatColor.RED + war.attackersNumber);
            ChunkState chunkState = war.getCapturePossibilitiesOnChunk(fplayer);
            lines.add(ChatColor.RED + "⏹" + ChatColor.WHITE + " ⛏ Capture ⛏");
            int currentCaptures = war.getCurrentCapturingCardinal();
            lines.add(ChatColor.RED + "⏹" + ChatColor.WHITE + " Current Captures : " + ChatColor.GOLD + currentCaptures + ChatColor.GRAY + "/" + war.maxCapturing);
            if (currentCaptures > 0) {
                lines.add(ChatColor.RED + "⏹" + ChatColor.WHITE + " Captures Pos : " + ChatColor.GREEN + war.getHumanizedCapturingPostList());
            }

            if (chunkState.equals(ChunkState.CAPTURED)) {
                lines.add(ChatColor.RED + "⏹" + ChatColor.WHITE + " State : " + ChatColor.BOLD + ChatColor.RED + "CAPTURED");
            }
            if (chunkState.equals(ChunkState.CAPTURING)) {
                lines.add(ChatColor.RED + "⏹" + ChatColor.WHITE + " State : " + ChatColor.YELLOW + ChatColor.RED + "CAPTURING");
                lines.add(ChatColor.RED + "⏹ ⏹ ⏹ ⏹" + ChatColor.WHITE + " Progress");
                int numberOfLeftBars = Math.round(20 * (war.getPercentProgressionOnChunk(fplayer) * 0.01f));
                String progressBarLeft = ChatColor.GREEN + "";
                String progressBarRight = ChatColor.WHITE + "";
                for (int i = 1; i <= numberOfLeftBars; i++) {
                    progressBarLeft += "⏹";
                }
                for (int i = 1; i <= 20-numberOfLeftBars; i++) {
                    progressBarRight += "⏹";
                }

                lines.add(ChatColor.RED + "⏹ [" + progressBarLeft + progressBarRight + "]");
            }
            if (chunkState.equals(ChunkState.CAN_BE_CAPTURED)) {
                lines.add(ChatColor.RED + "⏹" + ChatColor.WHITE + " State : " + ChatColor.GOLD + ChatColor.RED + "CAN BE CAPTURED");
            }
            if (chunkState.equals(ChunkState.CANT_BE_CAPTURED)) {
                lines.add(ChatColor.RED + "⏹" + ChatColor.WHITE + " State : " + ChatColor.GRAY + ChatColor.RED + "CAN'T BE CAPTURED");
            }
        }
        // Personal Score
        lines.add(ChatColor.RED + "⏹" + ChatColor.WHITE + " Your score : ⛏:" + ChatColor.GREEN + "1" + ChatColor.WHITE + " ⚔:" + ChatColor.GOLD + "0" + ChatColor.WHITE + " ☠:" + ChatColor.RED + "2");


        ListIterator<String> it = lines.listIterator();


        while (it.hasNext()) {
            String next = it.next();
            if (next == null) {
                it.remove();
                continue;
            }
            String replaced = replaceTags(faction, fplayer, next);
            if (replaced == null) {
                it.remove();
            } else {
                it.set(replaced);
            }
        }
        return lines;
    }
}
