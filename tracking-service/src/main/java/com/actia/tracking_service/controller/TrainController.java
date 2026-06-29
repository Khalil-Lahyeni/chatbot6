package com.actia.tracking_service.controller;

import com.actia.tracking_service.dto.PageResponse;
import com.actia.tracking_service.dto.TrainRequest;
import com.actia.tracking_service.dto.TrainResponse;
import com.actia.tracking_service.service.TrainService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/actia/trains")
@RequiredArgsConstructor
public class TrainController {

    private final TrainService service;

    @PostMapping
    public ResponseEntity<TrainResponse> createTrain(@Valid @RequestBody TrainRequest request) {
        TrainResponse created = service.createTrain(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.trainId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TrainResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getTrainById(id));
    }

    @GetMapping
    public ResponseEntity<PageResponse<TrainResponse>> getAll(
            @PageableDefault(page = 0, size = 20, sort = "updateAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(PageResponse.of(service.getAllTrains(pageable)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TrainResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody TrainRequest request) {
        return ResponseEntity.ok(service.updateTrain(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteTrain(id);
        return ResponseEntity.noContent().build();
    }
}
