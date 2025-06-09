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


    static String[] loadingBar = new String[]{"Ⓒ", "Ⓓ", "Ⓔ", "Ⓕ", "Ⓖ", "Ⓗ"};
    private final Faction faction;

    public FWarSidebar(Faction faction) {
        this.faction = faction;
    }

    @Override
    public String getTitle(FPlayer fplayer) {
        return "Ȑ";
    }

    @Override
    public List<String> getLines(FPlayer fplayer) {
        War war = War.getPlayerWar(fplayer);
        Boolean isAttacker = war.getAllAttackers().contains(fplayer.getFaction());
        List<String> lines = new ArrayList<>();
        // Phase Line + Time Remaining
        lines.add(ChatColor.GOLD + war.getHumanizedWarState() + ChatColor.WHITE + " | " + ChatColor.WHITE + "⌛:" + ChatColor.GREEN + war.getHumanizedTimeToNextPhase());
        lines.add(" ");
        lines.add("ꞹ " + ChatColor.GREEN + "1" + ChatColor.WHITE + " ᶊ " + ChatColor.GOLD + "0" + ChatColor.WHITE + " Ꞡ " + ChatColor.RED + "2");
        // Score Line
        lines.add(" ");
        lines.add("ꟗ " + ChatColor.GOLD + war.score + ChatColor.GRAY + "/1000" + ChatColor.WHITE + " | " + "ᴟ " + ChatColor.BLUE + war.defendersNumber + ChatColor.WHITE + " ᶊ " + ChatColor.RED + war.attackersNumber);
        // Defenders Line x + x allies
        if (war.warState == WarState.PRE_WAR_PHASE) {
            lines.add(ChatColor.BLUE + "" + war.getDefendersNumber() + ChatColor.WHITE + " ᴟ" + ChatColor.BLUE + war.getHumanizedDefenderList());
            lines.add(ChatColor.RED + "" + war.getAttackersNumber() + ChatColor.WHITE + " ᶊ" + ChatColor.RED + war.getHumanizedAttackerList());
        }
        // Chunk Line
        if (war.warState == WarState.WAR_PHASE) {
            ChunkState chunkState = war.getCapturePossibilitiesOnChunk(fplayer);
            int currentCaptures = war.getCurrentCapturingCardinal();
            lines.add(" ");
            lines.add("ꞹ " + ChatColor.GOLD + currentCaptures + ChatColor.GRAY + "/" + war.maxCapturing);
            lines.add(" ");
            if (chunkState.equals(ChunkState.CAPTURED)) {
                lines.add("ᵿ");
            }
            if (chunkState.equals(ChunkState.CAPTURING)) {
                int numberOfLeftBars = Math.round(5 * (war.getPercentProgressionOnChunk(fplayer) * 0.01f));
                lines.add("Ꞧ " + loadingBar[numberOfLeftBars]);
//                String progressBarLeft = ChatColor.GREEN + "";
//                String progressBarRight = ChatColor.WHITE + "";
//                for (int i = 1; i <= numberOfLeftBars; i++) {
//                    progressBarLeft += "⏹";
//                }
//                for (int i = 1; i <= 10-numberOfLeftBars; i++) {
//                    progressBarRight += "⏹";
//                }
            }
            if (chunkState.equals(ChunkState.CAN_BE_CAPTURED)) {
                lines.add("ﬓ");
            }
            if (chunkState.equals(ChunkState.CANT_BE_CAPTURED)) {
                lines.add("Ɬ");
            }
            lines.add(" ");
            lines.add(" ");
            lines.add(" ");
        }
        // Personal &

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
