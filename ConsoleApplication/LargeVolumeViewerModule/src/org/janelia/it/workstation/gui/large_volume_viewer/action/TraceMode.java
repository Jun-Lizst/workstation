package org.janelia.it.workstation.gui.large_volume_viewer.action;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;

import org.janelia.console.viewerapi.model.NeuronSet;
import org.janelia.console.viewerapi.model.NeuronVertex;
import org.janelia.it.jacs.model.domain.tiledMicroscope.AnnotationNavigationDirection;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmGeoAnnotation;
import org.janelia.it.jacs.shared.geom.Vec3;
import org.janelia.it.jacs.shared.viewer3d.BoundingBox3d;
import org.janelia.it.workstation.gui.large_volume_viewer.MenuItemGenerator;
import org.janelia.it.workstation.gui.large_volume_viewer.MouseModalWidget;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.SkeletonController;
import org.janelia.it.workstation.gui.large_volume_viewer.neuron_api.NeuronVertexAdapter;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.Anchor;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.Skeleton;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.SkeletonActor;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.SkeletonActorModel;
import org.janelia.it.workstation.gui.viewer3d.interfaces.Viewport;
import org.perf4j.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TraceMode extends BasicMouseMode 
implements MouseMode, KeyListener
{
    private Logger logger = LoggerFactory.getLogger(TraceMode.class);

    // Radius of an anchor
    private static final int pixelRadius = 5;
    
	private Skeleton skeleton;
	private SkeletonActor skeletonActor;
    private SkeletonController controller = SkeletonController.getInstance();
	private Anchor hoverAnchor = null;
	private Vec3 popupXyz = null;
	// Sometimes users move an anchor with the mouse
	private Anchor dragAnchor = null;
	private Point dragStart = null;
	private Cursor grabHandCursor = BasicMouseMode.createCursor("grab_closed.png", 7, 7);
	private Cursor penCursor = BasicMouseMode.createCursor("nib.png", 7, 0);
	private Cursor crossCursor = BasicMouseMode.createCursor("crosshair.png", 7, 7);
	private Cursor penPlusCursor = BasicMouseMode.createCursor("nib_plus.png", 7, 0);
	private Point pressPoint;
	private Viewport viewport;
	private BoundingBox3d boundingBox;
	// private Anchor nextParent = null;
	private boolean autoFocusNextAnchor = false;
	private static long time1;
	public static void startTimer() { time1=new Date().getTime();}
	public static String getTimerMs() { return new Long(new Date().getTime() - time1).toString(); }
	
	public TraceMode(Skeleton skeleton) {
		this.skeleton = skeleton;
		setHoverCursor(penCursor);
		setDragCursor(crossCursor);
		// Center on new anchors, and mark them with a "P"
		skeleton.skeletonChanged();
	}

	@Override 
	public String getToolTipText() {
		// But tooltip during tracing is annoying
		return null;
		/*
		// Use html to get multiline tooltip
		return  "<html>"
				+"SHIFT-click to place a new anchor<br>" // done
				+"Click and drag to move anchor<br>" // done
				+"Click to designate parent anchor<br>" // done
				+"Middle-button drag to Pan XY<br>" // done
				+"Scroll wheel to scan Z<br>" // done
				+"SHIFT-scroll wheel to zoom<br>" // done
				+"Double-click to recenter on a point<br>"
				+"Right-click for context menu" // done
				+"</html>";
				*/
	}
	
	@Override
	public void mouseClicked(MouseEvent event) {
        super.mouseClicked(event);
        // Java swing mouseClicked() requires zero motion and is therefore stupid.
        // onMouseActuallyClicked(event);
        //
        // But this will have to do for double clicking...
        // Double click to center; on anchor or on slice point
        if (event.getClickCount() == 2) {
            if (hoverAnchor != null) {
                camera.setFocus(hoverAnchor.getLocation());
                skeleton.getHistory().push(hoverAnchor);
            } else {
                // center on slice point
                camera.setFocus(worldFromPixel(event.getPoint()));
            }
        }
    }
	
	private void appendAnchor(Vec3 xyz) {
		autoFocusNextAnchor = true; // center on new position
		skeleton.addAnchorAtXyz(xyz, skeletonActor.getModel().getNextParent());
	}
	
	private void seedAnchor(Vec3 xyz) {
		autoFocusNextAnchor = true; // center on new position
		skeleton.addAnchorAtXyz(xyz, null);
	}
	
	private void onMouseActuallyClicked(MouseEvent event) {
		// Only want left/near clicks with SHIFT down
		// BUTTON1 is near-click for both left and right handed mice (right?)
		if ((event.getButton() == MouseEvent.BUTTON1) && event.isShiftDown()) {
			// Place new anchor
			Vec3 xyz = worldFromPixel(event.getPoint());
			// System.out.println("Trace click "+xyz);
            
            // TODO - nudge location to voxel center
            TraceMode.startTimer();
			appendAnchor(xyz);
		}
		else if (event.getButton() == MouseEvent.BUTTON1) {
			if (hoverAnchor != null) {
				controller.setNextParent(hoverAnchor);
			}
		}
	}
	
	@Override
	public void mouseDragged(MouseEvent event) {
		super.mouseDragged(event);
		// We might be moving an anchor
		if (dragAnchor != null) {
			Vec3 loc = worldFromPixel(event.getPoint());
			skeletonActor.getModel().lightweightPlaceAnchor(dragAnchor, loc);
		}
		// Middle button drag to pan
		if ((event.getModifiers() & InputEvent.BUTTON2_MASK) != 0) {
			// TODO reuse pan code
			checkCursor(grabHandCursor);
			Point p1 = getPreviousPoint();
			Point p2 = getPoint();
			Vec3 dx = new Vec3(p1.x - p2.x, p1.y - p2.y, 0);
			dx = viewerInGround.times(dx);
			getCamera().incrementFocusPixels(dx);
			if (boundingBox != null)
				getCamera().setFocus(boundingBox.clip(getCamera().getFocus()));
		}
	}
    
	// Amplify size of anchor when hovered
	@Override
	public void mouseMoved(MouseEvent event) {
		super.mouseMoved(event);
		final Vec3 xyz = worldFromPixel(event.getPoint());
        SkeletonActorModel skeletonActorModel = skeletonActor.getModel();

        final double worldRadius = pixelRadius / camera.getPixelsPerSceneUnit();
        // Find smallest squared distance
        final double cutoff = worldRadius * worldRadius;
        
        StopWatch stopwatch = new StopWatch();
        stopwatch.start();
        
        Anchor spatial = null;
        NeuronSet neuronSet = controller.getNeuronSet();
        if (neuronSet != null) {
                        
            List<NeuronVertex> vertexList = neuronSet.getAnchorClosestToMicronLocation(new double[]{xyz.x(), xyz.y(), xyz.z()}, 20);
            if (vertexList != null) {
                logger.trace("Got {} closest neurons for mouse position {}", vertexList.size(), xyz);
                List<Anchor> anchors = new ArrayList<>();
                for (NeuronVertex vertex : vertexList) {
                    TmGeoAnnotation annotation = ((NeuronVertexAdapter) vertex).getTmGeoAnnotation();
                    Anchor anchor = skeleton.getAnchorByID(annotation.getId());
                    if (anchor!=null) {
                        anchors.add(anchor);
                    }
                } 
                spatial = findBestAnchor(anchors, xyz, cutoff);
            }
        }

        stopwatch.stop();
        
        if (spatial != null) {
            logger.trace("Found closest anchor in spatial index: {} (elapsed = {} ms)", spatial.getGuid(), stopwatch.getElapsedTime());
        }

        Anchor closest = spatial;
        
        // closest == null means you're not on an anchor anymore
		if (skeletonActor != null && closest != hoverAnchor) {
			// test for closest == null because null will come back invisible,
			//	and we need hover-->null to unhover
			if (closest == null || skeletonActorModel.anchorIsVisible(closest)){
				hoverAnchor = closest;
				skeletonActor.getModel().setHoverAnchor(hoverAnchor);
			}
		}

		checkShiftPlusCursor(event);
	}
	
	private Anchor findBestAnchor(Collection<Anchor> anchors, Vec3 xyz, double cutoff) {
	    
	    SkeletonActorModel skeletonActorModel = skeletonActor.getModel();
        double minDist2 = 10 * cutoff; // start too big
        Anchor best = null;
        
        for (Anchor anchor : anchors) {
            //Vec3 du2 = xyz.minus(anchor.getLocation());
            //double dist = Math.sqrt(du2.dot(du2));
            //logger.trace("   Inspecting {} at {} (dist={})", anchor.getGuid(), anchor.getLocation(), dist);
            
            // we don't interact with invisible anchors, and since hovering
            //  is the key to all interactions, we can elegantly prevent that
            // interaction here
            if (!skeletonActorModel.anchorIsVisible(anchor)) {
                continue;
            }
            double dz = Math.abs(2.0 * (xyz.getZ() - anchor.getLocation().getZ()) * camera.getPixelsPerSceneUnit());
            if (dz >= 0.95 * viewport.getDepth()) {
                continue; // outside of Z (most of) range
            }
            // Use X/Y (not Z) for distance comparison
            Vec3 dv = xyz.minus(anchor.getLocation());
            dv = viewerInGround.inverse().times(dv); // rotate into screen space
            double dx = dv.getX();
            double dy = dv.getY();
            double d2 = dx*dx + dy*dy;
            if (d2 > cutoff) {
                continue;
            }
            if (d2 >= minDist2) {
                continue;
            }
            minDist2 = d2;
            best = anchor;
        }
        
        return best;
	}

	@Override
	public void mousePressed(MouseEvent event) {
		super.mousePressed(event);
		// Might start dragging an anchor
		if (event.getButton() == MouseEvent.BUTTON1) 
		{
			// start dragging anchor position
			if (hoverAnchor == null) {
				dragAnchor = null;
				dragStart = null;
			}
			else {
				dragAnchor = hoverAnchor;
				dragStart = event.getPoint();
			}
		}
		// middle button to Pan
		else if (event.getButton() == MouseEvent.BUTTON2) {
			checkCursor(grabHandCursor);
		}
		pressPoint = event.getPoint();		
	}
	
	@Override
	public void mouseReleased(MouseEvent event) {
		super.mouseReleased(event);
		// Might stop dragging an anchor
		if (event.getButton() == MouseEvent.BUTTON1) {
			if (dragAnchor != null) {
				Point finalPos = event.getPoint();
				if (dragStart.distance(event.getPoint()) > 2.0) {
					Vec3 location = worldFromPixel(finalPos);
					Vec3 oldLoc = dragAnchor.getLocation();
					Vec3 dLoc = location.minus(oldLoc);
					// relaxed restriction on move; used to disallow movement in z to
                    //  prevent changing plane of annotations dragged while not actually
                    //  on their plane (since they are visible and draggable in nearby
                    //  planes)
					// Vec3 viewPlane = viewerInGround.inverse().times(new Vec3(1,1,0));
					Vec3 viewPlane = viewerInGround.inverse().times(new Vec3(1,1,1));
					for (int i = 0; i < 3; ++i) {
						dLoc.set(i, dLoc.get(i) * viewPlane.get(i));
					}
					Vec3 newLoc = oldLoc.plus(dLoc);
					dragAnchor.setLocation(newLoc);
					skeleton.getHistory().push(dragAnchor);
				}
			}
			dragAnchor = null;
			dragStart = null;
		}
        // Check for click because Swing mouseClicked() event is stupid.
        if (pressPoint != null) {
            double clickDistance = event.getPoint().distance(pressPoint);
            if (clickDistance < 3) {
                onMouseActuallyClicked(event);
            }
        }
	}
	
	public void setActor(SkeletonActor actor) {
		this.skeletonActor = actor;
	}

	public SkeletonActor getActor() {
		return skeletonActor;
	}
	
    public BoundingBox3d getBoundingBox() {
		return boundingBox;
	}

	public void setBoundingBox(BoundingBox3d boundingBox) {
		this.boundingBox = boundingBox;
	}

	private Anchor getHoverAnchor() {
        return hoverAnchor;
	}
	
	@Override
    public MenuItemGenerator getMenuItemGenerator() {
        return new MenuItemGenerator() {
            @SuppressWarnings("serial")
			@Override
            public List<JMenuItem> getMenus(MouseEvent event) 
            {
            	    List<JMenuItem> result = new Vector<JMenuItem>();
                    popupXyz = worldFromPixel(event.getPoint());
                    // Cancel 
                    ///// Popup menu items that require an anchor under the mouse /////
                    Anchor hover = getHoverAnchor();
                    Anchor parent = skeletonActor.getModel().getNextParent();
                    result.add(null); // separator
                    if (hover == null) { // No anchor under mouse? Maybe user wants to create a new anchor.
                        ///// Popup menu items that do not require an anchor under the mouse /////
	                    if (parent == null) {
	                        // Start new tree
	                        AbstractAction beginNeuriteHereAction = new AbstractAction("Begin new neurite here [Shift-click]") {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    seedAnchor(popupXyz);
                                }
                            };
                            beginNeuriteHereAction.setEnabled(controller.editsAllowed());
	                        result.add(new JMenuItem(beginNeuriteHereAction));
	                    }
	                    else {
	                        // Add branch to tree
	                        AbstractAction appendNewAnchorAction = new AbstractAction("Append new anchor here [Shift-click]") {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    appendAnchor(popupXyz);
                                }
                            };
                            appendNewAnchorAction.setEnabled(controller.editsAllowed());
	                        result.add(new JMenuItem(appendNewAnchorAction));                 
	                        // Start new tree
	                        AbstractAction beginNeuriteHereAction = new AbstractAction("Begin new neurite here") {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    seedAnchor(popupXyz);
                                }
                            };
                            beginNeuriteHereAction.setEnabled(controller.editsAllowed());
	                        result.add(new JMenuItem(beginNeuriteHereAction));
	                    }
                    }
                    else { // There is an anchor under the mouse
                        // Center
                        result.add(new JMenuItem(new AbstractAction("Center on this anchor") {
                            private static final long serialVersionUID = 1L;
                            @Override
                            public void actionPerformed(ActionEvent e) {
                            	Anchor h = getHoverAnchor();
                            	if (h == null)
                            		return;
                            	skeleton.getHistory().push(h);
                                camera.setFocus(h.getLocation());
                            }
                        }));
                        // Trace connection to parent
                        final boolean showTraceMenu = true; // just for initial debugging?
                        if (showTraceMenu && (hover.getNeighbors().size() > 0)) {
                            AbstractAction tracePathToParentAction = new AbstractAction("Trace path to parent") {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    Anchor h = getHoverAnchor();
                                    skeleton.traceAnchorConnection(h);
                                }
                            };
                            tracePathToParentAction.setEnabled(controller.editsAllowed());
                            result.add(new JMenuItem(tracePathToParentAction));
                        }
                        // Make Parent
                        if (hover != parent) {
                            AbstractAction makeParentAction = new AbstractAction("Designate this anchor as parent [left click]") {
                                private static final long serialVersionUID = 1L;
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    controller.setNextParent(getHoverAnchor());
                                }
                            };
                            makeParentAction.setEnabled(controller.editsAllowed());
	                        result.add(new JMenuItem(makeParentAction));
                        }
                        /*
                        // this is not something we support right now; it'll draw right, but
                        //  we don't have a way to connect neurites yet
                        // Connect to current parent
                        if (parent != null) 
                        {
                        	if (parent != hover) {
	                            result.add(new JMenuItem(new AbstractAction("Connect parent anchor to this anchor") {
	                                @Override
	                                public void actionPerformed(ActionEvent e) {
	                                    skeleton.connect(getHoverAnchor(), skeletonActor.getNextParent());
	                                }
	                            }));                     
                        	}
                        }
                        */
                        // Delete
                        
                        AbstractAction deleteSubtreeAction = new AbstractAction("Delete subtree rooted at this anchor") {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                if (getHoverAnchor() != null) {
                                    skeleton.deleteSubtreeRequest(getHoverAnchor());
                                }
                                else {
                                    logger.warn("Null hover anchor when attempting delete subtree.");
                                }
                            }
                        };
                        deleteSubtreeAction.setEnabled(controller.editsAllowed());
                        result.add(new JMenuItem(deleteSubtreeAction));
                        
                        AbstractAction deleteLinkAction = new AbstractAction("Delete link") {
                            @Override
                            public void actionPerformed(ActionEvent actionEvent) {
                                if (getHoverAnchor() != null) {
                                    skeleton.deleteLinkRequest(getHoverAnchor());
                                }
                                else {
                                    logger.warn("Null hover anchor when attempting delete link.");
                                }
                            }
                        };
                        deleteLinkAction.setEnabled(controller.editsAllowed());
                        result.add(new JMenuItem(deleteLinkAction));
                        
                        AbstractAction transferNeuriteAction = new AbstractAction("Transfer neurite...") {
                            @Override
                            public void actionPerformed(ActionEvent actionEvent) {
                                if (getHoverAnchor() != null) {
                                    // used to be called "Move neurite", and that's
                                    //  still the internal name for the command
                                    skeleton.moveNeuriteRequest(getHoverAnchor());
                                }
                                else {
                                    logger.warn("Null hover anchor when attempting move neurite.");
                                }
                            }
                        };
                        transferNeuriteAction.setEnabled(controller.editsAllowed());
                        result.add(new JMenuItem(transferNeuriteAction));
                        
                        AbstractAction splitAnchorAction = new AbstractAction("Split anchor") {
                            @Override
                            public void actionPerformed(ActionEvent actionEvent) {
                                if (getHoverAnchor() != null) {
                                    skeleton.splitAnchorRequest(getHoverAnchor());
                                }
                                else {
                                    logger.warn("Null hover anchor when attempting split anchor.");
                                }
                            }
                        };
                        splitAnchorAction.setEnabled(controller.editsAllowed());
                        result.add(new JMenuItem(splitAnchorAction));
                        
                        AbstractAction splitNeuriteAction = new AbstractAction("Split neurite") {
                            @Override
                            public void actionPerformed(ActionEvent actionEvent) {
                                if (getHoverAnchor() != null) {
                                    skeleton.splitNeuriteRequest(getHoverAnchor());
                                }
                                else {
                                    logger.warn("Null hover anchor when attempting split neurite.");
                                }
                            }
                        };
                        splitNeuriteAction.setEnabled(controller.editsAllowed());
                        result.add(new JMenuItem(splitNeuriteAction));
                        
                        AbstractAction setAnchorAsRootAction = new AbstractAction("Set anchor as root") {
                            @Override
                            public void actionPerformed(ActionEvent actionEvent) {
                                if (getHoverAnchor() != null) {
                                    skeleton.rerootNeuriteRequest(getHoverAnchor());
                                }
                                else {
                                    logger.warn("Null hover anchor when attempting set anchor as root.");
                                }
                            }
                        };
                        setAnchorAsRootAction.setEnabled(controller.editsAllowed());
                        result.add(new JMenuItem(setAnchorAsRootAction));
                        
                        result.add(null); // separator
                        
                        AbstractAction addNoteAction = new AbstractAction("Add, edit, delete note...") {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                if (getHoverAnchor() != null) {
                                    skeleton.addEditNoteRequest(getHoverAnchor());
                                }
                                else {
                                    logger.warn("Null hover anchor when attempting note change.");
                                }
                            }
                        };
                        addNoteAction.setEnabled(controller.editsAllowed());
                        result.add(new JMenuItem(addNoteAction));
                        
                        AbstractAction editNeuronTagsAction = new AbstractAction("Edit neuron tags...") {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                if (getHoverAnchor() != null) {
                                    skeleton.editNeuronTagRequest(getHoverAnchor());
                                }
                                else {
                                    logger.warn("Null hover anchor when attempting edit tags.");
                                }
                            }
                        };
                        editNeuronTagsAction.setEnabled(controller.editsAllowed());
                        result.add(new JMenuItem(editNeuronTagsAction));
                        
                        result.add(null); // separator
                        
                        AbstractAction changeNeuronStyleAction = new AbstractAction("Change neuron color...") {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                if (getHoverAnchor() != null) {
                                    skeleton.changeNeuronStyle(getHoverAnchor());
                                }
                                else {
                                    logger.warn("Null hover anchor when attempting neuron style change.");
                                }
                            }
                        };
                        changeNeuronStyleAction.setEnabled(controller.editsAllowed());
						result.add(new JMenuItem(changeNeuronStyleAction));
						
						AbstractAction hideNeuronAction = new AbstractAction("Hide neuron") {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                if (getHoverAnchor() != null) {
                                    skeleton.setNeuronVisitility(getHoverAnchor(), false);
                                }
                                else {
                                    logger.warn("Null hover anchor when attempting hide neuron.");
                                }
                            }
                        };
                        hideNeuronAction.setEnabled(controller.editsAllowed());
						result.add(new JMenuItem(hideNeuronAction));
						
                        result.add(null); // separator
                    }
                    if (parent != null) {
                    	if (parent != hover) {
                            result.add(new JMenuItem(new AbstractAction("Center on current parent anchor") {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    camera.setFocus(skeletonActor.getModel().getNextParent().getLocation());
                                }
                            }));                    		
                    	}
                        result.add(new JMenuItem(new AbstractAction("Clear current parent anchor") {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                controller.setNextParent((Long)null);
                            }
                        }));                     
                    }
            	    return result;
            	}
        };
    }
	
	@Override
	public void setWidget(MouseModalWidget widget, boolean updateCursor) {
		super.setWidget(widget, updateCursor);
	}

	public Viewport getViewport() {
		return viewport;
	}

	public void setViewport(Viewport viewport) {
		this.viewport = viewport;
	}

	@Override
	public void keyPressed(KeyEvent event) {
		checkShiftPlusCursor(event);
		int keyCode = event.getKeyCode();
		Anchor historyAnchor = null;
		Anchor nextParent = skeletonActor.getModel().getNextParent();
		switch(keyCode) {
		case KeyEvent.VK_BACK_SPACE:
		case KeyEvent.VK_DELETE:
			if (nextParent != null) {
                skeleton.deleteLinkRequest(nextParent);
			}
			break;
		/*
		// disabling history nav for now
		case KeyEvent.VK_LEFT:
			// System.out.println("back");
			historyAnchor = skeleton.getHistory().back();
			break;
		case KeyEvent.VK_RIGHT:
			// System.out.println("next");
			historyAnchor = skeleton.getHistory().next();
			break;
		*/
		case KeyEvent.VK_LEFT:
			if (nextParent != null) {
				if (event.isAltDown()) {
				controller.navigationRelative(nextParent.getNeuronID(), nextParent.getGuid(),
						AnnotationNavigationDirection.ROOTWARD_STEP);
				} else {
					controller.navigationRelative(nextParent.getNeuronID(), nextParent.getGuid(),
							AnnotationNavigationDirection.ROOTWARD_JUMP);
				}
			}
			break;
		case KeyEvent.VK_RIGHT:
			if (nextParent != null) {
				if (event.isAltDown()) {
					controller.navigationRelative(nextParent.getNeuronID(), nextParent.getGuid(),
							AnnotationNavigationDirection.ENDWARD_STEP);
				} else {
					controller.navigationRelative(nextParent.getNeuronID(), nextParent.getGuid(),
							AnnotationNavigationDirection.ENDWARD_JUMP);
				}
			}
			break;
		case KeyEvent.VK_UP:
			if (nextParent != null) {
				controller.navigationRelative(nextParent.getNeuronID(), nextParent.getGuid(),
						AnnotationNavigationDirection.PREV_PARALLEL);
			}
			break;
		case KeyEvent.VK_DOWN:
			if (nextParent != null) {
				controller.navigationRelative(nextParent.getNeuronID(), nextParent.getGuid(),
						AnnotationNavigationDirection.NEXT_PARALLEL);
			}
			break;
		case KeyEvent.VK_A:
			// add/edit note dialog
			if (nextParent != null) {
				skeleton.addEditNoteRequest(nextParent);
			}
			break;
		}
		if (historyAnchor != null)
			camera.setFocus(historyAnchor.getLocation());
	}

	private void checkShiftPlusCursor(InputEvent event) {
		if (hoverAnchor != null) // no adding points while hovering over another point
			checkCursor(penCursor);
		else if (event.isShiftDown()) // display addable cursor
			checkCursor(penPlusCursor);
		else
			checkCursor(penCursor); 
	}
	
}