package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action;

import java.awt.Point;
import java.awt.event.MouseEvent;

import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Rotation;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;

// Click and drag mouse to drag the image in X and Y
public class PanMode
extends BasicMouseMode 
{
    private BoundingBox3d boundingBox;
    
    public PanMode() {
		setHoverCursor(BasicMouseMode.createCursor("grab_opened.png", 8, 8));
		setDragCursor(BasicMouseMode.createCursor("grab_closed.png", 8, 8));
	}
	
    @Override
	public void mouseDragged(MouseEvent event) {
		super.mouseDragged(event);
		if (getPreviousPoint() == null)
			return;
		if (getPoint() == null)
			return;
		Point p1 = getPreviousPoint();
		Point p2 = getPoint();
		// Point dx = new Point(p2.x - p1.x, p2.y - p1.y);
		Vec3 dx = new Vec3(p2.x - p1.x, p2.y - p1.y, 0.0);
		dx = getViewerInGround().times(dx);
		if (getCamera() != null) {
		    Vec3 oldFocus = getCamera().getFocus();
		    // How much to move camera focus?
		    Vec3 dFocus = new Vec3(-dx.x(), -dx.y(), -dx.z());
		    // Convert from pixels to scene units
		    dFocus = dFocus.times(1.0/getCamera().getPixelsPerSceneUnit());
		    // Nudge focus
		    Vec3 newFocus = oldFocus.plus(dFocus);
		    // Restrict to bounding box
		    if (boundingBox != null) {
        		    for (int i = 0; i < 3; ++i) {
        		        double x = newFocus.get(i);
        		        double min = boundingBox.getMin().get(i);
        		        double max = boundingBox.getMax().get(i);
        		        if (x < min)
        		            newFocus.set(i, min);
        		        if (x > max)
        		            newFocus.set(i, max);
        		    }
		    }
		    getCamera().setFocus(newFocus);
		}
	}

    public BoundingBox3d getBoundingBox() {
        return boundingBox;
    }

    public void setBoundingBox(BoundingBox3d boundingBox) {
        this.boundingBox = boundingBox;
    }
}
