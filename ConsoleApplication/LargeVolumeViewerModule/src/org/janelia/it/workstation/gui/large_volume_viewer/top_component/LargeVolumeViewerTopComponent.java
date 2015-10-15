/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.gui.large_volume_viewer.top_component;

import java.awt.BorderLayout;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.ToolTipManager;
import org.janelia.console.viewerapi.model.NeuronSet;
import org.janelia.it.workstation.gui.large_volume_viewer.LargeVolumeViewViewer;
import org.janelia.it.workstation.gui.large_volume_viewer.neuron_api.ReconstructionCollectionAdapter;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.windows.TopComponent;
import org.openide.util.NbBundle.Messages;
import static org.janelia.it.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerTopComponentDynamic.*;
import org.openide.util.lookup.Lookups;
import org.openide.windows.WindowManager;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(
        dtd = "-//org.janelia.it.workstation.gui.large_volume_viewer.top_component//LargeVolumeViewer//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = LVV_PREFERRED_ID,
        //iconBase="SET/PATH/TO/ICON/HERE", 
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "editor", openAtStartup = false)
@ActionID(category = "Window", id = "org.janelia.it.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerTopComponent")
@ActionReference(path = "Menu/Window" /*, position = 333 */)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_LargeVolumeViewerAction",
        preferredID = LVV_PREFERRED_ID
)
@Messages({
    ACTION,
    WINDOW_NAMER,
    HINT
})
public final class LargeVolumeViewerTopComponent extends TopComponent {
    static {
        // So popup menu shows over GLCanvas
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
    }
    
    private final LargeVolumeViewerTopComponentDynamic state = new LargeVolumeViewerTopComponentDynamic();
    
    public static final LargeVolumeViewerTopComponent findThisTopComponent() {
        return (LargeVolumeViewerTopComponent)WindowManager.getDefault().findTopComponent(LVV_PREFERRED_ID);
    }
    
    public static JComponent findThisComponent() {
        return findThisTopComponent();
    }

   public LargeVolumeViewerTopComponent() {
        initComponents();
        setName(Bundle.CTL_LargeVolumeViewerTopComponent());
        setToolTipText(Bundle.HINT_LargeVolumeViewerTopComponent());
        establishLookups();
    }

    public void openLargeVolumeViewer( Long entityId ) throws Exception {
        state.load( entityId );
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();

        jPanel1.setLayout(new java.awt.BorderLayout());

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel jPanel1;
    // End of variables declaration//GEN-END:variables
    @Override
    public void componentOpened() {
        jPanel1.add( state.getLvvv(), BorderLayout.CENTER );
    }

    @Override
    public void componentClosed() {
        jPanel1.remove( state.getLvvv() );
        state.close();
    }
    
    public LargeVolumeViewViewer getLvvv() {
        return state.getLvvv();
    }
    
    void writeProperties(java.util.Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        // TODO store your settings
    }

    void readProperties(java.util.Properties p) {
        String version = p.getProperty("version");
        // TODO read your settings according to their version
    }

    protected void establishLookups() {
        // Use Lookup to communicate sample location and camera position
        // TODO: separate data source from current view details
        LargeVolumeViewerLocationProvider locProvider = 
                new LargeVolumeViewerLocationProvider(state.getLvvv());
       
        // Use Lookup to communicate neuron reconstructions.
        // Based on tutorial at https://platform.netbeans.org/tutorials/74/nbm-selection-1.html
        NeuronSet neurons = new ReconstructionCollectionAdapter(this);
        
        associateLookup(Lookups.fixed(
            locProvider, 
            neurons));
    }
    
}
