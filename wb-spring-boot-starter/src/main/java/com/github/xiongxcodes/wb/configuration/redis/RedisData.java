/**
 * @Title: RedisData.java
 * @Package cn.hh.wb.configuration.redis
 * @Description: TODO(用一句话描述该文件做什么)
 * @author 348953327@qq.com
 * @date Jun 7, 2020 7:52:29 PM
 * @version V1.0
 */
package com.github.xiongxcodes.wb.configuration.redis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.BoundStreamOperations;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.BoundZSetOperations;

import com.alibaba.fastjson.JSON;
import com.wb.util.WebUtil;

/**
 * @ClassName: RedisData
 * @Description: TODO(这里用一句话描述这个类的作用)
 * @author 348953327@qq.com
 * @date Jun 7, 2020 7:52:29 PM
 * 
 */
public class RedisData implements ApplicationContextAware, EnvironmentAware {
    private static FastJsonRedisTemplate fastJsonRedisTemplate;
    private static Boolean enable = false;

    private static List<Map<String, Object>> getTreeNode(List<KeyNode> nodes, String parentid) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (KeyNode node : nodes) {
            if (parentid.equals(node.getParentid())) {
                List<Map<String, Object>> children = getTreeNode(nodes, node.getId());
                Map<String, Object> map = JSON.parseObject(JSON.toJSONString(node));
                map.put("children", children);
                map.put("leaf", children.size() == 0);
                map.put("text",
                    children.size() == 0 && !"0".equals(map.get("parentid").toString())
                        ? ("<b style='color:blue;'>" + map.get("parentid") + ":" + map.get("text") + "</b>")
                        : map.get("text"));
                result.add(map);
            }
        }
        return result;
    }

    public static boolean isEnabled() {
        return enable;
    }

    private static String getTimeinfo(long _seconds) {
        if (_seconds <= 0) {
            return _seconds + "";
        }
        long day = 24 * 60 * 60;
        long hour = 60 * 60;
        long min = 60;
        long days = 0;
        days = _seconds / day;
        long hours = 0;
        hours = (_seconds % day) / hour;
        long minutes = 0;
        minutes = (_seconds % hour) / min;
        long seconds = 0;
        seconds = _seconds % 60;
        String info = "";
        if (days > 0) {
            info += days + "天 ";
        }
        if (hours > 0) {
            info += hours + "小时 ";
        }
        if (minutes > 0) {
            info += minutes + "分钟 ";
        }
        if (seconds > 0) {
            info += seconds + "秒 ";
        }

        return info;
    }

    @SuppressWarnings("rawtypes")
    public static void delKey(HttpServletRequest request, HttpServletResponse response) {
        if (null != fastJsonRedisTemplate) {
            try {
                String destroy = request.getParameter("destroy");
                Map map = (Map)(JSON.parseArray(destroy).get(0));
                String key = (String)map.get("id");
                String code = fastJsonRedisTemplate.type(key).code();
                if ("none".equals(code)) {
                    throw new RuntimeException("请选择叶子节点。");
                }
                fastJsonRedisTemplate.delete(key);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static ValueNode getValueinfo(String key) {
        if (null == fastJsonRedisTemplate) {
            throw new RuntimeException(" redis not instance . ");
        }
        ValueNode value = new ValueNode();
        if (null != fastJsonRedisTemplate) {
            value.setKey(key);
            value.setType(fastJsonRedisTemplate.type(key).code());
            Long seconds = fastJsonRedisTemplate.getExpire(key, TimeUnit.SECONDS);
            value.setTil(getTimeinfo(seconds));
            /*
            NONE("none"), STRING("string"), LIST("list"), SET("set"), ZSET("zset"), HASH("hash"),
            STREAM("stream");
            */
            switch (value.getType()) {
                case "string":
                    BoundValueOperations boundValueOperations = fastJsonRedisTemplate.boundValueOps(key);
                    long size = boundValueOperations.size();
                    value.setSize(size);
                    try {
                        value.setValue(boundValueOperations.get(0l, size));
                    } catch (Exception e) {
                    }
                    break;
                case "list":
                    BoundListOperations boundListOperations = fastJsonRedisTemplate.boundListOps(key);
                    size = boundListOperations.size();
                    List<Object> list = boundListOperations.range(0l, size);
                    value.setSize(size);
                    value.setValue(list);
                    break;
                case "set":
                    BoundSetOperations boundSetOperations = fastJsonRedisTemplate.boundSetOps(key);
                    size = boundSetOperations.size();
                    Set<Object> set = boundSetOperations.members();
                    value.setSize(size);
                    value.setValue(set);
                    break;
                case "zset":
                    BoundZSetOperations boundZSetOperations = fastJsonRedisTemplate.boundZSetOps(key);
                    size = boundZSetOperations.size();
                    set = boundZSetOperations.range(0l, size);
                    value.setSize(size);
                    value.setValue(set);
                    break;
                case "hash":
                    BoundHashOperations boundHashOperations = fastJsonRedisTemplate.boundHashOps(key);
                    size = boundHashOperations.size();
                    Map map = boundHashOperations.entries();
                    value.setSize(size);
                    value.setValue(map);
                    break;
                case "stream":
                    BoundStreamOperations boundStreamOperations = fastJsonRedisTemplate.boundStreamOps(key);
                    size = boundStreamOperations.size();
                    value.setSize(size);
                    break;
                default:
                    break;
            }
        }
        return value;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void setValueinfo(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String key = request.getParameter("key");
        String type = request.getParameter("type");
        String value = request.getParameter("value");
        if (null == fastJsonRedisTemplate) {
            throw new RuntimeException(" redis not instance . ");
        }
        String _type = fastJsonRedisTemplate.type(key).code();
        if (!"none".equals(_type)) {
            throw new RuntimeException(" key  " + key + " is existed .");
        }
        ValueNode node = new ValueNode();
        node.setKey(key);
        node.setType(type);
        node.setValue(value);
        /*
        NONE("none"), STRING("string"), LIST("list"), SET("set"), ZSET("zset"), HASH("hash"),
        STREAM("stream");
        */
        switch (type) {
            case "string":
                BoundValueOperations boundValueOperations = fastJsonRedisTemplate.boundValueOps(key);
                // Object obj = JSON.parse(value);
                boundValueOperations.append(value);
                break;
            case "list":
                BoundListOperations boundListOperations = fastJsonRedisTemplate.boundListOps(key);
                List list = (List)JSON.parse(value);
                boundListOperations.leftPushAll(list);
                break;
            case "set":
                boundListOperations = fastJsonRedisTemplate.boundListOps(key);
                list = (List)JSON.parse(value);
                boundListOperations.leftPushAll(list);
                break;
            case "zset":
                boundListOperations = fastJsonRedisTemplate.boundListOps(key);
                list = (List)JSON.parse(value);
                boundListOperations.leftPushAll(list);
                break;
            case "hash":
                BoundHashOperations boundHashOperations = fastJsonRedisTemplate.boundHashOps(key);
                Map map = (Map)JSON.parse(value);
                boundHashOperations.putAll(map);
                break;
            case "stream":
                break;
            default:
                break;
        }
        WebUtil.send(response, JSON.toJSONString(node));
    }

    public static void getValueinfo(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // ValueNode value = new ValueNode();
        String key = request.getParameter("key");
        ValueNode value = getValueinfo(key);
        // if(null!=value.getValue()) {
        // value.setValue(JSONObject.toJSONString(value.getValue(),true));
        // }

        // ObjectMapper mapper = new ObjectMapper();
        // 普通输出
        // System.out.println(mapper.writeValueAsString(value.getValue()));
        // 格式化/美化/优雅的输出
        // System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value.getValue()));
        // WebUtil.send(response, JSON.toJSONString(value, SerializerFeature.WriteClassName));
        // WebUtil.send(response, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value));
        WebUtil.send(response, JSON.toJSONString(value));
    }

    @SuppressWarnings("rawtypes")
    public static void getKeyTree(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (null == fastJsonRedisTemplate) {
            Map<String, Object> root = new HashMap<String, Object>();
            root.put("children", new ArrayList());
            WebUtil.send(response, JSON.toJSONString(root));
            return;
        }
        /**
         * fastJsonRedisTemplate.boundValueOps("test:string").set("string");
         * fastJsonRedisTemplate.boundListOps("test:list").leftPush("list");
         * fastJsonRedisTemplate.boundSetOps("test:set").add("set");
         * fastJsonRedisTemplate.boundZSetOps("test:zset").add("zset", 1);
         * fastJsonRedisTemplate.boundHashOps("test:hash").put("hash", "hash");
         **/
        // fastJsonRedisTemplate.boundValueOps("test:string").expire(10, TimeUnit.SECONDS);

        Set<Object> keys = fastJsonRedisTemplate.keys("*");
        List<KeyNode> nodes = new ArrayList<KeyNode>();
        Set<String> ids = new HashSet<String>();
        for (Object key : keys) {
            String[] keystrs = key.toString().split(":");
            String parentid = "0";
            for (String keystr : keystrs) {
                KeyNode node = new KeyNode();
                String id = "";
                if ("0".equals(parentid)) {
                    id = keystr;
                } else {
                    id = parentid + ":" + keystr;
                }
                node.setId(id);
                node.setName(keystr);
                node.setText(keystr);
                node.setValue(id);
                node.setParentid(parentid);
                parentid = id;

                if (!ids.contains(id))
                    nodes.add(node);

                ids.add(id);
            }
        }
        List<Map<String, Object>> children = getTreeNode(nodes, "0");
        /**
         * for (Map<String, Object> map : children) { boolean leaf = (boolean)map.get("leaf"); if (!leaf) { Integer
         * count = getleafCount(map); String text = (String)map.get("text"); map.put("text", text + "
         * <span style='color:red;'><b>(" + count + ")</b></span>"); } }
         **/
        setCount(children);
        Map<String, Object> root = new HashMap<String, Object>();
        root.put("children", children);
        WebUtil.send(response, JSON.toJSONString(root));
    }

    private static void setCount(List<Map<String, Object>> children) {
        if (null == children || children.size() == 0) {
            return;
        }
        for (Map<String, Object> map : children) {
            boolean leaf = (boolean)map.get("leaf");
            if (!leaf) {
                Integer count = getleafCount(map);
                String text = (String)map.get("text");
                map.put("text", text + " <span style='color:red;'><b>(" + count + ")</b></span>");

                setCount((List<Map<String, Object>>)map.get("children"));
            }
        }
    }

    @SuppressWarnings({"unchecked"})
    private static Integer getleafCount(Map<String, Object> root) {
        Integer result = 0;
        List<Map<String, Object>> children = (List<Map<String, Object>>)root.get("children");
        if (null != children && children.size() > 0) {
            for (Map<String, Object> map : children) {
                boolean leaf = (boolean)map.get("leaf");
                if (leaf) {
                    result++;
                } else {
                    result += getleafCount(map);
                }
            }
        }
        return result;
    }

    public static class ValueNode {
        private String key;
        private String type;
        private Long size;
        private String til;
        private Object value;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Long getSize() {
            return size;
        }

        public void setSize(Long size) {
            this.size = size;
        }

        public String getTil() {
            return til;
        }

        public void setTil(String til) {
            this.til = til;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }
    }

    public static class KeyNode {
        private String id;
        private String name;
        private String text;
        private String value;
        private String parentid;
        private List<KeyNode> children = new ArrayList<KeyNode>();

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getParentid() {
            return parentid;
        }

        public void setParentid(String parentid) {
            this.parentid = parentid;
        }

        public List<KeyNode> getChildren() {
            return children;
        }

        public void setChildren(List<KeyNode> children) {
            this.children = children;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    /**
     * <p>
     * Title: setApplicationContext
     * </p>
     * <p>
     * Description:
     * </p>
     * 
     * @param applicationContext
     * @throws BeansException
     * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, FastJsonRedisTemplate> buffermap = applicationContext.getBeansOfType(FastJsonRedisTemplate.class);
        if (null != buffermap) {
            for (Entry<String, FastJsonRedisTemplate> entry : buffermap.entrySet()) {
                RedisData.fastJsonRedisTemplate = entry.getValue();
                break;
            }
        }
    }

    @Override
    public void setEnvironment(Environment environment) {
        boolean enabled = environment.getProperty("hh.wb.redis.enabled", Boolean.class, false);
        RedisData.enable = enabled;
    }
}
