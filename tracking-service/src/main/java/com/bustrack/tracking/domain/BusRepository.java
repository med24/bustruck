package com.bustrack.tracking.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BusRepository extends JpaRepository<Bus, String> {
    List<Bus> findByLineCode(String lineCode);
    Optional<Bus> findFirstByLineCodeOrderByLastTimestampDesc(String lineCode);
}
