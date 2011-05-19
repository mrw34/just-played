package com.markwoodbridge.justplayed;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLEncoder;
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
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathFactory;

import org.json.simple.JSONValue;
import org.w3c.dom.Document;

import com.google.appengine.api.memcache.stdimpl.GCacheFactory;

@SuppressWarnings({"serial", "rawtypes", "unchecked"})
public class JustplayedServlet extends HttpServlet {
	private static final String NETWORKS = "networks";
	private Cache cache;
	private List networksTemplate;

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
			resp.getWriter().println(JSONValue.toJSONString(networks));
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
					URL search = new URL(String.format("http://api.7digital.com/1.2/track/search?q=%s&oauth_consumer_key=milkroundabout", URLEncoder.encode(title, "UTF8")));
					Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(search.openStream());
					StringWriter writer = new StringWriter();
					TransformerFactory.newInstance().newTransformer().transform(new DOMSource(xml), new StreamResult(writer));
					String release = XPathFactory.newInstance().newXPath().evaluate(String.format("//track[artist/name/text()=\"%s\"][1]/release/@id", artist), xml);
					if (!release.isEmpty()) {
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
