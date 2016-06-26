package db;

/**
 * POJO for policy/policies records
 * 
 * @author jero
 *
 */
public class Policy {
	private int id;
	private String mainsite;
	private String connectingUrl;
	private String policyUrl;
	private String policy;
	private int ifByWhois;
	private String whoisRecord;
	private String date;

	public Policy(String mainsite, String connectingUrl, String policyUrl, String policy, int ifByWhois, String whoisRecord,
			String date) {
		this.mainsite = mainsite;
		this.connectingUrl = connectingUrl;
		this.policyUrl = policyUrl;
		this.policy=policy;
		this.date = date;
		this.setIfByWhois(ifByWhois);
		this.setWhoisRecord(whoisRecord);
	}

	public Policy() {
	}

	public int getId() {
		return id;
	}

	public String getMainsite() {
		return mainsite;
	}

	public String getConnectingUrl() {
		return connectingUrl;
	}

	public String getPolicyUrl() {
		return policyUrl;
	}

	public String getDate() {
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

	public void setId(int id) {
		this.id = id;
	}

	public void setPolicyUrl(String policyUrl) {
		this.policyUrl = policyUrl;
	}

	public String getPolicy() {
		return policy;
	}

	public void setPolicy(String policy) {
		this.policy = policy;
	}

}
