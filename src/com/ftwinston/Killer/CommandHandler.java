package com.ftwinston.Killer;

import java.util.HashSet;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import com.ftwinston.Killer.Game.GameState;
import com.ftwinston.Killer.StagingWorldManager.StagingWorldOption;

public class CommandHandler
{
	public static boolean spectatorCommand(Killer plugin, CommandSender sender, Command cmd, String label, String[] args)
	{
		if ( !(sender instanceof Player) )
			return false;
		
		if ( args.length == 0 )
		{
			sender.sendMessage("Usage: /spec main, /spec nether, /spec <player name>, or /spec follow");
			return true;
		}
		Player player = (Player)sender;
		
		Game game = plugin.getGameForWorld(player.getWorld());
		if ( game == null || Helper.isAlive(game, player) )
		{
			sender.sendMessage("Only spectators can use this command");
			return true;
		}
		
		if ( args[0].equalsIgnoreCase("main") )
		{
			plugin.playerManager.teleport(player, game.getGameMode().getSpawnLocation(player));
		}
		else if ( args[0].equalsIgnoreCase("nether") )
		{
			World nether = null;
			for ( World world : game.getWorlds() )
				if ( world.getEnvironment() == Environment.NETHER )
				{
					nether = world;
					break;
				}
			
			if ( nether != null )
				plugin.playerManager.teleport(player, nether.getSpawnLocation());
			else
				sender.sendMessage("Nether world not found, please try again");
		}
		else
		{
			Player other = plugin.getServer().getPlayer(args[0]);
			if ( other == null || !other.isOnline() || game != plugin.getGameForPlayer(other) || !Helper.isAlive(game, other))
				sender.sendMessage("Player not found: " + args[0]);
			else if ( Helper.getTargetName(game, player) != null )
				Helper.setTargetOf(game,  player, other);
			
			plugin.playerManager.moveToSee(player, other);
		}
		
		return true;
	}

	public static boolean teamChat(Killer plugin, CommandSender sender, Command cmd, String label, String[] args)
	{
		if ( !(sender instanceof Player) )
			return true;
		
		if ( args.length == 0 )
		{
			sender.sendMessage("Usage: /team <message>");
			return true;
		}
		
		Player player = (Player)sender;
		Game game = plugin.getGameForPlayer(player);
		
		if ( game == null || !game.getGameState().usesGameWorlds )
			return true;
		
		if ( game.getGameMode().teamAllocationIsSecret() )
		{
			sender.sendMessage("Team chat is not available in " + game.getGameMode().getName() + " mode");
			return true;
		}
		
		String message = "[Team] " + ChatColor.RESET + args[0];
		for ( int i=1; i<args.length; i++ )
			message += " " + args[i];
		
		PlayerManager.Info info = game.getPlayerInfo().get(player.getName());
		List<Player> recipients = game.getOnlinePlayers(new PlayerFilter().team(info.getTeam()));
		
		// most of this code is a clone of the actual chat code in NetServerHandler.chat
		AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(false, player, "ignored", new HashSet<Player>(recipients));
		plugin.getServer().getPluginManager().callEvent(event);

		if (event.isCancelled())
			return true;
	
		message = String.format(event.getFormat(), player.getDisplayName(), message);
		plugin.getServer().getConsoleSender().sendMessage(message);
		
		for (Player recipient : event.getRecipients())
			recipient.sendMessage(message);
		
		return true;
	}

