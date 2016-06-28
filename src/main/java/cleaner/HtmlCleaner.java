package cleaner;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.tidy.Tidy;
import org.xml.sax.InputSource;

import db.MyDB;
import db.Policy;
import de.l3s.boilerpipe.BoilerpipeExtractor;
import de.l3s.boilerpipe.document.TextDocument;
import de.l3s.boilerpipe.extractors.CommonExtractors;
import de.l3s.boilerpipe.sax.BoilerpipeSAXInput;
import de.l3s.boilerpipe.sax.HTMLDocument;
import de.l3s.boilerpipe.sax.HTMLFetcher;
import de.l3s.boilerpipe.sax.HTMLHighlighter;

public class HtmlCleaner {

	public static void main(String[] args) throws Exception {
		MyDB db = new MyDB();
		URL url = new URL("http://blog.openshift.com/day-18-boilerpipe-article-extraction-for-java-developers/");

		// choose from a set of useful BoilerpipeExtractors...
		final BoilerpipeExtractor extractor = CommonExtractors.ARTICLE_EXTRACTOR;
		// final BoilerpipeExtractor extractor =
		// CommonExtractors.DEFAULT_EXTRACTOR;
		// final BoilerpipeExtractor extractor =
		// CommonExtractors.CANOLA_EXTRACTOR;
		// final BoilerpipeExtractor extractor =
		// CommonExtractors.LARGEST_CONTENT_EXTRACTOR;

		// choose the operation mode (i.e., highlighting or extraction)
		//final HTMLHighlighter hh = HTMLHighlighter.newHighlightingInstance();
		 final HTMLHighlighter hh = HTMLHighlighter.newExtractingInstance();

		List<Policy> list = db.selectById(479);
		try (BufferedWriter bw = Files.newBufferedWriter(Paths.get("1.html"), StandardCharsets.UTF_8)) {
			for (Policy p : list) {
				bw.write("<base href=\"" + p.getPolicyUrl() + "\" >");
				bw.write("<meta http-equiv=\"Content-Type\" content=\"text-html; charset=utf-8\" />\n");
				// modified from HTMLHighlighter.process(URL url,
				// BoilerpipeExtractor extractor)
				// Source: final HTMLDocument htmlDoc = HTMLFetcher.fetch(url);
				HTMLDocument htmldoc = new HTMLDocument(p.getPolicy());
				InputSource is = htmldoc.toInputSource();
				TextDocument doc = new BoilerpipeSAXInput(is).getTextDocument();
				if (extractor.process(doc)) {
					System.out.println(hh.process(doc, p.getPolicy()));
					bw.write(hh.process(doc, p.getPolicy()) + "\n");
				}

			}
		}
		/*
		 * String htmlFileName = "1.html"; FileInputStream fis = null; try { fis
		 * = new FileInputStream(htmlFileName); } catch
		 * (java.io.FileNotFoundException e) { System.out.println(
		 * "File not found: " + htmlFileName); } Tidy tidy = new Tidy();
		 * tidy.setShowWarnings(false); tidy.setXmlTags(false);
		 * tidy.setInputEncoding("UTF-8"); tidy.setOutputEncoding("UTF-8");
		 * tidy.setXHTML(true);// tidy.setMakeClean(true); Document xmlDoc =
		 * tidy.parseDOM(fis, null); try { tidy.pprint(xmlDoc, new
		 * FileOutputStream("c.xhtml")); } catch (Exception e) { }
		 */

	}
}
