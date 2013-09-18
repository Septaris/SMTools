package jo.sm.logic.utils;

public class DebugLogic
{
    public static boolean DEBUG = false;
    
    private static String mIndent = "";

	public static final boolean	HULL_ONLY = false;

    public static void indent()
    {
        mIndent += "  ";
    }
    
    public static void outdent()
    {
        mIndent = mIndent.substring(2);
    }
    
    public static void debug(String msg)
    {
        if (DEBUG)
            System.out.println(mIndent+msg);
    }

}