	public static boolean killerCommand(Killer plugin, CommandSender sender, Command cmd, String label, String[] args)
	{
		Player player;
		if ( sender instanceof Player )
			player = (Player)sender;
		else
			player = null;
		
		// players, op or otherwise, can use /killer join and /killer quit,
		// ONLY IF the staging world isn't the server default.
		if ( !plugin.stagingWorldIsServerDefault )
		{
			if ( args.length > 0 )
			{
				String firstParam = args[0].toLowerCase();
				if ( firstParam.equals("join") )
				{
					if ( player == null )
					{
						sender.sendMessage("Only players can run this command");
						return true;
					}
					Game game = plugin.getGameForWorld(player.getWorld());
					if ( player.getWorld() == plugin.stagingWorld || game != null )
					{
						sender.sendMessage("You are already in Killer Minecraft, so you can't join again!");
						return true;
					}
					
					if ( !plugin.playerManager.isInventoryEmpty(player.getInventory()) )
					{
						sender.sendMessage("You must have a completely empty inventory to join Killer Minecraft!");
						return true;
					}
					
					plugin.playerManager.movePlayerIntoKillerGame(player);
					return true;
				}
				else if ( firstParam.equals("quit") || firstParam.equals("exit"))
				{
					if ( player == null )
					{
						sender.sendMessage("Only players can run this command");
						return true;
					}
					Game game = plugin.getGameForWorld(player.getWorld());
					if ( player.getWorld() != plugin.stagingWorld && game == null )
					{
						sender.sendMessage("You are not in Killer Minecraft, so you can't quit!");
						return true;
					}
					
					plugin.playerManager.movePlayerOutOfKillerGame(player);
					return true;
				}
				else if ( firstParam.equals("game") )
				{
					if ( player == null )
					{
						sender.sendMessage("Only players can run this command");
						return true;
					}
					
					Game game = plugin.getGameForPlayer(player);
					if ( game == null )
					{
						if ( player.getWorld() == plugin.stagingWorld )
							sender.sendMessage("You're in the Killer staging world, but you're not in a game");
						else
							sender.sendMessage("You're not in a Killer game");
					}	
					else if ( player.getWorld() == plugin.stagingWorld )
						sender.sendMessage("You're in the the setup room for Killer game #" + (game.getNumber()+1));
					else
						sender.sendMessage("You're in Killer game #" + (game.getNumber()+1));
					
					return true;
				}
			}
			if ( player != null && !player.isOp() )
			{
				sender.sendMessage("Invalid command: Use /killer join to enter the game, and /killer quit to leave it");
				return true;
			}
		}
	
		// op players and non-players can use the others
	
		if ( player != null && !player.isOp() )
		{
			sender.sendMessage("You must be a server op to run this command");
			return true;
		}
		
		if ( args.length == 0 )
		{
			if ( !plugin.stagingWorldIsServerDefault && player != null )
				sender.sendMessage("Usage: /killer join, /killer quit, /killer game, /killer restart, /killer end");
			else
				sender.sendMessage("Usage: /killer restart, /killer end");
			return true;
		}
		
		String firstParam = args[0].toLowerCase();
		if ( firstParam.equals("restart") )
		{
			Game game = plugin.getGameForPlayer(player);
			if ( game != null && game.getGameState().usesGameWorlds )
			{
				game.forcedGameEnd = true;
				game.getGameMode().gameFinished();
				game.restartGame(sender);
			}
		}
		else if ( firstParam.equals("end") )
		{
			Game game = plugin.getGameForPlayer(player);
			if ( game != null && game.getGameState().usesGameWorlds )
			{
				game.forcedGameEnd = true;
				game.getGameMode().gameFinished();
				game.endGame(sender);
			}
		}
		else
			sender.sendMessage("Invalid parameter: " + args[0] + " - type /killer to list allowed parameters");
		
		return true;
	}
	
