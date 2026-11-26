package pt.ua.tqs.barkito.maintenance.domain;

import java.util.List;

/** Port to read and store Field Technicians and the docks they cover (BR17, US-39). */
public interface FieldTechnicianRepository {

  FieldTechnician save(FieldTechnician technician);

  /** Every technician, ordered by name. */
  List<FieldTechnician> findAll();
}
