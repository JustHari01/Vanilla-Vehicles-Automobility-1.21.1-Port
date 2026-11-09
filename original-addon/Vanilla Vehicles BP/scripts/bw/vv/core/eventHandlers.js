/*
 Main event handling system for vehicle interactions
 Manages player-vehicle relationships, riding states, and entity events
 Coordinates between players, vehicles, and behavior instances
*/

import { world, system, EntityComponentTypes, EquipmentSlot, ItemStack } from "@minecraft/server";
import { VehicleRegistry } from "./vehicleRegistry";
import { findRiddenVehicle, syncBackseatRotation, getRidingEntity, getRiderEntity, applyForwardImpulse } from "../utils/entities";
import { EquipmentChecker } from "../utils/equipment";
import { smoothValue } from "../utils/math";
import { initWindSystem } from "./windSystem";
import { VEHICLE_CLIMB_STATE_ANIMATIONS, PLAYER_DRIVE_ANIMATION_VARIABLES, FUEL_TITLE_DELAY_TICKS, VEHICLE_TYPES, ITEM_IDS, ITEM_TAGS, PHYSICS_CONFIG } from "../config/constants";

// State management maps
const previousPropertyValues = new Map();
const previousYPositions = new Map();
//const entityRotations = new Map();
const playerVehicles = new Map();
const playerJumpStates = new Map(); // Track jump state per player
const boostingVehicles = new Map(); // vehicleId -> { vehicle, ticksRemaining }

// PLAYER WELCOME MESSAGE AND GUIDE BOOK SPAWN FUNCTION

const ROT_EPS = 0.01;          // sensitivity (degrees). raise to 0.1 if you get false positives
const CHECK_EVERY_TICKS = 1;

function rotationChanged(a, b) {
  // handle wrap-around nicely (e.g. 359 -> 0)
  const yawDelta = Math.abs((((a.y - b.y) + 540) % 360) - 180);
  const pitchDelta = Math.abs(a.x - b.x);
  return yawDelta > ROT_EPS || pitchDelta > ROT_EPS;
}

function runWelcomeOnce(player) {
  const message = {
    rawtext: [
      { translate: "bw_vv.welcom_screen.text1" },
      { text: ` ${player.name}` },
      { text: `!\n§a` },
      { translate: "bw_vv.welcom_screen.text2" },
      { text: `\n§7` },
      { translate: "bw_vv.welcom_screen.text3" },
      { text: `\n§r------------------§r\n` },
      { translate: "bw_vv.welcom_screen.text4" },
      { text: `\n\n` },
      { translate: "bw_vv.welcom_screen.text5" }
    ]
  };

  player.sendMessage(message);

  const x = Math.floor(player.location.x);
  const y = Math.floor(player.location.y);
  const z = Math.floor(player.location.z);

  player.dimension.runCommand(
    `structure load bw_vv:guide_book ${x} ${y} ${z} 0_degrees none true false`
  );

  player.playSound("bw_vv.ui.player_join");
  player.setDynamicProperty("hasBeenWelcomed", true);
}

function waitForHeadMoveThenWelcome(player) {
  
  player.setDynamicProperty("bw_vv:setting.fuel_actionbar", true);
  // snapshot rotation right now
  const startRot = player.getRotation(); // { x: pitch, y: yaw }

  // poll until rotation changes, then stop forever
  const handle = system.runInterval(() => {
    // player might have left / become invalid
    try {
      // if they already got welcomed somehow, stop
      if (player.getDynamicProperty("hasBeenWelcomed")) {
        system.clearRun(handle);
        return;
      }

      const currentRot = player.getRotation();
      if (rotationChanged(currentRot, startRot)) {
        system.clearRun(handle);
        runWelcomeOnce(player);
      }
    } catch (e) {
      // if player is invalid, stop interval
      system.clearRun(handle);
    }
  }, CHECK_EVERY_TICKS);
}

world.afterEvents.playerSpawn.subscribe(({ player, initialSpawn }) => {
  if (!initialSpawn) return;

  // already welcomed? do nothing
  if (player.getDynamicProperty("hasBeenWelcomed")) return;

  // arm the "head move" trigger
  waitForHeadMoveThenWelcome(player);
});

function buildFuelBar(fuel, FUEL_MAX, FUEL_BAR_STEPS) {
    const clampedFuel = Math.max(0, Math.min(FUEL_MAX, fuel ?? 0));

    const percent = Math.round((clampedFuel / FUEL_MAX) * 100);

    // Discrete bar: 0–10 blocks
    const filledBlocks = Math.round((percent / 100) * FUEL_BAR_STEPS);
    const emptyBlocks = FUEL_BAR_STEPS - filledBlocks;

    const bar =
        "█".repeat(filledBlocks) +
        "§7░".repeat(emptyBlocks);

    return { bar, percent };
}

