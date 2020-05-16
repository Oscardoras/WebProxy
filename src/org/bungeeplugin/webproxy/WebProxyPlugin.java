package org.bungeeplugin.webproxy;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.bungeeutils.BungeePlugin;
import org.webutils.WebServer;

import net.md_5.bungee.api.event.ProxyReloadEvent;
import net.md_5.bungee.event.EventHandler;

public final class WebProxyPlugin extends BungeePlugin {
	
	public static WebProxyPlugin plugin;
	
	public WebProxyPlugin() {
		plugin = this;
	}
	
	
	public final Map<String, WebServer> redirections = new HashMap<String, WebServer>();
	protected String address;
	protected Web web = null;
	
	@Override
	public void onEnable() {
		File folder = new File("web");
		if (!folder.exists()) folder.mkdirs();
		
		try {
			web = new Web(this);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		address = web.getAddress();
	}
	
	@Override
	public void onDisable() {
		if (web != null) web.stop();
	}
	
	@EventHandler
	public void onReload(ProxyReloadEvent e) {
		onDisable();
		onEnable();
	}
	
}