/**    
 * @Title: MemoryBufer.java  
 * @Package com.wb.buffer  
 * @Description: TODO(用一句话描述该文件做什么)  
 * @author 348953327@qq.com    
 * @date May 24, 2020 9:56:44 PM  
 * @version V1.0    
 */
package com.wb.cache;

import java.util.concurrent.ConcurrentHashMap;

/**  
 * @ClassName: MemoryBufer  
 * @Description: TODO(这里用一句话描述这个类的作用)  
 * @author 348953327@qq.com  
 * @date May 24, 2020 9:56:44 PM  
 *    
 */
public class MemoryCache<K,V> extends ConcurrentHashMap<K, V>  implements WbCache<K,V>{
	/**  
	 * @Fields serialVersionUID : TODO(用一句话描述这个变量表示什么)  
	 */ 
	private static final long serialVersionUID = 3403036823643316525L;

}