//fuel functions
function hidefueltitle(player) {
    //player.onScreenDisplay.setTitle('bw_vv:hide_fuel', {stayDuration: 1});
    player.onScreenDisplay.setActionBar('');
}

function showFuelHud(player, fuelTypeLabel, fuel) {
    const { bar, percent } = buildFuelBar(fuel, 10, 5);
    player.onScreenDisplay.setActionBar({
        rawtext: [
            { translate: fuelTypeLabel },
            { text: " " },
            //{ text: '>'},         // "Coal:"
            //{ text: " " },                         // space
            { text: bar },
            { text: " " },
            { text: `${percent}%` }                // 62%
        ]
    });
}

function showMechControlsHud(player, projectileLabel, boostLabel, cooldown_projectile, cooldown_boost) {
    const projectile =buildFuelBar(cooldown_projectile, 3, 4);
    const boost = buildFuelBar(cooldown_boost, 7, 7);
    player.onScreenDisplay.setActionBar({
        rawtext: [
            { translate: projectileLabel },
            { text: projectile.percent === 100 ? "§a " : " " },
            { text: projectile.bar }, 
            { text: "\n" }, 
            { text: "§f" }, 
            { translate: boostLabel },
            { text: boost.percent === 100 ? "§a " : " " },
            { text: boost.bar }
        ]
    });
}


// Smoothing history array maps - per vehicle for better accuracy
const velocityHistories = new Map();
const positionHistories = new Map();

// Cache for animation map
const animationCache = new Map();

// Fuel title cadence + visibility state (per-player) map
const fuelTitleTick = new Map(); // playerId -> 0..4
const fuelTitleVisible = new Map(); // playerId -> shown?
const fuelTitleDelay = new Map(); // playerId -> remaining ticks
/**
 * Retrieves or creates smoothing history arrays for a specific vehicle
 * Ensures each vehicle has its own history for accurate smoothing calculations
 * @param {string} vehicleId - Unique identifier of the vehicle
 * @returns {Object} Object containing velocity and position history arrays
 */
function getVehicleHistories(vehicleId) {
    if (!velocityHistories.has(vehicleId)) {
        velocityHistories.set(vehicleId, []);
        positionHistories.set(vehicleId, []);
    }
    return {
        velocity: velocityHistories.get(vehicleId),
        position: positionHistories.get(vehicleId)
    };
}





/**
 * Handles the boost mechanic for vehicles when player jumps
 * Checks prerequisites: helmet equipped, fuel available, boost ready
 * Applies forward impulse and consumes fuel when conditions are met
 * @param {Player} player - The player attempting to boost
 * @param {Entity} vehicle - The vehicle being boosted
 * @param {boolean} isJumpStart - Whether this is the start of a jump
 * @returns {boolean} True if boost was successfully applied
 */
const exclamation_red = "§c§l! §f§r";
const exclamation_white = "§l! §f§r";
const exclamation_green = "§a§l! §f§r";
const exclamation_yellow = "§e§l! §f§r";
const exclamation_volume = 0.6;

function registerBoostTick(vehicle) {
    const id = vehicle.id;
    if (boostingVehicles.has(id)) return; // already tracked

    boostingVehicles.set(id, vehicle);
}

function tickBoostingVehicles() {
    for (const [id, vehicle] of boostingVehicles) {
        // Cleanup invalid or stopped vehicles
        if (!vehicle?.isValid) {
            boostingVehicles.delete(id);
            continue;
        }

        const boost = vehicle.getProperty('bw_vv:boost');
        const fuel = vehicle.getProperty('bw_vv:fuel_progress');

        if (boost !== "active" || fuel <= 0) {
            boostingVehicles.delete(id);
            continue;
        }

        // Skip mech grounded check (matches your existing logic)
        if (vehicle.typeId === VEHICLE_TYPES.MECH && vehicle.isOnGround) {
            boostingVehicles.delete(id);
            continue;
        }

        // Apply impulse regardless of whether a player is riding
        applyForwardImpulse(vehicle);
        vehicle.setProperty(
            'bw_vv:fuel_progress',
            Math.max(0, fuel - PHYSICS_CONFIG.BOOST_FUEL_CONSUMPTION)
        );
    }
}

