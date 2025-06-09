package com.massivecraft.factions.cmd;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.struct.Role;
import com.massivecraft.factions.war.War;
import com.massivecraft.factions.war.struct.WarState;
import com.massivecraft.factions.zcore.util.TL;
public class CmdSetVip extends FCommand {

    /**
     * @author Kram
     */

    public CmdSetVip() {
        super();
        this.requiredArgs.add("player");
        this.aliases.addAll(Aliases.warvip);

        this.requirements = new CommandRequirements.Builder(Permission.WAR)
                .withRole(Role.MODERATOR)
                .playerOnly()
                .memberOnly()
                .build();
    }

    @Override
    public void perform(CommandContext context) {
//        int min_connected = FactionsPlugin.getInstance().getConfig().getInt("war.start_conditions.min_connected");
        FPlayer him = context.argAsFPlayer(0);
        // Check if their faction exists
        if (him == null) return;
        if (context.faction == null) return;

        // Can only set vip if you're at war
        if (!War.isFactionAtWar(context.faction)) {
            context.msg(TL.COMMAND_WAR_NOT_WAR);
            return;
        }
        // Can only set a player VIP if he is part of your faction
        Faction himFaction = him.getFaction();
        if (himFaction == null || himFaction != context.faction) {
            context.msg(TL.COMMAND_VIP_PLAYER_NOT_FACTION);
            return;
        }

        War yourWar = War.getFactionWar(context.faction);

        // can only set VIP DURING PRE WAR
          if (yourWar.warState != WarState.PRE_WAR_PHASE) {
            context.msg(TL.COMMAND_VIP_NOT_PREWAR);
            return;
        }
          yourWar.setDefVip(him);
    }


    @Override
    public TL getUsageTranslation() {
        return TL.COMMAND_WAR_DESCRIPTION;
    }


}
