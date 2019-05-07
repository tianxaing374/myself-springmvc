package com.txy.demo;

import com.txy.framework.annotation.GPAutowired;
import com.txy.framework.annotation.GPController;

@GPController
public class Student {
    @GPAutowired
    private Teacher teacher;

    @GPAutowired
    private IService iService;
}
