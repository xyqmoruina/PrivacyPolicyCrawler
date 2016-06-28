package whois;

/**
 * POJO for storing whois query result
 * @author jero
 *
 */
public class WhoisResult {
	private String whoisReport;
	private String registrantOrg;
	private String registrantUrl;
	
	public WhoisResult(){
		whoisReport="";
		registrantOrg="";
		registrantUrl="";
	}
	public String getWhoisReport() {
		return whoisReport;
	}
	public void setWhoisReport(String whoisReport) {
		this.whoisReport = whoisReport;
	}
	public String getRegistrantUrl() {
		return registrantUrl;
	}
	public void setRegistrantUrl(String registrantUrl) {
		this.registrantUrl = registrantUrl;
	}
	public String getRegistrantOrg() {
		return registrantOrg;
	}
	public void setRegistrantOrg(String registrantOrg) {
		this.registrantOrg = registrantOrg;
	}
}
