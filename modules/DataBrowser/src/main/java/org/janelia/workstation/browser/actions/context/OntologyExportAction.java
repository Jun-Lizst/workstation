package org.janelia.workstation.browser.actions.context;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.janelia.model.domain.ontology.EnumText;
import org.janelia.model.domain.ontology.Interval;
import org.janelia.model.domain.ontology.OntologyTerm;
import org.janelia.workstation.common.actions.BaseContextualNodeAction;
import org.janelia.workstation.common.gui.support.YamlFileFilter;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * Builds an action to export the ontology from the selected node.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ActionID(
        category = "actions",
        id = "OntologyExportAction"
)
@ActionRegistration(
        displayName = "#CTL_OntologyExportAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions/Ontology", position = 602, separatorAfter = 605)
})
@NbBundle.Messages("CTL_OntologyExportAction=Export Ontology...")
public class OntologyExportAction extends BaseContextualNodeAction {

    private static final String SAVE_FILE_EXTENSION = "yaml";

    private OntologyTerm selectedTerm;

    @Override
    protected void processContext() {
        if (getNodeContext().isSingleObjectOfType(OntologyTerm.class)) {
            selectedTerm = getNodeContext().getSingleObjectOfType(OntologyTerm.class);
            setEnabledAndVisible(true);
        }
        else {
            selectedTerm = null;
            setEnabledAndVisible(false);
        }
    }

    @Override
    public void performAction() {
        exportOntology(selectedTerm);
    }

    private void exportOntology(OntologyTerm ontologyTerm) {

        ActivityLogHelper.logUserAction("OntologyExportAction.exportOntology");

        String defaultSaveFilename = ontologyTerm.getName().replaceAll("\\s+", "_") + "." + SAVE_FILE_EXTENSION;

        final JFileChooser fc = new JFileChooser();
        FileFilter ff = new YamlFileFilter();
        fc.addChoosableFileFilter(ff);
        fc.setFileFilter(ff);
        fc.setSelectedFile(new File(defaultSaveFilename));

        int returnVal = fc.showSaveDialog(FrameworkAccess.getMainFrame());
        if (returnVal != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Map<String,Object> object = convertOntologyToMap(ontologyTerm);

        try {
            FileWriter writer = new FileWriter(fc.getSelectedFile());
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            Yaml yaml = new Yaml(options);
            yaml.dump(object, writer);
        }
        catch (IOException e) {
            FrameworkAccess.handleException(e);
        }
    }

    private Map<String,Object> convertOntologyToMap(OntologyTerm ontologyTerm) {

        Map<String,Object> map = new HashMap<>();
        map.put("name", ontologyTerm.getName());

        String typeName = ontologyTerm.getTypeName();
        if (!typeName.equals("Tag")) {
            map.put("type", typeName);
        }

        if (ontologyTerm instanceof Interval) {
            Interval interval = (Interval)ontologyTerm;
            map.put("lowerBound",interval.getLowerBound().toString());
            map.put("upperBound",interval.getUpperBound().toString());
        }
        else if (ontologyTerm instanceof EnumText) {
            EnumText enumText = (EnumText)ontologyTerm;
            map.put("enumId",enumText.getValueEnumId().toString());
        }

        List<OntologyTerm> children = ontologyTerm.getTerms();
        if (children!=null) {
            List<Object> childList = new ArrayList<>();
            for (OntologyTerm child : children) {
                childList.add(convertOntologyToMap(child));
            }
            map.put("children", childList);
        }

        return map;
    }
}
