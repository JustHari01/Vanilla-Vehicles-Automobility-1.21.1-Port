/**
 * Equipment checking utilities for player inventory
 * Provides functions to check equipped items in specific slots
 * Used for boost mechanics and tool-based interactions
 */

import { EntityComponentTypes } from "@minecraft/server";

export class EquipmentChecker {
    static isItemEquipped(player, itemId, slot) {
        const equipment = player.getComponent(EntityComponentTypes.Equippable);
        const equippedItem = equipment?.getEquipment(slot);
        return equippedItem?.typeId === itemId;
    }

    static hasItemWithTag(player, tag, slot) {
        const equipment = player.getComponent(EntityComponentTypes.Equippable);
        const equippedItem = equipment?.getEquipment(slot);
        return equippedItem?.hasTag(tag) || false;
    }
}