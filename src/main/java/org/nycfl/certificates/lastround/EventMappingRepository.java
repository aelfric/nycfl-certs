package org.nycfl.certificates.lastround;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.hibernate.orm.panache.common.ProjectedFieldName;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class EventMappingRepository implements PanacheRepository<EventMapping> {

    public List<EventMappingDTO> findByReportId(long reportId) {
        // Find by the parent ID directly
        return find("from EventMapping e left join e.event where e.report.id=?1", reportId)
            .project(EventMappingDTO.class)
            .list();
    }

    public Optional<EventMappingDTO> findDtoById(long mappingId) {
        return find("from EventMapping e where e.id=?1", mappingId)
            .project(EventMappingDTO.class)
            .firstResultOptional();
    }

    @RegisterForReflection
    public record EventMappingDTO(
        String eventRaw,
        @ProjectedFieldName("event.name") String eventName
    ) {

    }

}


