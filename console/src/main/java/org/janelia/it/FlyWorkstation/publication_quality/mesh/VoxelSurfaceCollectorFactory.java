package org.janelia.it.FlyWorkstation.publication_quality.mesh;

import org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.MaskChanStreamSource;
import org.janelia.it.jacs.shared.loader.MaskChanDataAcceptorI;
import org.janelia.it.jacs.shared.loader.MaskChanMultiFileLoader;
import org.janelia.it.jacs.shared.loader.file_resolver.FileResolver;
import org.janelia.it.jacs.shared.loader.renderable.MaskChanRenderableData;
import org.janelia.it.jacs.shared.loader.renderable.RenderableBean;

import java.util.Arrays;

/**
 * Setup a surface collector for minimal inputs.
 *
 * Created by fosterl on 3/27/14.
 */
public class VoxelSurfaceCollectorFactory {
    private FileResolver resolver;
    private boolean includeChannelData;

    public VoxelSurfaceCollectorFactory( FileResolver resolver, boolean includeChannelData ) {
        this.resolver = resolver;
        this.includeChannelData = includeChannelData;
    }

    public VoxelSurfaceCollector getSurfaceCollector( final MaskChanRenderableData renderableData ) throws Exception {
        RenderableBean renderableBean = renderableData.getBean();
        MaskChanMultiFileLoader loader = new MaskChanMultiFileLoader();

        VoxelSurfaceCollector surfaceCollector = new VoxelSurfaceCollector();
        loader.setAcceptors( Arrays.<MaskChanDataAcceptorI>asList(surfaceCollector) );

        MaskChanStreamSource streamSource = new MaskChanStreamSource(
                renderableData, resolver, includeChannelData
        );

        loader.read(renderableBean, streamSource);
        loader.close();
        return surfaceCollector;

    }
}
