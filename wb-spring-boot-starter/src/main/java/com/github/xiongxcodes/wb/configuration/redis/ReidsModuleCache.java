
package com.github.xiongxcodes.wb.configuration.redis;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.wb.cache.ModuleCache;

public class ReidsModuleCache implements ModuleCache {
    private RedisCache<String, List<String>> cache;

    public ReidsModuleCache(FastJsonRedisTemplate redisTemplate) {
        cache = new RedisCache<String, List<String>>(redisTemplate, "module");
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
        return cache.containsValue((List<String>)value);
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
    public List<String> get(Object key) {
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
    public List<String> put(String key, List<String> value) {
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
    public List<String> remove(Object key) {
        cache.remove((String)key);
        return cache.get((String)key);
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
    public void putAll(Map<? extends String, ? extends List<String>> m) {
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
    public Collection<List<String>> values() {
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
    public Set<Entry<String, List<String>>> entrySet() {
        return cache.entrySet();
    }
}
