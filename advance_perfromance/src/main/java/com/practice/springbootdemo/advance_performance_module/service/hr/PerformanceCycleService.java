package com.practice.springbootdemo.advance_performance_module.service.hr;

import com.practice.springbootdemo.advance_performance_module.entities.CycleStatus;
import com.practice.springbootdemo.advance_performance_module.entities.PerformanceCycle;
import com.practice.springbootdemo.advance_performance_module.exception.DuplicateResourceException;
import com.practice.springbootdemo.advance_performance_module.exception.InvalidPerformanceCycleException;
import com.practice.springbootdemo.advance_performance_module.exception.ResourceNotFoundException;
import com.practice.springbootdemo.advance_performance_module.repository.PerformanceCycleRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PerformanceCycleService {
    private final PerformanceCycleRepository repository;

    public PerformanceCycleService(PerformanceCycleRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public CycleResponse create(CreateCycleRequest request, Long hrUserId) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new InvalidPerformanceCycleException("End date cannot be before start date");
        }
        if (repository.existsByNameIgnoreCase(request.name().trim())) {
            throw new DuplicateResourceException("Performance cycle name already exists: " + request.name());
        }
        PerformanceCycle cycle = PerformanceCycle.builder()
                .name(request.name().trim())
                .description(request.description())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .status(CycleStatus.DRAFT)
                .createdBy(hrUserId)
                .build();
        PerformanceCycle saved = repository.save(cycle);
        log.info("HR user {} created Performance Cycle: ID={}, Name={}", hrUserId, saved.getId(), saved.getName());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CycleResponse> getAll() {
        return repository.findAll().stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public CycleResponse getActive() {
        PerformanceCycle cycle = repository.findFirstByStatusOrderByStartDateDesc(CycleStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("No ACTIVE performance cycle found"));
        return mapToResponse(cycle);
    }

    @Transactional(readOnly = true)
    public CycleResponse getById(Long id) {
        PerformanceCycle cycle = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Performance cycle not found with ID: " + id));
        return mapToResponse(cycle);
    }

    @Transactional
    public CycleResponse update(Long id, UpdateCycleRequest request) {
        PerformanceCycle cycle = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Performance cycle not found with ID: " + id));
        if (cycle.getStatus() == CycleStatus.CLOSED) {
            throw new InvalidPerformanceCycleException("Cannot modify a CLOSED performance cycle");
        }
        if (request.endDate().isBefore(request.startDate())) {
            throw new InvalidPerformanceCycleException("End date cannot be before start date");
        }
        cycle.setName(request.name().trim());
        cycle.setDescription(request.description());
        cycle.setStartDate(request.startDate());
        cycle.setEndDate(request.endDate());
        PerformanceCycle saved = repository.save(cycle);
        log.info("Updated performance cycle ID: {}", id);
        return mapToResponse(saved);
    }

    @Transactional
    public CycleResponse activate(Long id) {
        PerformanceCycle cycle = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Performance cycle not found with ID: " + id));
        if (cycle.getStatus() == CycleStatus.CLOSED) {
            throw new InvalidPerformanceCycleException("Cannot activate a CLOSED performance cycle");
        }
        cycle.setStatus(CycleStatus.ACTIVE);
        PerformanceCycle saved = repository.save(cycle);
        log.info("Performance cycle ID {} launched/activated", id);
        return mapToResponse(saved);
    }

    @Transactional
    public CycleResponse close(Long id) {
        PerformanceCycle cycle = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Performance cycle not found with ID: " + id));
        cycle.setStatus(CycleStatus.CLOSED);
        PerformanceCycle saved = repository.save(cycle);
        log.info("Performance cycle ID {} closed", id);
        return mapToResponse(saved);
    }

    private CycleResponse mapToResponse(PerformanceCycle cycle) {
        return new CycleResponse(
                cycle.getId(),
                cycle.getName(),
                cycle.getDescription(),
                cycle.getStartDate(),
                cycle.getEndDate(),
                cycle.getStatus(),
                cycle.getCreatedBy(),
                cycle.getCreatedAt()
        );
    }
}
