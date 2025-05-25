package net.appaura.ai.controller;

import net.appaura.ai.model.Appointment;
import net.appaura.ai.model.AppointmentResponse;
import net.appaura.ai.service.AppointmentService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService service;

    public AppointmentController(AppointmentService service) {
        this.service = service;
    }

    @GetMapping("/check")
    public Mono<Boolean> checkAvailability(
            @RequestParam String doctorName,
            @RequestParam String date,
            @RequestParam String time) {
        return service.checkAppointmentAvailability(doctorName, date, time);
    }

    @PostMapping
    public Mono<AppointmentResponse> createAppointment(@RequestBody Appointment appointment) {
        return service.saveAppointment(appointment);
    }
}
