package org.janelia.it.workstation.gui.large_volume_viewer.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.janelia.it.jacs.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.it.jacs.shared.geom.Vec3;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationManager;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationModel;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationPanel;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.FilteredAnnotationList;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.LargeVolumeViewerTranslator;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.WorkspaceInfoPanel;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.WorkspaceNeuronList;
import org.janelia.it.workstation.gui.large_volume_viewer.style.NeuronStyle;

/**
 * This will have access to setters, etc. on the panels, to provide
 * control feeds from external events.
 * @author fosterl
 */
public class PanelController {
    
    private PanelGlobalListener globalListener;
    private AnnotationPanel annotationPanel;
    private WorkspaceNeuronList wsNeuronList;
    private WorkspaceInfoPanel wsInfoPanel;
    private LargeVolumeViewerTranslator lvvTranslator;
    private FilteredAnnotationList filteredAnnotationList;
    private PanelNotesUpdateListener notesListener;
    private PanelAnnotationListener annotationListener;
    
    public PanelController(
            AnnotationPanel annoPanel,
            FilteredAnnotationList filteredAnnotationList,
            WorkspaceNeuronList wsNeuronList,
            LargeVolumeViewerTranslator lvvTranslator
    ) {
        this.annotationPanel = annoPanel;
        this.filteredAnnotationList = filteredAnnotationList;
        this.wsNeuronList = wsNeuronList;
        this.lvvTranslator = lvvTranslator;

        PanelPanListener ppl = new PanelPanListener();
        this.filteredAnnotationList.setPanListener(ppl);
        this.wsNeuronList.setPanListener(ppl);
        
        PanelTmGeoSelectListener ptgsl = new PanelTmGeoSelectListener();
        this.filteredAnnotationList.setAnnoSelectListener(ptgsl);
    }
    
    public void registerForEvents(AnnotationModel annotationModel) {
        
        globalListener = new PanelGlobalListener();
        annotationModel.addGlobalAnnotationListener(globalListener);
        
        notesListener = new PanelNotesUpdateListener();
        annotationModel.setNotesUpdateListener(notesListener);

        annotationListener = new PanelAnnotationListener();
        annotationModel.addTmGeoAnnotationModListener(annotationListener);
        
        PanelNeuronSelectedListener pnsl = new PanelNeuronSelectedListener(annotationModel);
        wsNeuronList.setNeuronSelectedListener(pnsl);
        
    }
    
    public void registerForEvents(AnnotationManager annotationManager) {
        PanelEditNoteRequestedListener penrl = new PanelEditNoteRequestedListener(annotationManager);
        filteredAnnotationList.setEditNoteRequestListener(penrl);
    }
    
    public void registerForEvents(WorkspaceInfoPanel wsip) {
        this.wsInfoPanel = wsip;
    }
    
    public void unregisterForEvents(AnnotationModel annotationModel) {
        annotationModel.removeGlobalAnnotationListener(globalListener);
        annotationModel.removeTmGeoAnnotationModListener(annotationListener);
        annotationModel.setNotesUpdateListener(null);
        this.globalListener = null;
        this.notesListener = null;
        this.annotationListener = null;
    }
    
    private class PanelGlobalListener extends GlobalAnnotationAdapter {
        @Override
        public void workspaceUnloaded(TmWorkspace workspace) {
            workspaceLoaded(null);
        }
        @Override
        public void workspaceLoaded(TmWorkspace workspace) {
            annotationPanel.loadWorkspace(workspace);
            filteredAnnotationList.loadWorkspace(workspace);
            wsNeuronList.loadWorkspace(workspace);
            wsInfoPanel.loadWorkspace(workspace);
        }

        @Override
        public void spatialIndexReady(TmWorkspace workspace) {
        }
        
        @Override
        public void neuronCreated(TmNeuronMetadata neuron) {
            TmWorkspace workspace = annotationPanel.getAnnotationModel().getCurrentWorkspace();
            filteredAnnotationList.loadNeuron(neuron);
            // TODO: could use a more granular update
            wsNeuronList.loadWorkspace(workspace);
        }

