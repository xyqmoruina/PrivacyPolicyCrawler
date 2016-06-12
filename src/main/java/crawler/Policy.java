package crawler;

public class Policy {
	private int id;
	private String mainsite;
	private String url;
	private String policyurl;
	private String date;
	
	public Policy(String mainsite, String url, String policyurl, String date){
		this.mainsite=mainsite;
		this.url=url;
		this.policyurl=policyurl;
		this.date=date;
	}
	public Policy(){}
	
	public int getId(){
		return id;
	}
	
	public String getMainsite(){
		return mainsite;
	}
	
	public String getUrl(){
		return url;
	}
	
	public String getPolicyurl(){
		return policyurl;
	}
	
	public String getDate(){
		return date;
	}
}
