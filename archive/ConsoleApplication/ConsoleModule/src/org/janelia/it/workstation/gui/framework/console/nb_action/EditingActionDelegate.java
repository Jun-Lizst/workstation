package org.janelia.it.workstation.gui.framework.console.nb_action;

import javax.swing.JFrame;
import org.janelia.it.workstation.gui.framework.pref_controller.PrefController;
import org.janelia.it.workstation.gui.util.WindowLocator;

/**
 *
 * @author fosterl
 */
public class EditingActionDelegate {
    public void establishPrefController(String prefLevel) {
        JFrame parent = (JFrame) WindowLocator.getMainFrame();
        parent.repaint();  // Derived from original, proprietary impl.
        PrefController.getPrefController().getPrefInterface(prefLevel, parent);
    }

}