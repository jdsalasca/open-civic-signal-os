package org.opencivic.signalos.repository;

import org.opencivic.signalos.domain.Signal;
import java.util.List;

public interface SignalRepository {
    List<Signal> findAll();
}
