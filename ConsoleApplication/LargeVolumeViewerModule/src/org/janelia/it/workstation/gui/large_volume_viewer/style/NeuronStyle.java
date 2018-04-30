package org.janelia.it.workstation.gui.large_volume_viewer.style;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.awt.*;

/**
 * This class encapsulates the visual draw style for a particular neuron;
 * we'll start with color and visibility, and perhaps later add other
 * things like line width, etc.
 */
public class NeuronStyle {
    private Color color;
    private boolean visible = true;
    private boolean nonInteractable = false;
    private boolean userToggleRadius = false;
    float[] cachedColorArray=new float[3]; // needed for performance

    // constants, because I already used "visible" instead of "visibility" once...
    private static final String COLOR_KEY = "color";
    private static final String VISIBILITY_KEY = "visibility";
    private static final String NONINTERACTABLE_KEY = "readonly";

    // default colors; we index into this list with neuron ID;
    //  note that our neuron IDs are all of the form 8*n+4,
    //  so make sure the length of this list is mutually prime,
    //  so we can maximize the color distribution
    private static Color[] neuronColors = {
        Color.red,
        Color.blue,
        Color.green,
        Color.magenta,
        Color.cyan,
        Color.yellow,
        Color.white,
        // I need more colors!  (1, 0.5, 0) and permutations:
        new Color(1.0f, 0.5f, 0.0f),
        new Color(0.0f, 0.5f, 1.0f),
        new Color(0.0f, 1.0f, 0.5f),
        new Color(1.0f, 0.0f, 0.5f),
        new Color(0.5f, 0.0f, 1.0f),
        new Color(0.5f, 1.0f, 0.0f)
    };

    /**
     * get a default style for a neuron
     */
    public static NeuronStyle getStyleForNeuron(Long neuronID) {
        return new NeuronStyle(neuronColors[(int) (neuronID % neuronColors.length)], true, false);
    }

    public static NeuronStyle getStyleForNeuron(Long neuronID, boolean visible, boolean noninteractable) {
        return new NeuronStyle(neuronColors[(int) (neuronID % neuronColors.length)], visible, noninteractable);
    }

    /**
     * given a json object, return a NeuronStyle; expected to
     * be in form {"color", [R, G, B in 0-255], "visibility": true/false}
     *
     * returns null if it can't parse the input JSON node
     *
     * @param rootNode
     * @return
     */
    public static NeuronStyle fromJSON(ObjectNode rootNode) {
        JsonNode colorNode = rootNode.path(COLOR_KEY);
        if (colorNode.isMissingNode() || !colorNode.isArray()) {
            return null;
        }
        Color color = new Color(colorNode.get(0).asInt(), colorNode.get(1).asInt(),
                colorNode.get(2).asInt());

        JsonNode visibilityNode = rootNode.path(VISIBILITY_KEY);
        if (visibilityNode.isMissingNode() || !visibilityNode.isBoolean()) {
            return null;
        }
        boolean visibility = visibilityNode.asBoolean();
        
        JsonNode nonInteractableNode = rootNode.path(NONINTERACTABLE_KEY);
        if (nonInteractableNode.isMissingNode() || !nonInteractableNode.isBoolean()) {
              return null;
        }
        boolean  nonInteractable = nonInteractableNode.asBoolean();

        return new NeuronStyle(color, visibility, nonInteractable);
    }

    public NeuronStyle() {
        setColor(NeuronStyle.neuronColors[2]);
        this.visible = true;
    }

    public NeuronStyle(Color color, boolean visible, boolean nonInteractable) {
        setColor(color);
        this.visible = visible;
        this.nonInteractable = nonInteractable;
    }

    public float getRedAsFloat() {
        return cachedColorArray[0];
    }

    public float getGreenAsFloat() {
        return cachedColorArray[1];
    }

    public float getBlueAsFloat() {
        return cachedColorArray[2];
    }

    public Color getColor() {
        return color;
    }

    public float[] getColorAsFloatArray() {
        return cachedColorArray;
    }

    public void setColor(Color color) {
        cachedColorArray[0]=color.getRed() / 255.0f;
        cachedColorArray[1]=color.getGreen() / 255.0f;
        cachedColorArray[2]=color.getBlue() / 255.0f;
        this.color = color;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    

    /**
     * returns a json node object, to be used in persisting styles; the
     * node will be aggregated with others before converting to string
     */
    public ObjectNode asJSON() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();

        ArrayNode colors = mapper.createArrayNode();
        colors.add(getColor().getRed());
        colors.add(getColor().getGreen());
        colors.add(getColor().getBlue());
        rootNode.put(COLOR_KEY, colors);

        rootNode.put(VISIBILITY_KEY, isVisible());
        rootNode.put(NONINTERACTABLE_KEY, isNonInteractable());

        return rootNode;
    }

    @Override
    public String toString() {
        return "NeuronStyle(" + color + ", visibility: " + visible + ", nonInteractable: " + nonInteractable + ")";
    }

    /**
     * @return the readOnly
     */
    public boolean isNonInteractable() {
        return nonInteractable;
    }

    /**
     * @param nonInteractable the nonInteractable to set
     */
    public void setNonInteractable(boolean nonInteractable) {
        this.nonInteractable = nonInteractable;
    }

    /**
     * @return the userToggleRadius
     */
    public boolean isUserToggleRadius() {
        return userToggleRadius;
    }

    /**
     * @param userToggleRadius the userToggleRadius to set
     */
    public void setUserToggleRadius(boolean userToggleRadius) {
        this.userToggleRadius = userToggleRadius;
    }
    
    public void setProperty(String property, boolean toggle) {
        switch (property) {
            case "Radius": 
                this.userToggleRadius = toggle;
                break;
            case "Background":
                this.nonInteractable = toggle;
                break;
            case "Visibility":
                this.visible = toggle;
                break;
        }
    }
}
