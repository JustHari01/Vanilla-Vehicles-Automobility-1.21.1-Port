import { world, system, MolangVariableMap } from "@minecraft/server";

const WIND_MESSAGES = [
  "bw_vv.wind_change.1", "bw_vv.wind_change.2", "bw_vv.wind_change.3",
  "bw_vv.wind_change.4", "bw_vv.wind_change.5", "bw_vv.wind_change.6",
  "bw_vv.wind_change.7", "bw_vv.wind_change.8", "bw_vv.wind_change.9",
  "bw_vv.wind_change.10", "bw_vv.wind_change.11", "bw_vv.wind_change.12"
];

// Wind state
export let currentWind = "N";
const windDirections = ["N", "E", "S", "W"];
let windIndex = 0;

const conditionWindParticle = (block) => block && (block.typeId === "bw_vv:wind_vane" || (block.typeId.startsWith("bw_vv:flag.") && !block.typeId.endsWith(".pole")));

// Track time for wind changes
const WIND_CHANGE_INTERVAL = 72000;
let lastWindChange = 0;

// --- OPTIMIZATION: Wind Vane Cache ---
// Stores known wind vanes so we don't have to scan thousands of blocks every second
const activeWindVanes = new Map();

world.afterEvents.worldLoad.subscribe(() => {
    lastWindChange = world.getAbsoluteTime();
    
    try {
        const savedWind = world.getDynamicProperty("windDirection");
        if (savedWind && windDirections.includes(savedWind)) {
            currentWind = savedWind;
            windIndex = windDirections.indexOf(savedWind);
        }
    } catch (e) {}
});

function saveWindDirection() {
    try { world.setDynamicProperty("windDirection", currentWind); } catch (e) {}
}

export function setWind(direction) {
    if (windDirections.includes(direction)) {
        currentWind = direction;
        windIndex = windDirections.indexOf(direction);
        saveWindDirection();
        updateAllWindVanes();
        world.sendMessage(`§eWind direction changed!`);
    }
}

export function getWind() { return currentWind; }

function getNextWind() {
    const nextIndex = (windIndex - 1 + windDirections.length) % windDirections.length;
    return windDirections[nextIndex];
}

function getWindName(direction) {
  const names = {
    N: { translate: "bw_vv.cardinal_direction.north" },
    E: { translate: "bw_vv.cardinal_direction.east" },
    S: { translate: "bw_vv.cardinal_direction.south" },
    W: { translate: "bw_vv.cardinal_direction.west" }
  };
  return names[direction] ?? String(direction);
}

/**
 * Updates ONLY the cached wind vanes instead of scanning 270k blocks
 */
function updateAllWindVanes() {
    for (const [key, vane] of activeWindVanes.entries()) {
        try {
            const block = vane.dimension.getBlock({x: vane.x, y: vane.y, z: vane.z});
            if (conditionWindParticle(block)) {
                block.setPermutation(block.permutation.withState("bw_vv:wind_dir", currentWind));
            } else {
                activeWindVanes.delete(key); // Remove destroyed/missing vanes from cache
            }
        } catch (e) {
            // Chunk is unloaded. It will self-heal when loaded and scanned later.
        }
    }
}

let soundTickCounter = 0;

function ParticleAndSound() {
    soundTickCounter++;
    const shouldPlaySound = soundTickCounter >= 3;
    if (shouldPlaySound) soundTickCounter = 0;

    // Pre-calculate wind variables once per tick
    let windDirX = 0; let windDirZ = 0;
    switch (currentWind) {
        case "N": windDirX = -0.1; break;
        case "E": windDirZ = 0.1; break;
        case "S": windDirX = 0.1; break;
        case "W": windDirZ = -0.1; break;
    }

    const molangVariables = new MolangVariableMap();
    molangVariables.setFloat("variable.wind_direction_x", windDirX);
    molangVariables.setFloat("variable.wind_direction_z", windDirZ);

    for (const player of world.getAllPlayers()) {
        const pLoc = player.location;
        let nearestVane = null;
        let nearestDistSq = 256; // Max radius 16 (16^2 = 256)

        // Loop ONLY through known vanes (lightning fast)
        for (const [key, vane] of activeWindVanes.entries()) {
            if (vane.dimension.id !== player.dimension.id) continue;
            
            const distSq = (pLoc.x - vane.x)**2 + (pLoc.y - vane.y)**2 + (pLoc.z - vane.z)**2;
            if (distSq < nearestDistSq) {
                nearestDistSq = distSq;
                nearestVane = vane;
            }
        }

        if (nearestVane) {
            const windparticlesetting = player.getDynamicProperty("bw_vv:setting.wind_particles") ?? true;
            if (windparticlesetting) {
                player.dimension.spawnParticle(
                    "bw_vv:wind",
                    { x: nearestVane.x + 0.5, y: nearestVane.y + 0.5, z: nearestVane.z + 0.5 },
                    molangVariables
                );
            }
            
            if (shouldPlaySound) {
                const windSoundSetting = player.getDynamicProperty("bw_vv:setting.wind_sound") ?? true;
                if (windSoundSetting) {
                    player.playSound("bw_vv.ui.ambient.wind", { location: nearestVane, volume: 0.65, pitch: 1.0 });
                }
            }
        }
    }
}

