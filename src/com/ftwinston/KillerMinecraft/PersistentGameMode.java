package com.ftwinston.KillerMinecraft;

import org.bukkit.configuration.ConfigurationSection;

public abstract class PersistentGameMode extends GameMode {
	@Override
	boolean isPersistent() {
		return true;
	}

	abstract void savePersistentData(ConfigurationSection data);

	abstract void loadPersistentData(ConfigurationSection data);
}
