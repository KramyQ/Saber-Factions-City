package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Faction;
import com.massivecraft.factions.FactionsPlugin;
import com.massivecraft.factions.event.FactionWarEvent;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.struct.Relation;
import com.massivecraft.factions.struct.Role;
import com.massivecraft.factions.war.War;
import com.massivecraft.factions.zcore.util.TL;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;

public class CmdWarInfo extends FCommand {

    /**
     * @author Kram
     */

    public CmdWarInfo() {
        super();
        this.requiredArgs.add("faction tag");
        this.aliases.addAll(Aliases.war);

        this.requirements = new CommandRequirements.Builder(Permission.WAR)
                .playerOnly()
                .build();
    }

    @Override
    public void perform(CommandContext context) {
//        int min_connected = FactionsPlugin.getInstance().getConfig().getInt("war.start_conditions.min_connected");
        Faction them = context.argAsFaction(0);
        // Check if their faction exists
        if (them == null) return;
        // Check if their faction exists
        if (!context.faction.isNormal()) return;
        if (!them.isNormal()) {
            context.msg(TL.COMMAND_WAR_NOPE);
            return;
        }

        War war = War.getFactionWar(them);

        if (war == null) {
            context.msg("This faction is not at war.");
            return;
        }
        context.msg(war.getMainAttackerTag() + " attacking " + war.getMainDefenderTag());
        context.msg("Current phase : " + war.warState.toString());
        context.msg("Time remaining to next phase : " + war.getHumanizedTimeToNextPhase());
        context.msg("Attacking score : " + war.score + " / 1000");
        context.msg("Attackers factions: " + war.getAttackersFactionTags());
        context.msg("Attackers : " + war.getAttackersNames());
        context.msg("Attackers number: " + war.getAttackersNumber());
        context.msg("Defensers factions: " + war.getDefendersFactionTags());
        context.msg("Defensers : " + war.getDefendersNames());
        context.msg("Defensers number: " + war.getDefendersNumber());
    }

    @Override
    public TL getUsageTranslation() {
        return TL.COMMAND_WAR_DESCRIPTION;
    }


}
