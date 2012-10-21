package com.github.Heliwr.RegexFilter;


import java.io.*;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.*;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;

import java.util.logging.Logger;


/**
* Regular Expression chat filter plugin for Bukkit
*
* @author FloydATC
*/
public class RegexFilter extends JavaPlugin {
    
    String baseDir = "plugins/RegexFilter";
    String configFile = "settings.txt";

    public CopyOnWriteArrayList<String> rules = new CopyOnWriteArrayList<String>();
    private ConcurrentHashMap<String, Pattern> patterns = new ConcurrentHashMap<String, Pattern>(); 
	public final Logger logger = Logger.getLogger("Minecraft.RegexFilter");

    public void onDisable() {
    	rules.clear();
    	patterns.clear();

        // NOTE: All registered events are automatically unregistered when a plugin is disabled
    	
        // EXAMPLE: Custom code, here we just output some info so we can check all is well
    	PluginDescriptionFile pdfFile = this.getDescription();
		logger.info( pdfFile.getName() + " version " + pdfFile.getVersion() + " is disabled!" );
    }

    public void onEnable() {
    	loadRules();
    	
        // Register our events
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new RegexFilterPlayerListener(this), this);
        
    	// EXAMPLE: Custom code, here we just output some info so we can check all is well
        PluginDescriptionFile pdfFile = this.getDescription();
		logger.info( pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!" );
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args ) {
    	String cmdname = cmd.getName().toLowerCase();
        Player player = null;
        if (sender instanceof Player) {
        	player = (Player)sender;
        }
        
        if (cmdname.equals("regex") && args.length > 0) {
        	if (player == null || player.isOp()) {
	        	if (args[0].equalsIgnoreCase("reload")) {
	        		if (player != null) {
	        			player.sendMessage("[Filter] Reloading rules.txt");
		        		logger.info("[Filter] rules.txt reloaded by " + player.getName());
	        		} else {
		        		logger.info("[Filter] rules.txt reloaded from server console");
	        		}
	        		rules.clear();
	        		patterns.clear();
	        		loadRules();
	        	}
        	} else {
        		logger.info("[Filter] Command access denied for " + player.getName() + " (Not an operator)");
        	}
    		return true;
        }
        
        return false;
    }
    
    private void loadRules() {
    	String fname = "plugins/RegexFilter/rules.txt";
    	File f;
    	
    	// Ensure that directory exists
    	String pname = "plugins/RegexFilter";
    	f = new File(pname);
    	if (!f.exists()) {
    		if (f.mkdir()) {
    			logger.info( "[Filter] Created directory '" + pname + "'" );
    		}
    	}
    	// Ensure that rules.txt exists
    	f = new File(fname);
    	if (!f.exists()) {
			BufferedWriter output;
			String newline = System.getProperty("line.separator");
			try {
				output = new BufferedWriter(new FileWriter(fname));
				output.write("# Each rule must have one 'match' statement and atleast one 'then' statement" + newline);
				output.write("# match <regular expression>" + newline);
				output.write("# then replace <string>|warn [<string>]|log|deny|debug" + newline);
				output.write("" + newline);
				output.write("# Example 1:" + newline);
				output.write("match f+u+c+k+" + newline);
				output.write("then replace cluck" + newline);
				output.write("then warn Watch your language please" + newline);
				output.write("then log" + newline);
				output.write("" + newline);
				output.write("# Example 2:" + newline);
				output.write("match dick" + newline);
				output.write("then replace duck" + newline);
				output.write("" + newline);
				output.write("# Emulate DotFilter" + newline);
				output.write("match ^\\.[a-z]+" + newline);
				output.write("then warn" + newline);
				output.write("then deny" + newline);
				output.write("" + newline);
				output.write("# Emulate 7Filter" + newline);
				output.write("match ^7[a-z]+" + newline);
				output.write("then warn" + newline);
				output.write("then deny" + newline);
				output.close();
    			logger.info( "[Filter] Created config file '" + fname + "'" );
			} catch (Exception e) {
				e.printStackTrace();
			}
    	}

    	
    	try {
        	BufferedReader input =  new BufferedReader(new FileReader(fname));
    		String line = null;
    		while (( line = input.readLine()) != null) {
    			line = line.trim();
    			if (!line.matches("^#.*") && !line.matches("")) {
    				rules.add(line);
    				if (line.startsWith("match ") || line.startsWith("replace ")) {
    					String[] parts = line.split(" ", 2);
    					compilePattern(parts[1]);
    				}
    			}
    		}
    		input.close();
    	}
    	catch (FileNotFoundException e) {
    		logger.warning("[Filter] Error reading config file '" + fname + "': " + e.getLocalizedMessage());
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    	}

    }
   
    private void compilePattern(String re) {
    	// Do not re-compile if we already have this pattern 
    	if (patterns.get(re) == null) {
    		try {
    			Pattern pattern = Pattern.compile(re, Pattern.CASE_INSENSITIVE);
    			patterns.put(re, pattern);
    			logger.fine("[Filter] Successfully compiled regex: " + re);
    		}
    		catch (PatternSyntaxException e) {
    			logger.warning("[Filter] Failed to compile regex: " + re);
    			logger.warning("[Filter] " + e.getMessage());
    		}
    		catch (Exception e) {
    			logger.severe("[Filter] Unexpected error while compiling expression '" + re + "'");
    			e.printStackTrace();
    		}
    	}
    }
    
    public Boolean matchPattern(String msg, String re_from) {
    	Pattern pattern_from = patterns.get(re_from);
    	if (pattern_from == null) {
    		// Pattern failed to compile, ignore
			logger.info("[Filter] Ignoring invalid regex: " + re_from);
    		return false;
    	}
    	Matcher matcher = pattern_from.matcher(msg);
    	return matcher.find();
    }
    
    public String replacePattern(String msg, String re_from, String to) {
    	Pattern pattern_from = patterns.get(re_from);
    	if (pattern_from == null) {
    		// Pattern failed to compile, ignore
    		return msg;
    	}
    	Matcher matcher = pattern_from.matcher(msg);
    	return matcher.replaceAll(to);
    }
}

