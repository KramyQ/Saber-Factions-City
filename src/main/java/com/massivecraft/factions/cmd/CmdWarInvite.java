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

public class CmdWarInvite extends FCommand {

    /**
     * @author Kram
     */

    public CmdWarInvite() {
        super();
        this.requiredArgs.add("faction tag");
        this.aliases.addAll(Aliases.war);

        this.requirements = new CommandRequirements.Builder(Permission.WAR)
                .withRole(Role.MODERATOR)
                .playerOnly()
                .memberOnly()
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

        // Can only invite if you're at war
        if (!War.isFactionAtWar(context.faction)) {
            context.msg(TL.COMMAND_WAR_NOT_WAR);
            return;
        }
        War yourWar = War.getFactionWar(context.faction);

        // Can only invite if main defender/attacker

        if (yourWar.getMainAttacker() != context.faction && yourWar.getMainDefender() != context.faction) {
            context.msg(TL.COMMAND_WARINVITE_NOT_MAIN);
            return;
        }

        // Can only invite a faction at War if they are allies
        if (context.faction.getRelationTo(them) == Relation.ALLY) {
            context.msg(TL.COMMAND_WARJOIN_NOT_ALLY, them.getTag());
            return;
        }

        // Can only invite a faction at War if the fac not already at War
        if (War.isFactionAtWar(them)) {
            context.msg(TL.COMMAND_WARINVITE_AL_WAR);
            return;
        }

        // Can only invite if not already invited
        if (!yourWar.getInvitedDefenders().contains(them) && !yourWar.getInvitedAttackers().contains(them)) {
            context.msg(TL.COMMAND_WARJOIN_NOT_AT_WAR);
            return;
        }


        yourWar.processInviteAttempt(context.faction, them);
    }

    @Override
    public TL getUsageTranslation() {
        return TL.COMMAND_WAR_DESCRIPTION;
    }


}
