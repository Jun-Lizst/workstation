package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.tiledMicroscope.TmAnchoredPath;

import java.util.Collection;

public class AnchoredPathCreateEvent extends AnchoredPathEvent {
    public AnchoredPathCreateEvent(Collection<TmAnchoredPath> paths) {
        this.paths = paths;
    }
}
