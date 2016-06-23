package extractor;

import java.io.IOException;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.net.whois.WhoisClient;

public class WhoisParser {
	private static final Pattern patternRegistrant=Pattern.compile("Registrant Email:\\s.*@(.*)"); 
	private static final Pattern patternReferal=Pattern.compile("Whois Server:\\s(.*)");
	private static Matcher matcher;
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String s="";
		try {
			s=Files.lines(Paths.get("test.txt")).collect(Collectors.joining("\n"));
			matcher=patternReferal.matcher(s);
			while(matcher.find()){
				System.out.println(matcher.group(1));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		WhoisClient client=new WhoisClient();
		try {
			client.connect(WhoisClient.DEFAULT_HOST);
			System.out.println(client.query("=yahoo.com"));
			client.disconnect();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//matcher=patternRegistrant.matcher(s);
		
	}

}
