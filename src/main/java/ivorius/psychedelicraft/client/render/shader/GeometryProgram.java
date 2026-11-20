package ivorius.psychedelicraft.client.render.shader;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;

import java.util.HashMap;
import java.util.Map;

/** Uniform locations owned by one renderer program. Set values only while it is bound. */
public final class GeometryProgram {
    private final int program;
    private final Map<String, Integer> locations = new HashMap<>();

    public GeometryProgram(int program) {
        this.program = program;
    }

    private int location(String name) {
        return locations.computeIfAbsent(name, uniform -> GL20.glGetUniformLocation(program, uniform));
    }

    boolean hasGeometry() {
        return location("PS_Enabled") >= 0;
    }

    void set(String name, float... values) {
        int location = location(name);
        if (location < 0) return;
        switch (values.length) {
            case 1 -> GL20.glUniform1f(location, values[0]);
            case 3 -> GL20.glUniform3f(location, values[0], values[1], values[2]);
            case 4 -> GL20.glUniform4f(location, values[0], values[1], values[2], values[3]);
            default -> throw new IllegalArgumentException(name);
        }
    }

    void setViewToWorld(Matrix4f value) {
        int location = location("PS_ViewToWorld");
        if (location < 0) return;
        try (var stack = MemoryStack.stackPush()) {
            GL20.glUniformMatrix4fv(location, false, value.get(stack.mallocFloat(16)));
        }
    }
}
