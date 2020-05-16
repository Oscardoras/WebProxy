package org.bungeeplugin.webproxy;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.webutils.WebRequest;
import org.webutils.WebServer;

import net.md_5.bungee.api.plugin.Plugin;

public class Web extends WebServer {

	public Web(Plugin plugin) throws IOException {
		super(plugin);
	}

	@Override
	public void onRequest(WebRequest request) throws IOException {
		Object object;
		try {
			object = WebProxyPlugin.plugin.redirections.get(request.getPath().split("/")[1]);
		} catch (ArrayIndexOutOfBoundsException ex) {
			object = null;
		}
		if (object != null) {
			try {
				Class<?> objectClass = object.getClass();
				Class<?> pluginClass = objectClass.getClassLoader().loadClass("org.webutils.WebRequest");
				Object pluginRequest = pluginClass.getConstructors()[0].newInstance(request.getHttpExchange());
				for (Method method : objectClass.getMethods()) {
					if (method.getName().equals("onRequest")) {
						method.invoke(object, pluginRequest);
						break;
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		} else {
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
	
}