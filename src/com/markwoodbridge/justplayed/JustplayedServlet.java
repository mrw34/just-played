package com.markwoodbridge.justplayed;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
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
	String TEMPLATE = "http://just-played.appspot.com/latest/%s.json";
	Map networks;

	@Override
	public void init() throws ServletException {
		super.init();
		try {
			Map props = new HashMap();
			props.put(GCacheFactory.EXPIRATION_DELTA, 60 * 2);
			cache = CacheManager.getInstance().getCacheFactory().createCache(props);
		} catch (CacheException e) {
			throw new ServletException(e);
		}
		try {
			networks = (Map) JSONValue.parse(new FileReader("networks.json"));
		} catch (FileNotFoundException e) {
			throw new ServletException(e);
		}
	}
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp)	throws IOException {
		if ("/recache".equals(req.getPathInfo())) {
			recache("radio2");//TODO
		} else {
			String network = req.getParameter("q");
			if (network == null) {
				return;
			}
			List<Map<String, String>> nowplaying = cache.containsKey(network) ? (List) cache.get(network) : recache(network);
			resp.setContentType("text/plain");
			for (Map<String, String> track : nowplaying) {
				resp.getWriter().println(track.get("artist") + " - " + track.get("title"));
			}
		}
	}
	
	private List recache(String network) throws IOException {
		System.out.println(network);//TODO
		URL url = new URL(String.format(TEMPLATE, network));
		Map latest = (Map) JSONValue.parse(new InputStreamReader(url.openStream()));
		List nowplaying = latest.containsKey("nowplaying") ? (List) latest.get("nowplaying") : (List) cache.get(network);
		cache.put(network, nowplaying);
		return nowplaying;
	}
}
