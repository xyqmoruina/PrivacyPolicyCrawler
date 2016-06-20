package db;

/**
 * POJO for policy/policies records
 * @author jero
 *
 */
public class Policy {
	private int id;
	private String mainsite;
	private String connectingUrl;
	private String policyurl;
	private int ifByWhois;
	private String whoisRecord;
	private String date;
	
	public Policy(String mainsite, String connectingUrl, String policyurl, int ifByWhois, String whoisRecord, String date){
		this.mainsite=mainsite;
		this.connectingUrl=connectingUrl;
		this.policyurl=policyurl;
		this.date=date;
		this.setIfByWhois(ifByWhois);
		this.setWhoisRecord(whoisRecord);
	}
	public Policy(){}
	
	public int getId(){
		return id;
	}
	
	public String getMainsite(){
		return mainsite;
	}
	
	public String getConnectingUrl(){
		return connectingUrl;
	}
	
	public String getPolicyurl(){
		return policyurl;
	}
	
	public String getDate(){
		return date;
	}
	public String getWhoisRecord() {
		return whoisRecord;
	}
	public void setWhoisRecord(String whoisRecord) {
		this.whoisRecord = whoisRecord;
	}
	public int getIfByWhois() {
		return ifByWhois;
	}
	public void setIfByWhois(int ifByWhois) {
		this.ifByWhois = ifByWhois;
	}
}
