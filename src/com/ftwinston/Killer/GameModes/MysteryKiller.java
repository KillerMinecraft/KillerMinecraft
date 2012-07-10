package com.ftwinston.Killer.GameModes;

import com.ftwinston.Killer.GameMode;
import com.ftwinston.Killer.Killer;

public class MysteryKiller extends GameMode
{
	@Override
	public String getName() { return "Mystery Killer"; }

	@Override
	public int absMinPlayers() { return 3; }

	@Override
	public boolean killersCompassPointsAtFriendlies() { return true; }

	@Override
	public boolean friendliesCompassPointsAtKiller() { return false; }

	@Override
	public boolean discreteDeathMessages() { return true; }

	@Override
	public boolean usesPlinth() { return true; }

	@Override
	public int howManyKillersToAssign(int numAlive, int numKillers, int numAliveKillers)
	{
		// if we're not set to auto-reassign the killer once one has been assigned at all, even if they're no longer alive / connected, don't do so
		if ( !Killer.instance.autoReassignKiller && numKillers > 0 )
			return 0;
		
		// for now, one living killer at a time is plenty
		return numAliveKillers > 0 ? 0 : 1;
	}
}
