package org.janelia.it.workstation.gui.browser.components;

import java.awt.Component;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.workstation.gui.browser.events.Events;
import org.janelia.it.workstation.gui.browser.events.model.DomainObjectRemoveEvent;
import org.janelia.it.workstation.gui.browser.events.selection.DomainObjectSelectionEvent;
import org.janelia.it.workstation.shared.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

/**
 * Manages the life cycle of domain list viewers based on user generated selected events. This manager
 * either reuses existing viewers, or creates them as needed and docks them in the appropriate place.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainListViewManager implements ViewerManager<DomainListViewTopComponent> {

    private final static Logger log = LoggerFactory.getLogger(DomainListViewManager.class);
    
    public static DomainListViewManager instance;
    
    private DomainListViewManager() {
    }
    
    public static DomainListViewManager getInstance() {
        if (instance==null) {
            instance = new DomainListViewManager();
            Events.getInstance().registerOnEventBus(instance);
        }
        return instance;
    }

    /* Manage the active instance of this top component */
    
    private DomainListViewTopComponent activeInstance;
    void activate(DomainListViewTopComponent instance) {
        activeInstance = instance;
    }
    boolean isActive(DomainListViewTopComponent instance) {
        return activeInstance == instance;
    }
    @Override
    public DomainListViewTopComponent getActiveViewer() {
        return activeInstance;
    }
    
    @Override
    public String getViewerName() {
        return "DomainListViewTopComponent";
    }

    @Override
    public Class<DomainListViewTopComponent> getViewerClass() {
        return DomainListViewTopComponent.class;
    }

    @Subscribe
    public void domainObjectsSelected(DomainObjectSelectionEvent event) {

        // We only care about single selections
        DomainObject domainObject = event.getObjectIfSingle();
        if (domainObject==null) {
            return;
        }
        
        // We only care about selection events
        if (!event.isSelect()) {
            log.debug("Event is not selection: {}",event);
            return;
        }

        // We only care about events generated by the explorer
        if (!Utils.hasAncestorWithType((Component)event.getSource(),DomainExplorerTopComponent.class)) {
            log.trace("Event source is not domain explorer: {}",event);
            return;
        }

        
        log.info("domainObjectSelected({})",Reference.createFor(domainObject));
        
        DomainListViewTopComponent targetViewer = ViewerUtils.provisionViewer(DomainListViewManager.getInstance(), "editor"); 
        targetViewer.loadDomainObject(domainObject, false);
    }
}