system.afterEvents.scriptEventReceive.subscribe((event) => {
    const entity = event.sourceEntity; // This is the player who triggered it
    if (entity && entity.typeId === "minecraft:player") {
        if (event.id === "bw_vv:fuel_warning") {
            warningCoalFuel(entity);
        }
        if (event.id === "bw_vv:smoke_bomb_warning") {
            warningSmokeBomb(entity);
        }
        if (event.id === "bw_vv:sticker_warning") {
            warningSticker(entity);
        }
        if (event.id === "bw_vv:bundle_warning") {
            warningBundle(entity);
        }
    }
    if (entity)  {   
            if (event.id === "bw_vv:fuel_up_vehicle") {
                fuelUpVehicle(entity);
        }
        
        if (event.id === "bw_vv:spawn_dye") {
            
            if (entity.hasTag("bw_vv:dyed")) {
                const colorComp = entity.getComponent(EntityComponentTypes.Color);
                
                //world.sendMessage(`Player ${entity.name} triggered dye spawn with color value: ${colorComp ? colorComp.value : 'none'}`);

                // Safety check: ensure component exists and value is 0-15
                if (colorComp) {
                    const dyeId = DYE_ITEMS[colorComp.value];
                    if (dyeId) {
                        entity.dimension.spawnItem(new ItemStack(dyeId, 1), entity.location);
                    }
                }
            }
        }
    }

}, { namespaces: ["bw_vv"] });

function fuelUpVehicle(entity) {
    //this is a function that takes the location of the entity and plays a sound there, depending on the type of fuel that is set in family_type component. also, depending on the bw_vv:fuel_progress property, it plays the sound with increasing pitch.
    const fuel_progress = entity.getProperty('bw_vv:fuel_progress') ?? 0;
    const familyComponent = entity.getComponent(EntityComponentTypes.TypeFamily);
    let fuelType = "unknown";
    if (familyComponent.hasTypeFamily('coal_based')) {
        fuelType = "coal";
    } else if (familyComponent.hasTypeFamily('redstone_based')) {
        fuelType = "redstone";
    }
    //coal based
    let soundEvent_pitch = "bw_vv.vehicle.fuel.coal.pitch";
    let soundEvent_no_pitch = "bw_vv.vehicle.fuel.coal.no_pitch";
    let soundEvent_full = "bw_vv.vehicle.fuel.coal.no_pitch";

    //redstone based
    if (fuelType === "redstone") {
        soundEvent_pitch = "bw_vv.vehicle.fuel.redstone.pitch";
        soundEvent_no_pitch = "bw_vv.vehicle.fuel.redstone.no_pitch";
        soundEvent_full = "bw_vv.vehicle.fuel.redstone.full";
    }
    const pitch = Math.min(2.0, 0.7 + (fuel_progress / 10)); // pitch increases with fuel progress, max 2.0
    
    // Get entity location and dimension
    const location = entity.location;
    const players = world.getPlayers();
        
    for (const player of players) {
        // Play sound at entity's location in the world
        if (fuel_progress < 9.8) {
            player.playSound(soundEvent_pitch,{ 
                location: location,
                volume: 0.8, 
                pitch: pitch
            });
            player.playSound(soundEvent_no_pitch,{ 
                location: location,
                volume: 0.8, 
            });
        }      
        if (fuel_progress >= 9.8) {
            player.playSound(soundEvent_full, {
                location: location,
                volume: 0.9
            });
            player.playSound(soundEvent_pitch,{ 
                location: location,
                volume: 0.8, 
                pitch: pitch
            });
        }
    }
}

function warningCoalFuel(player) {
    const fuelactionbarsetting = player.getDynamicProperty("bw_vv:setting.fuel_actionbar") ?? true;
        const warning_fuel = {
        rawtext: [
            { translate: "bw_vv.fuel_interact.warning" }
        ]
    };  
        if (getRidingEntity(player)) {
        fuelactionbarsetting
        ? player.sendMessage(warning_fuel)
        : player.onScreenDisplay.setActionBar(warning_fuel);
        }
        else {
        player.onScreenDisplay.setActionBar(warning_fuel);
        }
    player.playSound('bw_vv.ui.warning', { volume: exclamation_volume });
}

