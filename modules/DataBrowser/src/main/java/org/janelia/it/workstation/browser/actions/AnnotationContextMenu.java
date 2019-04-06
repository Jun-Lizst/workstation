package org.janelia.it.workstation.browser.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JMenuItem;

import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.api.ClientDomainUtils;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.StateMgr;
import org.janelia.it.workstation.browser.gui.dialogs.DomainDetailsDialog;
import org.janelia.it.workstation.browser.gui.model.ImageModel;
import org.janelia.it.workstation.browser.gui.support.PopupContextMenu;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.ontology.Accumulation;
import org.janelia.model.domain.ontology.Annotation;
import org.janelia.model.domain.ontology.EnumText;
import org.janelia.model.domain.ontology.Interval;
import org.janelia.model.domain.ontology.OntologyTerm;
import org.janelia.model.domain.ontology.Text;

/**
 * Context pop up menu for annotations.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AnnotationContextMenu extends PopupContextMenu {

    private ImageModel<DomainObject, Reference> imageModel;
    protected Annotation annotation;
    protected List<DomainObject> domainObjectList;
    protected boolean multiple;

    public AnnotationContextMenu(Annotation annotation, List<DomainObject> domainObjectList, ImageModel<DomainObject, Reference> imageModel) {
        this.imageModel = imageModel;
        this.annotation = annotation;
        this.domainObjectList = domainObjectList;
        this.multiple = domainObjectList.size() > 1;
    }
    
    public void addMenuItems() {
        
        if (domainObjectList.isEmpty()) {
            JMenuItem titleMenuItem = new JMenuItem("Nothing selected");
            titleMenuItem.setEnabled(false);
            add(titleMenuItem);
            return;
        }

        try {
            add(getTitleItem());
            add(getCopyNameToClipboardItem());
            add(getCopyIdToClipboardItem());
            add(getCopyAnnotationItem());
            add(getViewDetailsItem());
            addSeparator();
            add(getRemoveAnnotationItem());
            OntologyTerm keyTerm = DomainMgr.getDomainMgr().getModel().getOntologyTermByReference(annotation.getKeyTerm());
            if (keyTerm!=null) {
                add(getEditAnnotationItem(keyTerm));
            }
        }  
        catch (Exception ex) {
            ConsoleApp.handleException(ex);
        }
    }

    protected JMenuItem getTitleItem() {
        String name = multiple ? "(Multiple selected)" : annotation.getName();
        JMenuItem titleMenuItem = new JMenuItem(name);
        titleMenuItem.setEnabled(false);
        return titleMenuItem;
    }

    protected JMenuItem getCopyNameToClipboardItem() {
        if (multiple) return null;
        return getNamedActionItem(new CopyToClipboardAction("Name",annotation.getName()));
    }

    protected JMenuItem getCopyIdToClipboardItem() {
        if (multiple) return null;
        return getNamedActionItem(new CopyToClipboardAction("GUID",annotation.getId().toString()));
    }

    protected JMenuItem getCopyAnnotationItem() {
        if (multiple) return null;
        JMenuItem deleteByTermItem = new JMenuItem("  Copy Annotation");
        deleteByTermItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                ActivityLogHelper.logUserAction("AnnotationContextMenu.copyAnnotation", annotation);
                StateMgr.getStateMgr().setCurrentSelectedOntologyAnnotation(annotation);
            }
        });
        return deleteByTermItem;
    }

    protected JMenuItem getRemoveAnnotationItem() {
        if (!ClientDomainUtils.hasWriteAccess(annotation)) return null;
        final RemoveAnnotationsAction removeAction = new RemoveAnnotationsAction(imageModel, domainObjectList, annotation, true);
        return getNamedActionItem(removeAction);
    }

    protected JMenuItem getEditAnnotationItem(OntologyTerm keyTerm) {
        if (keyTerm==null) return null;
        if (keyTerm instanceof EnumText || keyTerm instanceof Text || keyTerm instanceof Accumulation || keyTerm instanceof Interval) {
            final BulkEditAnnotationKeyValueAction bulkEditAction = new BulkEditAnnotationKeyValueAction(domainObjectList, annotation);
            return getNamedActionItem(bulkEditAction);
        }
        return null;
    }

    protected JMenuItem getViewDetailsItem() {
        if (multiple) return null;
        JMenuItem detailsItem = new JMenuItem("  View Details");
        detailsItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                ActivityLogHelper.logUserAction("AnnotationContextMenu.viewDetails", annotation);
                new DomainDetailsDialog().showForDomainObject(annotation);
            }
        });
        return detailsItem;
    }
}
