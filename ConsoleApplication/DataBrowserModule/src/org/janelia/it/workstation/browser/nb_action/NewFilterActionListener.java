package org.janelia.it.workstation.browser.nb_action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.janelia.it.jacs.model.domain.gui.search.Filter;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.DomainModel;
import org.janelia.it.workstation.browser.components.DomainExplorerTopComponent;
import org.janelia.it.workstation.browser.components.DomainListViewManager;
import org.janelia.it.workstation.browser.components.DomainListViewTopComponent;
import org.janelia.it.workstation.browser.components.ViewerUtils;
import org.janelia.it.workstation.browser.gui.editor.FilterEditorPanel;
import org.janelia.it.workstation.browser.nodes.NodeUtils;
import org.janelia.it.workstation.browser.nodes.TreeNodeNode;
import org.janelia.it.workstation.browser.workers.SimpleWorker;

public final class NewFilterActionListener implements ActionListener {

    private TreeNodeNode parentNode;

    public NewFilterActionListener() {
    }

    public NewFilterActionListener(TreeNodeNode parentNode) {
        this.parentNode = parentNode;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        ActivityLogHelper.logUserAction("NewFilterActionListener.actionPerformed");

        final DomainExplorerTopComponent explorer = DomainExplorerTopComponent.getInstance();
        final DomainModel model = DomainMgr.getDomainMgr().getModel();

        if (parentNode==null) {
            // If there is no parent node specified, we don't actually have to
            // save a new filter. Just open up the editor:
            DomainListViewTopComponent browser = initView();
            browser.loadDomainObject(FilterEditorPanel.createUnsavedFilter(null), true);
            return;
        }

        // Since we're putting the filter under a parent, we need the name up front
        final String name = (String) JOptionPane.showInputDialog(ConsoleApp.getMainFrame(),
                "Filter Name:\n", "Create new filter", JOptionPane.PLAIN_MESSAGE, null, null, null);
        if (StringUtils.isEmpty(name)) {
            return;
        }

        // Save the filter and select it in the explorer so that it opens
        SimpleWorker newFilterWorker = new SimpleWorker() {

            private Filter filter;

            @Override
            protected void doStuff() throws Exception {
                filter = FilterEditorPanel.createUnsavedFilter(name);
                filter = model.save(filter);
                TreeNode parentFolder = parentNode.getTreeNode();
                model.addChild(parentFolder, filter);
            }

            @Override
            protected void hadSuccess() {
                initView();
                final Long[] idPath = NodeUtils.createIdPath(parentNode, filter);
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        explorer.selectAndNavigateNodeByPath(idPath);
                    }
                });
            }

            @Override
            protected void hadError(Throwable error) {
                ConsoleApp.handleException(error);
            }
        };

        newFilterWorker.execute();
    }

    private DomainListViewTopComponent initView() {
        final DomainListViewTopComponent browser = ViewerUtils.createNewViewer(DomainListViewManager.getInstance(), "editor");
        browser.setEditorClass(FilterEditorPanel.class);
        browser.requestActive();
        return browser;
    }
}
