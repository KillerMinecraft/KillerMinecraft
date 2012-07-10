package com.ftwinston.Killer.GameModes;

import com.ftwinston.Killer.GameMode;

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
}
