
package com.devsenior.Campusflow;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SaludoController {

    @GetMapping("/api/saludo")
    public String saludar() {
        return "CampusFlow funciona correctamente";
    }
}
