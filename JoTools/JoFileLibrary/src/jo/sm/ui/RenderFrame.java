package jo.sm.ui;

import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.util.Properties;

import javax.imageio.ImageIO;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import jo.sm.logic.RunnableLogic;
import jo.sm.logic.StarMadeLogic;
import jo.sm.mods.IBlocksPlugin;
import jo.sm.mods.IPluginCallback;
import jo.sm.mods.IRunnableWithProgress;
import jo.sm.ui.act.edit.RedoAction;
import jo.sm.ui.act.edit.UndoAction;
import jo.sm.ui.act.file.ExportImagesAction;
import jo.sm.ui.act.file.OpenExistingAction;
import jo.sm.ui.act.file.OpenFileAction;
import jo.sm.ui.act.file.QuitAction;
import jo.sm.ui.act.file.SaveAction;
import jo.sm.ui.act.file.SaveAsBlueprintAction;
import jo.sm.ui.act.file.SaveAsFileAction;
import jo.sm.ui.act.view.AxisAction;
import jo.sm.ui.act.view.FilterMissileDumbAction;
import jo.sm.ui.act.view.FilterMissileFafoAction;
import jo.sm.ui.act.view.FilterMissileHeatAction;
import jo.sm.ui.act.view.FilterNoneAction;
import jo.sm.ui.act.view.FilterPowerAction;
import jo.sm.ui.act.view.FilterRepairAction;
import jo.sm.ui.act.view.FilterSalvageAction;
import jo.sm.ui.act.view.FilterThrusterAction;
import jo.sm.ui.act.view.FilterWeaponsAction;
import jo.sm.ui.act.view.PlainAction;
import jo.sm.ui.logic.MenuLogic;
import jo.sm.ui.logic.ShipSpec;
import jo.sm.ui.logic.ShipTreeLogic;
import jo.sm.ui.lwjgl.LWJGLRenderPanel;

@SuppressWarnings("serial")
public class RenderFrame extends JFrame implements WindowListener
{
    private String[]    mArgs;
    private ShipSpec    mSpec;
    private RenderPanel mClient;

