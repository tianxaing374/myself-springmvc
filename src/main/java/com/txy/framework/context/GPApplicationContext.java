package com.txy.framework.context;

import com.txy.framework.annotation.GPAutowired;
import com.txy.framework.annotation.GPController;
import com.txy.framework.annotation.GPService;
import lombok.Getter;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class GPApplicationContext {

    private Map<String, Object> instanceMapping = new ConcurrentHashMap<>();
    private List<String> classCache = new ArrayList<>();

    @Getter
    private Properties config = new Properties();

    public GPApplicationContext(String location) {
        InputStream is = null;
        try {
            if(location.startsWith("classpath*:")){
                location = location.replace("classpath*:","");
            }
            is = getClass().getClassLoader().getResourceAsStream(location);
            //先加载配置文件
            config.load(is);
            //注册，把所有的class找出来
            String scanPackageName = config.getProperty("scanPackage");
            doRegister(scanPackageName);
            //初始化
            doCreateBean();
            //注入
            populate();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public Object getBean(String name) {
        return instanceMapping.get(name);
    }

    @SuppressWarnings("unchecked")
    public <T> T getBean(String name,Class<? extends T> type) {
        return (T)(instanceMapping.get(name));
    }

    public Map<String, Object> getAll() {
        return instanceMapping;
    }

    //把符合的class，加载进缓存
    private void doRegister(String scanPackageName) {
        URL url = getClass().getClassLoader().getResource( scanPackageName.replace(".", "/"));
        File dir = new File(url.getFile());
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                doRegister(scanPackageName + "." + file.getName());
            } else {
                classCache.add(scanPackageName + "." + file.getName().replace(".class", "").trim());
            }
        }
    }

    private void doCreateBean() {
        //检查注册信息
        if (classCache.size() == 0) return;
        try {
            for (String className : classCache) {
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(GPController.class) || clazz.isAnnotationPresent(GPService.class)) {

                    String id = lowerFirstChar(clazz.getSimpleName());
                    GPController gpController = clazz.getAnnotation(GPController.class);
                    String value = gpController.value();
                    id = "".equals(value) ? id : value;
                    if(!clazz.isInterface()){
                        Object newInstance = clazz.newInstance();
                        instanceMapping.put(id, newInstance);
                        Class<?>[] interfaces = clazz.getInterfaces();
                        //如果实现了接口，这将这个实例作为接口的实现类注入进容器
                        for (Class<?> i : interfaces) {
                            instanceMapping.put(lowerFirstChar(i.getSimpleName()),newInstance);
                        }
                    }
                } else {
                    //todo
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void populate() {
        if(instanceMapping.isEmpty()) return;
        for (Map.Entry<String, Object> entry : instanceMapping.entrySet()) {
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {
                if (field.isAnnotationPresent(GPAutowired.class)) {
                    GPAutowired autowired = field.getAnnotation(GPAutowired.class);
                    String id = autowired.value();
                    id = "".equals(id) ? lowerFirstChar(field.getType().getSimpleName()):id;
                    field.setAccessible(true);
                    try {
                        field.set(entry.getValue(),instanceMapping.get(id));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private String lowerFirstChar(String str) {
        char[] chars = str.toCharArray();
        char c = chars[0];
        if (c >= 'A' && c <= 'Z') {
            chars[0] += 32;
        }
        return String.valueOf(chars);
    }

}