	public static boolean stagingWorldCommand(Killer plugin, CommandSender sender, Command cmd, String label, String[] args)
	{
/*
Command block commands will be as follows:

|setup game X start
|setup game X confirm
|setup game X cancel
|setup game X difficulty up
|setup game X difficulty down
|setup game X monsters up
|setup game X monsters down
|setup game X animals up
|setup game X animals down
|setup game X show modes
|setup game X show worlds
|setup game X show modeconfig
|setup game X show worldconfig
|setup game X show misc
setup game X option Y
setup game X playerlimit on/off/up/down

setup game X include @p
setup game X exclude @p

|setup arena X spleef
|setup arena X survival
setup arena X reset
|setup arena X equip @p

setup tp @p pos CX CY CZ
setup tp @p game X
setup tp @p game X else CX CY CZ
setup tp @p exit

setup block TYPE DATA CX CY CZ
setup block TYPE DATA CX1 CY1 CZ1 CX2 CY2 CZ2
*/
		if ( args.length < 3 )
		{
			plugin.log.warning("Invalid command: too few arguments");
			return true;
		}
		
		if ( args[0] == "game" )
		{
			int num;
			try
			{
				num = Integer.parseInt(args[1]);
			}
			catch ( NumberFormatException ex )
			{
				plugin.log.warning("Command error: invalid game #: " + args[1]);
				return true;
			}
			
			if ( num > 0 && num <= plugin.games.length )
				gameCommand(plugin, plugin.games[num-1], args);
			else
				plugin.log.warning("Command error: no game #" + num);
		}
		else if ( args[0] == "arena" )
		{
			int num;
			try
			{
				num = Integer.parseInt(args[1]);
			}
			catch ( NumberFormatException ex )
			{
				plugin.log.warning("Command error: invalid arena #: " + args[1]);
				return true;
			}
			
			if ( num > 0 && num <= 1) // how to tell how many arenas we have?
				arenaCommand(plugin, num-1, args);
			else
				plugin.log.warning("Command error: no arena #" + num);
		}
		else if ( args[0] == "tp" )
		{
			teleportCommand(plugin, args);
		}
		else if ( args[0] == "block" )
		{
			
		}
		return true;
	}
	
	private static void gameCommand(Killer plugin, Game game, String[] args)
	{
		if ( game == null || !game.getGameState().canChangeGameSetup )
			return;
		
		String cmd = args[2];
		if ( cmd == "start" ) 
		{
			if ( game.getOnlinePlayers().size() >= game.getGameMode().getMinPlayers() )
			{
				game.setCurrentOption(StagingWorldOption.NONE);
				game.setGameState(GameState.waitingToGenerate);
			}
			else
				game.setGameState(GameState.stagingWorldConfirm);
		}
		else if ( cmd == "confirm" ) 
		{
			game.setCurrentOption(StagingWorldOption.NONE);
			game.setGameState(GameState.waitingToGenerate);
		}
		else if ( cmd == "cancel" )
			game.setGameState(GameState.stagingWorldSetup);
		if ( cmd == "difficulty" )
			changeDifficulty(plugin, game, args.length > 3 ? args[3] : "");
		else if ( cmd == "monsters" )
			changeMonsterNumbers(plugin, game, args.length > 3 ? args[3] : "");
		else if ( cmd == "animals" )
			changeAnimalNumbers(plugin, game, args.length > 3 ? args[3] : "");
		else if ( cmd == "show" )
			showGameOption(plugin, game, args.length > 3 ? args[3] : "");
		else if ( cmd == "option" )
			gameOptionChanged(plugin, game, args.length > 3 ? args[3] : "");
		else if ( cmd == "playerlimit" )
			gamePlayerLimit(plugin, game, args.length > 3 ? args[3] : "");
		else if ( cmd == "include" ) 
		{
			if ( args.length <= 3 )
			{
				plugin.log.warning("Player name not specified");
				return;
			}
			
			Player player = plugin.getServer().getPlayer(args[3]);
			if ( player == null || !player.isOnline() )
			{
				plugin.log.warning("Player not found: " + args[3]);
				return;									
			}
			
			if ( game.getGameState().usesGameWorlds )
			{
				if ( !Settings.allowLateJoiners )
				{
					plugin.log.warning("Cannot join: game is in progress");
					return;
				}
				if ( game.usesPlayerLimit() && game.getPlayers().size() >= game.getPlayerLimit() )
				{
					plugin.log.warning("Cannot join: game is full");
					return;
				}
				
				plugin.playerManager.teleport(player, game.getGameMode().getSpawnLocation(player));
			}
			else
				player.teleport(plugin.stagingWorldManager.getGameSetupSpawnLocation(game.getNumber()));
			plugin.playerManager.putPlayerInGame(player, game);
			plugin.stagingWorldManager.updateGameInfoSigns(game);
		}
		else if ( cmd == "exclude" ) 
		{
			if ( args.length <= 3 )
			{
				plugin.log.warning("Player name not specified");
				return;
			}
			
			Player player = plugin.getServer().getPlayer(args[3]);
			if ( player == null || !player.isOnline() )
			{
				plugin.log.warning("Player not found: " + args[3]);
				return;									
			}
			
			plugin.playerManager.removePlayerFromGame(player, game);
			plugin.stagingWorldManager.updateGameInfoSigns(game);
		}
		else
			plugin.log.warning("Invalid game command: " + cmd);
	}

