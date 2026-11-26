package pt.ua.tqs.barkito.maintenance.adapter.persistence;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;
import pt.ua.tqs.barkito.maintenance.domain.FieldTechnician;
import pt.ua.tqs.barkito.maintenance.domain.FieldTechnicianRepository;

@Repository
public class InMemoryFieldTechnicianRepository implements FieldTechnicianRepository {

  private final Map<UUID, FieldTechnician> technicians = new ConcurrentHashMap<>();

  @Override
  public FieldTechnician save(FieldTechnician technician) {
    technicians.put(technician.id(), technician);
    return technician;
  }

  @Override
  public List<FieldTechnician> findAll() {
    return technicians.values().stream()
        .sorted(Comparator.comparing(FieldTechnician::name))
        .toList();
  }
}
