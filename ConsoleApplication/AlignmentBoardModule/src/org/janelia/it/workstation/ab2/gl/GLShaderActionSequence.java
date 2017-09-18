package org.janelia.it.workstation.ab2.gl;

import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL4;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GLShaderActionSequence {

    Logger logger= LoggerFactory.getLogger(GLShaderActionSequence.class);

    String name;
    private GLShaderProgram shader;
    boolean applyMemoryBarrier=false;
    List<GLAbstractActor> actorSequence=new ArrayList<>();

    public GLShaderActionSequence(String name) {
        this.name=name;
    }

    public String getName() {
        return name;
    }

    public GLShaderProgram getShader() {
        return shader;
    }

    public void setShader(GLShaderProgram shader) {
        this.shader = shader;
    }

    public List<GLAbstractActor> getActorSequence() {
        return actorSequence;
    }

    public void setActorSequence(List<GLAbstractActor> actorSequence) {
        this.actorSequence = actorSequence;
    }

    public void dispose(GL4 gl) {
        for (GLAbstractActor actor : actorSequence) {
            actor.dispose(gl);
        }
        shader.dispose(gl);
    }

    public void disposeAndClearActorsOnly(GL4 gl) {
        for (GLAbstractActor actor : actorSequence) {
            actor.dispose(gl);
        }
        actorSequence.clear();
    }

    public void setApplyMemoryBarrier(boolean useBarrier) {
        this.applyMemoryBarrier=useBarrier;
    }


    public void init(GL4 gl) throws Exception {
        shader.init(gl);
        for (GLAbstractActor actor : actorSequence) {
            actor.init(gl);
        }
    }

    public void display(GL4 gl) {
        //logger.info("display() start - loading shader");
        shader.load(gl);
        //logger.info("display() - done loading shader");

        shader.display(gl);

        //logger.info("display() - done with shader.display()");

//        gl.glEnable(GL4.GL_DEPTH_TEST);
////        gl.glShadeModel(GL4.GL_SMOOTH);
////        gl.glDisable(GL4.GL_ALPHA_TEST);
////        gl.glAlphaFunc(GL4.GL_GREATER, 0.5f);
//        gl.glEnable(GL4.GL_BLEND);
//        gl.glBlendFunc(GL4.GL_SRC_ALPHA, GL4.GL_SRC_ALPHA);
//        gl.glBlendEquation(GL4.GL_FUNC_ADD);
//        gl.glDepthFunc(GL4.GL_LEQUAL);

        for (GLAbstractActor actor: actorSequence) {
            if (actor.isVisible()) {
                actor.display(gl);
            }
        }

//        //gl.glEnable(GL4.GL_DEPTH_TEST);
//        gl.glDisable(GL4.GL_BLEND);

        if (applyMemoryBarrier) {
            gl.glMemoryBarrier(GL4.GL_ALL_BARRIER_BITS);
        }

        //logger.info("display() - unloading shader");
        shader.unload(gl);
    }

}