	private static void gamePlayerLimit(Killer plugin, Game game, String cmd)
	{
		if ( cmd == "on")
			game.setUsesPlayerLimit(true);
		else if ( cmd == "off")
			game.setUsesPlayerLimit(false);
		else if ( cmd == "up")
		{
			int limit = game.getPlayerLimit();
			if ( limit < Settings.maxPlayerLimit )
				game.setPlayerLimit(limit+1);
			else
				return;
		}
		else if ( cmd == "down")
		{
			int limit = game.getPlayerLimit();
			if ( limit > Settings.minPlayerLimit )
				game.setPlayerLimit(limit - 1);
			else
				return;
		}
		else
		{
			plugin.log.warning("Invalid player limit command - expected on/off/up/down, got: " + cmd);
			return;
		}
		
		plugin.stagingWorldManager.updateGameInfoSigns(game);
	}

	private static void changeDifficulty(Killer plugin, Game game, String param)
	{
		if ( param == "up ")
		{
			if ( game.getDifficulty().getValue() < Difficulty.HARD.getValue() )
			{
				game.setDifficulty(Difficulty.getByValue(game.getDifficulty().getValue()+1));
				game.updateSigns(StagingWorldManager.GameSign.DIFFICULTY);
			}
		}
		else if ( param == "down" )
		{
			if ( game.getDifficulty().getValue() > Difficulty.PEACEFUL.getValue() )
			{
				game.setDifficulty(Difficulty.getByValue(game.getDifficulty().getValue()-1));
				game.updateSigns(StagingWorldManager.GameSign.DIFFICULTY);
			}
		}
		else
			plugin.log.warning("Invalid difficulty parameter, expected up/down, got: " + param);
	}

	private static void changeMonsterNumbers(Killer plugin, Game game, String param)
	{
		if ( param == "up ")
		{
			if ( game.monsterNumbers < Game.maxQuantityNum )
			{
				game.monsterNumbers++;
				game.updateSigns(StagingWorldManager.GameSign.ANIMALS);
			}
		}
		else if ( param == "down" )
		{
			if ( game.monsterNumbers > Game.minQuantityNum )
			{
				game.monsterNumbers--;
				game.updateSigns(StagingWorldManager.GameSign.ANIMALS);
			}
		}
		else
			plugin.log.warning("Invalid monster number parameter, expected up/down, got: " + param);
	}
	
	private static void changeAnimalNumbers(Killer plugin, Game game, String param)
	{
		if ( param == "up ")
		{
			if ( game.animalNumbers < Game.maxQuantityNum )
			{
				game.animalNumbers++;
				game.updateSigns(StagingWorldManager.GameSign.ANIMALS);
			}
		}
		else if ( param == "down" )
		{
			if ( game.animalNumbers > Game.minQuantityNum )
			{
				game.animalNumbers--;
				game.updateSigns(StagingWorldManager.GameSign.ANIMALS);
			}
		}
		else
			plugin.log.warning("Invalid animal number parameter, expected up/down, got: " + param);
	}

	private static void showGameOption(Killer plugin, Game game, String option)
	{
		if ( option == "modes")
			game.setCurrentOption(game.getCurrentOption() == StagingWorldOption.GAME_MODE ? StagingWorldOption.NONE : StagingWorldOption.GAME_MODE);
		else if ( option == "worlds")
			game.setCurrentOption(game.getCurrentOption() == StagingWorldOption.WORLD ? StagingWorldOption.NONE : StagingWorldOption.WORLD);
		else if ( option == "modeconfig")
			game.setCurrentOption(game.getCurrentOption() == StagingWorldOption.GAME_MODE_CONFIG ? StagingWorldOption.NONE : StagingWorldOption.GAME_MODE_CONFIG);
		else if ( option == "worldconfig")
			game.setCurrentOption(game.getCurrentOption() == StagingWorldOption.WORLD_CONFIG ? StagingWorldOption.NONE : StagingWorldOption.WORLD_CONFIG);
		else if ( option == "misc")
			game.setCurrentOption(game.getCurrentOption() == StagingWorldOption.GLOBAL_OPTION ? StagingWorldOption.NONE : StagingWorldOption.GLOBAL_OPTION);
		else
			plugin.log.warning("Invalid game show option: " + option);
	}
	