function warningSmokeBomb(player) {
    const fuelactionbarsetting = player.getDynamicProperty("bw_vv:setting.fuel_actionbar") ?? true;
        const warning_smoke_bomb = {
        rawtext: [
            { text: exclamation_yellow },
            { translate: "bw_vv.smoke_bomb.warning" }
        ]
    };  
        if (getRidingEntity(player)) {
        fuelactionbarsetting
        ? player.sendMessage(warning_smoke_bomb)
        : player.onScreenDisplay.setActionBar(warning_smoke_bomb);
        }
        else {
        player.onScreenDisplay.setActionBar(warning_smoke_bomb);
        }
    player.playSound('bw_vv.ui.warning', { volume: exclamation_volume });
}

function warningSticker(player) {
    const fuelactionbarsetting = player.getDynamicProperty("bw_vv:setting.fuel_actionbar") ?? true;
        const warning_sticker = {
        rawtext: [
            { text: exclamation_yellow },
            { translate: "bw_vv.sticker.warning" }
        ]
    };
        if (getRidingEntity(player)) {
        fuelactionbarsetting        ? player.sendMessage(warning_sticker)
        : player.onScreenDisplay.setActionBar(warning_sticker);
        }
        else {        player.onScreenDisplay.setActionBar(warning_sticker);
        }
    player.playSound('bw_vv.ui.warning', { volume: exclamation_volume });
}

function warningBundle(player) {
    const fuelactionbarsetting = player.getDynamicProperty("bw_vv:setting.fuel_actionbar") ?? true;
        const warning_bundle = {
        rawtext: [
            { text: exclamation_yellow },
            { translate: "bw_vv.bundle.warning" }
        ]
    };  
        if (getRidingEntity(player)) {
        fuelactionbarsetting
        ? player.sendMessage(warning_bundle)
        : player.onScreenDisplay.setActionBar(warning_bundle);
        }
        else {
        player.onScreenDisplay.setActionBar(warning_bundle);
        }
    player.playSound('bw_vv.ui.warning', { volume: exclamation_volume });
}
function handleBoost(player, vehicle, isJumpStart) {
    const fuelactionbarsetting = player.getDynamicProperty("bw_vv:setting.fuel_actionbar");
    if (!isJumpStart) return false;
    if (vehicle.typeId === VEHICLE_TYPES.MECH && vehicle.isOnGround) return false;
    // Skip boost for some vehicles
    if ( vehicle.typeId === VEHICLE_TYPES.PRISMER || vehicle.typeId === VEHICLE_TYPES.HOT_AIR_BALLOON || vehicle.typeId === VEHICLE_TYPES.MINECAR || vehicle.typeId === VEHICLE_TYPES.LOGCAR || vehicle.typeId === VEHICLE_TYPES.STEAMBOAT ) {
        return false;
    }
    
    const fuel = vehicle.getProperty('bw_vv:fuel_progress');
    const warning_fuel = {
        rawtext: [
            { text: exclamation_red },
            { translate: "bw_vv.boost.no_fuel" }
        ]
    };
    const warning_tesla_helmet = {
        rawtext: [
            { text: exclamation_yellow },
            { translate: "bw_vv.boost.no_tesla_helmet" }
        ]
    };
    const warning_cooldown = {
        rawtext: [
            { text: exclamation_yellow },
            { translate: "bw_vv.boost.cooldown" }
        ]
    };
    if (fuel <= 0) {
    player.playSound("bw_vv.ui.warning");

    fuelactionbarsetting
        ? player.sendMessage(warning_fuel)
        : player.onScreenDisplay.setActionBar(warning_fuel);
    }

    const boostProperty = vehicle.getProperty('bw_vv:boost');
    
    if (!EquipmentChecker.isItemEquipped(player, ITEM_IDS.TESLA_HELMET, EquipmentSlot.Head)) {
        fuelactionbarsetting
            ? player.sendMessage(warning_tesla_helmet)
            : player.onScreenDisplay.setActionBar(warning_tesla_helmet);
        player.playSound('bw_vv.ui.warning', { volume: exclamation_volume });
        return false;
    }

    if (boostProperty === "ready") {
        vehicle.setProperty('bw_vv:boost', "active");
        if (vehicle.typeId != VEHICLE_TYPES.MECH) {
            vehicle.playAnimation('animation.bw_vv.vehicle_boost');
        }
        applyForwardImpulse(vehicle);
        registerBoostTick(vehicle); // <-- add this
        return true;
    } else if (boostProperty === "cooldown") {        
        fuelactionbarsetting
            ? player.sendMessage(warning_cooldown)
            : player.onScreenDisplay.setActionBar(warning_cooldown);
        player.playSound('bw_vv.ui.warning', { volume: exclamation_volume });
        return false;
    }
    
    return false;
}

