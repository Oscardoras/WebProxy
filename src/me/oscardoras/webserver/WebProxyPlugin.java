package me.oscardoras.webserver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import me.oscardoras.bungeeutils.BungeePlugin;
import me.oscardoras.webutils.BungeeWebServer;
import me.oscardoras.webutils.WebRequest;
import me.oscardoras.webutils.WebServer;
import me.oscardoras.webutils.WebServer.Responder;
import net.md_5.bungee.api.event.ProxyReloadEvent;
import net.md_5.bungee.event.EventHandler;

public final class WebProxyPlugin extends BungeePlugin implements Responder {
	
	public static WebProxyPlugin plugin;
	
	public WebProxyPlugin() {
		plugin = this;
	}
	
	
	protected WebServer webServer = null;
	
	@Override
	public void onEnable() {
		File folder = new File("web");
		if (!folder.exists()) folder.mkdirs();
		
		try {
			webServer = BungeeWebServer.newBungeeWebServer(this, this);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void onDisable() {
		if (webServer != null) webServer.stop();
	}
	
	@EventHandler
	public void onReload(ProxyReloadEvent e) {
		onDisable();
		onEnable();
	}
	
	@Override
	public void respond(WebRequest request) throws IOException {
		String method = request.getRequestMethod();
		String path = request.getPath();
		
		if (path.equals("/favicon.ico")) {
			if (method.equals("GET") || method.equals("HEAD")) {
				request.getResponseHeaders().set("Content-Type", "image/x-icon");
				File file = new File("server-icon.png");
				if (file.isFile()) {
					request.sendResponseHeaders(200);
					if (method.equals("GET")) request.getResponseBody().write(Files.readAllBytes(Paths.get(file.getPath())));
				} else request.sendResponseHeaders(404);
			} else request.sendResponseHeaders(405);
		} else {
			File file = new File("web/" + path);
			if (file.exists() && !file.getName().startsWith(".") && !file.isHidden() && file.getCanonicalPath().startsWith(new File("web").getCanonicalPath())) {
				if (file.isFile()) request.respondFile(file);
				else if (file.isDirectory()) {
					File index = null;
					for (File f : file.listFiles()) {
						if (f.isFile() && f.getName().split("\\.")[0].equals("index")) {
							index = f;
							break;
						}
					}
					if (index != null) request.respondFile(index);
					else {
						request.getResponseHeaders().set("Content-Type", "text/html");
						request.sendResponseHeaders(200);
						if (request.getRequestMethod().equals("GET")) {
							String html = "<!DOCTYPE HTML><html><head><title>Index of " + path + "</title></head><body><h1>Index of " + path + "</h1>";
							html += path.equals("/") ? "" : ("<li><a href=\"" + (path.replaceFirst("/", "").contains("/") ? path.substring(0, path.lastIndexOf("/")) : "/") + "\"> Parent Directory</a></li>");
							for (File child : file.listFiles()) {
								if (!child.getName().startsWith(".") && !child.isHidden()) html += "<li><a href=\"" + path + "/" + child.getName() + "\"> " + child.getName() + "</a></li>";
							}
							html += "</ul></body></html>";
							request.getResponseBody().write(html.getBytes());
						}
					}
				} else request.respond404();
			} else request.respond404();
		}
		request.close();
	}
	
}