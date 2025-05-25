package net.appaura.ai.model;

import lombok.Data;

@Data
public class AppointmentResponse {
    private boolean success;
    private String message;
    private Appointment appointment;

    public static AppointmentResponse success(Appointment appointment) {
        AppointmentResponse response = new AppointmentResponse();
        response.setSuccess(true);
        response.setMessage("Appointment booked successfully");
        response.setAppointment(appointment);
        return response;
    }

    public static AppointmentResponse failure(String message) {
        AppointmentResponse response = new AppointmentResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }
}
