package net.bdew.wurm.betterdig;

import com.wurmonline.server.Items;
import com.wurmonline.server.NoSuchItemException;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.*;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.server.zones.Zones;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class BetterDigHooks {
    private final static Set<Integer> dirtItemTemplates = new HashSet<>(Arrays.asList(ItemList.dirtPile, ItemList.clay, ItemList.sand, ItemList.tar, ItemList.peat, ItemList.moss));

    public static boolean insertItemIntoVehicle(Item item, Item vehicle, Creature performer) {
        // If can put into crates, try that
        if (BetterDigMod.digToCrates && item.getTemplate().isBulk()) {
            for (Item container : vehicle.getAllItems(false)) {
                if (container.isCrate() && container.canAddToCrate(item)) {
                    if (item.AddBulkItemToCrate(performer, container)) {
                        performer.getCommunicator().sendNormalServerMessage(String.format("You put the %s in the %s in your %s.", item.getName(), container.getName(), vehicle.getName()));
                        return true;
                    }
                }
            }
        }
        // No empty crates or disabled, try the vehicle itself
        if (vehicle.getNumItemsNotCoins() < 100 && vehicle.getFreeVolume() >= item.getVolume() && vehicle.insertItem(item)) {
            performer.getCommunicator().sendNormalServerMessage(String.format("You put the %s in the %s.", item.getName(), vehicle.getName()));
            return true;
        } else {
            // Send message if the vehicle is too full
            performer.getCommunicator().sendNormalServerMessage(String.format("The %s is too full to hold the %s.", vehicle.getName(), item.getName()));
            return false;
        }
    }

    public static Item findItemInVehicle(int templateId, Item vehicle, Creature performer) {
        try {
            // First look in the vehicle itself
            Item result = vehicle.findItem(templateId);
            // If not found, and if it's enabled - look in crates
            if (result == null && BetterDigMod.levelFromCrates) {
                ItemTemplate template = ItemTemplateFactory.getInstance().getTemplate(templateId);
                for (Item container : vehicle.getAllItems(false)) {
                    if (container.isCrate()) {
                        for (Item bulkItem : container.getItems()) {
                            if (bulkItem.getRealTemplateId() == templateId && bulkItem.getBulkNumsFloat(false) >= 1f) {
                                bulkItem.setWeight(bulkItem.getWeightGrams() - template.getVolume(), true);
                                Item newItem = ItemFactory.createItem(templateId, bulkItem.getQualityLevel(), template.getMaterial(), (byte) 0, null);
                                newItem.setLastOwnerId(performer.getWurmId());
                                // We can't really return an item that is in a crate, so we remove 1 item from the crate,
                                // add it to the inventory temporarily and return that
                                performer.getInventory().insertItem(newItem, true);
                                performer.getCommunicator().sendNormalServerMessage(String.format("You grab a %s from the %s in your %s.", newItem.getName(), container.getName(), vehicle.getName()));
                                return newItem;
                            }
                        }
                    }
                }
            }

            return result;
        } catch (Throwable e) {
            BetterDigMod.logException(String.format("Error looking up item %d in %s", templateId, vehicle), e);
            return null;
        }
    }

    public static Item getVehicleSafe(Creature pilot) {
        try {
            if (pilot.getVehicle() != -10)
                return Items.getItem(pilot.getVehicle());
        } catch (NoSuchItemException ignored) {
        }
        return null;
    }

    public static boolean insertItemHook(Item item, Item destination, Creature performer, boolean dredging, boolean flattening) {
        try {
            if (dirtItemTemplates.contains(item.getTemplateId())) {
                // Is something that we are supposed to handle
                if (item.getTemplateId() == ItemList.clay && BetterDigMod.overrideClayWeight > 0) {
                    // If enabled, make clay dig more weight
                    item.setWeight(BetterDigMod.overrideClayWeight * 1000, false);
                }

                // Check if on a vehicle
                Item vehicleItem = getVehicleSafe(performer);
                if (vehicleItem != null && vehicleItem.isHollow()) {
                    // If on a vehicle, and a matching setting is enabled try to put in it
                    if ((dredging && BetterDigMod.dredgeToShip) || (flattening && BetterDigMod.levelToVehicle) || (!flattening && !dredging && BetterDigMod.digToVehicle)) {
                        if (insertItemIntoVehicle(item, vehicleItem, performer))
                            return true;
                    }
                }

                Item draggedItem = performer.getDraggedItem();
                if (draggedItem != null && draggedItem.isHollow()) {
                    if ((flattening && BetterDigMod.levelToDragged) || (!flattening && !dredging && BetterDigMod.digToDragged)) {
                        if (insertItemIntoVehicle(item, draggedItem, performer))
                            return true;
                    }
                }

                // If we got here the player is not on a vehicle or the vehicle is full, try putting on the ground
                item.putItemInfrontof(performer);
                return true;
            } else {
                // Not a dirt item, insert as normal
                return destination.insertItem(item, true);
            }
        } catch (Throwable e) {
            // Something is messed up, log error and revert to default
            BetterDigMod.logException("Error placing item from digging", e);
            return destination.insertItem(item, true);
        }
    }

    public static Item getCarriedItemHook(int itemTemplateId, Creature performer) {
        Item result = performer.getCarriedItem(itemTemplateId);

        if (result == null && BetterDigMod.levelFromGround) {
            VolaTile tempTile = Zones.getTileOrNull(performer.getTileX(), performer.getTileY(), performer.isOnSurface());
            if (tempTile != null) {
                for (Item groundItem : tempTile.getItems()) {
                    if (groundItem.getTemplateId() == itemTemplateId) {
                        return groundItem;
                    }
                }
            }
        }

        if (result == null && BetterDigMod.levelFromVehicle) {
            Item vehicleItem = getVehicleSafe(performer);
            if (vehicleItem != null && vehicleItem.isHollow()) {
                result = findItemInVehicle(itemTemplateId, vehicleItem, performer);
            }
        }

        if (result == null && BetterDigMod.levelFromDragged) {
            Item draggedItem = performer.getDraggedItem();
            if (draggedItem != null && draggedItem.isHollow()) {
                result = findItemInVehicle(itemTemplateId, draggedItem, performer);
            }
        }

        return result;
    }
}