/**
 * Updates vehicle physics properties including rotation preservation and vertical velocity
 * Handles stationary rotation locking to prevent unwanted spinning
 * Calculates and applies smoothed vertical velocity for animation purposes
 * @param {Entity} vehicle - The vehicle to update
 * @param {Player} player - The player riding the vehicle
 * @returns {Object} Object containing velocity, position, and fuel values
 */
function updateVehiclePhysics(vehicle, player) {

    const velocity = vehicle.getVelocity();
    const position = vehicle.location;
    const fuel = vehicle.getProperty('bw_vv:fuel_progress');
    const fuelactionbarsetting = player.getDynamicProperty("bw_vv:setting.fuel_actionbar") ?? true;
    
    // Handle stationary rotation preservation
    //if (velocity.x === 0 && velocity.z === 0 && fuel !== 0 && vehicle.typeId != VEHICLE_TYPES.MECH) {
    //    if (!entityRotations.has(vehicle)) {
    //        entityRotations.set(vehicle, vehicle.getRotation());
    //    }
    //    //vehicle.setRotation(entityRotations.get(vehicle));
    //} else {
    //    entityRotations.delete(vehicle);
    //}

    // Update vertical velocity property
    const vehicleId = vehicle.id;
    const histories = getVehicleHistories(vehicleId);
    
    const smoothVerticalLocation = smoothValue(position.y, histories.position, PHYSICS_CONFIG.POSITION_SMOOTHING_FACTOR);
    const previousY = previousYPositions.get(vehicleId) || smoothVerticalLocation;
    const verticalVelocity = smoothVerticalLocation - previousY;
    previousYPositions.set(vehicleId, smoothVerticalLocation);
    
    const smoothVerticalVelocity = smoothValue(verticalVelocity, histories.velocity, PHYSICS_CONFIG.VELOCITY_SMOOTHING_FACTOR);
    const finalVelocity = Math.max(-50, Math.min(50, smoothVerticalVelocity * 20));
    
    if (vehicle.getProperty('bw_vv:vertical_velocity') !== undefined) {
        vehicle.setProperty('bw_vv:vertical_velocity', finalVelocity);
    }

    return { velocity, position, fuel, fuelactionbarsetting };
}

/**
 * Updates player animations based on vehicle climb state changes
 * Uses caching to avoid repeated animation lookups
 * Only triggers animations when the climb state actually changes
 * @param {Player} player - The player whose animation to update
 * @param {Entity} vehicle - The vehicle determining the animation state
 */
function updatePlayerAnimation(vehicle) {
    const climbFactor = vehicle.getProperty("bw_vv:climb_animation");
    const currentValue = vehicle.getProperty("bw_vv:player_anim");
    const blendOutTime = vehicle.getProperty("bw_vv:player_anim_blend_time");
    const vehicleId = vehicle.id;
    const previousValue = previousPropertyValues.get(vehicleId);
    
    if (previousValue === undefined) {
        previousPropertyValues.set(vehicleId, '__initial__');
        return;
    }

    if (currentValue !== previousValue) {
        previousPropertyValues.set(vehicleId, currentValue);
        
        const animationKey = `${climbFactor}_degree`;
        if (!animationCache.has(animationKey)) {
            animationCache.set(animationKey, VEHICLE_CLIMB_STATE_ANIMATIONS[animationKey]);
        }
        
        const animations = animationCache.get(animationKey);
        const animationId = animations?.[currentValue];

        if (animationId && vehicle?.isValid) {
            // Get all riders on the vehicle
            const rideable = vehicle.getComponent(EntityComponentTypes.Rideable);
            if (rideable) {
                const riders = rideable.getRiders();
                
                // Apply animation to ALL riders
                for (const rider of riders) {
                    if (rider && rider.isValid && rider.typeId === "minecraft:player") {
                        system.runTimeout(() => {
                            rider.playAnimation(animationId, {
                                stopExpression: '!q.is_riding',
                                controller: "climb_animation_controller",
                                blendOutTime: 0.1
                            });
                        });
                    }
                }
            }
        }
    }
}

function autoNametagVehicle(vehicle) {
    // Check if this vehicle has already been nametagged
    if (vehicle.getDynamicProperty("bw_vv:auto_nametagged")) {
        return;
    }

    // Check if this is a vehicle type we care about
    const vehicleTypeId = vehicle.typeId;
    const isVehicle = Object.values(VEHICLE_TYPES).includes(vehicleTypeId);
    
    if (!isVehicle) {
        return;
    }

    // Extract the item key from the vehicle typeId
    // e.g., "bw_vv:minecar" -> "minecar"
    const itemKey = vehicleTypeId.split(':')[1];
    
    // Build the translation key
    // e.g., "item.bw_vv:minecar"
    const translationKey = `tile.bw_vv:${itemKey}.name`;
    
    // Set the nametag
    vehicle.nameTag = translationKey;
    
    // Mark as nametagged so we don't do it again
    vehicle.setDynamicProperty("bw_vv:auto_nametagged", true);
}

