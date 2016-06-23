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
	private static final Pattern patternWhoisServer = Pattern.compile("Whois Server:\\s*(.*)");
	private static final Pattern patternRegistrantOrg = Pattern.compile("Registrant Organization:\\s*(.*)");
	private static final Pattern patternRegistrantUrl = Pattern.compile("Registrant Email:\\s*.*@(.*)");
	private static final Pattern patternNoMatch = Pattern.compile("No match for ");

	private Matcher matcher;
	private WhoisResult whoisResult;
	private static Map<String, String> hostList;
	private static final PublicSuffixListFactory factory = new PublicSuffixListFactory();
	private static final PublicSuffixList suffixList = factory.build();

	public Whois(){
		whoisResult=new WhoisResult();
		String path="/whois_server_list.txt";
		try (Stream<String> stream = Files.lines(Paths.get(Whois.class.getResource(path).getPath()))) {
			List<Tld> tlds = new ArrayList<Tld>();
			stream.forEach((record) -> {
				String[] r = record.split(" ");// format: tld host
				tlds.add(new Tld(r[0], r[1]));
			});

			hostList = tlds.stream().collect(Collectors.toMap(Tld::getTld, Tld::getWhoisHost));
			// hostList.entrySet().stream().forEach(System.out::println);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("2121");
		}
	}
	public Whois(String path) {
		whoisResult = new WhoisResult();
		System.out.println(path);
		try (Stream<String> stream = Files.lines(Paths.get(path))) {
			List<Tld> tlds = new ArrayList<Tld>();
			stream.forEach((record) -> {
				String[] r = record.split(" ");// format: tld host
				tlds.add(new Tld(r[0], r[1]));
			});

			hostList = tlds.stream().collect(Collectors.toMap(Tld::getTld, Tld::getWhoisHost));
			// hostList.entrySet().stream().forEach(System.out::println);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("2121");
		}
	}

	private boolean findRegistrantOrg(String whoisReport) {
		boolean flag = false;

		matcher = patternRegistrantOrg.matcher(whoisReport);
		if (matcher.find()) {
			this.whoisResult.setRegistrantOrg(matcher.group(1));
			flag = true;
		}
		return flag;
	}

	private boolean findRgistrantUrl(String whoisReport) {
		boolean flag = false;

		matcher = patternRegistrantUrl.matcher(whoisReport);
		if (matcher.find()) {
			this.whoisResult.setRegistrantUrl(matcher.group(1));
			flag = true;
		}

		return flag;
	}

	private String findWhoisServer(String whoisReport) {
		String server = "";

		matcher = patternWhoisServer.matcher(whoisReport);
		// find last whois server
		while (matcher.find()) {
			server = matcher.group(1);
		}

		return server;
	}

	private boolean findNoMatch(String whoisReport) {
		matcher = patternNoMatch.matcher(whoisReport);
		return matcher.find();
	}

	/**
	 * store
	 * 
	 * @param whoisReport
	 * @return
	 */
	private boolean storeRegistrantInfo(String whoisReport) {
		boolean flag = findRegistrantOrg(whoisReport) && findRgistrantUrl(whoisReport);

		return flag;

	}

	public boolean getWhois(String url) {
		boolean flag = false;
		String registableUrl = suffixList.getRegistrableDomain(url);
		String whoisReport = "";
		WhoisClient client = new WhoisClient();
		try {
			client.connect(WhoisClient.DEFAULT_HOST);
			whoisReport = client.query("=" + registableUrl);
			System.out.println(whoisReport);
			if (!(flag = storeRegistrantInfo(whoisReport))) {
				System.out.println("vvv");
				String server = findWhoisServer(whoisReport);
				String tempReport = "";
				if (!"".equals(server)) {
					// secondary search
					System.out.println("sss");
					client.connect(server);
					tempReport = client.query(registableUrl);
					if (!(flag = storeRegistrantInfo(tempReport))) {
						client.connect(hostList.get(suffixList.getPublicSuffix(url)));
						tempReport = client.query(registableUrl);
					}
				} else if (findNoMatch(whoisReport)) {
					// refer to pre-define host
					System.out.println("hhh");
					client.connect(hostList.get(suffixList.getPublicSuffix(url)));
					tempReport = client.query(registableUrl);

				}
				whoisReport += tempReport;
				flag = storeRegistrantInfo(tempReport);

			}
			whoisResult.setWhoisReport(whoisReport);
			client.disconnect();

		} catch (SocketException e) {
			System.err.println("Fail to connect whois server of: " + url);
			e.printStackTrace();

		} catch (IOException e) {
			e.printStackTrace();
		}

		return flag;
	}

	public WhoisResult getWhoisResult() {
		return whoisResult;
	}

	public static void main(String[] args) {
		Whois whois = new Whois(Whois.class.getResource("/whois_server_list.txt").getPath());
		if (whois.getWhois("https://fls-na.amazon.com")) {
			WhoisResult result = whois.getWhoisResult();
			System.out.println(
					result.getWhoisReport() + "\n" + result.getRegistrantOrg() + " " + result.getRegistrantUrl());
		} else {
			System.out.println("ffffff");
		}
	}
}
