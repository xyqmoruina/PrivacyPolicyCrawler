package whois;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Parsing url like http://www.example.com
 * @author jero
 *
 */
public class DomainParser {

	private List<String> specialCase;

	/**
	 * Load from default whois server list file(resources/whois_server_list.txt). 
	 * Store the special case like co.uk
	 */
	public DomainParser() {
		specialCase = new ArrayList<String>();
		try (Stream<String> stream = Files
				.lines(Paths.get(DomainParser.class.getResource("/whois_server_list.txt").getPath()))) {
			stream.filter(o -> o.split(" ")[0].split("\\.").length > 1).forEach(o -> specialCase.add(o.split(" ")[0]));
			

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public DomainParser(String path) {
		specialCase = new ArrayList<String>();
		try (Stream<String> stream = Files
				.lines(Paths.get(path))) {
			stream.filter(o -> o.split(" ")[0].split("\\.").length > 1).forEach(o -> specialCase.add(o.split(" ")[0]));
			

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/**
	 * http://www.example.com will get 'com'
	 * @param url
	 * @return
	 */
	public String getTld(String url) {
		String tld = "";
		String[] str = url.split("//")[1].split("\\.");
		tld = str[str.length - 2] + "." + str[str.length - 1];

		if (!specialCase.contains(tld)) {
			tld = str[str.length - 1];
		}
		return tld;
	}

	public String getRegistableDomain(String url) {
		String result = "";
		String tld=getTld(url);
		String[] s=url.split("//")[1].split("\\.");
		if(specialCase.contains(tld)){
			//www.example.co.uk will get example.co.uk
			result=s[s.length-3]+"."+tld;
		}else{
			result=s[s.length-2]+"."+tld;
		}
		return result;
	}
	
	/**
	 * Load url from file(url format: http://www.example.com)
	 * @param path Path to url file
	 * @return
	 */
	public List<String> loadTarget(String path){
		List<String> list=new ArrayList<String>();
		try {
			list=Files.lines(Paths.get(path)).collect(Collectors.toList());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return list;
	}
	
	public void setSpecialCase(List<String> specialCase){
		this.specialCase=specialCase;
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		DomainParser parser = new DomainParser();
		List<String> urls=parser.loadTarget(DomainParser.class.getResource("/list.txt").getPath());
		urls.stream().forEach(o->{System.out.println(o+"  "+parser.getRegistableDomain(o)+"  "+parser.getTld(o));});
	}

}
