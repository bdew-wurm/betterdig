package net.bdew.wurm.betterdig;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.Configurable;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.PreInitable;
import org.gotti.wurmunlimited.modloader.interfaces.WurmMod;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BetterDigMod implements WurmMod, Initable, PreInitable, Configurable {
    private static final Logger logger = Logger.getLogger("BetterDigMod");

    static int overrideClayWeight = 20;
    static boolean digToVehicle = true;
    static boolean dredgeToShip = true;
    static boolean levelToVehicle = true;
    static boolean digToCrates = true;
    static boolean digToDragged = true;
    static boolean levelToDragged = true;
    static boolean levelFromVehicle = true;
    static boolean levelFromDragged = true;
    static boolean levelFromCrates = true;
    static boolean levelFromGround = true;

    public static void logException(String msg, Throwable e) {
        if (logger != null)
            logger.log(Level.SEVERE, msg, e);
    }

    public static void logWarning(String msg) {
        if (logger != null)
            logger.log(Level.WARNING, msg);
    }

    public static void logInfo(String msg) {
        if (logger != null)
            logger.log(Level.INFO, msg);
    }

    @Override
    public void configure(Properties properties) {
        overrideClayWeight = Integer.parseInt(properties.getProperty("overrideClayWeight", "-1"), 10);
        digToVehicle = Boolean.parseBoolean(properties.getProperty("digToVehicle", "false"));
        dredgeToShip = Boolean.parseBoolean(properties.getProperty("dredgeToShip", "false"));
        levelToVehicle = Boolean.parseBoolean(properties.getProperty("levelToVehicle", "false"));
        digToCrates = Boolean.parseBoolean(properties.getProperty("digToCrates", "false"));
        digToDragged = Boolean.parseBoolean(properties.getProperty("digToDragged", "false"));
        levelToDragged = Boolean.parseBoolean(properties.getProperty("levelToDragged", "false"));
        levelFromVehicle = Boolean.parseBoolean(properties.getProperty("levelFromVehicle", "false"));
        levelFromDragged = Boolean.parseBoolean(properties.getProperty("levelFromDragged", "false"));
        levelFromCrates = Boolean.parseBoolean(properties.getProperty("levelFromCrates", "false"));
        levelFromGround = Boolean.parseBoolean(properties.getProperty("levelFromGround", "false"));
        logInfo("overrideClayWeight = " + overrideClayWeight);
        logInfo("digToVehicle = " + digToVehicle);
        logInfo("dredgeToShip = " + dredgeToShip);
        logInfo("levelToVehicle = " + levelToVehicle);
        logInfo("digToCrates = " + digToCrates);
        logInfo("digToDragged = " + digToDragged);
        logInfo("levelToDragged = " + levelToDragged);
        logInfo("levelFromVehicle = " + levelFromVehicle);
        logInfo("levelFromDragged = " + levelFromDragged);
        logInfo("levelFromCrates = " + levelFromCrates);
        logInfo("levelFromGround = " + levelFromGround);
    }

    @Override
    public void preInit() {

    }

    @Override
    public void init() {
        try {
            ClassPool classPool = HookManager.getInstance().getClassPool();

            classPool.get("com.wurmonline.server.behaviours.Terraforming").getMethod("dig", "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;IIIFLcom/wurmonline/mesh/MeshIO;)Z").instrument(new ExprEditor() {
                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    if ("com.wurmonline.server.items.Item".equals(m.getClassName()) && "insertItem".equals(m.getMethodName())) {
                        m.replace("$_ = net.bdew.wurm.betterdig.BetterDigHooks.insertItemHook($1, $0, performer, dredging, false);");
                        logInfo("Installed insertItem dig hook at line " + m.getLineNumber());
                    } else if ("com.wurmonline.server.items.Item".equals(m.getClassName()) && "getNumItemsNotCoins".equals(m.getMethodName())) {
                        m.replace("$_ = 0;");
                        logInfo("Installed getNumItemsNotCoins digging override at line " + m.getLineNumber());
                    } else if ("com.wurmonline.server.creatures.Creature".equals(m.getClassName()) && "canCarry".equals(m.getMethodName())) {
                        m.replace("$_ = true;");
                        logInfo("Installed canCarry digging override at line " + m.getLineNumber());
                    } else if ("com.wurmonline.server.items.Item".equals(m.getClassName()) && "getFreeVolume".equals(m.getMethodName())) {
                        m.replace("$_ = 1000;");
                        logInfo("Installed getFreeVolume digging override at line " + m.getLineNumber());
                    }
                }
            });

            CtClass ctFlattening = classPool.getCtClass("com.wurmonline.server.behaviours.Flattening");

            ctFlattening.getMethod("getDirt", "(Lcom/wurmonline/server/creatures/Creature;IIIIZ)V").instrument(new ExprEditor() {
                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    if ("com.wurmonline.server.items.Item".equals(m.getClassName()) && "insertItem".equals(m.getMethodName())) {
                        m.replace("$_ = net.bdew.wurm.betterdig.BetterDigHooks.insertItemHook($1, $0, performer, false, true);");
                        logInfo("Installed insertItem flattening hook at line " + m.getLineNumber());
                    } else if ("com.wurmonline.server.items.Item".equals(m.getClassName()) && "getNumItemsNotCoins".equals(m.getMethodName())) {
                        m.replace("$_ = 0;");
                        logInfo("Installed getNumItemsNotCoins flattening override at line " + m.getLineNumber());
                    } else if ("com.wurmonline.server.creatures.Creature".equals(m.getClassName()) && "canCarry".equals(m.getMethodName())) {
                        m.replace("$_ = true;");
                        logInfo("Installed canCarry digging flattening at line " + m.getLineNumber());
                    }
                }
            });

            ctFlattening.getMethod("useDirt", "(Lcom/wurmonline/server/creatures/Creature;IIIIZ)V").instrument(new ExprEditor() {
                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    if ("com.wurmonline.server.creatures.Creature".equals(m.getClassName()) && "getCarriedItem".equals(m.getMethodName())) {
                        m.replace("$_ = net.bdew.wurm.betterdig.BetterDigHooks.getCarriedItemHook($1, $0);");
                        logInfo("Installed getCarriedItem hook in useDirt at line " + m.getLineNumber());
                    }
                }
            });

            ctFlattening.getMethod("checkUseDirt", "(IIIILcom/wurmonline/server/creatures/Creature;IILcom/wurmonline/server/behaviours/Action;Z)V").instrument(new ExprEditor() {
                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    if ("com.wurmonline.server.creatures.Creature".equals(m.getClassName()) && "getCarriedItem".equals(m.getMethodName())) {
                        m.replace("$_ = net.bdew.wurm.betterdig.BetterDigHooks.getCarriedItemHook($1, $0);");
                        logInfo("Installed getCarriedItem hook in checkUseDirt at line " + m.getLineNumber());
                    }
                }
            });

        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

}
