package org.janelia.it.workstation.ab2.gl;

import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL4;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;

import org.janelia.it.workstation.ab2.renderer.AB2Renderer;

public abstract class GLRegion implements GLEventListener {

    protected List<AB2Renderer> renderers=new ArrayList<>();

    @Override
    public void init(GLAutoDrawable drawable) {
        final GL4 gl=drawable.getGL().getGL4();
        for (AB2Renderer renderer : renderers) {
            renderer.init(gl);
        }
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        final GL4 gl=drawable.getGL().getGL4();
        for (AB2Renderer renderer : renderers) {
            renderer.dispose(gl);
        }
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        final GL4 gl=drawable.getGL().getGL4();
        for (AB2Renderer renderer : renderers) {
            renderer.display(gl);
        }
    }

    @Override
    public abstract void reshape(GLAutoDrawable drawable, int x, int y, int width, int height);

}
