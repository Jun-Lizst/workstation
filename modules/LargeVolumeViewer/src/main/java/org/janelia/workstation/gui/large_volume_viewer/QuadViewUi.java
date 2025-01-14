package org.janelia.workstation.gui.large_volume_viewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.media.opengl.GLProfile;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.janelia.model.domain.tiledMicroscope.*;
import org.janelia.workstation.controller.widgets.ToolButton;
import org.janelia.workstation.controller.color_slider.SliderPanel;
import org.janelia.workstation.controller.listener.ColorModelInitListener;
import org.janelia.workstation.controller.model.color.ImageColorModel;
import org.janelia.workstation.controller.eventbus.LoadNeuronsEvent;
import org.janelia.workstation.controller.model.TmSelectionState;
import org.janelia.workstation.geom.CoordinateAxis;
import org.janelia.workstation.geom.Vec3;
import org.janelia.workstation.geom.BoundingBox3d;
import org.janelia.rendering.RenderedVolumeLoader;
import org.janelia.rendering.RenderedVolumeLoaderImpl;
import org.janelia.rendering.RenderedVolumeLocation;
import org.janelia.workstation.common.gui.dialogs.MemoryCheckDialog;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.controller.NeuronManager;
import org.janelia.workstation.controller.access.ModelTranslation;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.controller.tileimagery.*;
import org.janelia.workstation.gui.large_volume_viewer.action.*;
import org.janelia.workstation.gui.large_volume_viewer.controller.AnnotationManager;
import org.janelia.workstation.gui.large_volume_viewer.camera.BasicObservableCamera3d;
import org.janelia.workstation.gui.large_volume_viewer.listener.CameraListener;
import org.janelia.workstation.gui.large_volume_viewer.listener.PathTraceRequestListener;
import org.janelia.workstation.gui.large_volume_viewer.controller.QuadViewController;
import org.janelia.workstation.gui.large_volume_viewer.skeleton.SkeletonController;
import org.janelia.workstation.controller.listener.VolumeLoadListener;
import org.janelia.workstation.gui.large_volume_viewer.listener.WorkspaceClosureListener;
import org.janelia.workstation.gui.large_volume_viewer.options.ApplicationPanel;
import org.janelia.workstation.gui.large_volume_viewer.skeleton.Anchor;
import org.janelia.workstation.gui.large_volume_viewer.skeleton.Skeleton;
import org.janelia.workstation.gui.large_volume_viewer.skeleton.SkeletonActor;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.janelia.workstation.gui.large_volume_viewer.tracing.PathTraceToParentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.janelia.workstation.gui.large_volume_viewer.LargeVolumeViewerTopComponent.LVV_PREFERRED_ID;

/**
 * Main window for QuadView application. Maintained using Google WindowBuilder
 * design tool.
 *
 * @author Christopher M. Bruns
 */
@SuppressWarnings("serial")
public class QuadViewUi extends JPanel implements VolumeLoadListener {

    private static final Logger LOG = LoggerFactory.getLogger(QuadViewUi.class);

    private static final String IMAGES_FOLDER_OPEN = "folder_open.png";
    private static final String IMAGES_GREEN_CHECK = "Green_check.png";
    private static final String IMAGES_SPINNER = "spinner.gif";
    private static final String IMAGES_MOUSE_SCROLL = "mouse_scroll.png";
    private static final String IMAGES_MOUSE_LEFT = "mouse_left.png";

    private static final int MINIMUM_MEMORY_REQUIRED_GB = 7;

    private static GLProfile glProfile = GLProfile.get(GLProfile.GL2);

    // One shared camera for all viewers.
    // (there's only one viewer now actually, but you know...)
    private BasicObservableCamera3d camera = new BasicObservableCamera3d();
    private GLContextSharer orthoViewContextSharer = new GLContextSharer(glProfile);

    private LargeVolumeViewer largeVolumeViewer = new LargeVolumeViewer(
            orthoViewContextSharer.getCapabilities(),
            orthoViewContextSharer.getChooser(),
            orthoViewContextSharer.getContext(),
            camera);

    private TileServer tileServer = largeVolumeViewer.getTileServer();
    private SharedVolumeImage volumeImage = TmModelManager.getInstance().getTileServer().getSharedVolumeImage();
    private ImageColorModel imageColorModel = new ImageColorModel(volumeImage.getMaximumIntensity(), volumeImage.getNumberOfChannels());

    // Four quadrants for orthogonal views
    private OrthogonalPanel nwViewer = new OrthogonalPanel(CoordinateAxis.Z, orthoViewContextSharer);

    private JPanel zViewerPanel = new JPanel();
    private JPanel viewerPanel = new JPanel();

    // we never finished the multi-panel orthogonal view, and it's not
    //  on the agenda now; so in this list, only leave the one we use
    // at some point we should disentangle and remove all the unused viewers
    List<TileConsumer> allSliceViewers = Arrays.asList(new TileConsumer[]{
        nwViewer.getViewer()
    });

    private boolean modifierKeyPressed = false;
    private JPanel zScanPanel = new JPanel();
    private JSlider zScanSlider = new JSlider();
    private JSpinner zScanSpinner = new JSpinner();
    private SpinnerCalculationValue spinnerValue = new SpinnerCalculationValue(zScanSpinner);
    private JSlider zoomSlider = new JSlider(SwingConstants.VERTICAL, 0, 1000, 500);

    private JMenuBar menuBar = new JMenuBar();
    private JPanel toolBarPanel = new JPanel();
    private JSplitPane splitPane = new JSplitPane();
    private SliderPanel sliderPanel = new SliderPanel(imageColorModel, SliderPanel.ModelType.COLORMODEL_2D);
    private JLabel statusLabel = new JLabel("status area");
    private LoadStatusLabel loadStatusLabel = new LoadStatusLabel();

    private ZScanMode zScanMode = new ZScanMode(volumeImage);

    // annotation things
    private final NeuronManager annotationModel;
    private final AnnotationManager annotationMgr;

