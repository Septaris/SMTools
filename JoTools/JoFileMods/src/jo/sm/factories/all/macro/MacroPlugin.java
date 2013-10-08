package jo.sm.factories.all.macro;

import jo.sm.data.SparseMatrix;
import jo.sm.data.StarMade;
import jo.sm.mods.IBlocksPlugin;
import jo.sm.mods.IPluginCallback;
import jo.sm.ship.data.Block;

public class MacroPlugin implements IBlocksPlugin
{
	private MacroDefinition	mDef;
	
	public MacroPlugin(MacroDefinition def)
	{
		mDef = def;
	}

	@Override
	public String getName()
	{
		return mDef.getTitle();
	}

	@Override
	public String getDescription()
	{
		return mDef.getDescription();
	}

	@Override
	public String getAuthor()
	{
		return mDef.getAuthor();
	}

	@Override
	public Object newParameterBean()
	{
		return null;
	}
	@Override
	public void initParameterBean(SparseMatrix<Block> original, Object params,
			StarMade sm, IPluginCallback cb)
	{
	}

	@Override
	public int[][] getClassifications()
	{
		return mDef.getClassifications();
	}

	@Override
	public SparseMatrix<Block> modify(SparseMatrix<Block> original,
			Object params, StarMade sm, IPluginCallback cb)
	{
        return null;
	}
}
