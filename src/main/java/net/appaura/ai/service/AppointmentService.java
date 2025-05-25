package net.appaura.ai.service;

import net.appaura.ai.model.Appointment;
import net.appaura.ai.model.AppointmentResponse;
import net.appaura.ai.repository.AppointmentRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class AppointmentService {

    private final AppointmentRepository repository;

    public AppointmentService(AppointmentRepository repository) {
        this.repository = repository;
    }

    public Mono<Boolean> checkAppointmentAvailability(String doctorName, String date, String time) {
        return repository.findByDoctorNameAndDateAndTime(doctorName, date, time)
                .map(appointment -> false) // Appointment exists, not available
                .switchIfEmpty(Mono.just(true)); // No appointment, available
    }

    public Mono<AppointmentResponse> saveAppointment(Appointment appointment) {
        return checkAppointmentAvailability(
                appointment.getDoctorName(),
                appointment.getDate(),
                appointment.getTime()
        )
                .flatMap(isAvailable -> {
                    if (!isAvailable) {
                        return Mono.just(AppointmentResponse.failure("Record already exists"));
                    }
                    return repository.save(appointment)
                            .map(AppointmentResponse::success);
                });
    }
}