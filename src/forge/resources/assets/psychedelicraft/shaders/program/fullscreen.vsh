#version 120

uniform vec2 TextureScale;
varying vec2 texCoord;

void main() {
    gl_Position = gl_Vertex;
    texCoord = (gl_Vertex.xy * 0.5 + 0.5) * TextureScale;
}