        @Override
        public void neuronDeleted(TmNeuronMetadata neuron) {
            TmWorkspace workspace = annotationPanel.getAnnotationModel().getCurrentWorkspace();
            filteredAnnotationList.loadNeuron(neuron);
            // TODO: could use a more granular update
            wsNeuronList.loadWorkspace(workspace);
        }

        @Override
        public void neuronChanged(TmNeuronMetadata neuron) {
            TmWorkspace workspace = annotationPanel.getAnnotationModel().getCurrentWorkspace();
            filteredAnnotationList.loadNeuron(neuron);
            // TODO: could use a more granular update
            wsNeuronList.loadWorkspace(workspace);
        }

        @Override
        public void neuronRenamed(TmNeuronMetadata neuron) {
            filteredAnnotationList.loadNeuron(neuron);
            wsNeuronList.updateModel(neuron);
        }
        
        @Override
        public void neuronSelected(TmNeuronMetadata neuron) {
            filteredAnnotationList.loadNeuron(neuron);
            wsNeuronList.selectNeuron(neuron);
        }

        @Override
        public void neuronStyleChanged(TmNeuronMetadata neuron, NeuronStyle style) {
            wsNeuronList.neuronStyleChanged(neuron, style);
        }

        @Override
        public void neuronStylesChanged(Map<TmNeuronMetadata, NeuronStyle> neuronStyleMap) {
            wsNeuronList.neuronStylesChanged(neuronStyleMap);
        }

        @Override
        public void neuronTagsChanged(List<TmNeuronMetadata> neuronList) {
            wsNeuronList.neuronTagsChanged(neuronList);
        }

    }
    
    private class PanelAnnotationListener implements TmGeoAnnotationModListener {

        @Override
        public void annotationAdded(TmGeoAnnotation annotation) {
            filteredAnnotationList.annotationChanged(annotation);
        }

        @Override
        public void annotationsDeleted(List<TmGeoAnnotation> annotations) {   
            filteredAnnotationList.annotationsChanged(annotations);         
        }

        @Override
        public void annotationReparented(TmGeoAnnotation annotation, Long prevNeuronId) {
            filteredAnnotationList.annotationChanged(annotation);
        }

        @Override
        public void annotationNotMoved(TmGeoAnnotation annotation) {
            filteredAnnotationList.annotationChanged(annotation);
        }

        @Override
        public void annotationMoved(TmGeoAnnotation annotation) {
            filteredAnnotationList.annotationChanged(annotation);   
        }

        @Override
        public void annotationRadiusUpdated(TmGeoAnnotation annotation) {
            filteredAnnotationList.annotationChanged(annotation);
               
        }    
    }
    
    private class PanelPanListener implements CameraPanToListener {

        @Override
        public void cameraPanTo(Vec3 location) {
            lvvTranslator.cameraPanTo(location);
        }
        
    }
    
    private class PanelTmGeoSelectListener implements AnnotationSelectionListener {

        @Override
        public void annotationSelected(Long annotationID) {
            lvvTranslator.fireNextParentEvent(annotationID);
        }
        
    }
    
    private class PanelNotesUpdateListener implements NotesUpdateListener {

        @Override
        public void notesUpdated(TmGeoAnnotation ann) {
            filteredAnnotationList.notesChanged(ann);
        }
        
    }
    
    private class PanelNeuronSelectedListener implements NeuronSelectedListener {

        private AnnotationModel model;
        
        public PanelNeuronSelectedListener(AnnotationModel model) {
            this.model = model;
        }
        
        @Override
        public void selectNeuron(TmNeuronMetadata neuron) {
            model.selectNeuron(neuron);
        }
        
    }
    
    private class PanelEditNoteRequestedListener implements EditNoteRequestedListener {

        private AnnotationManager mgr;
        
        public PanelEditNoteRequestedListener(AnnotationManager mgr) {
            this.mgr = mgr;
        }
        
        @Override
        public void editNote(TmGeoAnnotation annotation) {
            mgr.addEditNote(annotation.getNeuronId(), annotation.getId());
        }
        
    }
}