package extractor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.net.whois.WhoisClient;

import de.malkusch.whoisServerList.publicSuffixList.PublicSuffixList;
import de.malkusch.whoisServerList.publicSuffixList.PublicSuffixListFactory;

/**
 * Usage: obj.getWhois(url)
 * 
 * @author jero
 *
 */
public class Whois {
	private static final Pattern pattern = Pattern.compile("Whois Server:\\s(.*)"); // \\s
																					// match
																					// any
																					// white
																					// space
																					// character.
	private static Pattern patternRegistrar = Pattern.compile("URL:\\s(.*)");
	private static Pattern patternRegistrar2 = Pattern.compile("");
	private Matcher matcher;
	private static Map<String, String> host;

	// regex whois parser
	private static final String WHOIS_SERVER_PATTERN = "";
	private static final String WHOIS_REGISTRAR_PATTERN = "Registrar URL:\\s(.*)";

	public Whois() {
	}

	/**
	 * Construct with path to whois server list file.
	 * 
	 * @param path
	 */
	public Whois(String path) {
		try (Stream<String> stream = Files.lines(Paths.get(Tld.class.getResource(path).toURI()))) {
			List<Tld> tlds = new ArrayList<Tld>();
			stream.forEach((record) -> {
				String[] r = record.split(" ");
				tlds.add(new Tld(r[0], r[1]));
			});

			host = tlds.stream().collect(Collectors.toMap(Tld::getTld, Tld::getWhoisHost));
			host.entrySet().stream().sorted((o1, o2) -> {
				return getDomainLevels(o1.getKey()) - getDomainLevels(o2.getKey());
			}).forEach(System.out::println);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private String queryWithWhoisServer(String domainName, String whoisServer) {

		String result = "";
		WhoisClient whois = new WhoisClient();
		try {

			whois.connect(whoisServer);
			result = whois.query(domainName);
			whois.disconnect();

		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return result;

	}

	private int getDomainLevels(String url) {

		return url.split("\\.").length;
	}

	private String getHostByTld(String url) {
		return host.get(extractTld(url));
	}

	public String getWhoisByShell(String url) {
		Process p;
		String result = "";
		try {
			p = Runtime.getRuntime().exec("whois " + url);
			p.waitFor();
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			result = br.lines().collect(Collectors.joining("\n"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public String getWhois(String url) {
		String result = "";

		// parse as domainname.sld.tld
		PublicSuffixListFactory factory = new PublicSuffixListFactory();
		PublicSuffixList suffixList = factory.build();

		result = getWhoisByShell(suffixList.getRegistrableDomain(url));
		//System.out.println(result);
		return result;
	}

	public String getWhois(String url, String mainHost) {
		String result = "";
		WhoisClient client = new WhoisClient();
		try {
			client.connect(mainHost);
			String whoisReport = client.query(url);
			client.disconnect();

			result = whoisReport;
			// Find the url of its registrar
			/*
			 * result = getRegistrarUrl(whoisReport);
			 * System.out.println("!!!"+result); if (result.equals("")) { // get
			 * the address of real whois server result =
			 * getRegistrarUrl(queryWithWhoisServer(url,
			 * getWhoisServer(whoisReport))); }
			 */
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// System.out.println("****"+result);

		return result;
	}

	private String getWhoisServer(String whois) {

		String result = "";

		matcher = pattern.matcher(whois);

		// get last whois server
		while (matcher.find()) {
			result = matcher.group(1);
		}
		return result;
	}

	public String getRegistrarUrl(String whois) {
		String result = "";
		System.out.println("===\n" + whois);
		matcher = patternRegistrar.matcher(whois);
		if (matcher.find()) {
			result = matcher.group(1);
		}
		return result;
	}

	public String extractTld(String url) {
		String[] ss = url.split("\\.");

		return ss[ss.length - 1];

	}
	
	public static void main(String[] args) {
		Whois whois = new Whois();
		// Whois whois = new Whois("/whois_server_list.txt");
		// System.out.println(whois.getWhoisByShell("bbci.co.uk"));
		String url = "fdsafd.chartbeat.net";
		whois.getWhois(url);
		// System.out.println(whois.getWhois(url));

	}
}
