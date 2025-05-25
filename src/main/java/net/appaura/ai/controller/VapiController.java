package net.appaura.ai.controller;

import lombok.extern.log4j.Log4j2;
import net.appaura.ai.model.Appointment;
import net.appaura.ai.service.AppointmentService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/vapi")
@Log4j2
public class VapiController {

    private final AppointmentService service;

    public VapiController(AppointmentService service) {
        this.service = service;
    }

    @PostMapping
    public Mono<Map<String, Object>> handleVapiFunction(@RequestBody Map<String, Object> request) {
        String type = (String) request.get("type");
        if ("function-call".equals(type)) {
            Map<String, Object> functionCall = (Map<String, Object>) request.get("functionCall");
            String functionName = (String) functionCall.get("name");
            Map<String, Object> parameters = (Map<String, Object>) functionCall.get("parameters");

            if ("checkAvailability".equals(functionName)) {
                String doctorName = (String) parameters.get("doctorName");
                String date = (String) parameters.get("date");
                String time = (String) parameters.get("time");

                return service.checkAppointmentAvailability(doctorName, date, time)
                        .map(isAvailable -> Map.of(
                                "result", isAvailable ? "Slot is available" : "Slot is unavailable",
                                "isAvailable", isAvailable
                        ));
            } else if ("bookAppointment".equals(functionName)) {
                Appointment appointment = new Appointment();
                appointment.setPatientName((String) parameters.get("patientName"));
                appointment.setPhone((String) parameters.get("phone"));
                appointment.setEmail((String) parameters.get("email"));
                appointment.setAddress((String) parameters.get("address"));
                appointment.setDoctorName((String) parameters.get("doctorName"));
                appointment.setDate((String) parameters.get("date"));
                appointment.setTime((String) parameters.get("time"));
                appointment.setFees(((Number) parameters.get("fees")).doubleValue());
                appointment.setIssue((String) parameters.get("issue"));

                return service.saveAppointment(appointment)
                        .map(response -> Map.of(
                                "result", response.getMessage(),
                                "success", response.isSuccess(),
                                "appointmentId", response.isSuccess() ? response.getAppointment().getId() : ""
                        ));
            }
        }

        return Mono.just(Map.of("message", "Unhandled function call"));
    }

}
