package com.txy.framework.context;

import com.txy.demo.Student;
import com.txy.demo.Teacher;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by TianXiang on 2019/5/4.
 */
public class GPApplicationContextTest {

    @Test
    public void test01() {
        GPApplicationContext context = new GPApplicationContext("application.properties");
        Map<String, Object> map = context.getAll();
        System.out.println(map);
        Object student = context.getBean("student");
        Teacher teacher = context.getBean("teacher", Teacher.class);
        System.out.println(teacher);
        Object iService = context.getBean("iService");
        Object iServiceImpl = context.getBean("iServiceImpl");
        Assert.assertNotNull(iService);
        Assert.assertEquals(iService,iServiceImpl);
    }

}