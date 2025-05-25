package net.appaura.ai.repository;


import net.appaura.ai.model.Appointment;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface AppointmentRepository extends ReactiveMongoRepository<Appointment, String> {
    Mono<Appointment> findByDoctorNameAndDateAndTime(String doctorName, String date, String time);
}
