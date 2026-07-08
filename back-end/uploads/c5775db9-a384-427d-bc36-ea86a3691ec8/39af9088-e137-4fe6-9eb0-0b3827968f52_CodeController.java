package com.bugkiller_cityu.code_diagnostician.controller;

import com.bugkiller_cityu.code_diagnostician.service.CodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@CrossOrigin
@RestController
public class CodeController {
    private final CodeService codeService;

    @Autowired
    public CodeController(CodeService codeService){
        this.codeService = codeService;
    }

    @RequestMapping(value = "echo", method = RequestMethod.GET)
    public Map<String, Object> echo(String msg){
        Map<String, Object> response = new HashMap<>();
        response.put("msg", msg);
        return response;
    }
}
