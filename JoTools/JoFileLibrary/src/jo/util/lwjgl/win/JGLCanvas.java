package jo.util.lwjgl.win;

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import jo.util.jgl.enm.JGLColorMaterialFace;
import jo.util.jgl.enm.JGLColorMaterialMode;
import jo.util.jgl.enm.JGLFogMode;
import jo.util.jgl.obj.JGLScene;
import jo.vecmath.logic.Color4fLogic;
import jo.vecmath.logic.Matrix4fLogic;

import org.lwjgl.BufferUtils;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

@SuppressWarnings("serial")
public class JGLCanvas extends Canvas
{
    private JGLScene           mScene;

    private final IntBuffer mIB16;
    //private int mViewportX;
    //private int mViewportBottom;
    private int mWidth;
    private int mHeight;
    
    private boolean mCloseRequested = false;
    private AtomicReference<Dimension> mNewCanvasSize = new AtomicReference<Dimension>();
    
    private boolean[] mMouseState;
    private List<MouseListener>	mMouseListeners = new ArrayList<MouseListener>();
    private List<MouseMotionListener>	mMouseMotionListeners = new ArrayList<MouseMotionListener>();
    private List<MouseWheelListener>	mMouseWheelListeners = new ArrayList<MouseWheelListener>();

    public JGLCanvas()
    {
        mIB16 = BufferUtils.createIntBuffer(16);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e)
            { mNewCanvasSize.set(getSize()); }
         });
    }
    
    /**
     * <p>Queries the current view port size & position and updates all related
     * internal state.</p>
     *
     * <p>It is important that the internal state matches the OpenGL viewport or
     * clipping won't work correctly.</p>
     *
     * <p>This method should only be called when the viewport size has changed.
     * It can have negative impact on performance to call every frame.</p>
     * 
     * @see #getWidth()
     * @see #getHeight()
     */
    public void syncViewportSize() {
        mIB16.clear();
        GL11.glGetInteger(GL11.GL_VIEWPORT, mIB16);
        //mViewportX = mIB16.get(0);
        mWidth = mIB16.get(2);
        mHeight = mIB16.get(3);
        //mViewportBottom = mIB16.get(1) + mHeight;
    }
    private void init()
    {
        Thread t = new Thread("Render Thread") { public void run() { doRenderLoop(); } };
        t.start();
    }

    private void initFog()
    {
        GL11.glEnable(GL11.GL_FOG);
        GL11.glFogi(GL11.GL_FOG_MODE, conv(mScene.getFogMode()));
        if (!Matrix4fLogic.epsilonEquals(mScene.getFogDensity(), 1))
            GL11.glFogf(GL11.GL_FOG_DENSITY, (float)mScene.getFogDensity());
        if (!Matrix4fLogic.epsilonEquals(mScene.getFogStart(), 0))
            GL11.glFogf(GL11.GL_FOG_START, (float)mScene.getFogStart());
        if (!Matrix4fLogic.epsilonEquals(mScene.getFogEnd(), 1))
            GL11.glFogf(GL11.GL_FOG_END, (float)mScene.getFogEnd());
        if (!Matrix4fLogic.epsilonEquals(mScene.getFogIndex(), 0))
            GL11.glFogf(GL11.GL_FOG_INDEX, (float)mScene.getFogIndex());
        if (mScene.getFogColor() != null)
            GL11.glFog(GL11.GL_FOG_COLOR, Color4fLogic.toFloatBuffer(mScene.getFogColor()));
    }

    private void initMaterial()
    {
        int face = conv(mScene.getColorMaterialFace());
        GL11.glEnable(GL11.GL_COLOR_MATERIAL);
        if (mScene.getColorMaterialFace() != JGLColorMaterialFace.UNSET)
            GL11.glColorMaterial(face, conv(mScene.getColorMaterialMode()));
        if (mScene.getMaterialAmbient() != null)
            GL11.glMaterial(face, GL11.GL_AMBIENT, Color4fLogic.toFloatBuffer(mScene.getMaterialAmbient()));
        if (mScene.getMaterialDiffuse() != null)
            GL11.glMaterial(face, GL11.GL_DIFFUSE, Color4fLogic.toFloatBuffer(mScene.getMaterialDiffuse()));
        if (mScene.getMaterialSpecular() != null)
            GL11.glMaterial(face, GL11.GL_SPECULAR, Color4fLogic.toFloatBuffer(mScene.getMaterialSpecular()));
        if (mScene.getMaterialEmission() != null)
            GL11.glMaterial(face, GL11.GL_EMISSION, Color4fLogic.toFloatBuffer(mScene.getMaterialEmission()));
        if (mScene.getMaterialShininess() >= 0)
            GL11.glMaterialf(face, GL11.GL_SHININESS, mScene.getMaterialShininess());
    }

    private int conv(JGLFogMode fogMode)
    {
        switch (fogMode)
        {
            case UNSET:
                return -1;
            case LINEAR:
                return GL11.GL_LINEAR;
            case EXP:
                return GL11.GL_EXP;
            case EXP2:
                return GL11.GL_EXP2;
        }
        return -1;
    }

    private int conv(JGLColorMaterialFace colorMaterialFace)
    {
        switch (colorMaterialFace)
        {
            case UNSET:
                return -1;
            case FRONT:
                return GL11.GL_FRONT;
            case BACK:
                return GL11.GL_BACK;
            case FRONT_AND_BACK:
                return GL11.GL_FRONT_AND_BACK;
        }
        return -1;
    }

    private int conv(JGLColorMaterialMode colorMaterialMode)
    {
        switch (colorMaterialMode)
        {
            case UNSET:
                return -1;
            case EMISSION:
                return GL11.GL_EMISSION;
            case AMBIENT:
                return GL11.GL_AMBIENT;
            case DIFFUSE:
                return GL11.GL_DIFFUSE;
            case SPECULAR:
                return GL11.GL_SPECULAR;
            case AMBIENT_AND_DIFFUSE:
                return GL11.GL_AMBIENT_AND_DIFFUSE;
        }
        return -1;
    }

    private void doRenderLoop()
    {
        try {
        	while (!isDisplayable())
        		Thread.sleep(100);
            Display.setParent(this);
            Display.setVSyncEnabled(true);
            Display.create();
            mMouseState = new boolean[Mouse.getButtonCount()];
            for (int i = 0; i < mMouseState.length; i++)
            	mMouseState[i] = Mouse.isButtonDown(i);

            //GL11.glsetSwapInterval(1);
            GL11.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
            //gl11.glColor3f(1.0f, 0.0f, 0.0f);
            GL11.glHint(GL11.GL_PERSPECTIVE_CORRECTION_HINT, GL11.GL_NICEST);
            GL11.glClearDepth(1.0);
            GL11.glLineWidth(2);
            GL11.glEnable(GL11.GL_DEPTH_TEST);        
            if (mScene.getAmbientLight() != null)
            {
                GL11.glEnable(GL11.GL_LIGHTING);
                GL11.glLightModel(GL11.GL_LIGHT_MODEL_AMBIENT , Color4fLogic.toFloatBuffer(mScene.getAmbientLight()));
            }
            if (mScene.getColorMaterialFace() != JGLColorMaterialFace.UNSET)
                initMaterial();
            if (mScene.getFogMode() != JGLFogMode.UNSET)
                initFog();        

            Dimension newDim;
            
            while(!Display.isCloseRequested() && !mCloseRequested)
            {
               newDim = mNewCanvasSize.getAndSet(null);               
               if (newDim != null)
               {
                  GL11.glViewport(0, 0, newDim.width, newDim.height);
                  syncViewportSize();
               }
               doRender();
               doMouse();
               Display.update();
            }

            Display.destroy();
         } catch (Exception e) {
            e.printStackTrace();
         }        
    }
    
    private void doMouse()
    {
    	while (Mouse.next())
    	{
    		int		button = Mouse.getEventButton();
    		boolean	buttonState = Mouse.getEventButtonState();
    		int		dWheel = Mouse.getDWheel();
    		int		dX = Mouse.getEventDX();
    		int		dY = Mouse.getEventDY();
    		long	nanoseconds = Mouse.getEventNanoseconds();
    		int		x = Mouse.getEventX();
    		int		y = Mouse.getEventY();
    		int modifiers = 0;
    		if (button >= 0)
    		{
    			int jButton = (button == 0) ? MouseEvent.BUTTON1 : (button == 1) ? MouseEvent.BUTTON2 : MouseEvent.BUTTON3;
    			if (mMouseState[button] != buttonState)
    			{
    	    		System.out.println("Button="+button+", state="+buttonState+", x="+x+", y="+y);
	    			if (buttonState)
	    			{
	    				MouseEvent event = new MouseEvent(this, MouseEvent.MOUSE_PRESSED, nanoseconds, modifiers, x, y, 1, false, jButton);
	    				fireMouseEvent(event);
	    			}
	    			else
	    			{
	    				MouseEvent event = new MouseEvent(this, MouseEvent.MOUSE_RELEASED, nanoseconds, modifiers, x, y, 1, false, jButton);
	    				fireMouseEvent(event);
	    			}
	    			mMouseState[button] = buttonState;
    			}
    		}
			if ((dX > 0) || (dY > 0))
			{
    			int jButton = 0;
    			if (mMouseState[0])
    				jButton |= MouseEvent.BUTTON1;
    			if (mMouseState[1])
    				jButton |= MouseEvent.BUTTON2;
    			if (mMouseState[2])
    				jButton |= MouseEvent.BUTTON3;
    			if (jButton != 0)
    			{
    				MouseEvent event = new MouseEvent(this, MouseEvent.MOUSE_DRAGGED, nanoseconds, modifiers, x, y, 1, false, jButton);
    				fireMouseMoveEvent(event);
    			}
    			else
    			{
    				MouseEvent event = new MouseEvent(this, MouseEvent.MOUSE_MOVED, nanoseconds, modifiers, x, y, 1, false, jButton);
    				fireMouseMoveEvent(event);
    			}
			}
			if (dWheel != 0)
			{
				System.out.println("Wheel="+dWheel);
				MouseWheelEvent event = new MouseWheelEvent(this, MouseEvent.MOUSE_WHEEL, nanoseconds, modifiers, 
						x, y, 1, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, dWheel, dWheel/120);
				fireMouseWheelEvent(event);
			}
		}
    }

    private void doRender()
    {
        for (Runnable r : mScene.getBetweenRenderers())
            r.run();
        DrawLogic.draw(mWidth, mHeight, System.currentTimeMillis(), mScene);
    }

    public JGLScene getScene()
    {
        return mScene;
    }

    public void setScene(JGLScene scene)
    {
        if ((mScene != null) && (mScene != scene))
            throw new IllegalArgumentException("Cannot set a new scene!");
        mScene = scene;
        if (mScene != null)
            init();
    }

    public boolean isCloseRequested()
    {
        return mCloseRequested;
    }

    public void setCloseRequested(boolean closeRequested)
    {
        mCloseRequested = closeRequested;
    }
    
    @Override
    public synchronized void addMouseListener(MouseListener l)
    {
        if (System.getProperty("os.name").indexOf("Mac") >= 0)
            super.addMouseListener(l);
        else
    	    mMouseListeners.add(l);
    }
    
    @Override
    public synchronized void addMouseMotionListener(MouseMotionListener l)
    {
        if (System.getProperty("os.name").indexOf("Mac") >= 0)
            super.addMouseMotionListener(l);
        else
            mMouseMotionListeners.add(l);
    }
    
    @Override
    public synchronized void addMouseWheelListener(MouseWheelListener l)
    {
        if (System.getProperty("os.name").indexOf("Mac") >= 0)
            super.addMouseWheelListener(l);
        else
            mMouseWheelListeners.add(l);
    }
    
    @Override
    public synchronized void removeMouseListener(MouseListener l)
    {
    	mMouseListeners.remove(l);
    }
    
    @Override
    public synchronized void removeMouseMotionListener(MouseMotionListener l)
    {
    	mMouseMotionListeners.remove(l);
    }
    
    @Override
    public synchronized void removeMouseWheelListener(MouseWheelListener l)
    {
    	mMouseWheelListeners.remove(l);
    }
    
    private void fireMouseEvent(MouseEvent e)
    {
    	for (MouseListener l : mMouseListeners)
    		if (e.getID() == MouseEvent.MOUSE_PRESSED)
    			l.mousePressed(e);
    		else if (e.getID() == MouseEvent.MOUSE_RELEASED)
    			l.mouseReleased(e);
    }
    
    private void fireMouseMoveEvent(MouseEvent e)
    {
    	for (MouseMotionListener l : mMouseMotionListeners)
    		if (e.getID() == MouseEvent.MOUSE_MOVED)
    			l.mouseMoved(e);
    		else if (e.getID() == MouseEvent.MOUSE_DRAGGED)
    			l.mouseDragged(e);
    }

    private void fireMouseWheelEvent(MouseWheelEvent e)
    {
    	for (MouseWheelListener l : mMouseWheelListeners)
    		if (e.getID() == MouseEvent.MOUSE_WHEEL)
    			l.mouseWheelMoved(e);
    }
}