    // actions
    private final OpenFolderAction openFolderAction = new OpenFolderAction(largeVolumeViewer.getComponent(), this);
    private RecentFileList recentFileList = new RecentFileList(new JMenu("Open Recent"));
    private final ResetViewAction resetViewAction = new ResetViewAction(allSliceViewers, volumeImage);
    private final ResetColorsAction resetColorsAction = new ResetColorsAction(imageColorModel);
    private final RefreshSharedUpdatesAction refreshSharedUpdatesAction = new RefreshSharedUpdatesAction();
    // mode actions (and groups)
    private final ZoomMouseModeAction zoomMouseModeAction = new ZoomMouseModeAction();
    private final PanModeAction panModeAction = new PanModeAction();
    private Skeleton skeleton = new Skeleton();
    private final TraceMouseModeAction traceMouseModeAction = new TraceMouseModeAction();
    //
    private final ButtonGroup mouseModeGroup = new ButtonGroup();
    private final ZScanScrollModeAction zScanScrollModeAction = new ZScanScrollModeAction();
    private final ZoomScrollModeAction zoomScrollModeAction = new ZoomScrollModeAction();
    private final ButtonGroup scrollModeGroup = new ButtonGroup();
    private final OrthogonalModeAction orthogonalModeAction = new OrthogonalModeAction(this);
    // zoom actions
    private final ZoomInAction zoomInAction = new ZoomInAction(camera);
    private final ZoomOutAction zoomOutAction = new ZoomOutAction(camera);
    private final ZoomMaxAction zoomMaxAction = new ZoomMaxAction(camera, volumeImage);
    private final ResetZoomAction resetZoomAction = new ResetZoomAction(allSliceViewers, volumeImage);
    // Z scan actions
    private final SliceScanAction nextZSliceAction = new NextZSliceAction(volumeImage, camera);
    private final SliceScanAction previousZSliceAction = new PreviousZSliceAction(volumeImage, camera);
    private final SliceScanAction advanceZSlicesAction = new AdvanceZSlicesAction(volumeImage, camera, 10);
    private final SliceScanAction goBackZSlicesAction = new GoBackZSlicesAction(volumeImage, camera, -10);
    // go to actions
    private final GoToLocationAction goToLocationAction = new GoToLocationAction(camera);

    private QuadViewController quadViewController;
    private URL loadedUrl;

    // annotation-related
    private final CenterNextParentAction centerNextParentAction = new CenterNextParentAction(this);
    private final BacktrackNeuronAction backtrackNeuronAction = new BacktrackNeuronAction(this);
    private TileFormat tileFormat;

    private SkeletonController skeletonController;
    private PathTraceRequestListener pathTraceListener;
    private WorkspaceClosureListener wsCloseListener;

    private final Action clearCacheAction = new AbstractAction() {
        private static final long serialVersionUID = 1L;

        @Override
        public void actionPerformed(ActionEvent arg0) {
            clearCache();
        }
    };
    private final Action autoContrastAction = new AbstractAction() {
        private static final long serialVersionUID = 1L;

        @Override
        public void actionPerformed(ActionEvent arg0) {
            largeVolumeViewer.autoContrastNow();
        }
    };
    private final Action collectGarbageAction = new AbstractAction() {
        private static final long serialVersionUID = 1L;

        @Override
        public void actionPerformed(ActionEvent arg0) {
            System.gc();
        }
    };

    private int zSliceIndexForZMicrons(double zMicrons) {
        return (int) Math.round(zMicrons / volumeImage.getZResolution() - 0.5);
    }

    public void focusChanged(Vec3 focus) {
        int z = zSliceIndexForZMicrons(focus.getZ());
        zScanSlider.setValue(z);
        spinnerValue.setValue(z);
    }

    private void zoomChanged(Double zoom) {
        double zoomMin = Math.log(getMinZoom()) / Math.log(2.0);
        double zoomMax = Math.log(getMaxZoom()) / Math.log(2.0);
        double zoomLog = Math.log(zoom) / Math.log(2.0);
        double relativeZoom = (zoomLog - zoomMin) / (zoomMax - zoomMin);
        int sliderValue = (int) Math.round(relativeZoom * 1000.0);
        zoomSlider.setValue(sliderValue);
    }

    public void setMouseMode(MouseMode.Mode mode) {
        // Only display anchors in Trace mode
        if (mode == MouseMode.Mode.TRACE) {
            getSkeletonActor().getModel().setAnchorsVisible(true);
        } else {
            getSkeletonActor().getModel().setAnchorsVisible(false);
        }
    }

    public void setCameraFocus(Vec3 focus) {
        LOG.info("Setting camera focus: {}", focus);
        camera.setFocus(focus);
    }

    public Vec3 getCameraFocus() {
        return camera.getFocus();
    }

    public void setPixelsPerSceneUnit(double pixelsPerSceneUnit) {
        camera.setPixelsPerSceneUnit(pixelsPerSceneUnit);
    }

    public double getPixelsPerSceneUnit() {
        return camera.getPixelsPerSceneUnit();
    }

    /**
     * move toward the neuron root to the next branch or the root
     */
    public void backtrackNeuronMicron() {
        TmNeuronMetadata neuron = TmSelectionState.getInstance().getCurrentNeuron();
        if (neuron != null) {
            Anchor anchor = getSkeletonActor().getModel().getNextParent();
            if (anchor != null) {
                TmGeoAnnotation ann = annotationModel.getGeoAnnotationFromID(anchor.getNeuronID(), anchor.getGuid());
                if (!ann.isRoot()) {
                    ann = neuron.getParentOf(ann);
                    while (!ann.isRoot() && !ann.isBranch()) {
                        ann = neuron.getParentOf(ann);
                    }
                    skeletonController.setNextParent(getSkeleton().getAnchorByID(ann.getId()));
                }
            }
        }
    }

    public void centerNextParentMicron() {
        Anchor anchor = getSkeletonActor().getModel().getNextParent();
        if (anchor != null) {
            setCameraFocus(anchor.getLocation());
        }
    }

