package com.markwoodbridge.justplayed;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;

import net.sf.jsr107cache.Cache;
import net.sf.jsr107cache.CacheException;
import net.sf.jsr107cache.CacheManager;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Node;
import nu.xom.Nodes;

import org.json.simple.JSONValue;

import com.google.appengine.api.memcache.jsr107cache.GCacheFactory;

@SuppressWarnings({"serial", "rawtypes", "unchecked"})
public class JustplayedServlet extends HttpServlet {
	private static final String NETWORKS = "networks";
	private Cache cache;
	private List networksTemplate;
	private Builder builder;

	@Override
	public void init() throws ServletException {
		super.init();
		try {
			Map props = new HashMap();
			props.put(GCacheFactory.EXPIRATION_DELTA, 60 * 1);
			cache = CacheManager.getInstance().getCacheFactory().createCache(props);
		} catch (CacheException e) {
			throw new ServletException(e);
		}
		try {
			networksTemplate = (List) JSONValue.parse(new FileReader("json/networks.json"));
		} catch (FileNotFoundException e) {
			throw new ServletException(e);
		}
		builder = new Builder();
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)	throws IOException {
		if ("/networks".equals(req.getPathInfo())) {
			List<Map> networks;
			if (cache.containsKey(NETWORKS)) {
				networks = (List) cache.get(NETWORKS);
			} else {
				networks = networksTemplate;
				for (Map network : networks) {
					URL nowplaying_url = new URL((String) network.get("nowplaying_url"));
					Map latest = (Map) JSONValue.parse(new InputStreamReader(nowplaying_url.openStream()));
					List nowplaying = latest.containsKey("nowplaying") ? (List) latest.get("nowplaying") : Collections.EMPTY_LIST;
					network.put("nowplaying", nowplaying);
					if (network.containsKey("onair_url")) {
						URL onair_url = new URL((String) network.get("onair_url"));
						Map onair = (Map) JSONValue.parse(new InputStreamReader(onair_url.openStream()));
						network.put("onair", onair.containsKey("onair") ? (Map) onair.get("onair") : Collections.EMPTY_MAP);
					}
				}
				cache.put(NETWORKS, networks);
			}
			resp.setContentType("application/json");
			JSONValue.writeJSONString(networks, resp.getWriter());
		} else if ("/preview".equals(req.getPathInfo())) {
			try {
				URL url = new URL(req.getParameter("playlist"));
				String href = XPathFactory.newInstance().newXPath().evaluate("//connection/@href", DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(url.openStream()));
				resp.sendRedirect(href);
			} catch (Exception e) {
				throw new IOException(e);
			}
		} else if ("/purchase".equals(req.getPathInfo())) {
			String artist = req.getParameter("artist");
			String title = req.getParameter("title");
			String target;
			String key = artist + "^" + title;
			if (cache.containsKey(key)) {
				target = (String) cache.get(key);
			} else {
				try {
					Document doc = builder.build(String.format("http://api.7digital.com/1.2/track/search?q=%s&oauth_consumer_key=%s", URLEncoder.encode(title, "UTF8"), System.getProperty("7digital.oauth_consumer_key")));
					Nodes tracks = doc.query("//track");
					String release = null;
					for (int i = 0; i < tracks.size(); i++) {
						Node track = tracks.get(i);
						if (artist.equalsIgnoreCase(track.query("artist/name/text()").get(0).getValue())) {
							release = track.query("release/@id").get(0).getValue();
							break;
						}
					}
					if (release != null) {
						target = String.format("http://m.7digital.com/GB/releases/%s", release);
					} else {
						target = String.format("http://m.7digital.com/GB/search/tracks?q=%s", URLEncoder.encode(title, "UTF8"));
					}
					cache.put(key, target);
				} catch (Exception e) {
					throw new IOException(e);
				}
			}
			resp.sendRedirect(target);
		}
	}
}
