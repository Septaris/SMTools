package jo.sm.plugins.ship.fill;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jo.sm.data.BlockTypes;
import jo.sm.data.SparseMatrix;
import jo.sm.mods.IBlocksPlugin;
import jo.sm.ship.data.Block;
import jo.vecmath.Point3i;

public class FillPlugin implements IBlocksPlugin
{
    public static final String NAME = "Fill";
    public static final String DESC = "Autofill Ship Interior";
    public static final String AUTH = "Jo Jaquinta";
    public static final int[][] CLASSIFICATIONS = 
        {
        { TYPE_SHIP, SUBTYPE_MODIFY },
        };

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public String getDescription()
    {
        return DESC;
    }

    @Override
    public String getAuthor()
    {
        return AUTH;
    }

    @Override
    public Object getParameterBean()
    {
        return new FillParameters();
    }

    @Override
    public int[][] getClassifications()
    {
        return CLASSIFICATIONS;
    }

    @Override
    public SparseMatrix<Block> modify(SparseMatrix<Block> original,
            Object p)
    {
        FillParameters params = (FillParameters)p;    
        SparseMatrix<Block> modified = new SparseMatrix<Block>();
        List<Point3i> interior = new ArrayList<Point3i>();
        Point3i lower = new Point3i();
        Point3i upper = new Point3i();
        original.getBounds(lower, upper);
        scopeInterior(original, modified, interior, lower, upper);
        int interiorSize = interior.size();
        int oneHundredPercent = params.getEmpty() + params.getMissileDumb() + params.getMissileFafo()
                + params.getMissileHeat() + params.getPower() + params.getSalvage()
                + params.getShield() + params.getThrusters() + params.getWeapon();
        fill(modified, interior, params.getThrusters()*interiorSize/oneHundredPercent, (short)-1, BlockTypes.THRUSTER_ID,
                new FillStrategy(FillStrategy.MINUS, FillStrategy.Z));
        
        return modified;
    }

    private void fill(SparseMatrix<Block> modified, List<Point3i> interior,
            int numBlocks, short controllerID, short blockID, FillStrategy fillStrategy)
    {
        Collections.sort(interior, fillStrategy);
        if ((controllerID > 0) && (interior.size() > 0))
            place(modified, interior, controllerID);
        while ((numBlocks-- > 0) && (interior.size() > 0))
            place(modified, interior, blockID);
    }
    
    private void place(SparseMatrix<Block> modified, List<Point3i> interior, short blockID)
    {
        Block b = new Block();
        b.setBlockID(blockID);
        b.setHitPoints((short)100);
        Point3i p = interior.get(0);
        interior.remove(0);
        modified.set(p,  b);
    }

    private void scopeInterior(SparseMatrix<Block> original,
            SparseMatrix<Block> modified, List<Point3i> interior,
            Point3i lower, Point3i upper)
    {
        for (int x = lower.x; x <= upper.x; x++)
            for (int y = lower.y; y <= upper.y; y++)
            {
                int bottom = findBottom(original, x, y, lower.z, upper.z);
                int top = findTop(original, x, y, lower.z, upper.z);
                if (bottom > top)
                    continue; // no blocks;
                for (int z = bottom; z <= top; z++)
                {
                    Point3i xyz = new Point3i(x, y, z);
                    if (original.contains(xyz))
                        modified.set(xyz, original.get(xyz));
                    else
                        interior.add(xyz);
                }
            }
    }

    private int findBottom(SparseMatrix<Block> grid, int x, int y, int lowZ, int highZ)
    {
        for (int z = lowZ; z <= highZ; z++)
            if (grid.contains(x, y, z))
                return z;
        return highZ + 1;
    }

    private int findTop(SparseMatrix<Block> grid, int x, int y, int lowZ, int highZ)
    {
        for (int z = highZ; z >= lowZ; z--)
            if (grid.contains(x, y, z))
                return z;
        return lowZ - 1;
    }
}