/**
 * Executes all necessary updates when a player is actively riding a vehicle
 * Manages behavior instances, physics, boost mechanics, animations, and debug info
 * Called every tick while the player remains on the vehicle
 * @param {Player} player - The player riding the vehicle
 * @param {Entity} vehicle - The vehicle being ridden
 */
function onPlayerStartRiding(player, vehicle) {
    const playerId = player.id;

    // Create behavior instance if needed
    VehicleRegistry.createBehavior(vehicle, player);
    
    // Update physics
    const { velocity, position, fuel } = updateVehiclePhysics(vehicle, player);
    
    // Update vehicle WASD property
    const inputVector = player.inputInfo.getMovementVector();
    if (fuel > 0) {
        const inputCase = inputVector.y > 0.1 ? 'forward' : 
                        inputVector.y < -0.1 ? 'backward' : 
                        inputVector.x > 0.1 ? 'left' : 
                        inputVector.x < -0.1 ? 'right' : 
                        'neutral';

        switch(inputCase) {
            case 'forward':
                vehicle.setProperty('bw_vv:wasd', 'w');
                break;
            case 'backward':
                vehicle.setProperty('bw_vv:wasd', 's');
                break;
            case 'left':
                vehicle.setProperty('bw_vv:wasd', 'a');
                break;
            case 'right':
                vehicle.setProperty('bw_vv:wasd', 'd');
                break;
        }
    }


    // Handle boost
    const playerJumpState = playerJumpStates.get(playerId) || { wasJumping: false };
    const isJumpStart = player.isJumping && !playerJumpState.wasJumping;
    
    if (isJumpStart) {
        handleBoost(player, vehicle, true);
    }
    
    playerJumpState.wasJumping = player.isJumping;
    playerJumpStates.set(playerId, playerJumpState);
    
    // Update animations
    updatePlayerAnimation(vehicle);
}

/**
 * Cleans up all vehicle-related data when a player stops riding
 * Resets vehicle properties, clears history maps, and removes behavior instances
 * Ensures no memory leaks by removing all references to the vehicle
 * @param {Player} player - The player who stopped riding
 * @param {Entity} vehicle - The vehicle that was being ridden
 */
function onPlayerStopRiding(player, vehicle) {

    if (!vehicle || !vehicle?.isValid) {
        // Still cleanup player-specific data
        const playerId = player.id;
        if (fuelTitleVisible.get(playerId)) {
            fuelTitleVisible.set(playerId, false);
        }
        fuelTitleTick.delete(playerId);
        playerVehicles.delete(playerId);
        playerJumpStates.delete(playerId);
        return;
    }

    if (vehicle.hasComponent('bw_vv:vertical_velocity')) {
        vehicle.setProperty('bw_vv:vertical_velocity', 0);
    }

    VehicleRegistry.clearPlayerFromBehavior(vehicle);
    
    const vehicleId = vehicle.id;
    //entityRotations.delete(vehicle);
    previousPropertyValues.delete(vehicleId);
    previousYPositions.delete(vehicleId);
    velocityHistories.delete(vehicleId);
    positionHistories.delete(vehicleId);

    // Hide the fuel title once on stop/vehicle removal
    const playerId = player.id;
    if (fuelTitleVisible.get(playerId)) {
        hidefueltitle(player);
        fuelTitleVisible.set(playerId, false);
        fuelTitleDelay.delete(playerId);
    }
    fuelTitleTick.delete(playerId);
    
}

// Event mapping constants for cleaner code
const EVENT_MAPPINGS = {
    BACKSEAT: {
        "event:open": "event:backdoor_open",
        "event:open_animal_in": "event:backdoor_open",
        "event:close": "event:backdoor_close",
        "event:close_animal_in": "event:backdoor_close",
        "event:remove_creative": "event:remove_creative",
        "event:damager": "event:damager",
        "event:damager_weak": "event:damager_weak"
    },
    MOBTRUCK: {
        "event:remove_drop_items": "event:remove",
        "event:remove": "event:remove",
        "event:remove_creative": "event:remove"
    }
};

