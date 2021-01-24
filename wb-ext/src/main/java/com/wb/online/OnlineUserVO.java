/**    
 * @Title: OnlineUserVO.java  
 * @Package com.wb.online  
 * @Description: TODO(用一句话描述该文件做什么)  
 * @author 348953327@qq.com    
 * @date Jun 6, 2020 4:50:30 PM  
 * @version V1.0    
 */
package com.wb.online;

import java.io.Serializable;

/**  
 * @ClassName: OnlineUserVO  
 * @Description: TODO(这里用一句话描述这个类的作用)  
 * @author 348953327@qq.com  
 * @date Jun 6, 2020 4:50:30 PM  
 *    
 */
public class OnlineUserVO implements Serializable {
	/**  
	 * @Fields serialVersionUID : TODO(用一句话描述这个变量表示什么)  
	 */ 
	private static final long serialVersionUID = -5599359909632819294L;
	private int sessionCount = 1;
	private String user;
	private String username;
	private String dispname;
	private String ip;
	private String userAgent;
	public int getSessionCount() {
		return sessionCount;
	}
	public void setSessionCount(int sessionCount) {
		this.sessionCount = sessionCount;
	}
	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getDispname() {
		return dispname;
	}
	public void setDispname(String dispname) {
		this.dispname = dispname;
	}
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	public String getUserAgent() {
		return userAgent;
	}
	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}
}
