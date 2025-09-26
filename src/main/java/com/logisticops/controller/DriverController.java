// DriverController.java content
package com.logisticops.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DriverController {
    @GetMapping("/drivers")
    public String getDrivers() {
        return "List of drivers";
    }
}