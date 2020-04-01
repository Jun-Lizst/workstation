package org.janelia.workstation.controller.eventbus;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;

import java.util.Collection;
import java.util.List;

public class SelectionEvent {
    public enum Type {
        SELECT, DESELECT, CLEAR;
    };

    private Type type;
    private AnnotationCategory category;
    private Collection<DomainObject> items;

    public SelectionEvent(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public AnnotationCategory getCategory() {
        return category;
    }

    public void setCategory(AnnotationCategory category) {
        this.category = category;
    }

    public Collection<DomainObject> getItems() {
        return items;
    }

    public void setItems(Collection<DomainObject> items) {
        this.items = items;
    }
}
