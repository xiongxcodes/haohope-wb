/**    
 * @Title: OnlineSessionVO.java  
 * @Package com.wb.online  
 * @Description: TODO(用一句话描述该文件做什么)  
 * @author 348953327@qq.com    
 * @date Jun 6, 2020 4:52:29 PM  
 * @version V1.0    
 */
package com.wb.online;

import java.io.Serializable;
import java.util.Date;

import com.alibaba.fastjson.annotation.JSONField;

/**  
 * @ClassName: OnlineSessionVO  
 * @Description: TODO(这里用一句话描述这个类的作用)  
 * @author 348953327@qq.com  
 * @date Jun 6, 2020 4:52:29 PM  
 *    
 */
public class OnlineSessionVO implements Serializable  {

	/**  
	 * @Fields serialVersionUID : TODO(用一句话描述这个变量表示什么)  
	 */ 
	private static final long serialVersionUID = 419101426096843858L;
	private String ip;
	private String userAgent;
	private Date createDate;
	private Date lastAccessDate;
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
	@JSONField(format = "yyyy-mm-dd hh:mm:ss")
	public Date getCreateDate() {
		return createDate;
	}
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}
	@JSONField(format = "yyyy-mm-dd hh:mm:ss")
	public Date getLastAccessDate() {
		return lastAccessDate;
	}
	public void setLastAccessDate(Date lastAccessDate) {
		this.lastAccessDate = lastAccessDate;
	}
}