    /**
     * Create the frame.
     */
    public QuadViewUi(JFrame parentFrame, boolean overrideFrameMenuBar) {
        new MemoryCheckDialog().warnOfInsufficientMemory(LVV_PREFERRED_ID, MINIMUM_MEMORY_REQUIRED_GB, FrameworkAccess.getMainFrame());

        this.annotationModel = NeuronManager.getInstance();
        this.annotationMgr = new AnnotationManager(this, largeVolumeViewer, tileServer);

        volumeImage.addVolumeLoadListener(this);
        largeVolumeViewer.setImageColorModel(imageColorModel);
        sliderPanel.setVisible(false);

        camera.addCameraListener(new CameraListener() {
            @Override
            public void zoomChanged(Double zoom) {
                // Re-position the 3D cache.
                TileStackCacheController.getInstance().setZoom(zoom);
                QuadViewUi.this.zoomChanged(zoom);
            }

            @Override
            public void focusChanged(Vec3 focus) {
                TileStackCacheController.getInstance().setFocus(focus);
                QuadViewUi.this.focusChanged(focus);
            }

            @Override
            public void viewChanged() {
                // Re-position the 3D cache.
                TileStackCacheController.getInstance().setFocus(camera.getFocus());
                tileServer.refreshCurrentTileSet();
                // If we are using this optimization, the anchor set needs to be updated whenever the view is changed
                if (ApplicationPanel.isAnchorsInViewport()) {
                    getSkeletonActor().getModel().forceUpdateAnchors();
                }
            }
        });

        quadViewController = new QuadViewController(this, annotationMgr, largeVolumeViewer);

        setupUi(parentFrame, overrideFrameMenuBar);
        interceptModifierKeyPresses();
        interceptModeChangeGestures();
        interceptResizeEvents();
        setupAnnotationGestures();

        // connect up text UI and model with graphic UI(s):
        getSkeletonActor().getModel().addAnchorUpdateListener(annotationMgr);

        // Toggle skeleton actor with v key
        // see note in interceptModeChangeGestures() regarding which input map
        InputMap inputMap = viewerPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, 0, false), "vKeyPressed");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, 0, true), "vKeyReleased");
        ActionMap actionMap = viewerPanel.getActionMap();
        actionMap.put("vKeyPressed", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getSkeletonActor().setVisible(false);
            }
        });
        actionMap.put("vKeyReleased", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getSkeletonActor().setVisible(true);
            }
        });

        skeletonController = SkeletonController.getInstance();
        skeletonController.reestablish(skeleton, annotationMgr);
        skeletonController.registerForEvents(largeVolumeViewer.getSkeletonActor());
        annotationMgr.connectSkeletonSignals(skeleton, skeletonController);

        clearCacheAction.putValue(Action.NAME, "Clear Cache");
        clearCacheAction.putValue(Action.SHORT_DESCRIPTION, "Empty image cache (for testing only)");

        autoContrastAction.putValue(Action.NAME, "Auto Contrast");
        autoContrastAction.putValue(Action.SHORT_DESCRIPTION, "Optimize contrast for current view");

        collectGarbageAction.putValue(Action.NAME, "Collect Garbage");

        largeVolumeViewer.setSkeleton(skeleton);

        largeVolumeViewer.setWheelMode(WheelMode.Mode.SCAN);
        // Respond to orthogonal mode changes
        setZViewMode();
        // Connect mode changes to widgets
        // First connect mode actions to one signal
        quadViewController.registerForEvents(panModeAction);
        quadViewController.registerForEvents(zoomMouseModeAction);
        quadViewController.registerForEvents(traceMouseModeAction);
        quadViewController.registerForEvents(zoomScrollModeAction);
        quadViewController.registerForEvents(zScanScrollModeAction);
        quadViewController.registerForEvents(tileServer);
        quadViewController.registerForEvents(goToLocationAction);

        OrthogonalPanel[] viewPanels = {nwViewer};
        SkeletonActor sharedSkeletonActor = getSkeletonActor();
        quadViewController.registerForEvents(imageColorModel);
        quadViewController.unregisterOrthPanels();
      //  quadViewController.registerAsOrthPanelForRepaint(seViewer); // Must do separately.
        skeletonController.registerForEvents(quadViewController);  // Pass-through
        NeuronManager.getInstance().setViewStateListener(quadViewController);
        for (OrthogonalPanel v : viewPanels) {
            quadViewController.registerForEvents(v);
            v.setCamera(camera);
            v.setSharedVolumeImage(volumeImage);
            v.setSystemMenuItemGenerator(new MenuItemGenerator() {
                @Override
                public List<JMenuItem> getMenus(MouseEvent event) {
                    List<JMenuItem> result = new ArrayList<>();
                    result.add(addFileMenuItem());
                    result.add(addCopyMicronLocMenuItem());
                    result.add(addCopyTileLocMenuItem());
                    result.add(addCopyRawTileFileLocMenuItem(TmModelManager.getInstance().getCurrentSample()));
                    result.add(addCopyOctreePathMenuItem(TmModelManager.getInstance().getCurrentSample()));
                    result.add(addViewMenuItem());

                    return result;
                }
            });
            v.setTileServer(tileServer);
            v.getViewer().getSliceActor().setImageColorModel(imageColorModel);

            final boolean bShowTileOutlines = false; // Debugging aid
            if (bShowTileOutlines) {
                v.getViewer().addActor(new TileOutlineActor(v.getViewTileManager())); // for debugging
            }
            // Add skeleton actor AFTER slice actor
            v.getViewer().setSkeletonActor(sharedSkeletonActor);
        }
        // Set starting interaction modes
        traceMouseModeAction.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
        zoomScrollModeAction.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
    }

    public void setOrthogonalMode(OrthogonalModeAction.OrthogonalMode mode) {
        if (mode == OrthogonalModeAction.OrthogonalMode.ORTHOGONAL) {
            setOrthogonalMode();
        } else if (mode == OrthogonalModeAction.OrthogonalMode.Z_VIEW) {
            setZViewMode();
        }
    }

    public void updateSliderLockButtons() {
        sliderPanel.updateLockButtons();
    }

    public void setLoadStatus(TileServer.LoadStatus loadStatus) {
        loadStatusLabel.setLoadStatus(loadStatus);
    }

    public void pathTraceRequested(Long neuronId, Long annotationID) {
        // this needs to happen before you draw anchored paths; should
        //  go somewhere else so it only happens once, but not clear where;
        //  not clear we have a trigger for when the image is loaded enough for
        //  this info to be available (it loads asynchronously)
        getSkeletonActor().getModel().setTileFormat(
                tileServer.getLoadAdapter().getTileFormat());

        // construct new request; add image data to anchor and pass it on
        PathTraceToParentRequest request = new PathTraceToParentRequest(neuronId, annotationID);
        request.setImageVolume(volumeImage);
        request.setTextureCache(tileServer.getTextureCache());
        if (pathTraceListener != null) {
            pathTraceListener.pathTrace(request);
        }
    }

    public Skeleton getSkeleton() {
        return skeleton;
    }

    public NeuronManager getAnnotationModel() {
        return annotationModel;
    }

    public void clear() {
        tileServer.stop();
    }

    public void clearCache() {
        tileServer.clearCache();
    }

    public BoundingBox3d getBoundingBox() {
        return volumeImage.getBoundingBox3d();
    }

    public TileFormat getTileFormat() {
        return tileFormat;
    }

    private void updateRanges() {
        // Z range
        double zMin = volumeImage.getBoundingBox3d().getMin().getZ();
        double zMax = volumeImage.getBoundingBox3d().getMax().getZ();
        int z0 = (int) Math.round(zMin / volumeImage.getZResolution());
        int z1 = (int) Math.round(zMax / volumeImage.getZResolution()) - 1;
        if (z0 > z1) {
            z1 = z0;
        }
        // Z-scan is only relevant if there is more than one slice.
        boolean useZScan = ((z1 - z0) > 1);
        if (useZScan) {
            zScanPanel.setVisible(true);
            largeVolumeViewer.setWheelMode(WheelMode.Mode.SCAN);
            zScanScrollModeAction.setEnabled(true);
            zScanScrollModeAction.actionPerformed(new ActionEvent(this, 0, ""));
            int z = zSliceIndexForZMicrons(camera.getFocus().getZ());
            if (z < z0) {
                z = z0;
            }
            if (z > z1) {
                z = z1;
            }
            zScanSlider.setMinimum(z0);
            zScanSlider.setMaximum(z1);
            zScanSlider.setValue(z);

            // Allow octree zsteps to depend on zoom
            tileFormat = tileServer.getLoadAdapter().getTileFormat();
            final int zOrigin = tileFormat.getOrigin()[2];
            spinnerValue.setOffsetFromZero(zOrigin);

            zScanSpinner.setModel(
                    new SpinnerNumberModel(
                            spinnerValue.getInternalValue(z),
                            spinnerValue.getInternalValue(z0),
                            spinnerValue.getInternalValue(z1),
                            1
                    )
            );

            zScanMode.setTileFormat(tileFormat);
            nextZSliceAction.setTileFormat(tileFormat);
            previousZSliceAction.setTileFormat(tileFormat);
            advanceZSlicesAction.setTileFormat(tileFormat);
            goBackZSlicesAction.setTileFormat(tileFormat);
            skeleton.setTileFormat(tileFormat);
        } else { // no Z scan
            zScanPanel.setVisible(false);
            zoomScrollModeAction.actionPerformed(new ActionEvent(this, 0, ""));
            zScanScrollModeAction.setEnabled(false);
        }
    }

    private double getMaxZoom() {
        double maxRes = Math.min(volumeImage.getXResolution(), Math.min(
                volumeImage.getYResolution(),
                volumeImage.getZResolution()));
        return 300.0 / maxRes; // 300 pixels per voxel is probably zoomed enough...
    }

    private double getMinZoom() {
        double result = getMaxZoom();
        BoundingBox3d box = volumeImage.getBoundingBox3d();
        Vec3 volSize = new Vec3(box.getWidth(), box.getHeight(), box.getDepth());
        // note: this used to loop over allSliceViewers, at a point in time
        //  when we thought we would have more than one, to find the active one;
        // this is problematic when the active viewer is not visible at startup
        // now just take the first viewer, since it's the only one
        TileConsumer viewer = allSliceViewers.get(0);
        int w = viewer.getViewport().getWidth();
        int h = viewer.getViewport().getHeight();
        if (w > 0 && h > 0) {
            // Fit two of the whole volume on the screen
            // Rotate volume to match viewer orientation
            Vec3 rotSize = viewer.getViewerInGround().inverse().times(volSize);
            double zx = 0.5 * w / Math.abs(rotSize.x());
            double zy = 0.5 * h / Math.abs(rotSize.y());
            double z = Math.min(zx, zy);
            result = Math.min(z, result);
        }
        return result;
    }

    private void setOrthogonalMode() {
        nwViewer.setVisible(true);
        tileServer.refreshCurrentTileSet();
    }

    private SkeletonActor getSkeletonActor() {
        return largeVolumeViewer.getSkeletonActor();
    }

    private void setZViewMode() {
        nwViewer.setVisible(true);
        tileServer.refreshCurrentTileSet();
    }

    private void setupUi(JFrame parentFrame, boolean overrideFrameMenuBar) {
        setBounds(100, 100, 994, 653);
        setBorder(new EmptyBorder(5, 5, 5, 5));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JPanel toolBarPanel = setupToolBar();

        // JSplitPane splitPane = new JSplitPane();
        splitPane.setResizeWeight(1.00);
        // FW-2805: specify min size explicitly, or it'll grab a large
        //  default minimum from who knows where and blow the vertical extent way up:
        splitPane.setMinimumSize(new Dimension(0, 10));
        splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        toolBarPanel.add(splitPane, BorderLayout.CENTER);

        JPanel rightComponentPanel = new JPanel();
        rightComponentPanel.setLayout(new BorderLayout());
        rightComponentPanel.add(sliderPanel, BorderLayout.CENTER);
        ColorButtonPanel colorButtonPanel = new ColorButtonPanel(imageColorModel, 1);
        rightComponentPanel.add(colorButtonPanel, BorderLayout.EAST);
        splitPane.setRightComponent(rightComponentPanel);
        imageColorModel.addColorModelInitListener(new ColorModelInitListener() {
            @Override
            public void colorModelInit() {
                splitPane.resetToPreferredSizes();
            }
        });

        sliderPanel.setTop(SliderPanel.VIEW.LVV);
        sliderPanel.guiInit();

        JSplitPane splitPane_1 = new JSplitPane();
        splitPane_1.setResizeWeight(1.00);
        splitPane.setLeftComponent(splitPane_1);

        // this contains the viewer panel plus the z scroll bar
        JPanel viewerPlusPanel = new JPanel();
        viewerPlusPanel.setLayout(new BoxLayout(viewerPlusPanel, BoxLayout.X_AXIS));
        splitPane_1.setLeftComponent(viewerPlusPanel);

        viewerPlusPanel.add(viewerPanel);
        viewerPanel.setLayout(new GridBagLayout());

        // Stupid WindowBuilder won't accept reuse of GridBagConstraints object;
        // ...so the usual Java "create another class"...
        class QuadrantConstraints extends GridBagConstraints {

            private QuadrantConstraints(int x, int y) {
                this.gridx = x;
                this.gridy = y;
                this.gridwidth = 1;
                this.gridheight = 1;
                this.fill = GridBagConstraints.BOTH;
                this.weightx = 1.0;
                this.weighty = 1.0;
                this.insets = new Insets(1, 1, 1, 1);
            }
        }

        // Four quadrants for orthogonal views
        // One panel for Z slice viewer (upper left northwest)
        viewerPanel.add(nwViewer, new QuadrantConstraints(0, 0));
        largeVolumeViewer.setCamera(camera);
        largeVolumeViewer.getComponent().setBackground(Color.DARK_GRAY);
        zViewerPanel.setLayout(new BoxLayout(zViewerPanel, BoxLayout.Y_AXIS));
        zViewerPanel.add(largeVolumeViewer.getComponent());

        // JPanel zScanPanel = new JPanel();
        zViewerPanel.add(zScanPanel);
        zScanPanel.setLayout(new BoxLayout(zScanPanel, BoxLayout.X_AXIS));

        ToolButton button_2 = new ToolButton(goBackZSlicesAction);
        button_2.setAction(goBackZSlicesAction);
        button_2.setMargin(new Insets(0, 0, 0, 0));
        button_2.setHideActionText(true);
        button_2.setAlignmentX(0.5f);
        zScanPanel.add(button_2);

        ToolButton button_1 = new ToolButton(previousZSliceAction);
        button_1.setAction(previousZSliceAction);
        button_1.setMargin(new Insets(0, 0, 0, 0));
        button_1.setHideActionText(true);
        button_1.setAlignmentX(0.5f);
        zScanPanel.add(button_1);
        zScanSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent arg0) {
                setZSlice(zScanSlider.getValue());
            }
        });

        // JSlider zScanSlider = new JSlider();
        zScanSlider.setPreferredSize(new Dimension(32767, 29));
        zScanSlider.setMajorTickSpacing(10);
        zScanSlider.setPaintTicks(true);
        zScanPanel.add(zScanSlider);

        ToolButton button_3 = new ToolButton(nextZSliceAction);
        button_3.setAction(nextZSliceAction);
        button_3.setMargin(new Insets(0, 0, 0, 0));
        button_3.setHideActionText(true);
        button_3.setAlignmentX(0.5f);
        zScanPanel.add(button_3);

        ToolButton button_4 = new ToolButton(advanceZSlicesAction);
        button_4.setAction(advanceZSlicesAction);
        button_4.setMargin(new Insets(0, 0, 0, 0));
        button_4.setHideActionText(true);
        button_4.setAlignmentX(0.5f);
        zScanPanel.add(button_4);
        zScanSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent arg0) {
                //(Integer)zScanSpinner.getValue()
                setZSlice(spinnerValue.getValue());
            }
        });

        // JSpinner zScanSpinner = new JSpinner();
        zScanSpinner.setPreferredSize(new Dimension(75, 28));
        zScanSpinner.setMaximumSize(new Dimension(120, 28));
        zScanSpinner.setMinimumSize(new Dimension(65, 28));
        zScanPanel.add(zScanSpinner);

        JPanel controlsPanel = new JPanel();
        splitPane_1.setRightComponent(controlsPanel);
        controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.X_AXIS));

        JPanel panel_1 = new JPanel();
        panel_1.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
        viewerPlusPanel.add(panel_1);
        panel_1.setLayout(new BoxLayout(panel_1, BoxLayout.Y_AXIS));

        ToolButton zoomInButton = new ToolButton(zoomInAction);
        zoomInButton.setAlignmentX(0.5f);
        zoomInButton.setMargin(new Insets(0, 0, 0, 0));
        zoomInButton.setHideActionText(true);
        zoomInButton.setAction(zoomInAction);
        // Use a more modest auto repeat for zoom
        zoomInButton.setAutoRepeatDelay(150);
        panel_1.add(zoomInButton);

        // JSlider zoomSlider = new JSlider();
        zoomSlider.setOrientation(SwingConstants.VERTICAL);
        zoomSlider.setMaximum(1000);
        // Kludge to get decent vertical JSlider on Windows
        zoomSlider.setPaintTicks(true);
        zoomSlider.setMajorTickSpacing(1000);
        //
        panel_1.add(zoomSlider);
        zoomSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent arg0) {
                int value = zoomSlider.getValue();
                double relativeZoom = value / 1000.0;
                // log scale
                double zoomMin = Math.log(getMinZoom()) / Math.log(2.0);
                double zoomMax = Math.log(getMaxZoom()) / Math.log(2.0);
                double zoom = zoomMin + relativeZoom * (zoomMax - zoomMin);
                zoom = Math.pow(2.0, zoom);
                camera.setPixelsPerSceneUnit(zoom);
            }
        });

        ToolButton zoomOutButton = new ToolButton(zoomOutAction);
        zoomOutButton.setAction(zoomOutAction);
        zoomOutButton.setMargin(new Insets(0, 0, 0, 0));
        zoomOutButton.setHideActionText(true);
        zoomOutButton.setAlignmentX(0.5f);
        zoomOutButton.setAutoRepeatDelay(150); // slow down auto zoom
        panel_1.add(zoomOutButton);

        JPanel buttonsPanel = new JPanel();
        controlsPanel.add(buttonsPanel);
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));

        JButton btnNewButton_1 = new JButton("New button");
        btnNewButton_1.setAction(resetZoomAction);
        buttonsPanel.add(btnNewButton_1);

        JButton btnNewButton = new JButton("New button");
        btnNewButton.setAction(zoomMaxAction);
        buttonsPanel.add(btnNewButton);

        JButton resetViewButton = new JButton("New button");
        resetViewButton.setAction(resetViewAction);
        buttonsPanel.add(resetViewButton);

        JButton gotoLocationButton = new JButton("New button");
        gotoLocationButton.setAction(goToLocationAction);
        buttonsPanel.add(gotoLocationButton);

        JButton loadUpdatesButton = new JButton("Refresh Updates");
        loadUpdatesButton.setAction(refreshSharedUpdatesAction);

        final JCheckBox receiveSharedUpdatesCheckbox = new JCheckBox("Shared Updates");
        receiveSharedUpdatesCheckbox.setSelected(true);
        receiveSharedUpdatesCheckbox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.DESELECTED) {
                    receiveSharedUpdatesCheckbox.setSelected(false);
                  //  annotationModel.setReceiveUpdates(false);
                    loadUpdatesButton.setEnabled(true);
                } else if (e.getStateChange() == ItemEvent.SELECTED) {
                    receiveSharedUpdatesCheckbox.setSelected(true);
                   // annotationModel.setReceiveUpdates(true);
                    loadUpdatesButton.setEnabled(false);
                }
            }
        });
        buttonsPanel.add(receiveSharedUpdatesCheckbox);

        buttonsPanel.add(loadUpdatesButton);

        buttonsPanel.add(new TileStackCacheStatusPanel());

        Component verticalGlue = Box.createVerticalGlue();
        buttonsPanel.add(verticalGlue);

        buttonsPanel.add(loadStatusLabel);

        final JCheckBox volumeCacheCheckbox = new JCheckBox("Volume Cache");
        volumeCacheCheckbox.setSelected(VolumeCache.useVolumeCache());
        volumeCacheCheckbox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.DESELECTED) {
                    volumeCacheCheckbox.setSelected(false);
                    VolumeCache.setVolumeCache(false);
                } else if (e.getStateChange() == ItemEvent.SELECTED) {
                    volumeCacheCheckbox.setSelected(true);
                    VolumeCache.setVolumeCache(true);
                }
            }
        });
        buttonsPanel.add(volumeCacheCheckbox);

        JButton btnClearCache = new JButton("Clear Cache");
        btnClearCache.setAction(clearCacheAction);
        buttonsPanel.add(btnClearCache);

        Component verticalGlue_1 = Box.createVerticalGlue();
        buttonsPanel.add(verticalGlue_1);

        JButton autoContrastButton = new JButton();
        autoContrastButton.setAction(autoContrastAction);
        buttonsPanel.add(autoContrastButton);

        JButton resetColorsButton = new JButton();
        resetColorsButton.setAction(resetColorsAction);
        buttonsPanel.add(resetColorsButton);

        JPanel statusBar = new JPanel();
        statusBar.setMaximumSize(new Dimension(32767, 30));
        statusBar.setMinimumSize(new Dimension(10, 30));
        add(statusBar);
        statusBar.setLayout(new BoxLayout(statusBar, BoxLayout.X_AXIS));

        statusBar.add(statusLabel);

        // For large volume viewer popup menu.
        largeVolumeViewer.setSystemMenuItemGenerator(new MenuItemGenerator() {
            @Override
            public List<JMenuItem> getMenus(MouseEvent event) {
                List<JMenuItem> result = new ArrayList<>();
                result.add(addFileMenuItem());
                result.add(addViewMenuItem());
                return result;
            }
        });
    }

    private void interceptModifierKeyPresses() {
        // Intercept Shift key strokes at the highest level JComponent we can find.
        InputMap inputMap = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SHIFT, KeyEvent.SHIFT_DOWN_MASK, false),
                "ModifierPressed");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_CONTROL, KeyEvent.CTRL_DOWN_MASK, false),
                "ModifierPressed");

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_CONTROL, 0, true),
                "ModifierReleased");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SHIFT, 0, true),
                "ModifierReleased");

        ActionMap actionMap = getActionMap();
        actionMap.put("ModifierPressed", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                setModifierKeyPressed(true);
            }
        });
        actionMap.put("ModifierReleased", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                setModifierKeyPressed(false);
            }
        });
    }

    private void interceptModeChangeGestures() {
        // Press "H" (hand) for Pan mode, etc.
        Action modeActions[] = {
            panModeAction,
            zoomMouseModeAction,
            traceMouseModeAction,
            zoomInAction,
            zoomOutAction,
            nextZSliceAction,
            previousZSliceAction,
            goToLocationAction
        };
        // input map for viewer area, not all of QuadViewUi or anything that has
        //  text entry fields, or we'll trigger actions while typing in them!
        InputMap inputMap = viewerPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        for (Action action : modeActions) {
            KeyStroke accelerator = (KeyStroke) action.getValue(Action.ACCELERATOR_KEY);
            String actionName = (String) action.getValue(Action.NAME);
            inputMap.put(accelerator, actionName);
            viewerPanel.getActionMap().put(actionName, action);
        }
    }

    private void interceptResizeEvents() {
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // If we are using this optimization, the anchor set needs to be updated whenever the view is resized
                if (ApplicationPanel.isAnchorsInViewport()) {
                    getSkeletonActor().getModel().forceUpdateAnchors();
                }
            }
        });
    }

    private void setupAnnotationGestures() {
        // like the two "intercept" routines, but annotation-related;
        //  broken out for clarity and organization more than anything

        Action modeActions[] = {
            centerNextParentAction,
            backtrackNeuronAction
        };
        /// see note in interceptModeChangeGestures() re: which input map
        InputMap inputMap = viewerPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        for (Action action : modeActions) {
            KeyStroke accelerator = (KeyStroke) action.getValue(Action.ACCELERATOR_KEY);
            String actionName = (String) action.getValue(Action.NAME);
            inputMap.put(accelerator, actionName);
            viewerPanel.getActionMap().put(actionName, action);
        }

    }

    private JPanel setupToolBar() {
        add(toolBarPanel);
        toolBarPanel.setLayout(new BorderLayout(0, 0));

        JToolBar toolBar = new JToolBar();
        toolBarPanel.add(toolBar, BorderLayout.NORTH);

        JLabel lblNewLabel_1 = new JLabel("");
        lblNewLabel_1.setToolTipText("Mouse Mode:");
        lblNewLabel_1.setFocusable(false);
        lblNewLabel_1.setIcon(Icons.getIcon(IMAGES_MOUSE_LEFT));
        toolBar.add(lblNewLabel_1);

        // TODO - create a shared base class for these mode buttons
        JToggleButton traceMouseModeButton = new JToggleButton("Trace");
        mouseModeGroup.add(traceMouseModeButton);
        traceMouseModeButton.setAction(traceMouseModeAction);
        traceMouseModeButton.setMargin(new Insets(0, 0, 0, 0));
        traceMouseModeButton.setHideActionText(true);
        traceMouseModeButton.setFocusable(false);
        toolBar.add(traceMouseModeButton);

        JToggleButton tglBtnPanMode = new JToggleButton("");
        mouseModeGroup.add(tglBtnPanMode);
        tglBtnPanMode.setSelected(true);
        tglBtnPanMode.setAction(panModeAction);
        tglBtnPanMode.setMargin(new Insets(0, 0, 0, 0));
        tglBtnPanMode.setHideActionText(true);
        tglBtnPanMode.setFocusable(false);
        toolBar.add(tglBtnPanMode);

        JToggleButton tglbtnZoomMouseMode = new JToggleButton("");
        mouseModeGroup.add(tglbtnZoomMouseMode);
        tglbtnZoomMouseMode.setMargin(new Insets(0, 0, 0, 0));
        tglbtnZoomMouseMode.setFocusable(false);
        tglbtnZoomMouseMode.setHideActionText(true);
        tglbtnZoomMouseMode.setAction(zoomMouseModeAction);
        toolBar.add(tglbtnZoomMouseMode);

        toolBar.addSeparator();

        JLabel scrollModeLabel = new JLabel("");
        scrollModeLabel.setIcon(Icons.getIcon(IMAGES_MOUSE_SCROLL));
        scrollModeLabel.setFocusable(false);
        toolBar.add(scrollModeLabel);

        JToggleButton toggleButton = new JToggleButton("");
        scrollModeGroup.add(toggleButton);
        toggleButton.setSelected(true);
        toggleButton.setAction(zScanScrollModeAction);
        toggleButton.setMargin(new Insets(0, 0, 0, 0));
        toggleButton.setHideActionText(true);
        toggleButton.setFocusable(false);
        toolBar.add(toggleButton);

        JToggleButton toggleButton_1 = new JToggleButton("");
        scrollModeGroup.add(toggleButton_1);
        toggleButton_1.setAction(zoomScrollModeAction);
        toggleButton_1.setMargin(new Insets(0, 0, 0, 0));
        toggleButton_1.setHideActionText(true);
        toggleButton_1.setFocusable(false);
        toolBar.add(toggleButton_1);

        toolBar.addSeparator();

        return toolBarPanel;
    }

    private void setModifierKeyPressed(boolean pressed) {
        // Has the status changed since last time?
        if (pressed == modifierKeyPressed) {
            return; // no change
        }
        modifierKeyPressed = pressed; // changed!
        // Shift to select zoom scroll mode
        if (pressed) {
            zoomScrollModeAction.actionPerformed(new ActionEvent(this, 0, ""));
        } else if (zScanScrollModeAction.isEnabled()) {
            zScanScrollModeAction.actionPerformed(new ActionEvent(this, 0, ""));
        }
    }

    private boolean setZSlice(int z) {
        Vec3 oldFocus = camera.getFocus();
        int oldValue = zSliceIndexForZMicrons(oldFocus.getZ());
        if (oldValue == z) {
            return false; // camera is already pretty close
        }
        double halfVoxel = 0.5 * volumeImage.getZResolution();
        double newZ = z * volumeImage.getZResolution() + halfVoxel;
        double minZ = volumeImage.getBoundingBox3d().getMin().getZ() + halfVoxel;
        double maxZ = volumeImage.getBoundingBox3d().getMax().getZ() - halfVoxel;
        newZ = Math.max(newZ, minZ);
        newZ = Math.min(newZ, maxZ);
        if (!Double.isNaN(newZ)) {
            camera.setFocus(new Vec3(oldFocus.getX(), oldFocus.getY(), newZ));
        }
        return true;
    }

    private JMenuItem addViewMenuItem() {
        JMenu mnView = new JMenu("View");
        menuBar.add(mnView);

        JMenu mnMouseMode = new JMenu("Mouse Mode");
        mnMouseMode.setIcon(Icons.getIcon(IMAGES_MOUSE_LEFT));
        mnView.add(mnMouseMode);

        JRadioButtonMenuItem panModeItem = new JRadioButtonMenuItem("New radio item");
        panModeItem.setSelected(true);
        panModeItem.setAction(panModeAction);
        mnMouseMode.add(panModeItem);

        JRadioButtonMenuItem zoomMouseModeItem = new JRadioButtonMenuItem("New radio item");
        zoomMouseModeItem.setAction(zoomMouseModeAction);
        mnMouseMode.add(zoomMouseModeItem);

        JRadioButtonMenuItem traceMouseModeItem = new JRadioButtonMenuItem();
        traceMouseModeItem.setAction(traceMouseModeAction);
        mnMouseMode.add(traceMouseModeItem);

        JMenu mnScrollMode = new JMenu("Scroll Mode");
        mnScrollMode.setIcon(Icons.getIcon(IMAGES_MOUSE_SCROLL));
        mnView.add(mnScrollMode);

        JRadioButtonMenuItem rdbtnmntmNewRadioItem = new JRadioButtonMenuItem("New radio item");
        rdbtnmntmNewRadioItem.setSelected(true);
        rdbtnmntmNewRadioItem.setAction(zScanScrollModeAction);
        mnScrollMode.add(rdbtnmntmNewRadioItem);

        JRadioButtonMenuItem mntmNewMenuItem_2 = new JRadioButtonMenuItem("New menu item");
        mntmNewMenuItem_2.setAction(zoomScrollModeAction);
        mnScrollMode.add(mntmNewMenuItem_2);

        JSeparator separator = new JSeparator();
        mnView.add(separator);

        JMenu mnZoom = new JMenu("Zoom");
        mnView.add(mnZoom);

        mnZoom.add(resetZoomAction);
        mnZoom.add(zoomOutAction);
        mnZoom.add(zoomInAction);
        mnZoom.add(zoomMaxAction);

        JSeparator separator_1 = new JSeparator();
        mnView.add(separator_1);

        JMenu mnZScan = new JMenu("Z Scan");
        mnView.add(mnZScan);

        JMenuItem mntmNewMenuItem = new JMenuItem("New menu item");
        mntmNewMenuItem.setAction(goBackZSlicesAction);
        mnZScan.add(mntmNewMenuItem);

        JMenuItem menuItem_2 = new JMenuItem("New menu item");
        menuItem_2.setAction(previousZSliceAction);
        mnZScan.add(menuItem_2);

        JMenuItem menuItem_1 = new JMenuItem("New menu item");
        menuItem_1.setAction(nextZSliceAction);
        mnZScan.add(menuItem_1);

        JMenuItem menuItem = new JMenuItem("New menu item");
        menuItem.setAction(advanceZSlicesAction);
        mnZScan.add(menuItem);

        JSeparator separator_2 = new JSeparator();
        mnView.add(separator_2);

        JMenuItem mntmNewMenuItem_3 = new JMenuItem("New menu item");
        mntmNewMenuItem_3.setAction(resetColorsAction);
        mnView.add(mntmNewMenuItem_3);
        return mnView;
    }

    private JMenuItem addFileMenuItem() {
        JMenu mnFile = new JMenu("File");
        menuBar.add(mnFile);

        JMenuItem mntmNewMenuItem_1 = new JMenuItem("New menu item");
        mntmNewMenuItem_1.setAction(openFolderAction);
        mnFile.add(mntmNewMenuItem_1);

        JMenu recentFileMenu = new JMenu("Open Recent");
        recentFileMenu.setVisible(false);
        mnFile.add(recentFileMenu);
        recentFileList = new RecentFileList(recentFileMenu);
        quadViewController.registerForEvents(recentFileList);

        return mnFile;
    }

    private JMenuItem addCopyMicronLocMenuItem() {
        JMenuItem mnCopyMicron = new JMenuItem(
                new MicronsToClipboardAction(statusLabel)
        );
        return mnCopyMicron;
    }

    private JMenuItem addCopyOctreePathMenuItem(TmSample tmSample) {
        JMenuItem menuItem = new JMenuItem(
                new OctreeFilePathToClipboardAction(
                        statusLabel,
                        tileFormat,
                        getRenderedVolumeLocation(tmSample),
                        camera,
                        CoordinateAxis.Z
                )
        );
        return menuItem;
    }

    private JMenuItem addCopyTileLocMenuItem() {
        JMenuItem mnCopyTileInx = new JMenuItem(
                new TileLocToClipboardAction(
                        statusLabel, tileFormat, camera, CoordinateAxis.Z
                )
        );
        return mnCopyTileInx;
    }

    JMenuItem addCopyRawTileFileLocMenuItem(TmSample tmSample) {
        JMenuItem mnCopyTileFileLoc = new JMenuItem(
                new RawFileLocToClipboardAction(
                        statusLabel,
                        tileFormat,
                        tmSample.getAcquisitionFilepath(),
                        getRenderedVolumeLocation(tmSample),
                        getRenderedVolumeLoader())
        );
        return mnCopyTileFileLoc;
    }

    private RenderedVolumeLocation getRenderedVolumeLocation(TmSample tmSample) {
        return TmModelManager.getInstance().getTileLoader().getRenderedVolumeLocation(tmSample);
    }

    private RenderedVolumeLoader getRenderedVolumeLoader() {
        return new RenderedVolumeLoaderImpl();
    }

    /**
     * this is called only via right-click File > Open folder menu
     */
    public boolean loadRender(URL url) {
        // need to close/clear workspace and sample here:
        if (wsCloseListener != null) {
            wsCloseListener.closeWorkspace();
        }
        // then just go ahead and load the file
        boolean rtnVal = loadDataFromURL(url);
        return rtnVal;
    }

    public boolean loadDataFromURL(URL url) {
        LOG.info("loadDataFromURL: {}", url);
        boolean rtnVal = volumeImage.loadURL(url);
        loadedUrl = url;
        return rtnVal;
    }

    public void loadNeurons(LoadNeuronsEvent loadNeuronsEvent) {
        skeletonController.workspaceNeuronsLoaded(loadNeuronsEvent);
    }

    public void setStatusLabelText(String text) {
        statusLabel.setText(text);
    }

    public ImageColorModel getImageColorModel() {
        return imageColorModel;
    }

    public void setImageColorModel(TmColorModel tmColorModel) {
        ModelTranslation.updateColorModel(tmColorModel, imageColorModel);
    }

    /**
     * this method returns a provider of read-only subvolume of data (maximum
     * zoom, intended for calculations)
     */
    public SubvolumeProvider getSubvolumeProvider() {
        return new SubvolumeProvider(volumeImage, tileServer);
    }

    //---------------------------IMPLEMENTS VolumeLoadListener
    @Override
    public void volumeLoaded(URL url) {
        updateRanges();

        recentFileList.add(url);

        // this is a little sketchy; right now, Horta 2d (aka LVV) only loads color models
        //  from the workspace once, at first load; so we do that directly here; really we
        //  probably ought to have an event bus message that triggers this, but I just cannot
        //  figure out where to wire that in; this is cleaner and easier, but it cannot
        //  be extended in the future should we choose to implement loadable color models
        //  for Horta 2d
        TmWorkspace workspace = TmModelManager.getInstance().getCurrentWorkspace();
        if (workspace != null && workspace.getColorModel()!=null) {
            setImageColorModel(workspace.getColorModel());
        }

        resetViewAction.actionPerformed(null);

        getSkeletonActor().getModel().setTileFormat(
                tileServer.getLoadAdapter().getTileFormat());
    }

    /**
     * @param pathTraceListener the pathTraceListener to set
     */
    public void setPathTraceListener(PathTraceRequestListener pathTraceListener) {
        this.pathTraceListener = pathTraceListener;
    }

    static class LoadStatusLabel extends JLabel {

        private TileServer.LoadStatus loadStatus = null;
        private final ImageIcon busyIcon;
        private final ImageIcon checkIcon;
        private final ImageIcon emptyIcon;

        LoadStatusLabel() {
            this.busyIcon = Icons.getIcon(IMAGES_SPINNER);
            this.checkIcon = Icons.getIcon(IMAGES_GREEN_CHECK);
            this.emptyIcon = Icons.getIcon(IMAGES_FOLDER_OPEN);
            // Place text over icon
            setHorizontalTextPosition(JLabel.CENTER);
            setVerticalTextPosition(JLabel.CENTER);
            setLoadStatus(TileServer.LoadStatus.UNINITIALIZED);
        }

        void setLoadStatus(TileServer.LoadStatus loadStatus) {
            if (this.loadStatus == loadStatus) {
                return; // no change
            }
            this.loadStatus = loadStatus;
            setText(Integer.toString(loadStatus.ordinal() - 1));
            if (loadStatus.ordinal() >= TileServer.LoadStatus.BEST_TEXTURES_LOADED.ordinal()) {
                setIcon(checkIcon);
            } else if (loadStatus.ordinal() >= TileServer.LoadStatus.NO_TEXTURES_LOADED.ordinal()) {
                setIcon(busyIcon);
            } else {
                setIcon(emptyIcon);
            }
        }
    }

    public AnnotationManager getAnnotationMgr() {
        return annotationMgr;
    }

    public void resetZoom() {
        resetViewAction.resetZoom();
    }

    public BasicObservableCamera3d getCamera() {
        return camera;
    }

}
