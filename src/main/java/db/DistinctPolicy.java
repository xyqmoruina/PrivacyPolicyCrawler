package db;

/**
 * POJO for storing distinct policy (Structure: id, policyUrl, cnt, policy)
 * @author jero
 *
 */
public class DistinctPolicy {
	private int id;
	private String policyUrl;
	private int cnt;
	private String policy;
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
	public int getCnt() {
		return cnt;
	}
	public void setCnt(int cnt) {
		this.cnt = cnt;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	
}
