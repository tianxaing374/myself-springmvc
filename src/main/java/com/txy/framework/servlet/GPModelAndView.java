package com.txy.framework.servlet;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GPModelAndView {
    private String view;
    private Map<String,Object> model;

    public GPModelAndView(String view) {
        this.view = view;
    }
}
