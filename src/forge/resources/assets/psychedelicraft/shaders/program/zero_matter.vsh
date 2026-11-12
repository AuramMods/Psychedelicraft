#version 120

varying vec4 screenProjection;
varying vec4 vertexColor;

void main() {
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
    vertexColor = gl_Color;
    screenProjection = vec4((gl_Position.xy + gl_Position.ww) * 0.5, gl_Position.zw);
}
