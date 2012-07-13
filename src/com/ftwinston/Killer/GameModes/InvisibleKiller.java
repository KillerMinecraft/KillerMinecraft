package com.ftwinston.Killer.GameModes;

import com.ftwinston.Killer.GameMode;
import com.ftwinston.Killer.Killer;
import com.ftwinston.Killer.PlayerManager;

import org.bukkit.ChatColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionType;
import org.bukkit.Material;

public class InvisibleKiller extends GameMode
{
	@Override
	public String getName() { return "Invisible Killer"; }

	@Override
	public int absMinPlayers() { return 2; }

	@Override
	public boolean killersCompassPointsAtFriendlies() { return true; }

	@Override
	public boolean friendliesCompassPointsAtKiller() { return true; }

	@Override
	public boolean discreteDeathMessages() { return false; }

	@Override
	public boolean usesPlinth() { return true; }

	@Override
	public int determineNumberOfKillersToAdd(int numAlive, int numKillers, int numAliveKillers)
	{
		// if we're not set to auto-reassign the killer once one has been assigned at all, even if they're no longer alive / connected, don't do so
		if ( !Killer.instance.autoReassignKiller && numKillers > 0 )
			return 0;
		
		// for now, one living killer at a time is plenty
		return numAliveKillers > 0 ? 0 : 1;
	}
	
	@Override
	public String describePlayer(boolean killer)
	{
		if ( killer )
			return "killer";
		else
			return "friendly player";
	}
	
	@Override
	public boolean informOfKillerAssignment(PlayerManager pm) { return true; }
	
	@Override
	public boolean informOfKillerIdentity(PlayerManager pm) { return true; }
	
	@Override
	public boolean playerJoined(Player player, PlayerManager pm, boolean isNewPlayer, boolean isKiller, int numKillersAssigned)
	{
		Killer plugin = Killer.instance;
	
		if ( isKiller ) // inform them that they're still a killer
			player.sendMessage("Welcome back. " + ChatColor.RED + "You are still " + (numKillersAssigned > 1 ? "a" : "the" ) + " killer!"); 
		else if ( isNewPlayer ) // this is a new player, tell them the rules & state of the game
		{
			String message = "Welcome to Killer Minecraft! One player ";
			message += numKillersAssigned > 0 ? "has been" : "will soon be";
			message += " assigned as the killer, and must kill the rest. They will be invisible, but will become briefly visible when damaged. To win, the other players must bring a ";
			
			message += plugin.tidyItemName(plugin.winningItems[0]);
			
			if ( plugin.winningItems.length > 1 )
			{
				for ( int i=1; i<plugin.winningItems.length-1; i++)
					message += ", a " + plugin.tidyItemName(plugin.winningItems[i]);
				
				message += " or a " + plugin.tidyItemName(plugin.winningItems[plugin.winningItems.length-1]);
			}
			
			message += " to the plinth near the spawn. To help, they all get infinity bows and splash damage potions, and compasses will point at the killer.";
			player.sendMessage(message);
			
			if ( numKillersAssigned > 0 && plugin.lateJoinersStartAsSpectator )
				return false; // not alive, should be a spectator
		}
		else
			player.sendMessage("Welcome back. You are not the killer, and you're still alive.");
			
		return true; // this player should now be alive
	}
	
	@Override
	public void prepareKiller(Player player, PlayerManager pm)
	{
		pm.makePlayerInvisibleToAll(player);
		
		PlayerInventory inv = player.getInventory();
		
		inv.addItem(new ItemStack(Material.COMPASS, 1));
		inv.addItem(new ItemStack(Material.COOKED_BEEF, 10));
	}
	
	@Override
	public void prepareFriendly(Player player, PlayerManager pm)
	{
		PlayerInventory inv = player.getInventory();
		
		ItemStack stack = new ItemStack(Material.BOW, 1);
		stack.addEnchantment(Enchantment.ARROW_INFINITE, 1);
		inv.addItem(stack);
		
		// give some splash potions of damage
		Potion pot = new Potion(PotionType.INSTANT_DAMAGE);
		pot.setLevel(1);
		pot.splash();
		stack = pot.toItemStack(Math.min(2, 64 / (pm.numSurvivors() - 1)));
		inv.addItem(stack);
		
		
		// give them the items needed to make a compass
		inv.addItem(new ItemStack(Material.IRON_INGOT, 4));
		inv.addItem(new ItemStack(Material.REDSTONE, 1));
	}
	
	@Override
	public void checkForEndOfGame(PlayerManager pm, Player playerOnPlinth, Material itemOnPlinth)
	{
		// if there's no one alive at all, game was drawn
		if (pm.numSurvivors() == 0 )
		{
			pm.gameFinished(false, false, null, null);
			return;
		}
		
		// if someone stands on the plinth with a winning item, the friendlies win
		if ( playerOnPlinth != null && itemOnPlinth != null )
		{
			pm.gameFinished(false, true, playerOnPlinth.getName(), itemOnPlinth);
			return;
		}
		
		boolean killersAlive = false, friendliesAlive = false;
		for ( String survivor : pm.getSurvivors() )
			if ( pm.isKiller(survivor) )
				killersAlive = true;
			else
				friendliesAlive = true;
		
		// if only killers are left alive, the killer won
		if ( killersAlive && !friendliesAlive )
			pm.gameFinished(true, false, null, null);
		// if only friendlies are left alive, the friendlies won
		else if ( !killersAlive && friendliesAlive )
			pm.gameFinished(false, true, null, null);
	}
	
	/*
	 * Still to implement
	 * 
make killer invisible to everyone

disable buckets for killer (within 5 blocks of other players)

assign killer immediately

reveal killer on assigning

MOVE killer on assigning

when killer takes damage, reveal for 5 seconds
	 */
}
