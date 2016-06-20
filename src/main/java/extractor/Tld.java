/**
 * 
 */
package extractor;

import java.io.IOException;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.net.whois.WhoisClient;

/**
 * POJO for storing whois server by top level domain(TLD)
 * 
 * @author jero
 *
 */
public class Tld {
	private String tld;
	private String whoisHost;

	public Tld(String tld, String whoisHost) {
		this.tld = tld;
		this.whoisHost = whoisHost;
	}

	public String getTld() {
		return tld;
	}

	public String getWhoisHost() {
		return whoisHost;
	}

	public static void main(String[] args) {
		Map<String, String> host = null;
		try (Stream<String> stream = Files.lines(Paths.get(Tld.class.getResource("/whois_server_list.txt").toURI()))) {
			List<Tld> tlds = new ArrayList<Tld>();
			stream.forEach((record) -> {
				String[] r = record.split(" ");
				tlds.add(new Tld(r[0], r[1]));
			});
			host = tlds.stream().collect(Collectors.toMap(Tld::getTld, Tld::getWhoisHost));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		WhoisClient client = new WhoisClient();
		try {
			//client.connect(host.get("net"));
			client.connect("whois.tucows.com");
			System.out.println(client.query("maxymiser.net"));
			client.disconnect();
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// host.forEach((k, v) -> {
		// System.out.println(k+"---"+v);
		// });
		// System.out.println(host.get("pr"));

	}
}
