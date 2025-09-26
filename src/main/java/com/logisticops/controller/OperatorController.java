// OperatorController.java content
package com.logisticops.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OperatorController {
    @GetMapping("/operators")
    public String getOperators() {
        return "List of operators";
    }
}