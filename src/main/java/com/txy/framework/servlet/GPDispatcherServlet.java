package com.txy.framework.servlet;

import com.txy.framework.annotation.GPController;
import com.txy.framework.annotation.GPRequestMapping;
import com.txy.framework.annotation.GPRequestParam;
import com.txy.framework.context.GPApplicationContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GPDispatcherServlet extends HttpServlet {

    private static final String LOCATION = "contextConfigLocation";
//    private Map<Pattern,Handler> handlerMapping = new ConcurrentHashMap<>();
    private List<Handler> handlerMapping = new ArrayList<>();
    private List<ViewResolver> viewResolvers = new ArrayList<>();

    //adaptorMapping也可以改造成list
    private Map<Handler,HandlerAdaptor> adaptorMapping = new ConcurrentHashMap<>();

    @Override
    public void init(ServletConfig config) throws ServletException {
        //IOC容器必须要初始化
        GPApplicationContext context = new GPApplicationContext(config.getInitParameter(LOCATION));
        //请求解析
        initMultipartResolver(context);
        //多语言国际化
        initLocaleResolver(context);
        //主题View层
        initThemeResolver(context);

        //解析url和method的关联关系
        initHandlerMappings(context);
        //适配器，匹配过程
        initHandlerAdapters(context);

        //异常解析
        initHandlerExceptionResolvers(context);
        //视图的转发，根据视图的名字匹配到一个具体的模板
        initRequestToViewNameTranslator(context);
        //解析模板中的内容，拿到服务器传来的数据，生成HTML代码
        initViewResolvers(context);
        //一个管理器
        initFlashMapManager(context);

        System.out.println("GPSpringMVC init 完成。");
    }

    private void initMultipartResolver(GPApplicationContext context) {
    }

    private void initLocaleResolver(GPApplicationContext context) {
    }

    private void initThemeResolver(GPApplicationContext context) {
    }

    //url和Method的关系
    private void initHandlerMappings(GPApplicationContext context) {
        Map<String, Object> ioc = context.getAll();
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            if(!clazz.isAnnotationPresent(GPController.class)) continue;
            String url = "";
            if(clazz.isAnnotationPresent(GPRequestMapping.class)){
                GPRequestMapping requestMapping = clazz.getAnnotation(GPRequestMapping.class);
                url += requestMapping.value();
            }
            Method[] methods = clazz.getDeclaredMethods();
            for (Method method : methods) {
                if (!method.isAnnotationPresent(GPRequestMapping.class)) {
                    continue;
                }
                GPRequestMapping requestMapping = method.getAnnotation(GPRequestMapping.class);
                String regex = url + requestMapping.value().replaceAll("/+","/");
                Pattern pattern = Pattern.compile(regex);
                handlerMapping.add(new Handler(pattern,entry.getValue(),method));
            }
        }
    }

    //适配器（匹配过程）
    //主要是用来动态匹配参数列表
    //动态赋值
    private void initHandlerAdapters(GPApplicationContext context) {
        //参数类型key，参数索引value
        Map<String,Integer> paramMapping = new HashMap<>();
        for (Handler handler : handlerMapping) {
            Class<?>[] parameterTypes = handler.getMethod().getParameterTypes();
            //匹配Request,Response
            for (int i = 0, length = parameterTypes.length; i < length; i++) {
                Class<?> type = parameterTypes[i];
                if(type == HttpServletRequest.class || type == HttpServletResponse.class){
                    paramMapping.put(type.getName(),i);
                }
            }
            //匹配自定义参数列表
            Annotation[][] pa = handler.getMethod().getParameterAnnotations();
            for (int i = 0; i < pa.length; i++) {
                for (Annotation a : pa[i]) {
                    if(a instanceof GPRequestParam){
                        String paramName = ((GPRequestParam) a).value();
                        if(!"".equals(paramName.trim())){
                            paramMapping.put(paramName,i);
                        }
                    }
                }
            }
            adaptorMapping.put(handler,new HandlerAdaptor(paramMapping));
        }
    }

    private void initHandlerExceptionResolvers(GPApplicationContext context) {
    }

    private void initRequestToViewNameTranslator(GPApplicationContext context) {
    }

    private void initViewResolvers(GPApplicationContext context) {
        String templateRoot = context.getConfig().getProperty("templateRoot");
        String rootPath = getClass().getClassLoader().getResource(templateRoot).getFile();
        File rootDir = new File(rootPath);
        for (File template : rootDir.listFiles()) {
            viewResolvers.add(new ViewResolver(template.getName(),template));
        }
    }

    private void initFlashMapManager(GPApplicationContext context) {
    }


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doDispatch(req,resp);
        } catch (Exception e) {
            resp.getWriter().write("500 Exception . Msg:"+ Arrays.toString(e.getStackTrace()));
        }
    }

    private Handler getHandler(HttpServletRequest req){
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replace(contextPath,"").replaceAll("/+","/");

        for (Handler handler : handlerMapping) {
            Matcher matcher = handler.getPattern().matcher(url);
            if(!matcher.matches()) continue;
            return handler;
        }

        return null;
    }

    private HandlerAdaptor getHandlerAdaptor(Handler handler){
        return adaptorMapping.get(handler) ;
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception{
        try {
            //先从HandlerMapping取出一个Handler
            Handler handler = getHandler(req);
            if (handler == null){
                resp.getWriter().write("404 not found");
                return;
            }
            //在取出一个适配器
            HandlerAdaptor ha = getHandlerAdaptor(handler);
            //再由适配器去调用方法
            GPModelAndView mv = ha.handle(req, resp, handler);

            //自己写一个模板框架
            // @{name} 进行取值
            applyDefaultViewName(resp,mv);

        } catch (Exception e) {
            throw e;
        }
    }

    public void applyDefaultViewName(HttpServletResponse resp, GPModelAndView mv) throws IOException {
        if (mv == null) return;
        for (ViewResolver resolver : viewResolvers) {
            if(!mv.getView().equals(resolver.getViewName())) continue;
            String r = resolver.parse(mv);
            if(r!=null){
                resp.getWriter().write(r);
                break;
            }
        }
    }

    //HandleMapping的定义
    //一个url与哪个实例的哪个方法对应
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private class Handler {
        protected Pattern pattern;
        protected Object controller;
        protected Method method;
    }

    //方法适配器
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private class HandlerAdaptor{
        private Map<String, Integer> paramMapping;
        public GPModelAndView handle(HttpServletRequest req, HttpServletResponse resp, Handler handler) throws Exception {
            Class<?>[] parameterTypes = handler.getMethod().getParameterTypes();
            Object[] paramValue = new Object[parameterTypes.length];
            Map<String, String[]> params = req.getParameterMap();
            for (Map.Entry<String, String[]> param : params.entrySet()) {
                String value = Arrays.toString(param.getValue()).replaceAll("(\\[|\\])", "").replaceAll(",\\s", ",");
                if(!paramMapping.containsKey(param.getKey())) continue;
                int index = paramMapping.get(param.getKey());
                paramValue[index] = castString(value,parameterTypes[index]);
            }

            String reqName = HttpServletRequest.class.getName();
            if(paramMapping.containsKey(reqName)){
                Integer reqIndex = paramMapping.get(reqName);
                paramValue[reqIndex] = req;
            }

            String respName = HttpServletResponse.class.getName();
            if(paramMapping.containsKey(respName)){
                Integer respIndex = paramMapping.get(respName);
                paramValue[respIndex] = resp;
            }
            boolean isModelAndView = handler.getMethod().getReturnType() == GPModelAndView.class;
            Object r = handler.method.invoke(handler.getController(), paramValue);
            if(isModelAndView){
                return (GPModelAndView)r;
            } else {
                return null;
            }
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private class ViewResolver {
        protected String viewName;
        private File file;
        protected String parse(GPModelAndView mv) throws IOException {
            StringBuffer sb = new StringBuffer();
            RandomAccessFile ra = new RandomAccessFile(this.file, "r");
            String line = null;
            try {
                //所有的模板框架都是通过正则处理字符串
                while (null!=(line = ra.readLine())){
                    Matcher matcher = matcher(line);
                    while (matcher.find()){
                        for (int i = 1; i <= matcher.groupCount() ; i++) {
                            String paramName = matcher.group(i);
                            Object paramValue = mv.getModel().get(paramName);
                            if(null == paramValue) continue;
                            line = line.replaceAll("@\\{"+paramName+"\\}",paramValue.toString());
                        }
                    }
                    sb.append(line);
                }
            } finally {
                ra.close();
            }
            return sb.toString();
        }

        private Matcher matcher(String str){
            Pattern pattern = Pattern.compile("@\\{(.+?)\\}", Pattern.CASE_INSENSITIVE);
            return pattern.matcher(str);
        }

    }

    private Object castString(String value,Class<?> clazz){
        if(clazz == String.class){
            return value;
        } else if(clazz == Integer.class || clazz == int.class){
            return Integer.valueOf(value);
        } else {
            return null;
        }
    }

}