    public RenderFrame(String[] args)
    {
        super("SMEdit");
        mArgs = args;
        // instantiate
        JMenuBar menuBar = new JMenuBar();
        JMenu menuFile = new JMenu("File");
        JMenu menuEdit = new JMenu("Edit");
        JMenu menuView = new JMenu("View");
        JMenu menuModify = new JMenu("Modify");
        JMenu menuViewMissiles = new JMenu("Missiles");
        if ((mArgs.length > 0) && (mArgs[0].equals("-opengl")))
            mClient = new LWJGLRenderPanel();
        else
            mClient = new AWTRenderPanel();
        // layout
        setJMenuBar(menuBar);
        menuBar.add(menuFile);
        menuFile.add(new OpenExistingAction(this));
        menuFile.add(new OpenFileAction(this));
        menuFile.add(new SaveAction(this));
        JMenu saveAs = new JMenu("Save As");
        menuFile.add(saveAs);
        saveAs.add(new SaveAsBlueprintAction(this, false));
        saveAs.add(new SaveAsBlueprintAction(this, true));
        saveAs.add(new SaveAsFileAction(this));
        menuFile.add(new ExportImagesAction(this));
        menuFile.add(new QuitAction(this));
        menuBar.add(menuEdit);
        menuEdit.add(new UndoAction(this));
        menuEdit.add(new RedoAction(this));
        menuBar.add(menuView);
        menuView.add(new JCheckBoxMenuItem(new PlainAction(this)));
        menuView.add(new JCheckBoxMenuItem(new AxisAction(this)));
        menuView.add(new FilterNoneAction(this));
        menuView.add(new FilterPowerAction(this));
        menuView.add(new FilterThrusterAction(this));
        menuView.add(new FilterRepairAction(this));
        menuView.add(new FilterSalvageAction(this));
        menuView.add(new FilterWeaponsAction(this));
        menuView.add(menuViewMissiles);
        menuViewMissiles.add(new FilterMissileDumbAction(this));
        menuViewMissiles.add(new FilterMissileHeatAction(this));
        menuViewMissiles.add(new FilterMissileFafoAction(this));
        menuBar.add(menuModify);
        getContentPane().add(BorderLayout.WEST, new EditPanel(mClient));
        getContentPane().add(BorderLayout.CENTER, mClient);
        getContentPane().add(BorderLayout.SOUTH, new StatusPanel());
        // link
        menuModify.addMenuListener(new MenuListener() {            
            @Override
            public void menuSelected(MenuEvent ev)
            {
                updateModify((JMenu)ev.getSource());
            }            
            @Override
            public void menuDeselected(MenuEvent e)
            {
            }            
            @Override
            public void menuCanceled(MenuEvent e)
            {
            }
        });
        menuEdit.addMenuListener(new MenuListener() {            
            @Override
            public void menuSelected(MenuEvent ev)
            {
                updateEdit((JMenu)ev.getSource());
            }            
            @Override
            public void menuDeselected(MenuEvent e)
            {
            }            
            @Override
            public void menuCanceled(MenuEvent e)
            {
            }
        });        

        this.addWindowListener(this);
        this.addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent e)
            { mClient.requestFocusInWindow(); }
         });
        setSize(1024, 768);
        Image icon;
		try
		{
			icon = ImageIO.read(getClass().getResourceAsStream("icon64.png"));
	        setIconImage(icon);
		} catch (IOException e1)
		{
			e1.printStackTrace();
		}
		if (mClient instanceof Runnable)
		{
		    Thread t = new Thread((Runnable)mClient);
		    t.start();
		}
    }

    public void windowClosing(WindowEvent evt)
    {
        this.setVisible(false);
        this.dispose();
        System.exit(0);
    }

    public void windowOpened(WindowEvent evt)
    {
    }

    public void windowClosed(WindowEvent evt)
    {
    }

    public void windowIconified(WindowEvent evt)
    {
    }

    public void windowDeiconified(WindowEvent evt)
    {
    }

    public void windowActivated(WindowEvent evt)
    {
    }

    public void windowDeactivated(WindowEvent evt)
    {
    }

    private void updateModify(JMenu modify)
    {
        MenuLogic.clearPluginMenus(modify);
        if (mSpec == null)
            return;
        int type = mSpec.getClassification();
        int modCount = MenuLogic.addPlugins(mClient, modify, type, IBlocksPlugin.SUBTYPE_MODIFY);
        int lastModIndex = modify.getItemCount();
        int genCount = MenuLogic.addPlugins(mClient, modify, type, IBlocksPlugin.SUBTYPE_GENERATE);
        if (modCount > 0 && genCount > 0)
        {
            JSeparator sep = new JSeparator();
            sep.setToolTipText("plugin");
            modify.add(sep, lastModIndex);
        }
    }

    private void updateEdit(JMenu edit)
    {
        MenuLogic.clearPluginMenus(edit);
        if (mSpec == null)
            return;
        int type = mSpec.getClassification();
        MenuLogic.addPlugins(mClient, edit, type, IBlocksPlugin.SUBTYPE_EDIT);
    }
    
    private static void preLoad()
    {
        Properties props = StarMadeLogic.getProps();
        String home = props.getProperty("starmade.home", "");
        if (!StarMadeLogic.isStarMadeDirectory(home))
        {
            home = System.getProperty("user.dir");
            if (!StarMadeLogic.isStarMadeDirectory(home))
            {
                home = JOptionPane.showInputDialog(null, "Enter in the home directory for StarMade", home);
                if (home == null)
                    System.exit(0);
            }
            props.put("starmade.home", home);
            StarMadeLogic.saveProps();
        }
        StarMadeLogic.setBaseDir(home);
    }

    public static void main(String[] args)
    {
        preLoad();
        final RenderFrame f = new RenderFrame(args);
        f.setVisible(true);
        final ShipSpec spec = ShipTreeLogic.getBlueprintSpec("Isanth-VI", true);
        if (spec != null)
        {
        	IRunnableWithProgress t = new IRunnableWithProgress() {				
				@Override
				public void run(IPluginCallback cb)
				{
		        	f.setSpec(spec);
		        	f.getClient().setGrid(ShipTreeLogic.loadShip(spec, cb));
				}
			};
			RunnableLogic.run(f, "Loading...", t);
        }
    }

    public ShipSpec getSpec()
    {
        return mSpec;
    }

    public void setSpec(ShipSpec spec)
    {
        mSpec = spec;
        mClient.setSpec(mSpec);
    }

    public RenderPanel getClient()
    {
        return mClient;
    }

    public void setClient(RenderPanel client)
    {
        mClient = client;
    }
}
