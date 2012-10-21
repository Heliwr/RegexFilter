package com.github.Heliwr.RegexFilter;


import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.Listener;


/**
 * Handle events for all Player related events
 * @author FloydATC
 */
public class RegexFilterPlayerListener implements Listener {
    private final RegexFilter plugin;

    public RegexFilterPlayerListener(RegexFilter instance) {
        plugin = instance;
    }
    
    //Insert Player related code here
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String message = event.getMessage();
        Player player = event.getPlayer();
        String pname = player.getName();

    	Boolean cancel = false;
    	Boolean kick = false;
    	Boolean console = false;
    	String consolecmd = "";
    	String reason = "chat filter";
    	Boolean command = false;
    	Boolean matched = false;
    	String regex = "";
    	String matched_msg = "";
    	Boolean log = false;
    	String warning = "";
    	Boolean aborted = false;

    	Boolean valid;

    	// Apply rules
    	for (String line : plugin.rules) {
    		if (aborted) { break; } 
    		valid = false;
    		if (line.startsWith("match ")) {
    			regex = line.substring(6);
    			matched = plugin.matchPattern(message, regex);
    			if (matched) {
    				matched_msg = message;
    			}
    			valid = true;
    		}
    		if (matched) {
        		if (line.startsWith("ignore user ")) {
        			String users = line.substring(12);
    				valid = true;
        			for (String check : users.split(" ")) {
        				if (pname.equalsIgnoreCase(check)) {
        					matched = false;
        					break;
        				}
        			}
        		}
        		if (line.startsWith("ignore permission ")) {
        			String check = line.substring(18);
    				valid = true;
        			if (player.hasPermission(check)) {
        				matched = false;
        			}
        		}
        		if (line.startsWith("require user ")) {
        			String users = line.substring(13);
    				valid = true;
    				Boolean found = false;
        			for (String check : users.split(" ")) {
        				if (pname.equalsIgnoreCase(check)) {
        					found = true;
        					break;
        				}
        			}
        			matched = found;
        		}
				if (line.startsWith("then replace ") || line.startsWith("then rewrite ")) {
					message = plugin.replacePattern(message, regex, line.substring(13));
	    			valid = true;
				}
				if (line.matches("then replace")) {
					message = plugin.replacePattern(message, regex, "");
	    			valid = true;
				}
				if (line.startsWith("then warn ")) {
					warning = line.substring(10);
	    			valid = true;
				}
				if (line.matches("then warn")) {
					warning = event.getMessage();
	    			valid = true;
				}
				if (line.matches("then log")) {
					log = true;
	    			valid = true;
				}
				if (line.startsWith("then command ")) {
					message = line.substring(13).concat(" " + message);
					command = true;
	    			valid = true;
				}
				if (line.matches("then command")) {
					command = true;
	    			valid = true;
				}
				if (line.matches("then debug")) {
					System.out.println("[Filter] Debug match: " + regex);
					System.out.println("[Filter] Debug original: " + event.getMessage());
					System.out.println("[Filter] Debug matched: " + matched_msg);
					System.out.println("[Filter] Debug current: " + message);
					System.out.println("[Filter] Debug warning: " + (warning.equals("")?"(none)":warning));
					System.out.println("[Filter] Debug log: " + (log?"yes":"no"));
					System.out.println("[Filter] Debug deny: " + (cancel?"yes":"no"));
	    			valid = true;
				}
				if (line.startsWith("then deny")) {
					cancel = true;
	    			valid = true;
				}
				if (line.startsWith("then kick ")) {
					reason = line.substring(10);
	    			valid = true;
				}
				if (line.startsWith("then kick")) {
					kick = true;
	    			valid = true;
				}
				if (line.startsWith("then console ")) {
					consolecmd = line.substring(13);
					console = true;
					valid = true;
				}
				if (line.startsWith("then abort")) {
					aborted = true;
	    			valid = true;
				}
	    		if (valid == false) {
	    			plugin.logger.warning("[Filter] Ignored syntax error in rules.txt: " + line);    			
	    		}
    		}
    	}
    	
    	// Perform flagged actions
    	if (log) {
    		plugin.logger.info("[Filter] " +  player.getName() + "> " + event.getMessage());
    	}
    	if (!warning.matches("")) {
    		player.sendMessage("�4[Filter] " + warning);
    	}

    	if (cancel == true) {
    		event.setCancelled(true);
    	}
    	if (command == true) {
			// Convert chat message to command
			event.setCancelled(true);
			plugin.logger.info("[Filter] Helped " + player.getName() + " execute command: " + message);
			player.chat("/" + message);
		} else {
			event.setMessage(message);
		}
    	
    	if (kick) {
    		player.kickPlayer(reason);
    		plugin.logger.info("[Filter] Kicked " + player.getName() + ": " + reason);
    	}
    	if (console) {
    		consolecmd = consolecmd.replaceAll("&world", player.getLocation().getWorld().getName());
            consolecmd = consolecmd.replaceAll("&player", player.getName());
    		plugin.logger.info("[Filter] sending console command: " + consolecmd);
    		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), consolecmd);
    	}
    }    
}