/**
 * Handles critical hit mechanics for destroying vehicles with appropriate tools
 * Checks if player is falling with correct tool type for vehicle material
 * Metal vehicles require pickaxes, wooden vehicles require axes
 * @param {Object} event - The entity hit event containing attacker and target
 */
function handleEntityHitEvent({damagingEntity: attacker, hitEntity: hit}) {
    if (attacker?.typeId !== "minecraft:player" || !attacker.isFalling) return;
    
    const familyComponent = hit.getComponent(EntityComponentTypes.TypeFamily);
    if (!familyComponent?.hasTypeFamily('vehicle') || !familyComponent.hasTypeFamily('critical_hit')) return;
    
    const isMetal = familyComponent.hasTypeFamily('metal');
    const isWooden = familyComponent.hasTypeFamily('wooden');
    
    const shouldRemove = (
        (isMetal && EquipmentChecker.hasItemWithTag(attacker, ITEM_TAGS.PICKAXE, EquipmentSlot.Mainhand)) ||
        (isWooden && EquipmentChecker.hasItemWithTag(attacker, ITEM_TAGS.AXE, EquipmentSlot.Mainhand))
    );
    
    if (shouldRemove) {
        hit.triggerEvent("event:remove");
    }
}

/**
 * Handles custom entity trigger events for vehicle interactions
 * Propagates events between mobtruck and backseat entities
 * Ensures synchronized state between connected vehicle components
 * @param {Object} eventData - The entity trigger event data
 */
function handleDataDrivenEntityTrigger(eventData) {
    const { entity, eventId } = eventData;
    const entityType = entity.typeId;
    
    if (entityType === VEHICLE_TYPES.MOBTRUCK_BACKSEAT) {
        const vehicleEvent = EVENT_MAPPINGS.BACKSEAT[eventId];
        if (vehicleEvent) {
            const vehicle = getRidingEntity(entity);
            if (vehicle?.typeId === VEHICLE_TYPES.MOBTRUCK) {
                vehicle.triggerEvent(vehicleEvent);
            }
        }
    } else if (entityType === VEHICLE_TYPES.MOBTRUCK) {
        const vehicleEvent = EVENT_MAPPINGS.MOBTRUCK[eventId];
        if (vehicleEvent) {
            getRiderEntity(entity)?.triggerEvent(vehicleEvent);
        }
    }
}

/**
 * Initializes all event handlers and starts the main game loop
 * Sets up the tick interval for continuous updates
 * Registers event subscriptions for entity interactions
 * Main entry point for the event handling system
 */
const PARTICLE_SPAWN_INTERVAL = 3; // ticks

let tickCounter = 0;

const DYE_ITEMS = [
    "minecraft:white_dye",      // 0
    "minecraft:orange_dye",     // 1
    "minecraft:magenta_dye",    // 2
    "minecraft:light_blue_dye", // 3
    "minecraft:yellow_dye",     // 4
    "minecraft:lime_dye",       // 5
    "minecraft:pink_dye",       // 6
    "minecraft:gray_dye",       // 7
    "minecraft:light_gray_dye", // 8
    "minecraft:cyan_dye",       // 9
    "minecraft:purple_dye",     // 10
    "minecraft:blue_dye",       // 11
    "minecraft:brown_dye",      // 12
    "minecraft:green_dye",      // 13
    "minecraft:red_dye",        // 14
    "minecraft:black_dye"       // 15
];

function handleDyeInteraction(event) {
    const { itemStack, target, player } = event;
    if (!itemStack || !target) return;

    // Check if item is a dye
    if (!itemStack.typeId.endsWith("_dye")) return;
    
    //player.sendMessage(`Interacted with ${target.typeId} using ${itemStack.typeId}`);

    // Mark as dyed (using a Tag is cheapest/easiest)
    if (!target.hasTag("bw_vv:dyed")) {
        target.addTag("bw_vv:dyed");
    }
}

