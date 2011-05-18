package com.markwoodbridge.justplayed;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONValue;

import com.google.appengine.api.memcache.stdimpl.GCacheFactory;

@SuppressWarnings("serial")
public class JustplayedServlet extends HttpServlet {
	private Cache cache;
	//String TEMPLATE = "http://www.bbc.co.uk/%s/nowplaying/latest.json";
	//String TEMPLATE = "http://just-played.appspot.com/json/%s.json";//TODO
	String TEMPLATE = "http://localhost:8888/json/%s.json";
	List networksTemplate;
	private static final String NETWORKS = "networks";

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
	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp)	throws IOException {
		System.out.println(req.getPathInfo());
		if ("/networks.json".equals(req.getPathInfo())) {
			List<Map> networks;
			if (cache.containsKey(NETWORKS)) {
				networks = (List) cache.get(NETWORKS);
			} else {
				networks = networksTemplate;
				for (Map network : networks) {
					URL url = new URL(String.format(TEMPLATE, network.get("id")));
					Map latest = (Map) JSONValue.parse(new InputStreamReader(url.openStream()));
					List nowplaying = latest.containsKey("nowplaying") ? (List) latest.get("nowplaying") : Collections.EMPTY_LIST;
					network.put("nowplaying", nowplaying);
				}
				cache.put(NETWORKS, networks);
			}
			resp.setContentType("application/json");
			resp.getWriter().println(JSONValue.toJSONString(networks));
		}
	}
}