function windSystemTick() {
    const currentTime = world.getAbsoluteTime();
    ParticleAndSound();
    
    if (currentTime - lastWindChange >= WIND_CHANGE_INTERVAL) {
        windIndex = (windIndex + 1) % windDirections.length;
        currentWind = windDirections[windIndex];
        
        saveWindDirection();
        updateAllWindVanes();
        
        const nextWind = getNextWind();
        const key = WIND_MESSAGES[Math.floor(Math.random() * WIND_MESSAGES.length)];
        world.sendMessage({
            rawtext: [
                { text: "§e" },
                { translate: key, with: { rawtext: [getWindName(nextWind)] } }
            ],
        });
        
        lastWindChange = currentTime;
    }
}

/**
 * Background scanner: Smoothly finds old wind vanes around players over time without lagging.
 */
function* scanForVanesBackground() {
    while (true) {
        for (const player of world.getAllPlayers()) {
            const { x, y, z } = player.location;
            const dim = player.dimension;
            const radius = 16;
            const rSq = radius * radius;
            let queries = 0;

            for (let dx = -radius; dx <= radius; dx++) {
                for (let dy = -radius; dy <= radius; dy++) {
                    for (let dz = -radius; dz <= radius; dz++) {
                        if (dx*dx + dy*dy + dz*dz > rSq) continue; // Keep spherical

                        const bx = Math.floor(x + dx);
                        const by = Math.floor(y + dy);
                        const bz = Math.floor(z + dz);
                        const key = `${bx},${by},${bz},${dim.id}`;

                        if (!activeWindVanes.has(key)) {
                            try {
                                const block = dim.getBlock({ x: bx, y: by, z: bz });
                                if (conditionWindParticle(block)) {
                                    activeWindVanes.set(key, {x: bx, y: by, z: bz, dimension: dim});
                                    
                                    // Self-heal wind direction if it loaded improperly
                                    const currentState = block.permutation.getState("bw_vv:wind_dir");
                                    if (currentState !== currentWind) {
                                        block.setPermutation(block.permutation.withState("bw_vv:wind_dir", currentWind));
                                    }
                                }
                            } catch (e) { /* Ignore unloaded chunks */ }
                        }

                        queries++;
                        // Yield execution to the main thread every 500 blocks so the server never drops TPS
                        if (queries >= 500) {
                            queries = 0;
                            yield;
                        }
                    }
                }
            }
        }
        yield; // Pause before looping players again
    }
}

export function initWindSystem() {
    system.runInterval(() => {
        windSystemTick();
    }, 20);
    
    // Start the background scanner
    system.runJob(scanForVanesBackground());
    
    system.afterEvents.scriptEventReceive.subscribe((event) => {
        if (event.id === "bw:setwind") {
            const direction = event.message.toUpperCase();
            setWind(direction);
        }
    });
    
    // Add dynamically placed vanes directly to the cache
    world.afterEvents.playerPlaceBlock.subscribe((event) => {
        if (conditionWindParticle(event.block)) {
            event.block.setPermutation(event.block.permutation.withState("bw_vv:wind_dir", currentWind));
            const key = `${event.block.x},${event.block.y},${event.block.z},${event.dimension.id}`;
            activeWindVanes.set(key, {x: event.block.x, y: event.block.y, z: event.block.z, dimension: event.dimension});
        }
    });

    // Remove broken blocks from the cache blindly to keep it clean
    world.afterEvents.playerBreakBlock.subscribe((event) => {
        const key = `${event.block.x},${event.block.y},${event.block.z},${event.dimension.id}`;
        activeWindVanes.delete(key);
    });
}