export function initializeEventHandlers() {
    
    initWindSystem();
    // Main game loop - runs every tick (20 times per second)
    

    system.runInterval(() => {
        tickBoostingVehicles();
        world.afterEvents.playerInteractWithEntity.subscribe(handleDyeInteraction);
        const players = world.getPlayers();
        
        for (const player of players) {
            const vehicle = findRiddenVehicle(player, ["bw_vanilla_vehicles", "vehicle"]);
             let animationVariables = PLAYER_DRIVE_ANIMATION_VARIABLES;
            // Apply custom arm animation calculations for vehicle riding

            player.playAnimation("animation.bw_vv.player_vehicle_null", {
                nextState: null,
                stopExpression: animationVariables
            });
            

            // Tesla Particle
            tickCounter++;
            if (tickCounter % PARTICLE_SPAWN_INTERVAL === 0) {
                const headlocation = player.getHeadLocation();
                if (EquipmentChecker.isItemEquipped(player, ITEM_IDS.TESLA_HELMET, EquipmentSlot.Head)) {
                    player.dimension.spawnParticle("bw_vv:redstone_electricty", headlocation);
                }
            }

            
            const vehicle_coal = findRiddenVehicle(player, ["bw_vanilla_vehicles", "vehicle", "coal_based"]);
            const vehicle_redstone = findRiddenVehicle(player, ["bw_vanilla_vehicles", "vehicle", "redstone_based"]);
            const vehicle_mech = findRiddenVehicle(player, ["bw_vanilla_vehicles", "vehicle", "mech"]);
            const playerId = player.id;
            const storedVehicle = playerVehicles.get(playerId);
            
            if (vehicle) {
                if (storedVehicle !== vehicle) {
                    // Vehicle changed - cleanup old and setup new
                    if (storedVehicle) {
                        onPlayerStopRiding(player, storedVehicle);
                    }
                    playerVehicles.set(playerId, vehicle);
                    fuelTitleDelay.set(playerId, FUEL_TITLE_DELAY_TICKS);
                }

                onPlayerStartRiding(player, vehicle);

                if (fuelTitleDelay.has(playerId)) {
                    const remaining = fuelTitleDelay.get(playerId) - 1;

                    if (remaining <= 0) {
                        fuelTitleDelay.delete(playerId);
                    } else {
                        fuelTitleDelay.set(playerId, remaining);
                    }
                }
                // Fuel title every 5 ticks while riding (affected by the fuel acitonbar setting) + other controls (mech)
                if (vehicle.typeId != VEHICLE_TYPES.HOT_AIR_BALLOON && vehicle.typeId != VEHICLE_TYPES.PRISMER && !fuelTitleDelay.has(playerId) && player.getDynamicProperty("bw_vv:setting.fuel_actionbar")) {
                    const fuel = vehicle.getProperty('bw_vv:fuel_progress') ?? 0;
                    const cooldown_boost = 7-vehicle.getProperty('bw_vv:boost_cooldown');
                    const cooldown_projectile = 3-vehicle.getProperty('bw_vv:weapons_cooldown');
                    if (vehicle_redstone) {
                        showFuelHud(player, "bw_vv_vehicle_fuel_redstone", fuel);
                        fuelTitleVisible.set(playerId, true);
                    }
                    if (vehicle_coal) {
                        showFuelHud(player, "bw_vv_vehicle_fuel_coal", fuel);
                        fuelTitleVisible.set(playerId, true);
                    }
                    if (vehicle_mech) {
                        showMechControlsHud(player, "bw_vv_mech_projectile_cooldown", "bw_vv_vehicle_boost_cooldown", cooldown_projectile, cooldown_boost);
                        
                        fuelTitleVisible.set(playerId, true);
                    }
                }

            } else if (storedVehicle) {
                // No longer riding any vehicle
                onPlayerStopRiding(player, storedVehicle);
                playerVehicles.delete(playerId);
                playerJumpStates.delete(playerId);

                // Hide the fuel title once on exit
                if (fuelTitleVisible.get(playerId)) {
                    hidefueltitle(player);
                    fuelTitleVisible.set(playerId, false);
                }
                fuelTitleTick.delete(playerId);
            }
        }

        syncBackseatRotation();
    });

    // Register event subscriptions
    world.afterEvents.entityHitEntity.subscribe(handleEntityHitEvent);
    world.afterEvents.dataDrivenEntityTrigger.subscribe(handleDataDrivenEntityTrigger);
    
    // Cleanup when player leaves the world
    world.afterEvents.playerLeave.subscribe(({ playerId }) => {
        const vehicle = playerVehicles.get(playerId);
        if (vehicle) {
            // Clean up all vehicle-related data for the leaving player
            previousPropertyValues.delete(vehicle.id);
            previousYPositions.delete(vehicle.id);
            velocityHistories.delete(vehicle.id);
            positionHistories.delete(vehicle.id);
            playerVehicles.delete(playerId);
            playerJumpStates.delete(playerId);
        }

        // Clear fuel title state for that playerId
        fuelTitleTick.delete(playerId);
        fuelTitleVisible.delete(playerId);
    });


}
