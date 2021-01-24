/**    
 * @Title: KVCache.java  
 * @Package com.wb.cache  
 * @Description: TODO(用一句话描述该文件做什么)  
 * @author 348953327@qq.com    
 * @date Jun 6, 2020 7:55:21 PM  
 * @version V1.0    
 */
package com.wb.cache;

import java.util.concurrent.ConcurrentHashMap;

/**  
 * @ClassName: KVCache  
 * @Description: TODO(这里用一句话描述这个类的作用)  
 * @author 348953327@qq.com  
 * @date Jun 6, 2020 7:55:21 PM  
 *    
 */
public interface KVCache extends WbCache<String, ConcurrentHashMap<Object, String>> {

}
