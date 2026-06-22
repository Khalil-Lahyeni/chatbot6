package actia.monitoring.controller;

import actia.monitoring.service.TrainService;
import actia.monitoring.dto.TrainRequest;
import actia.monitoring.dto.TrainResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/actia/trains")
public class TrainController {

    private final TrainService service;

    @Autowired
    public TrainController(TrainService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<TrainResponse> createTrain(@RequestBody TrainRequest request) {
        TrainResponse created = service.createTrain(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.id())
            .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TrainResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getTrainById(id));
    }

    @GetMapping
    public ResponseEntity<Page<TrainResponse>> getAll(
        @PageableDefault(page = 0, size = 20, sort = "updateAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<TrainResponse> page = service.getAllTrains(pageable);
        return ResponseEntity.ok(page);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TrainResponse> update(@PathVariable Long id, @RequestBody TrainRequest request) {
        return ResponseEntity.ok(service.updateTrain(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteTrain(id);
        return ResponseEntity.noContent().build();
    }
}
