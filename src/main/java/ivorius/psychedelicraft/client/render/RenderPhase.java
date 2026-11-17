package ivorius.psychedelicraft.client.render;

/** Render-thread scopes keep world effects out of native GUI and post-processing draws. */
public enum RenderPhase {
    NORMAL, WORLD, MODEL, SKY, END_SKY, STARS, CLOUDS, HAND, SCREEN, BORDER, POST;
    private static RenderPhase current=NORMAL;
    public static RenderPhase current(){return current;}
    public Scope enter(){var previous=current;current=this;return new Scope(previous);}
    public static Scope model(){return (current==WORLD?MODEL:current).enter();}
    public static void reset(){current=NORMAL;}
    public boolean active(){return this!=NORMAL&&this!=POST;}
    public boolean deforms(){return this==WORLD||this==MODEL||this==CLOUDS||this==HAND||this==BORDER;}
    public static final class Scope implements AutoCloseable {
        private final RenderPhase previous;private boolean closed;
        private Scope(RenderPhase previous){this.previous=previous;}
        @Override public void close(){if(!closed){current=previous;closed=true;}}
    }
}
