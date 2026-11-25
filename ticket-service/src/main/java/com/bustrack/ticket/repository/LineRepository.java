package com.bustrack.ticket.repository;

import com.bustrack.ticket.model.Line;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LineRepository extends JpaRepository<Line, Long> {}
