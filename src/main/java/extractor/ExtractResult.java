package extractor;

import whois.WhoisResult;

public class ExtractResult {
	public enum Status {
		SUCCESS, FAIL, REFERTOWHOIS
	};
	private String policyUrl;
	private String policy;
	private WhoisResult whoisResult;
	private Status status; 
	public ExtractResult(){
		setStatus(Status.FAIL);
		policyUrl="";
		policy="";
		//whoisResult=new WhoisResult();//TODO
	}
	public String getPolicyUrl() {
		return policyUrl;
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
	public WhoisResult getWhoisResult() {
		return whoisResult;
	}
	public void setWhoisResult(WhoisResult whoisResult) {
		this.whoisResult = whoisResult;
	}
	public Status getStatus() {
		return status;
	}
	public void setStatus(Status status) {
		this.status = status;
	}
	
}
