package net.bdew.wurm.betterdig;

import com.wurmonline.server.behaviours.Actions;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.*;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BetterDigMod implements WurmMod, Initable, PreInitable, Configurable, ServerStartedListener {
    private static final Logger logger = Logger.getLogger("BetterDigMod");

    public static Set<Short> allowWhenMountedIds = new HashSet<>();
    private String allowWhenMounted = "";

    static int overrideClayWeight = 20;
    static int overrideMossWeight = 20;
    static int overridePeatWeight = 20;
    static int overrideTarWeight = 20;
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
        overrideTarWeight = Integer.parseInt(properties.getProperty("overrideTarWeight", "-1"), 10);
        overridePeatWeight = Integer.parseInt(properties.getProperty("overridePeatWeight", "-1"), 10);
        overrideMossWeight = Integer.parseInt(properties.getProperty("overrideMossWeight", "-1"), 10);
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

        allowWhenMounted = properties.getProperty("allowWhenMounted", "");

        logInfo("overrideClayWeight = " + overrideClayWeight);
        logInfo("overrideTarWeight = " + overrideTarWeight);
        logInfo("overridePeatWeight = " + overridePeatWeight);
        logInfo("overrideMossWeight = " + overrideMossWeight);
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
        logInfo("allowWhenMounted = " + allowWhenMounted);
    }

    @Override
    public void preInit() {

    }

    @Override
    public void init() {
        try {
            ClassPool classPool = HookManager.getInstance().getClassPool();

            classPool.get("com.wurmonline.server.behaviours.Terraforming").getMethod("dig", "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;IIIFZLcom/wurmonline/mesh/MeshIO;)Z").instrument(new ExprEditor() {
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

            ctFlattening.getMethod("getDirt", "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;IIIIZLcom/wurmonline/server/behaviours/Action;)V").instrument(new ExprEditor() {
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

            ctFlattening.getMethod("useDirt", "(Lcom/wurmonline/server/creatures/Creature;IIIIZLcom/wurmonline/server/behaviours/Action;)V").instrument(new ExprEditor() {
                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    if ("com.wurmonline.server.creatures.Creature".equals(m.getClassName()) && "getCarriedItem".equals(m.getMethodName())) {
                        m.replace("$_ = net.bdew.wurm.betterdig.BetterDigHooks.getCarriedItemHook($1, $0);");
                        logInfo("Installed getCarriedItem hook in useDirt at line " + m.getLineNumber());
                    }
                }
            });

            ctFlattening.getMethod("checkUseDirt", "(IIIILcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;IILcom/wurmonline/server/behaviours/Action;Z)V").instrument(new ExprEditor() {
                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    if ("com.wurmonline.server.creatures.Creature".equals(m.getClassName()) && "getCarriedItem".equals(m.getMethodName())) {
                        m.replace("$_ = net.bdew.wurm.betterdig.BetterDigHooks.getCarriedItemHook($1, $0);");
                        logInfo("Installed getCarriedItem hook in checkUseDirt at line " + m.getLineNumber());
                    }
                }
            });

            ExprEditor actionMountedFixer = new ExprEditor() {
                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    if ("getVehicle".equals(m.getMethodName())) {
                        m.replace("$_ = net.bdew.wurm.betterdig.BetterDigMod.allowWhenMountedIds.contains(new Short(action)) ? -10 : $proceed($$);");
                        logInfo("Patched getVehicle check in Action constructor line " + m.getLineNumber());
                    }
                }
            };

            CtClass ctAction = classPool.getCtClass("com.wurmonline.server.behaviours.Action");
            for (CtConstructor c : ctAction.getConstructors()) {
                c.instrument(actionMountedFixer);
            }


        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onServerStarted() {
        if (allowWhenMounted.length() > 0) {
            for (String actionName : allowWhenMounted.split(",")) {
                actionName = actionName.trim().toUpperCase();
                try {
                    Short actionNum = Actions.class.getField(actionName).getShort(null);
                    logInfo(String.format("Adding action allowed when mounted: %s (%d)", actionName, actionNum));
                    allowWhenMountedIds.add(actionNum);
                } catch (IllegalAccessException | NoSuchFieldException e) {
                    logException("Error location action named " + actionName, e);
                }
            }
        }
    }
}
