package net.appaura.ai.controller;

import lombok.extern.log4j.Log4j2;
import net.appaura.ai.model.Appointment;
import net.appaura.ai.service.AppointmentService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/vapi")
@Log4j2
public class VapiController {

    private final AppointmentService service;
    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern TIME_PATTERN = Pattern.compile("\\d{2}:\\d{2}");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public VapiController(AppointmentService service) {
        this.service = service;
    }

    @PostMapping
    public Mono<Map<String, Object>> handleVapiFunction(@RequestBody Map<String, Object> request) {
        log.info("<<<< inside handleVapiFunction() >>>>");
        log.info("Full Request: " + request.toString());

        // Check if the request contains a "message" key with "tool-calls" type
        if (request.containsKey("message")) {
            Map<String, Object> message = (Map<String, Object>) request.get("message");
            String type = (String) message.getOrDefault("type", "");
            log.info("Message Type: " + type);

            if ("tool-calls".equals(type)) {
                // Extract toolCalls list
                List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) message.get("toolCalls");
                if (toolCalls != null && !toolCalls.isEmpty()) {
                    // For simplicity, process the first tool call (you can loop if expecting multiple)
                    Map<String, Object> toolCall = toolCalls.get(0);
                    String toolType = (String) toolCall.get("type");
                    log.info("Tool Type: " + toolType);

                    if ("function".equals(toolType)) {
                        Map<String, Object> functionCall = (Map<String, Object>) toolCall.get("function");
                        log.info("functionCall: " + functionCall.toString());
                        String functionName = (String) functionCall.get("name");
                        log.info("functionName: " + functionName);
                        Map<String, Object> parameters = (Map<String, Object>) functionCall.get("arguments");
                        log.info("parameters: " + parameters.toString());

                        // Process checkAvailability
                        if ("checkAvailability".equals(functionName)) {
                            String doctorName = (String) parameters.get("doctorName");
                            String date = (String) parameters.get("date");
                            String time = (String) parameters.get("time");
                            log.info("Doctor name {}, Date {}, time {} ", doctorName, date, time);

                            // Validate date and time formats
                            if (!isValidDate(date) || !isFutureDate(date)) {
                                Map<String, Object> response = new HashMap<>();
                                response.put("result", "Invalid date format or date is in the past. Please use YYYY-MM-DD and ensure the date is in the future.");
                                response.put("isAvailable", false);
                                return Mono.just(response);
                            }
                            if (!isValidTime(time)) {
                                Map<String, Object> response = new HashMap<>();
                                response.put("result", "Invalid time format. Please use HH:MM in 24-hour format (e.g., 14:30).");
                                response.put("isAvailable", false);
                                return Mono.just(response);
                            }

                            return service.checkAppointmentAvailability(doctorName, date, time)
                                    .doOnNext(isAvailable -> log.info("checkAppointmentAvailability result: {}", isAvailable))
                                    .map(isAvailable -> {
                                        Map<String, Object> response = new HashMap<>();
                                        response.put("result", isAvailable ? "Slot is available" : "Slot is unavailable");
                                        response.put("isAvailable", isAvailable);
                                        log.info("checkAvailability response sent: {}", response);
                                        return response;
                                    }).doOnSuccess(response -> log.info("HTTP response sent successfully for checkAvailability"))
                                    .onErrorResume(e -> {
                                        log.error("Error in checkAppointmentAvailability: ", e);
                                        Map<String, Object> response = new HashMap<>();
                                        response.put("result", "Error checking availability: " + e.getMessage());
                                        response.put("isAvailable", false);
                                        return Mono.just(response);
                                    })
                                    .switchIfEmpty(Mono.defer(() -> {
                                        Map<String, Object> response = new HashMap<>();
                                        response.put("result", "No availability result returned from service");
                                        response.put("isAvailable", false);
                                        return Mono.just(response);
                                    }));
                        }
                        // Process bookAppointment
                        else if ("bookAppointment".equals(functionName)) {
                            log.info("Processing bookAppointment with parameters: {}", parameters);
                            String date = (String) parameters.get("date");
                            String time = (String) parameters.get("time");

                            // Validate date and time formats
                            if (!isValidDate(date) || !isFutureDate(date)) {
                                Map<String, Object> response = new HashMap<>();
                                response.put("result", "Invalid date format or date is in the past. Please use YYYY-MM-DD and ensure the date is in the future.");
                                response.put("success", false);
                                response.put("appointmentId", "");
                                return Mono.just(response);
                            }
                            if (!isValidTime(time)) {
                                Map<String, Object> response = new HashMap<>();
                                response.put("result", "Invalid time format. Please use HH:MM in 24-hour format (e.g., 14:30).");
                                response.put("success", false);
                                response.put("appointmentId", "");
                                return Mono.just(response);
                            }

                            Appointment appointment = new Appointment();
                            appointment.setPatientName((String) parameters.get("patientName"));
                            appointment.setPhone((String) parameters.get("phone"));
                            appointment.setDate(date);
                            appointment.setTime(time);
                            appointment.setIssue((String) parameters.get("issue"));
                            log.info("appointment: " + appointment.toString());

                            return service.saveAppointment(appointment)
                                    .doOnNext(response -> log.info("saveAppointment result: {}", response))
                                    .map(response -> {
                                        Map<String, Object> result = new HashMap<>();
                                        result.put("result", response.getMessage());
                                        result.put("success", response.isSuccess());
                                        result.put("appointmentId", response.isSuccess() ? response.getAppointment().getId() : "");
                                        return result;
                                    })
                                    .onErrorResume(e -> {
                                        log.error("Error in saveAppointment: ", e);
                                        Map<String, Object> response = new HashMap<>();
                                        response.put("result", "Error booking appointment: " + e.getMessage());
                                        response.put("success", false);
                                        response.put("appointmentId", "");
                                        return Mono.just(response);
                                    });
                        }
                    }
                }
            }
        }

        // Default response for unhandled function calls
        log.info("Unhandled request or call terminated: {}", request);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Unhandled function call");
        return Mono.just(response);
    }

    private boolean isValidDate(String date) {
        if (date == null || !DATE_PATTERN.matcher(date).matches()) {
            return false;
        }
        try {
            LocalDate.parse(date, DATE_FORMATTER);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private boolean isFutureDate(String date) {
        try {
            LocalDate inputDate = LocalDate.parse(date, DATE_FORMATTER);
            LocalDate today = LocalDate.now(); // Today is May 27, 2025
            return inputDate.isAfter(today);
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private boolean isValidTime(String time) {
        if (time == null || !TIME_PATTERN.matcher(time).matches()) {
            return false;
        }
        try {
            LocalTime.parse(time, TIME_FORMATTER);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
