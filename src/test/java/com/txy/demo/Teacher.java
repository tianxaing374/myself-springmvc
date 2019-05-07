package com.txy.demo;

import com.txy.framework.annotation.GPAutowired;
import com.txy.framework.annotation.GPController;

@GPController
public class Teacher {
    @GPAutowired
    private Student student;
}
