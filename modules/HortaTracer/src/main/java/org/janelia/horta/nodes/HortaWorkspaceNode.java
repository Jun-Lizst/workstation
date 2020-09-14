package org.janelia.horta.nodes;

import java.awt.Color;
import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import org.janelia.console.viewerapi.ObservableInterface;
import org.janelia.console.viewerapi.model.NeuronSet;
import org.janelia.console.viewerapi.model.VantageInterface;
import org.janelia.console.viewerapi.model.HortaMetaWorkspace;
import org.janelia.gltools.GL3Actor;
import org.janelia.gltools.MeshActor;
import org.janelia.horta.loader.DroppedFileHandler;
import org.janelia.horta.loader.GZIPFileLoader;
import org.janelia.horta.loader.SwcLoader;
import org.janelia.horta.loader.TarFileLoader;
import org.janelia.horta.loader.TgzFileLoader;
import org.janelia.model.domain.tiledMicroscope.TmObjectMesh;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.workstation.controller.model.TmModelManager;
import org.openide.ErrorManager;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.datatransfer.PasteType;
import org.openide.util.lookup.Lookups;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Presentation layer for neuron reconstructions in Horta.
 * Following tutorial at https://platform.netbeans.org/tutorials/74/nbm-nodesapi2.html
 * @author Christopher Bruns
 */
public class HortaWorkspaceNode extends AbstractNode
{
    private TmWorkspace workspace;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    public HortaWorkspaceNode(TmWorkspace workspace) {
        super(Children.create(new HortaWorkspaceChildFactory(workspace.getObjectMeshList()), true));
        this.workspace = workspace;
        updateDisplayName();
    }

    public HortaWorkspaceNode(Children children) {
        super(children);
    }

    private void updateDisplayName() {
        setDisplayName("Scene"); //  (" + workspace.getNeuronSets().size() + " neurons)");
    }
    
    public VantageInterface getVantage() {
        //return workspace.getVantage();
        return null;
    }
    
    @Override
    public PasteType getDropType(final Transferable transferable, int action, int index) {
        /** move this to a common location
         *
         */
        final DroppedFileHandler droppedFileHandler = new DroppedFileHandler();
        droppedFileHandler.addLoader(new GZIPFileLoader());
        droppedFileHandler.addLoader(new TarFileLoader());
        droppedFileHandler.addLoader(new TgzFileLoader());
        
        return new PasteType() {
            @Override
            public Transferable paste() throws IOException
            {
                logger.info("Dropping neuron...");
                try {
                    List<File> fileList = (List) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                    for (File f : fileList) {
                        droppedFileHandler.handleFile(f);
                    }
                    // Update after asynchronous load completes
                    /*swcLoader.runAfterLoad(new Runnable() {
                    @Override
                    public void run()
                    {
                        if (! neuronList.getMembershipChangeObservable().hasChanged())
                            return;
                        // Update models after drop.
                        neuronList.getMembershipChangeObservable().notifyObservers();
                        // TODO force repaint - just once per drop action though.
                        triggerRepaint();
                    }
                    });*/
                } catch (UnsupportedFlavorException ex) {
                    logger.error("unsupported flavor during drag and drop; data has flavors:");
                    for (DataFlavor f: transferable.getTransferDataFlavors()){
                        logger.error(f.getHumanPresentableName());
                    }
                    Exceptions.printStackTrace(ex);
                }
                return null;
            }
        };
    }
    
    @Override
    public Image getIcon(int type) {
        return ImageUtilities.loadImage("org/janelia/horta/images/brain-icon2.png");
    }
    
    @Override
    public Image getOpenedIcon(int i) {
        return getIcon(i);
    }

    // need to figure out how to support object meshes
    public Integer getSize() {
        if (workspace!=null)
            return workspace.getObjectMeshList().size();
        return 0;
    }
    
    @Override 
    protected Sheet createSheet() { 
        Sheet sheet = Sheet.createDefault(); 
        Sheet.Set set = Sheet.createPropertiesSet(); 
        try { 
            // size - number of neurons
            Property prop = new PropertySupport.Reflection(this, Integer.class, "getSize", null); 
            prop.setName("size");
            set.put(prop);
        } 
        catch (NoSuchMethodException ex) {
            ErrorManager.getDefault(); 
        } 
        sheet.put(set); 
        return sheet; 
    } // - See more at: https://platform.netbeans.org/tutorials/nbm-nodesapi2.html#sthash.0xrEv8DO.dpuf
    
}
