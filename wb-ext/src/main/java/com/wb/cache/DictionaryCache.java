/**
 * @Title: DictionaryBuffer.java
 * @Package com.wb.buffer
 * @Description: TODO(用一句话描述该文件做什么)
 * @author 348953327@qq.com
 * @date Jun 6, 2020 7:49:21 PM
 * @version V1.0
 */
package com.wb.cache;

import java.util.concurrent.ConcurrentHashMap;

import com.wb.tool.ParentDictRecord;

/**
 * @ClassName: DictionaryBuffer
 * @Description: TODO(这里用一句话描述这个类的作用)
 * @author 348953327@qq.com
 * @date Jun 6, 2020 7:49:21 PM
 * 
 */
public interface DictionaryCache extends WbCache<String, ConcurrentHashMap<String, ParentDictRecord>> {

}