	private static void gameOptionChanged(Killer plugin, Game game, String optionNum)
	{
		int num;
		try
		{
			num = Integer.parseInt(optionNum);
		}
		catch ( NumberFormatException ex )
		{
			plugin.log.warning("Command error: invalid option #: " + optionNum);
			return;
		}
		
		plugin.stagingWorldManager.gameOptionButtonPressed(game, game.getCurrentOption(), num);
	}
	
	private static void arenaCommand(Killer plugin, int num, String[] args)
	{
		String cmd = args[2];
		Arena arena = plugin.stagingWorldManager.getArena(num);
		if ( arena == null )
		{
			plugin.log.warning("Invalid arena command: number out of range (" + num + ")");
			return;
		}
		
		if ( cmd == "spleef" )
		{
			if ( arena.mode == Arena.Mode.SPLEEF )
				return;
			arena.mode = Arena.Mode.SPLEEF;
			arena.endMonsterArena();
			arena.updateIndicators();
		}
		else if ( cmd == "survival" )
		{
			if ( arena.mode == Arena.Mode.SURVIVAL )
				return;
			arena.mode = Arena.Mode.SURVIVAL;
			arena.endMonsterArena();
			arena.updateIndicators();
		}
		else if ( cmd == "reset" )
		{
			arena.rebuildArena();
			arena.endMonsterArena();
		}
		else if ( cmd == "equip" )
		{
			if ( args.length < 4 )
			{
				plugin.log.warning("Invalid arena command: missing player name");
				return;
			}

			Player player = plugin.getServer().getPlayer(args[3]);
			if ( player == null )
				plugin.log.warning("Invalid arena command: player not found (" + args[3] + ")");
			else
				arena.equipPlayer(player);
		}
		else
			plugin.log.warning("Invalid arena command: " + cmd);
	}
	
	private static void teleportCommand(Killer plugin, String[] args)
	{
		String playerName = args[1];
		String cmd = args[2];
		
		if ( cmd == "exit" )
		{
			// teleport out of killer
		}
		else if ( cmd == "pos" )
		{
			if ( args.length < 6 )
			{
				plugin.log.warning("Teleport command error: incomplete coordinates provided");
				return;
			}
			int cx, cy, cz;
			
			try
			{
				cx = Integer.parseInt(args[3]);
				cy = Integer.parseInt(args[4]);
				cz = Integer.parseInt(args[5]);
			}
			catch ( NumberFormatException ex )
			{
				plugin.log.warning("Teleport command error: invalid coordinates #: " + args[3] + " " + args[4] + " " + args[5]);
				return;
			}
			
			// teleport to the provided coordinates
		}
		else if ( cmd == "game" )
		{
			int num;
			try
			{
				num = Integer.parseInt(args[3]);
				if ( num < 0 || num > plugin.games.length)
				{
					plugin.log.warning("Teleport command error: no game #" + num);
					return;
				}
			}
			catch ( NumberFormatException ex )
			{
				plugin.log.warning("Teleport command error: invalid game #: " + args[3]);
				return;
			}
			
			Game game = plugin.games[num-1];
			
			if ( game.getGameState().usesGameWorlds )
			{
				; // teleport into this game
				return;
			}
			
			if ( args.length > 7 && args[4] == "else" )
			{
				int cx, cy, cz;
				
				try
				{
					cx = Integer.parseInt(args[5]);
					cy = Integer.parseInt(args[6]);
					cz = Integer.parseInt(args[7]);
				}
				catch ( NumberFormatException ex )
				{
					plugin.log.warning("Teleport command error: invalid coordinates #: " + args[5] + " " + args[6] + " " + args[7]);
					return;
				}
				
				// teleport to the provided coordinates
			}
		}
		else
			plugin.log.warning("Invalid teleport command: " + cmd);
	}
}