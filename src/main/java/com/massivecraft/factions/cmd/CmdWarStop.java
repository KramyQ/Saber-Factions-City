package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Faction;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.war.War;
import com.massivecraft.factions.zcore.util.TL;

public class CmdWarStop extends FCommand {

    /**
     * @author Kram
     */

    public CmdWarStop() {
        super();
        this.requiredArgs.add("faction tag");
        this.aliases.addAll(Aliases.war);
        this.requirements = new CommandRequirements.Builder(Permission.ADMIN).build();
    }

    @Override
    public void perform(CommandContext context) {
//        int min_connected = FactionsPlugin.getInstance().getConfig().getInt("war.start_conditions.min_connected");
        Faction them = context.argAsFaction(0);
        // Check if their faction exists
        if (them == null) return;
        if (!them.isNormal()) {
            context.msg(TL.COMMAND_WAR_NOPE);
            return;
        }

        // Can stop if faction at war
        if (!War.isFactionAtWar(them)) {
            context.msg(TL.COMMAND_WAR_NOT_WAR);
            return;
        }
        War theirWar = War.getFactionWar(them);
        theirWar.interruptWar("War has been interrupted by admin, more information on discord.");
        context.msg("War was stopped.");
    }

    @Override
    public TL getUsageTranslation() {
        return TL.COMMAND_WAR_DESCRIPTION;
    }


}
