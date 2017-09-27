package org.janelia.it.workstation.ab2.renderer;

import java.nio.IntBuffer;

import javax.media.opengl.GL4;
import javax.media.opengl.glu.GLU;

import org.janelia.it.workstation.ab2.gl.GLActorUpdateCallback;
import org.janelia.it.workstation.ab2.gl.GLShaderActionSequence;
import org.janelia.it.workstation.ab2.gl.GLShaderProgram;
import org.janelia.it.workstation.ab2.gl.GLShaderUpdateCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AB23DRenderer implements AB2Renderer3DControls {
    Logger logger = LoggerFactory.getLogger(AB23DRenderer.class);
    protected static GLU glu = new GLU();

    protected IntBuffer pickFramebufferId;
    protected IntBuffer pickColorTextureId;
    protected IntBuffer pickDepthTextureId;

    protected GLShaderActionSequence drawActionSequence=new GLShaderActionSequence(AB2Basic3DRenderer.class.getName()+"_DRAW");
    protected GLShaderActionSequence pickActionSequence=new GLShaderActionSequence( AB2Basic3DRenderer.class.getName()+"_PICK");

    protected final GLShaderProgram drawShader;
    protected final GLShaderProgram pickShader;

    public AB23DRenderer(GLShaderProgram drawShader, GLShaderProgram pickShader) {
        this.drawShader=drawShader;
        this.pickShader=pickShader;
    }

    protected void checkGlError(GL4 gl, String message) {
        int errorNumber = gl.glGetError();
        if (errorNumber <= 0)
            return;
        String errorStr = glu.gluErrorString(errorNumber);
        logger.error( "OpenGL Error " + errorNumber + ": " + errorStr + ": " + message );
    }

    public void init(GL4 gl) {
        initSync(gl);
    }

    protected synchronized void initSync(GL4 gl) {
        try {

            drawActionSequence.setShader(drawShader);
            drawActionSequence.setApplyMemoryBarrier(false);
            drawActionSequence.init(gl);

            pickActionSequence.setShader(pickShader);
            pickActionSequence.setApplyMemoryBarrier(false);
            pickActionSequence.init(gl);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    protected abstract GLShaderUpdateCallback getDrawShaderUpdateCallback();

    protected abstract GLActorUpdateCallback getActorSequenceDrawUpdateCallback();

    protected abstract GLShaderUpdateCallback getPickShaderUpdateCallback();

    protected abstract GLActorUpdateCallback getActorSequencePickUpdateCallback();

    public void dispose(GL4 gl) {
        drawActionSequence.dispose(gl);
        pickActionSequence.dispose(gl);
    }

    public abstract void display(GL4 gl);

    public abstract void reshape(GL4 gl, int x, int y, int width, int height);

//    public int getPickBufferAtXY(int x, int y, boolean invertY) {
//        int fX=x;
//        int fY=y;
//        if (invertY) {
//            fY=viewport.getHeightPixels()-y-1;
//            if (fY<0) { fY=0; }
//        }
//        gl.glBindFramebuffer(GL4.GL_READ_FRAMEBUFFER, frameBufId);
//        int yPos = viewportHeight - y;
//        byte[] pixels = readPixels(gl, colorTextureId_1, GL4.GL_COLOR_ATTACHMENT1, x, yPos, 1, 1);
//        int id = getId(pixels);
//        gl.glBindFramebuffer(GL3.GL_FRAMEBUFFER, 0);
//    }

    protected void disposePickFramebuffer(GL4 gl) {
        if (pickDepthTextureId!=null) {
            gl.glDeleteTextures(1, pickDepthTextureId);
            pickDepthTextureId=null;
        }
        if (pickColorTextureId!=null) {
            gl.glDeleteTextures(1, pickColorTextureId);
            pickColorTextureId=null;
        }
        if (pickFramebufferId!=null) {
            gl.glDeleteFramebuffers(1, pickFramebufferId);
            pickFramebufferId=null;
        }
    }

    protected void resetPickFramebuffer(GL4 gl, int width, int height) {
        disposePickFramebuffer(gl);

        pickFramebufferId=IntBuffer.allocate(1);
        gl.glGenFramebuffers(1, pickFramebufferId);
        gl.glBindFramebuffer(gl.GL_FRAMEBUFFER, pickFramebufferId.get(0));

        pickColorTextureId=IntBuffer.allocate(1);
        gl.glGenTextures(1, pickColorTextureId);
        gl.glBindTexture(gl.GL_TEXTURE_2D, pickColorTextureId.get(0));
        gl.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MIN_FILTER, GL4.GL_NEAREST);
        gl.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MAG_FILTER, GL4.GL_NEAREST);
        gl.glTexImage2D(GL4.GL_TEXTURE_2D, 0, GL4.GL_R32I, width, height, 0, GL4.GL_RED_INTEGER, GL4.GL_INT, null);

        pickDepthTextureId=IntBuffer.allocate(1);
        gl.glGenTextures(1, pickDepthTextureId);
        gl.glBindTexture(gl.GL_TEXTURE_2D, pickDepthTextureId.get(0));
        gl.glTexImage2D(GL4.GL_TEXTURE_2D, 0, GL4.GL_DEPTH_COMPONENT, width, height, 0, GL4.GL_DEPTH_COMPONENT, GL4.GL_FLOAT, null);

        gl.glFramebufferTexture(gl.GL_FRAMEBUFFER, gl.GL_COLOR_ATTACHMENT0, pickColorTextureId.get(0), 0);
        gl.glFramebufferTexture(gl.GL_FRAMEBUFFER, gl.GL_DEPTH_ATTACHMENT, pickDepthTextureId.get(0), 0);

        gl.glBindFramebuffer(gl.GL_FRAMEBUFFER, 0);
        gl.glBindTexture(gl.GL_TEXTURE_2D, 0);

        int status = gl.glCheckFramebufferStatus(GL4.GL_FRAMEBUFFER);
        if (status != GL4.GL_FRAMEBUFFER_COMPLETE) {
            logger.error("Failed to establish framebuffer: {}", decodeFramebufferStatus(status));
        }
        else {
            logger.info("Picking Framebuffer complete.");
        }
    }

    private String decodeFramebufferStatus( int status ) {
        String rtnVal = null;
        switch (status) {
            case GL4.GL_FRAMEBUFFER_UNDEFINED:
                rtnVal = "GL_FRAMEBUFFER_UNDEFINED means target is the default framebuffer, but the default framebuffer does not exist.";
                break;
            case GL4.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT :
                rtnVal = "GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT is returned if any of the framebuffer attachment points are framebuffer incomplete.";
                break;
            case GL4.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:
                rtnVal = "GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT is returned if the framebuffer does not have at least one image attached to it.";
                break;
            case GL4.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER:
                rtnVal = "GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER is returned if the value of GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE\n" +
                        "		 is GL_NONE for any color attachment point(s) named by GL_DRAW_BUFFERi.";
                break;
            case GL4.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER:
                rtnVal = "GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER is returned if GL_READ_BUFFER is not GL_NONE\n" +
                        "		 and the value of GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE is GL_NONE for the color attachment point named\n" +
                        "		 by GL_READ_BUFFER.";
                break;
            case GL4.GL_FRAMEBUFFER_UNSUPPORTED:
                rtnVal = "GL_FRAMEBUFFER_UNSUPPORTED is returned if the combination of internal formats of the attached images violates\n" +
                        "		 an implementation-dependent set of restrictions.";
                break;
            case GL4.GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE:
                rtnVal = "GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE is returned if the value of GL_RENDERBUFFER_SAMPLES is not the same\n" +
                        "		 for all attached renderbuffers; if the value of GL_TEXTURE_SAMPLES is the not same for all attached textures; or, if the attached\n" +
                        "		 images are a mix of renderbuffers and textures, the value of GL_RENDERBUFFER_SAMPLES does not match the value of\n" +
                        "		 GL_TEXTURE_SAMPLES.  GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE  also returned if the value of GL_TEXTURE_FIXED_SAMPLE_LOCATIONS is\n" +
                        "		 not the same for all attached textures; or, if the attached images are a mix of renderbuffers and textures, the value of GL_TEXTURE_FIXED_SAMPLE_LOCATIONS\n" +
                        "		 is not GL_TRUE for all attached textures.";
                break;
            case GL4.GL_FRAMEBUFFER_INCOMPLETE_LAYER_TARGETS:
                rtnVal = " is returned if any framebuffer attachment is layered, and any populated attachment is not layered,\n" +
                        "		 or if all populated color attachments are not from textures of the same target.";
                break;
            default:
                rtnVal = "--Message not decoded: " + status;
        }
        return rtnVal;
    }

}
