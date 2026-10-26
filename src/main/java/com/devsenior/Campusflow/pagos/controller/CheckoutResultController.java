package com.devsenior.Campusflow.pagos.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CheckoutResultController {

    @GetMapping("/pago-exitoso")
    public String mostrarPagoExitoso() {
        return "forward:/pago-exitoso.html";
    }

    @GetMapping("/pago-cancelado")
    public String mostrarPagoCancelado() {
        return "forward:/pago-cancelado.html";
    }
}
