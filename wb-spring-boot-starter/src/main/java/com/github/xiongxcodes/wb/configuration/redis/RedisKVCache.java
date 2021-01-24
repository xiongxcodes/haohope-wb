/**
 * @Title: RedisKVCache.java
 * @Package cn.hh.wb.configuration.redis
 * @Description: TODO(用一句话描述该文件做什么)
 * @author 348953327@qq.com
 * @date Jun 6, 2020 9:05:28 PM
 * @version V1.0
 */
package com.github.xiongxcodes.wb.configuration.redis;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.wb.cache.KVCache;

/**
 * @ClassName: RedisKVCache
 * @Description: TODO(这里用一句话描述这个类的作用)
 * @author 348953327@qq.com
 * @date Jun 6, 2020 9:05:28 PM
 * 
 */
public class RedisKVCache implements KVCache {
    private RedisCache<String, ConcurrentHashMap<Object, String>> cache;

    public RedisKVCache(FastJsonRedisTemplate redisTemplate) {
        cache = new RedisCache<String, ConcurrentHashMap<Object, String>>(redisTemplate, "kv");
    }

    /**
     * <p>
     * Title: size
     * </p>
     * <p>
     * Description:
     * </p>
     * 
     * @return
     * @see java.util.Map#size()
     */
    @Override
    public int size() {
        return cache.size();
    }

    /**
     * <p>
     * Title: isEmpty
     * </p>
     * <p>
     * Description:
     * </p>
     * 
     * @return
     * @see java.util.Map#isEmpty()
     */
    @Override
    public boolean isEmpty() {
        return cache.isEmpty();
    }

    /**
     * <p>
     * Title: containsKey
     * </p>
     * <p>
     * Description:
     * </p>
     * 
     * @param key
     * @return
     * @see java.util.Map#containsKey(java.lang.Object)
     */
    @Override
    public boolean containsKey(Object key) {
        return cache.containsKey((String)key);
    }

    /**
     * <p>
     * Title: containsValue
     * </p>
     * <p>
     * Description:
     * </p>
     * 
     * @param value
     * @return
     * @see java.util.Map#containsValue(java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean containsValue(Object value) {
        return cache.containsValue((ConcurrentHashMap<Object, String>)value);
    }

    /**
     * <p>
     * Title: get
     * </p>
     * <p>
     * Description:
     * </p>
     * 
     * @param key
     * @return
     * @see java.util.Map#get(java.lang.Object)
     */
    @Override
    public ConcurrentHashMap<Object, String> get(Object key) {
        return cache.get((String)key);
    }

    /**
     * <p>
     * Title: put
     * </p>
     * <p>
     * Description:
     * </p>
     * 
     * @param key
     * @param value
     * @return
     * @see java.util.Map#put(java.lang.Object, java.lang.Object)
     */
    @Override
    public ConcurrentHashMap<Object, String> put(String key, ConcurrentHashMap<Object, String> value) {
        return cache.put(key, value);
    }

    /**
     * <p>
     * Title: remove
     * </p>
     * <p>
     * Description:
     * </p>
     * 
     * @param key
     * @return
     * @see java.util.Map#remove(java.lang.Object)
     */
    @Override
    public ConcurrentHashMap<Object, String> remove(Object key) {
        cache.remove((String)key);
        return cache.remove((String)key);
    }

    /**
     * <p>
     * Title: putAll
     * </p>
     * <p>
     * Description:
     * </p>
     * 
     * @param m
     * @see java.util.Map#putAll(java.util.Map)
     */
    @Override
    public void putAll(Map<? extends String, ? extends ConcurrentHashMap<Object, String>> m) {
        cache.putAll(m);
    }

    /**
     * <p>
     * Title: clear
     * </p>
     * <p>
     * Description:
     * </p>
     * 
     * @see java.util.Map#clear()
     */
    @Override
    public void clear() {
        cache.clear();
    }

    /**
     * <p>
     * Title: keySet
     * </p>
     * <p>
     * Description:
     * </p>
     * 
     * @return
     * @see java.util.Map#keySet()
     */
    @Override
    public Set<String> keySet() {
        return cache.keySet();
    }

    /**
     * <p>
     * Title: values
     * </p>
     * <p>
     * Description:
     * </p>
     * 
     * @return
     * @see java.util.Map#values()
     */
    @Override
    public Collection<ConcurrentHashMap<Object, String>> values() {
        return cache.values();
    }

    /**
     * <p>
     * Title: entrySet
     * </p>
     * <p>
     * Description:
     * </p>
     * 
     * @return
     * @see java.util.Map#entrySet()
     */
    @Override
    public Set<Entry<String, ConcurrentHashMap<Object, String>>> entrySet() {
        return cache.entrySet();
    }

}
