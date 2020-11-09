package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;

import java.util.Collection;

public class NeuronUpdateEvent extends NeuronEvent {
    public NeuronUpdateEvent(Collection<TmNeuronMetadata> neurons) {
        super(neurons);
    }
}

