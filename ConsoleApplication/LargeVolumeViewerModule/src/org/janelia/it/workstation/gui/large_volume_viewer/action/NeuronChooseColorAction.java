package org.janelia.it.workstation.gui.large_volume_viewer.action;

import java.awt.event.ActionEvent;

import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationManager;
import org.janelia.it.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerTopComponent;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

@ActionID(
        category = "Large Volume Viewer",
        id = "NeuronChooseColorAction"
)
@ActionRegistration(
        displayName = "Choose neuron color",
        lazy = true
)
@ActionReferences({
    @ActionReference(path = "Shortcuts", name = "OS-C")
})
public class NeuronChooseColorAction extends EditAction {
    
    public NeuronChooseColorAction() {
        super("Choose neuron color..");
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        AnnotationManager annotationMgr = LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr();
        annotationMgr.chooseNeuronColor();
    }
}
