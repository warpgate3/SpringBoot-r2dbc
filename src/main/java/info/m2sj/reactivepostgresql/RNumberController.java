package info.m2sj.reactivepostgresql;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import static org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE;

@RestController
@RequestMapping(value = "/api/numbers")
public class RNumberController {
    private final RNumberRepository numberRepository;

    public RNumberController(RNumberRepository numberRepository) {
        this.numberRepository = numberRepository;
    }

    @GetMapping(value = "", produces = TEXT_EVENT_STREAM_VALUE)
    public Flux<RNumber> getHome() {
        return numberRepository.findAll();
    }
}
