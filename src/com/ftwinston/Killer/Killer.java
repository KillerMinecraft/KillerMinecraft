package com.ftwinston.Killer;

/*
 * Killer Minecraft
 * a Minecraft Bukkit Mod by 
 * David "Canazza" McQuillan and
 * Andrew Winston "FTWinston" Winston Watkins AKA Winston
 * Created 18/06/2012
 */

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import com.ftwinston.Killer.CraftBukkit.CraftBukkitAccess;

public class Killer extends JavaPlugin
{
	public static Killer instance;
	CraftBukkitAccess craftBukkit;
	public Logger log = Logger.getLogger("Minecraft");

	boolean stagingWorldIsServerDefault;
	
	EventManager eventListener = new EventManager(this);
	WorldManager worldManager;
	PlayerManager playerManager;
	RecipeManager recipeManager;
	VoteManager voteManager;
	StatsManager statsManager;
	Game[] games;

	World stagingWorld;
	int stagingWorldUpdateProcess = -1;
	
	public static CraftBukkitAccess craftBukkitHelper()
	{
		return instance.craftBukkit;
	}
	
	public ChunkGenerator getDefaultWorldGenerator(String worldName, String id)
	{
		return stagingWorld == null ? new StagingWorldGenerator() : null;
	}
	
	public void onEnable()
	{
        instance = this;
        craftBukkit = CraftBukkitAccess.createCorrectVersion(this);
        if ( craftBukkit == null )
        {
        	setEnabled(false);
        	return;
        }
        
        Settings.setup(this);
        
        GameConfiguration.instance = new GameConfiguration();
        worldManager = new WorldManager(this);
        playerManager = new PlayerManager(this);
        recipeManager = new RecipeManager();
        voteManager = new VoteManager(this);

        if ( Settings.nothingButKiller )
        	worldManager.hijackDefaultWorld(Settings.stagingWorldName);
		
        // delay this by 1 tick so that the plugins are all loaded and the worlds are generated 
        getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
			@Override
			public void run() {
				if ( GameMode.gameModes.size() == 0 )
				{
					log.warning("Killer cannot start: No game modes have been loaded!");
					log.warning("Add some game mode plugins to your server!");
					setEnabled(false);
					return;
				}
				if ( WorldGenerator.worldGenerators.size() == 0 )
				{
					log.warning("Killer cannot start: No world generators have been loaded!");
					log.warning("Add some world generator plugins to your server!");
					setEnabled(false);
					return;
				}
				
				if ( stagingWorld == null )
				{
					stagingWorld = getServer().getWorld(Settings.stagingWorldName);
					
					if ( stagingWorld == null )
						worldManager.createStagingWorld(Settings.stagingWorldName);
				}
				
				if ( !Settings.setupGames(Killer.instance) )
				{
					setEnabled(false);
					return;
				}
								
				statsManager = new StatsManager(Killer.instance, Killer.instance.games.length);
		        getServer().getPluginManager().registerEvents(eventListener, Killer.instance);
		        
		        // because listening for ChunkLoadEvent is unreliable, if the relevant chunk isn't loaded when we try to write a sign,
		        // it's saved off in this map and updated when necessary.
		        stagingWorldUpdateProcess = getServer().getScheduler().scheduleSyncRepeatingTask(instance, new Runnable() {
					public void run()
					{
						boolean updateAny = false;
						Iterator<Map.Entry<Location, String[]>> it = Game.signsNeedingUpdated.entrySet().iterator();
					    while (it.hasNext()) {
					        Map.Entry<Location, String[]> pair = (Map.Entry<Location, String[]>)it.next();

				        	if ( craftBukkit.isChunkGenerated(pair.getKey().getChunk()))
				        	{
				        		if ( Game.writeSign(pair.getKey(), pair.getValue()) )
				        			updateAny = true;
				        		
				        		it.remove();
				        	}
					    }
				    
				    	for ( Game game : games )
				    	{
				    		if ( updateAny )
				    			game.drawProgressBar();
				    		game.checkRenderer();
				    	}
					}
				}, 20L, 120L); // check every 6 seconds
			}
		}, 1);
        
		// remove existing Killer world files
		worldManager.deleteWorldFolders(Settings.killerWorldNamePrefix + "_");
	}
	
	public void onDisable()
	{
		if ( stagingWorldUpdateProcess != -1 )
		{
			getServer().getScheduler().cancelTask(stagingWorldUpdateProcess);
			stagingWorldUpdateProcess = -1;
		}
		
		if ( games != null )
			for ( Game game : games )
				playerManager.reset(game);
		
		worldManager.onDisable();
		
		craftBukkit = null;
        playerManager = null;
        worldManager = null;
        voteManager = null;
        statsManager = null;
	}
	
	static void registerGameMode(GameModePlugin plugin)
	{
		plugin.initialize(instance);
	}
	
	static void registerWorldGenerator(WorldGeneratorPlugin plugin)
	{
		plugin.initialize(instance);
	}
	
	List<Recipe> monsterRecipes = new ArrayList<Recipe>();
	ShapedRecipe dispenserRecipe;
	ShapelessRecipe enderRecipe;

	boolean isDispenserRecipe(Recipe recipe)
	{
		if ( recipe.getResult().getType() != dispenserRecipe.getResult().getType() || !(recipe instanceof ShapedRecipe) )
    		return false;
    	
    	// this is *a* dispenser recipe. it's the right one if it includes a sapling in the ingredients
    	ShapedRecipe shaped = (ShapedRecipe)recipe;
    	for ( ItemStack ingredient : shaped.getIngredientMap().values() )
    		if ( ingredient.getType() == Material.SAPLING )
				return true;
    	
		return false;	
	}
	
	boolean isEnderEyeRecipe(Recipe recipe)
	{
		return recipe == enderRecipe; // any reason not to do this? 
		/*
		if ( recipe.getResult().getType() != enderRecipe.getResult().getType() || !(recipe instanceof ShapelessRecipe) )
    		return false;
    	
    	// this is *an* eye of ender recipe. it's the right one if it includes a spider eye in the ingredients
    	ShapelessRecipe shapeless = (ShapelessRecipe)recipe;
    	for ( ItemStack ingredient : shapeless.getIngredientList() )
    		if ( ingredient.getType() == Material.SPIDER_EYE )
    			return true;
    			
    	return false;*/
	}
	
	boolean isMonsterEggRecipe(Recipe recipe)
	{
		return recipe.getResult().getType() == Material.MONSTER_EGG;
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		return CommandHandler.onCommand(this, sender, cmd, label, args);
	}
	
	public Game getGameForWorld(World w)
	{
		if ( w == stagingWorld )
			return null;
		
		for ( Game game : games )
			for ( World world : game.getWorlds() )
				if ( w == world )
					return game;
		
		return null;
	}
	
	public Game getGameForPlayer(Player player)
	{
		Game game = getGameForWorld(player.getWorld());
		if ( game == null )
		{// check if this player is a member of any game
			for ( Game g : games )
				if ( g.getPlayerInfo().containsKey(player.getName()))
					return g;
		}
		return game;
	}
}