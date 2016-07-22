package cleaner;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.w3c.dom.Document;
import org.w3c.tidy.Tidy;
import org.xml.sax.InputSource;

import db.DistinctPolicy;
import db.MyDB;
import db.Policy;
import de.l3s.boilerpipe.BoilerpipeExtractor;
import de.l3s.boilerpipe.document.TextBlock;
import de.l3s.boilerpipe.document.TextDocument;
import de.l3s.boilerpipe.extractors.CommonExtractors;
import de.l3s.boilerpipe.sax.BoilerpipeSAXInput;
import de.l3s.boilerpipe.sax.HTMLDocument;
import de.l3s.boilerpipe.sax.HTMLFetcher;
import de.l3s.boilerpipe.sax.HTMLHighlighter;

public class HtmlCleaner {
	public static Map<Integer, String> readCSV() {
		Map<Integer, String> map = new HashMap<Integer, String>();
		try (Stream<String> stream = Files.lines(Paths.get("1.csv"))) {
			stream.forEach(line -> {
				String[] s = line.split(",");
				map.put(Integer.valueOf(s[0]), s[3]);
			});

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return map;
	}

	public static void main(String[] args) throws Exception {
		Map<Integer, String> map = readCSV();

		MyDB db = new MyDB();
		// choose from a set of useful BoilerpipeExtractors...
		final BoilerpipeExtractor extractor = CommonExtractors.ARTICLE_EXTRACTOR;
		final HTMLHighlighter hh = HTMLHighlighter.newExtractingInstance();
		hh.setOutputHighlightOnly(true);
		List<DistinctPolicy> list = db.getAllDistinctPolicy();
		for (DistinctPolicy p : list) {
			// System.out.println(p.getId()+"\t"+p.getCnt()+"\t"+p.getPolicyUrl());
			String path="/Users/jero/codebase/project/data/htmls/";
			if (map.get(p.getId()).equals("use origin")) {

				try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(path + p.getId() + ".html"),
						StandardCharsets.UTF_8)) {
					bw.write("<meta charset=utf8 />\n");
					bw.write("<a href=\"" + p.getPolicyUrl() + "\" >Origin page</a>");
					bw.write(p.getPolicy() + "\n");
				}
			} else {
				try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(path + p.getId() + ".html"),
						StandardCharsets.UTF_8)) {
					bw.write("<meta charset=utf8 />\n");
					bw.write("<a href=\"" + p.getPolicyUrl() + "\" >Origin page</a>");
					// modified from HTMLHighlighter.process(URL url,
					// BoilerpipeExtractor extractor)
					// Source: final HTMLDocument htmlDoc =
					// HTMLFetcher.fetch(url);
					HTMLDocument htmldoc = new HTMLDocument(p.getPolicy());
					InputSource is = htmldoc.toInputSource();
					TextDocument doc = new BoilerpipeSAXInput(is).getTextDocument();
					if (extractor.process(doc)) {
						// System.out.println(hh.process(doc, p.getPolicy()));
						bw.write(hh.process(doc, p.getPolicy()) + "\n");

					}

				}
			}

		}

	}
}
