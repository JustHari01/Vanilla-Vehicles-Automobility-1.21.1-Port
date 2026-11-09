
//Initializes the addon systems and starts the main game loop

import { world, system, CommandPermissionLevel, CustomCommandStatus, Player } from "@minecraft/server";
import { initializeEventHandlers } from "./core/eventHandlers";
import { VehicleRegistry } from "./core/vehicleRegistry";
import { showGuidebook, ShowSettingsPage } from "./ui/guideBookHandler";


// Initialize the vehicle registry
VehicleRegistry.initialize();

// Initialize event handlers
initializeEventHandlers();

// Initialize Guide Book handler
world.afterEvents.itemUse.subscribe((event) => {
    const { source: player, itemStack } = event;

    // Check if the item used is the "Guidebook"
    // For this example, we check if the item ID is a book
    // You can also check itemStack.nameTag === "Guidebook" if you renamed it in an anvil
    if (itemStack.typeId === "bw_vv:guide_book") {
        
        // We run this on the next tick to ensure the UI opens smoothly
        system.run(() => {
            showGuidebook(player);
            player.playSound('bw_vv.ui.guide_book.open');
        });
    }
});

// Export registry for access by other modules
export { VehicleRegistry };

// Custom Commands
system.beforeEvents.startup.subscribe((init) => {
  const vanillavehiclesSettings = {
    name: "bw_vv:vehiclesettings",
    description: "Opens the Settings Menu for the Vanilla Vehicles Addon",
    permissionLevel: CommandPermissionLevel.Any
  };

  init.customCommandRegistry.registerCommand(
    vanillavehiclesSettings,
    ShowVanillaVehiclesSettingsPage
  );
});

function ShowVanillaVehiclesSettingsPage(origin) {
    const source = origin.initiator ?? origin.sourceEntity;
    if (!(source instanceof Player)) {
                return {
                    status: CustomCommandStatus.Failure,
                    message: "This command can only be executed by players.",
            };
    }
    const player = source;
    system.run(() => {
        ShowSettingsPage(player);
        player.playSound("bw_vv.ui.guide_book.open");
    });
    return {
        status: CustomCommandStatus.Success,
    };
}