package com.ftwinston.KillerMinecraft;

import org.bukkit.configuration.ConfigurationSection;

public abstract class PersistentGameMode extends GameMode {
	@Override
	boolean isPersistent() {
		return true;
	}

	protected abstract void savePersistentData(ConfigurationSection data);

	protected abstract void loadPersistentData(ConfigurationSection data);
}
