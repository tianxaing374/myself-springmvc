package com.txy.demo;

import com.txy.framework.annotation.GPController;
import com.txy.framework.annotation.GPRequestMapping;
import com.txy.framework.annotation.GPRequestParam;
import com.txy.framework.annotation.GPResponseBody;
import com.txy.framework.servlet.GPModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@GPController
@GPRequestMapping("/web")
public class FirstAction {

    @GPRequestMapping("/query/.*.json")
//    @GPResponseBody
    public GPModelAndView query(HttpServletRequest request,
                                HttpServletResponse response,
                                @GPRequestParam("name") String name,
                                @GPRequestParam("address") String address){
        Map<String,Object> map = new HashMap<>();
        map.put("name",name);
        map.put("address",address);
        return new GPModelAndView("first.gpml",map);
    }

    @GPRequestMapping("/add.json")
    public GPModelAndView add(HttpServletRequest request,
                                HttpServletResponse response){
        out(response,"this is json string");
        return null;
    }

    public void out(HttpServletResponse response,
                    String str){
        try {
            response.getWriter().write(str);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}









/*
package com.txy.framework.servlet;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.GPModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/web")
public class FirstAction {

    @RequestMapping("first/{url}")
    public GPModelAndView first(@RequestParam("name") String name,
                              @PathVariable("url") String url,
                              HttpServletRequest request,
                              HttpServletResponse response){
        System.out.println(name+url);
        return null;
    }

}
*/
