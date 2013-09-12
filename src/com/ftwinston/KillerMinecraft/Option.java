package com.ftwinston.KillerMinecraft;

public final class Option
{
	public Option(String name, boolean enabledByDefault)
	{
		this.name = name;
		this.enabled = enabledByDefault;
	}
	
	private String name;
	public String getName() { return name; }
	
	private boolean enabled;
	public boolean isEnabled() { return enabled; }
	public void setEnabled(boolean enabled) { this.enabled = enabled; }
	
	public static void ensureOnlyOneEnabled(Option[] options, int changedIndex, int... indices)
	{
		boolean careAboutChangedIndex = false;
		for ( int index : indices )
			if ( index == changedIndex )
			{
				careAboutChangedIndex = true;
				break;
			}
		
		if ( !careAboutChangedIndex )
			return;
		
		if ( options[changedIndex].isEnabled() )
		{// turned on; turn the others off
			for ( int index : indices )
				if ( index != changedIndex )
					options[index].setEnabled(false);
		}
		else
		{// turned off; if all are off, turn this one back on
			boolean allOff = true;
			for ( int index : indices )
				if ( options[index].isEnabled() )
				{
					allOff = false;
					break;
				}
			if ( allOff )
				options[changedIndex].setEnabled(true);
		}
	}
	
	public static void ensureAtLeastOneEnabled(Option[] options, int changedIndex, int... indices)
	{
		boolean careAboutChangedIndex = false;
		for ( int index : indices )
			if ( index == changedIndex )
			{
				careAboutChangedIndex = true;
				break;
			}
		
		if ( !careAboutChangedIndex )
			return;
		
		if ( !options[changedIndex].isEnabled() )
		{// turned off; if all are off, turn this one back on
			boolean allOff = true;
			for ( int index : indices )
				if ( options[index].isEnabled() )
				{
					allOff = false;
					break;
				}
			if ( allOff )
				options[changedIndex].setEnabled(true);
		}
	}
}