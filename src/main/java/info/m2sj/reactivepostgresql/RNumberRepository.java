package info.m2sj.reactivepostgresql;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RNumberRepository extends ReactiveCrudRepository<RNumber, Long> {

}