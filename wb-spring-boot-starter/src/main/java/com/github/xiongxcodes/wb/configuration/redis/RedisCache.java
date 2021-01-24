/**
 * @Title: RedisCache.java
 * @Package cn.hh.wb.configuration.redis
 * @Description: TODO(用一句话描述该文件做什么)
 * @author 348953327@qq.com
 * @date Jun 6, 2020 7:57:29 PM
 * @version V1.0
 */
package com.github.xiongxcodes.wb.configuration.redis;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.data.redis.core.HashOperations;

/**
 * @ClassName: RedisCache
 * @Description: TODO(这里用一句话描述这个类的作用)
 * @author 348953327@qq.com
 * @date Jun 6, 2020 7:57:29 PM
 * 
 */
public class RedisCache<K, V> {
    private final String NAMESPACE = "haohope:cache:wb";
    @SuppressWarnings("rawtypes")
    private HashOperations hash;
    private String key = "";

    public RedisCache(FastJsonRedisTemplate redisTemplate, String key) {
        this.hash = redisTemplate.opsForHash();
        this.key = NAMESPACE + ":" + key;
    }

    @SuppressWarnings("unchecked")
    public int size() {
        return Integer.valueOf(hash.size(this.key).toString());
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
    public boolean isEmpty() {
        return size() == 0;
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
    @SuppressWarnings("unchecked")
    public boolean containsKey(K key) {
        return hash.hasKey(this.key, key);
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
    public boolean containsValue(V value) {
        return values().contains(value);
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
    @SuppressWarnings("unchecked")
    public V get(K key) {
        return (V)hash.get(this.key, key);
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
    @SuppressWarnings("unchecked")
    public V put(K key, V value) {
        hash.put(this.key, key, value);
        return value;
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
    @SuppressWarnings("unchecked")
    public V remove(K key) {
        V v = (V)hash.get(this.key, key);
        hash.delete(this.key, key);
        return v;
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
    @SuppressWarnings("unchecked")
    public void putAll(Map<? extends K, ? extends V> m) {
        hash.putAll(this.key, m);
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
    @SuppressWarnings("unchecked")
    public void clear() {
        hash.delete(this.key, keySet());
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
    @SuppressWarnings("unchecked")
    public Set<K> keySet() {
        return hash.keys(this.key);
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
    @SuppressWarnings("unchecked")
    public Collection<V> values() {
        return hash.values(this.key);
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
    @SuppressWarnings("unchecked")
    public Set<Entry<K, V>> entrySet() {
        return hash.entries(this.key).entrySet();
    }
}
