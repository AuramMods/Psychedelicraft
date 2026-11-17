package ivorius.psychedelicraft.client.render.shader;

/** Accepted half-tick history cadence and donor slot-based weights. Independent of frame rate. */
public final class MotionTimeline {
    public static final int SAMPLES=30;
    private final float[] times=new float[SAMPLES];
    private int latest=-1;
    private float lastCapture;
    public MotionTimeline(){reset();}
    public void update(float ticks,Capture capture){
        if(ticks<lastCapture)reset();
        if(latest<0)capture(ticks,capture);
        else {int intervals=(int)Math.floor((ticks-lastCapture)/.5F);float finalCapture=lastCapture+intervals*.5F;for(int n=Math.min(intervals,SAMPLES)-1;n>=0;n--)capture(finalCapture-n*.5F,capture);}
    }
    private void capture(float time,Capture action){latest=(latest+1)%SAMPLES;times[latest]=time;lastCapture=time;action.capture(latest);}
    public int index(int offset){return latest<0?-1:(latest+offset)%SAMPLES;}
    public boolean visible(int index,float ticks){return latest>=0&&Float.isFinite(times[index])&&ticks>=times[index]&&ticks-times[index]<15;}
    public float opacity(int index,float strength){return Math.min(1,(index+1)*.002F*strength);}
    public void reset(){latest=-1;lastCapture=0;java.util.Arrays.fill(times,Float.NaN);}
    public boolean empty(){return latest<0;}
    @FunctionalInterface public interface Capture {void capture(int index);}
}
