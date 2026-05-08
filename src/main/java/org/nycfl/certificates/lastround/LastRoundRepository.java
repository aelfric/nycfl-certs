package org.nycfl.certificates.lastround;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class LastRoundRepository implements PanacheRepository<LastRoundImport> {

}
