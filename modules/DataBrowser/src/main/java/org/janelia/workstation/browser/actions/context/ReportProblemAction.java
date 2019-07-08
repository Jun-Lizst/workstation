package org.janelia.workstation.browser.actions.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JMenuItem;

import org.janelia.it.jacs.shared.utils.domain.DataReporter;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.ontology.Annotation;
import org.janelia.model.domain.ontology.OntologyTerm;
import org.janelia.model.domain.sample.Sample;
import org.janelia.workstation.common.actions.BaseContextualPopupAction;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.core.api.StateMgr;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.janelia.workstation.core.workers.SimpleListenableFuture;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ActionID(
        category = "Actions",
        id = "ReportProblemAction"
)
@ActionRegistration(
        displayName = "#CTL_ReportProblemAction",
        lazy = false
)
@ActionReferences({
        @ActionReference(path = "Menu/Actions/Sample", position = 500, separatorBefore = 499)
})
@NbBundle.Messages("CTL_ReportProblemAction=Report A Problem With This Data")
public class ReportProblemAction extends BaseContextualPopupAction {

    private static final Logger log = LoggerFactory.getLogger(ReportProblemAction.class);

    private Sample selectedObject;

    @Override
    protected void processContext() {
        if (getNodeContext().isSingleObjectOfType(Sample.class)) {
            this.selectedObject = getNodeContext().getSingleObjectOfType(Sample.class);
            setEnabledAndVisible(true);
        }
        else {
            setEnabledAndVisible(false);
        }
    }

    @Override
    protected List<JComponent> getItems() {
        List<JComponent> items = new ArrayList<>();

        DomainObject domainObject = selectedObject;

        OntologyTerm errorOntology = StateMgr.getStateMgr().getErrorOntology();
        if (errorOntology==null) return items;

        for (final OntologyTerm term : errorOntology.getTerms()) {
            JMenuItem item = new JMenuItem(term.getName());
            item.addActionListener(e -> {

                ActivityLogHelper.logUserAction("DomainObjectContentMenu.reportAProblemWithThisData", domainObject);

                final ApplyAnnotationAction action = ApplyAnnotationAction.get();
                SimpleListenableFuture<List<Annotation>> future =
                        action.annotateReferences(term, Collections.singletonList(Reference.createFor(domainObject)));

                if (future!=null) {
                    future.addListener(() -> {
                        try {
                            List<Annotation> annotations = future.get();
                            if (annotations!=null && !annotations.isEmpty()) {
                                reportData(domainObject, annotations.get(0));
                            }
                        }
                        catch (Exception ex) {
                            FrameworkAccess.handleException(ex);
                        }
                    });
                }
            });
            items.add(item);

        }

        return items;
    }

    private void reportData(DomainObject domainObject, Annotation annotation) {

        String fromEmail = ConsoleProperties.getString("console.FromEmail", null);
        if (fromEmail==null) {
            log.error("Cannot send exception report: no value for console.FromEmail is configured.");
            return;
        }

        String toEmail = ConsoleProperties.getString("console.HelpEmail", null);
        if (toEmail==null) {
            log.error("Cannot send exception report: no value for console.HelpEmail is configured.");
            return;
        }

        String webstationUrl = ConsoleProperties.getString("webstation.url", null);

        DataReporter reporter = new DataReporter(fromEmail, toEmail, webstationUrl);
        reporter.reportData(domainObject, annotation.getName());
    }
}