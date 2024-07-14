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

public class CmdWar extends FCommand {

    /**
     * @author Kram
     */

    public CmdWar() {
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
        context.msg("Trying to go at war");
        if (them == null) return;

        if (!context.faction.isNormal()) return;

        if (!them.isNormal()) {
            context.msg(TL.COMMAND_WAR_NOPE);
            return;
        }

        if (them == context.faction) {
            context.msg(TL.COMMAND_WAR_MORENOPE);
            return;
        }

        if (context.faction.getRelationTo(them) == Relation.ENEMY) {
            context.msg(TL.COMMAND_WAR_NOT_ENEMY, them.getTag());
            return;
        }

//        if (them.getOnlinePlayers().size() < min_connected){
//              context.msg(TL.COMMAND_WAR_NOT_ENOUGH_ENEMY);
//            return;
//        }
//
//        if (context.faction.getOnlinePlayers().size() < min_connected){
//              context.msg(TL.COMMAND_WAR_NOT_ENOUGH_MEMBERS);
//            return;
//        }

        FactionWarEvent warEvent = new FactionWarEvent(context.fPlayer, context.faction, them);
        Bukkit.getPluginManager().callEvent(warEvent);
        if (warEvent.isCancelled()) {
            return;
        }

        War newWar = new War(context.faction, them);
        ArrayList<War> warList = FactionsPlugin.getInstance().getWarList();
        warList.add(newWar);
        newWar.setWarScoreboards();

         context.msg("Nice job you are at War");

//        // if economy is enabled, they're not on the bypass list, and this command has a cost set, make 'em pay
//        if (!context.payForCommand(targetRelation.getRelationCost(), TL.COMMAND_RELATIONS_TOMARRY, TL.COMMAND_RELATIONS_FORMARRY)) {
//            return;
//        }

        //
    }

    @Override
    public TL getUsageTranslation() {
        return TL.COMMAND_WAR_DESCRIPTION;
    }


}
