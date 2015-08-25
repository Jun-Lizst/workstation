/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.cache.large_volume;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.large_volume_viewer.SharedVolumeImage;
import org.janelia.it.workstation.gui.large_volume_viewer.TileFormat;
import org.janelia.it.workstation.gui.large_volume_viewer.camera.ObservableCamera3d;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.CameraListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ensures that cache manager gets updated at the right times.
 * @author fosterl
 */
public class CacheController {
    private CacheFacade manager;
    private CacheCameraListener cameraListener;
    private static ExecutorService executor;
    
    private static CacheController instance = new CacheController();
    
    public static CacheController getInstance() {
        return instance;
    }
    
    /**
     * Singleton use only.
     */
    private CacheController() {
        // Create a queue-like thread pool.
        executor = Executors.newFixedThreadPool(1);
    }
    
    /**
     * @return the manager
     */
    public CacheFacade getManager() {
        return manager;
    }

    /**
     * @param manager the manager to set
     */
    public void setManager(CacheFacade manager) {
        this.manager = manager;
        this.cameraListener = new CacheCameraListener(manager);
    }

    /**
     * Wires up the event-passage, so that the camera focus change will
     * signal the cache manager.
     * 
     * @param camera observed.
     */
    public void registerForEvents(ObservableCamera3d camera, SharedVolumeImage sharedVolumeImage) {
        if (cameraListener != null) {
            camera.addCameraListener(cameraListener);
            cameraListener.setCamera(camera);
            cameraListener.setSharedVolumeImage(sharedVolumeImage);
        }
        else {
            Logger log = LoggerFactory.getLogger(CacheController.class);
            log.warn("Attempt to register a camera for events, before the cache manager has been set.");
        }
    }

    private static class CacheCameraListener implements CameraListener {

        private CacheFacade manager;
        private SharedVolumeImage sharedVolumeImage;
        private ObservableCamera3d camera;
        
        public CacheCameraListener( CacheFacade manager ) {
            this.manager = manager;
        }
        
        public void setCamera(ObservableCamera3d camera) {
            this.camera = camera;
        }
        
        public void setSharedVolumeImage( SharedVolumeImage sharedVolumeImage ) {
            this.sharedVolumeImage = sharedVolumeImage;
        }
        
        @Override
        public void viewChanged() {
            
        }

        @Override
        public void zoomChanged(Double zoom) {
            manager.setCameraZoom(zoom);
        }

        @Override
        public void focusChanged(Vec3 focus) {
            // Create a new change-of-focus runnable, and
            // run it in a thread, serialized behind all others before it.
            Runnable r = new FocusChanger(camera, sharedVolumeImage, manager, focus);
            executor.submit(r);
        }
        
    }

    /**
     * This task will carry out focus modification in a separate thread
     * from the event thread.
     */
    private static class FocusChanger implements Runnable {
        private ObservableCamera3d camera;
        private SharedVolumeImage sharedVolumeImage;
        private CacheFacade manager;
        private Vec3 focus;
        
        public FocusChanger(ObservableCamera3d camera, SharedVolumeImage sharedVolumeImage, CacheFacade manager, Vec3 focus) {
            this.camera = camera;
            this.sharedVolumeImage = sharedVolumeImage;
            this.manager = manager;
            this.focus = focus;
        }
        
        public void run() {
            TileFormat tileFormat = sharedVolumeImage.getLoadAdapter().getTileFormat();
            Double zoom = (double) tileFormat.zoomLevelForCameraZoom(camera.getPixelsPerSceneUnit());
            if (zoom != null) {
                manager.setCameraZoom(zoom);
            }

            double[] focusArr = new double[3];
            for (int i = 0; i < focusArr.length; i++) {
                focusArr[i] = focus.elementAt(i);
            }
            manager.setFocus(focusArr);
        }
    }
}
