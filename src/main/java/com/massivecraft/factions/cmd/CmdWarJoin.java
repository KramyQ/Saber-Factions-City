package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Faction;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.struct.Relation;
import com.massivecraft.factions.struct.Role;
import com.massivecraft.factions.war.War;
import com.massivecraft.factions.zcore.util.TL;

public class CmdWarJoin extends FCommand {

    /**
     * @author Kram
     */

    public CmdWarJoin() {
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

        // Can only join a faction at War if they are allies
        if (context.faction.getRelationTo(them) == Relation.ALLY) {
            context.msg(TL.COMMAND_WARJOIN_NOT_ALLY, them.getTag());
            return;
        }

        // Can only join a War if not already at War
        if (War.isFactionAtWar(context.faction)) {
            context.msg(TL.COMMAND_WAR_AL_WAR, them.getTag());
            return;
        }

        // Can only Join a faction at War
        if (!War.isFactionAtWar(them)) {
            context.msg(TL.COMMAND_WARJOIN_NOT_AT_WAR);
            return;
        }
        War warToJoin = War.getFactionWar(them);

        // You have already tried to join the WAR.
        if (warToJoin.getJoiningAttackers().contains(context.faction) || warToJoin.getJoiningDefenders().contains(context.faction)) {
            context.msg(TL.COMMAND_WARJOIN_ALREADY_JOINING);
            return;
        }

        warToJoin.processJoinAttempt(context.faction, them);
    }

    @Override
    public TL getUsageTranslation() {
        return TL.COMMAND_WAR_DESCRIPTION;
    }


}
