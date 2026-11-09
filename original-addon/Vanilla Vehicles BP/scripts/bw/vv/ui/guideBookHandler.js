import { ActionFormData, ModalFormData } from "@minecraft/server-ui";

export function showGuidebook(player) {
  const main_page = new ActionFormData()
    .title("bw_vv.guide_book.main_page.title")
    .body({ "rawtext": [{ "translate": "bw_vv.guide_book.main_page.body" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.main_page.label" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.main_page.label2" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.main_page.label3" }] })
    .divider()
    .button("bw_vv.guide_book.button.vehicles", "textures/bw/vv/items/vehicle_spawns/minecar")
    .button("bw_vv.guide_book.button.utilities", "textures/bw/vv/items/redstone_compressor")
    .button("bw_vv.guide_book.button.systems", "textures/bw/vv/ui/guide_book/wind")
    .button("bw_vv.guide_book.button.items", "textures/bw/vv/items/blueprints")
    .button("bw_vv.guide_book.button.mobs", "textures/bw/vv/items/mob_spawns/mechanic")
    .divider()
    .button("bw_vv.guide_book.button.settings", "textures/bw/vv/ui/guide_book/settings_icon")
    .button("bw_vv.guide_book.button.support", "textures/bw/vv/ui/guide_book/support_icon")
    .label({ "rawtext": [{ "translate": "bw_vv.guide_book.main_page.about" }, {"text":": "}, {"text":"1.0.0"}] });

  main_page.show(player).then((response) => {
    if (!response.canceled) player.playSound("bw_vv.ui.guide_book.button");
    if (response.canceled) { player.playSound("bw_vv.ui.guide_book.close"); return; }
    switch (response.selection) {
      case 0: ShowVehiclesPage(player); break;
      case 1: ShowUtilitiesPage(player); break;
      case 2: ShowSystemsPage(player); break;
      case 3: OtherItemsPage(player); break;
      case 4: MobsPage(player); break;
      case 5: ShowSettingsPage(player); break;
      case 6: ShowSupportPage(player); break;
    }
  }).catch((error) => console.error("Error showing form: " + error));
}

const SETTINGS = {
  FUEL_ACTIONBAR: 'fuel_actionbar',
  WIND_SOUND: 'wind_sound',
  WIND_PARTICLES: 'wind_particles'
};

export function ShowSettingsPage(player) {
  const form = new ModalFormData()
    .title("bw_vv.guide_book.settings.title")
    .label("bw_vv.guide_book.settings.description")
    .label("bw_vv.guide_book.settings.general")
    .toggle("bw_vv.guide_book.settings.fuel_actionbar", { 
      defaultValue: getSetting(player, SETTINGS.FUEL_ACTIONBAR), 
      tooltip: "bw_vv.guide_book.settings.fuel_actionbar.tooltip" 
    })
    .toggle("bw_vv.guide_book.settings.wind_sound", { 
      defaultValue: getSetting(player, SETTINGS.WIND_SOUND),
    })
    .toggle("bw_vv.guide_book.settings.wind_particles", { 
      defaultValue: getSetting(player, SETTINGS.WIND_PARTICLES),
    });

  form.show(player).then((res) => {
    if (res.canceled) { 
      player.playSound("bw_vv.ui.guide_book.button_back");
      return; 
    }

    const settings = [
      SETTINGS.FUEL_ACTIONBAR,
      SETTINGS.WIND_SOUND,
      SETTINGS.WIND_PARTICLES
    ];

    // Skip label indices (0, 1) and get only toggle values
    res.formValues.slice(2).forEach((value, index) => {
      setSetting(player, settings[index], value);
    });

    player.playSound("bw_vv.ui.guide_book.button");
  }).catch((e) => console.error("Settings form error: " + e));
}

// Generic setting functions
function getSetting(player, settingKey) {
  return player.getDynamicProperty(`bw_vv:setting.${settingKey}`) ?? false;
}

function setSetting(player, settingKey, value) {
  player.setDynamicProperty(`bw_vv:setting.${settingKey}`, value);
}

function ShowVehiclesPage(player) {
  const vehicles_menu = new ActionFormData()
    .title("bw_vv.guide_book.vehicles_page.title")
    .body({ "rawtext": [{ "translate": "bw_vv.guide_book.vehicles_page.description" }] })
    .button("bw_vv.guide_book.button.back", "textures/bw/vv/ui/guide_book/back_icon")
    .divider()
    .button("bw_vv.guide_book.button.minecar", "textures/bw/vv/items/vehicle_spawns/minecar")
    .button("bw_vv.guide_book.button.mobtruck", "textures/bw/vv/items/vehicle_spawns/mobtruck")
    .button("bw_vv.guide_book.button.logcar", "textures/bw/vv/items/vehicle_spawns/logcar/oak")
    .button("bw_vv.guide_book.button.plane", "textures/bw/vv/items/vehicle_spawns/plane/oak")
    .button("bw_vv.guide_book.button.hot_air_balloon", "textures/bw/vv/items/vehicle_spawns/hot_air_balloon")
    .button("bw_vv.guide_book.button.steamboat", "textures/bw/vv/items/vehicle_spawns/steamboat")
    .button("bw_vv.guide_book.button.prismer", "textures/bw/vv/items/vehicle_spawns/prismer")
    .button("bw_vv.guide_book.button.mech", "textures/bw/vv/items/vehicle_spawns/mech");

  vehicles_menu.show(player).then((response) => {
    if (!response.canceled && response.selection !== 0) player.playSound("bw_vv.ui.guide_book.button");
    if (!response.canceled && response.selection == 0) player.playSound("bw_vv.ui.guide_book.button_back");
    if (response.canceled) { player.playSound("bw_vv.ui.guide_book.close"); return; }
    switch (response.selection) {
      case 0: showGuidebook(player); break;
      case 1: ShowVehiclePage_Minecar(player); break;
      case 2: ShowVehiclePage_Mobtruck(player); break;
      case 3: ShowVehiclePage_Logcar(player); break;
      case 4: ShowVehiclePage_Plane(player); break;
      case 5: ShowVehiclePage_HotAirBalloon(player); break;
      case 6: ShowVehiclePage_Steamboat(player); break;
      case 7: ShowVehiclePage_Prismer(player); break;
      case 8: ShowVehiclePage_Mech(player); break;
    }
  }).catch((error) => console.error("Error showing form: " + error));
}

function ShowUtilitiesPage(player) {
  const vehicles_menu = new ActionFormData()
    .title("bw_vv.guide_book.utilities_page.title")
    .body({ "rawtext": [{ "translate": "bw_vv.guide_book.utilities_page.description" }] })
    .button("bw_vv.guide_book.button.back", "textures/bw/vv/ui/guide_book/back_icon")
    .divider()
    .button("bw_vv.guide_book.button.redstone_compressor", "textures/bw/vv/items/redstone_compressor")
    .button("bw_vv.guide_book.button.wind_vane", "textures/bw/vv/items/wind_vane")
    .button("bw_vv.guide_book.button.smoke_bombs", "textures/bw/vv/items/smoke_bombs/red")
    .button("bw_vv.guide_book.button.tesla_helmet", "textures/bw/vv/items/tesla_helmet");

  vehicles_menu.show(player).then((response) => {
    if (!response.canceled && response.selection !== 0) player.playSound("bw_vv.ui.guide_book.button");
    if (!response.canceled && response.selection == 0) player.playSound("bw_vv.ui.guide_book.button_back");
    if (response.canceled) { player.playSound("bw_vv.ui.guide_book.close"); return; }
    switch (response.selection) {
      case 0: showGuidebook(player); break;
      case 1: ShowUtilityPage_RedstoneCompressor(player); break;
      case 2: ShowUtilityPage_WindVane(player); break;
      case 3: ShowUtilityPage_SmokeBombs(player); break;
      case 4: ShowUtilityPage_TeslaHelmet(player); break;
    }
  }).catch((error) => console.error("Error showing form: " + error));
}

function ShowSystemsPage(player) {
  const vehicles_menu = new ActionFormData()
    .title("bw_vv.guide_book.systems_page.title")
    .body({ "rawtext": [{ "translate": "bw_vv.guide_book.systems_page.description" }] })
    .button("bw_vv.guide_book.button.back", "textures/bw/vv/ui/guide_book/back_icon")
    .divider()
    .button("bw_vv.guide_book.button.boosting", "textures/bw/vv/ui/guide_book/speed")
    .button("bw_vv.guide_book.button.customisation", "textures/bw/vv/ui/guide_book/stickers")
    .button("bw_vv.guide_book.button.wind", "textures/bw/vv/ui/guide_book/wind_icon");

  vehicles_menu.show(player).then((response) => {
    if (!response.canceled && response.selection !== 0) player.playSound("bw_vv.ui.guide_book.button");
    if (!response.canceled && response.selection == 0) player.playSound("bw_vv.ui.guide_book.button_back");
    if (response.canceled) { player.playSound("bw_vv.ui.guide_book.close"); return; }
    switch (response.selection) {
      case 0: showGuidebook(player); break;
      case 1: ShowSystemPage_Boosting(player); break;
      case 2: ShowSystemPage_Customisation(player); break;
      case 3: ShowSystemPage_Wind(player); break;
    }
  }).catch((error) => console.error("Error showing form: " + error));
}

function OtherItemsPage(player) {
  const vehicles_menu = new ActionFormData()
    .title("bw_vv.guide_book.other_items_page.title")
    .body({ "rawtext": [{ "translate": "bw_vv.guide_book.other_items_page.description" }] })
    .button("bw_vv.guide_book.button.back", "textures/bw/vv/ui/guide_book/back_icon")
    .divider()
    .button("bw_vv.guide_book.button.blueprints", "textures/bw/vv/items/blueprints")
    .button("bw_vv.guide_book.button.mechanical_heart", "textures/bw/vv/items/mechanical_heart")
    .button("bw_vv.guide_book.button.hot_air_balloon_envelope", "textures/bw/vv/items/balloon_envelope/blank")
    .button("bw_vv.guide_book.button.smoke_bomb", "textures/bw/vv/items/smoke_bombs/empty")
    .button("bw_vv.guide_book.button.stickers", "textures/bw/vv/items/stickers/bloom")
    .button("bw_vv.guide_book.button.flags", "textures/bw/vv/items/prop_spawns/flags/white")
    .button("bw_vv.guide_book.button.propeller", "textures/bw/vv/items/propeller")
    .button("bw_vv.guide_book.button.redstone_pulser", "textures/bw/vv/items/redstone_pulser")
    .button("bw_vv.guide_book.button.compressed_redstone", "textures/bw/vv/items/compressed_redstone")
    .button("bw_vv.guide_book.button.tesla_helmet_upgrade", "textures/bw/vv/items/tesla_helmet_upgrade");

  vehicles_menu.show(player).then((response) => {
    if (!response.canceled && response.selection !== 0) player.playSound("bw_vv.ui.guide_book.button");
    if (!response.canceled && response.selection === 0) player.playSound("bw_vv.ui.guide_book.button_back");
    if (response.canceled) { player.playSound("bw_vv.ui.guide_book.close"); return; }
    switch (response.selection) {
      case 0: showGuidebook(player); break;
      case 1: ShowItemPage_Blueprints(player); break;
      case 2: ShowItemPage_MechanicalHeart(player); break;
      case 3: ShowItemPage_HotAirBalloonEnvelope(player); break;
      case 4: ShowItemPage_SmokeBomb(player); break;
      case 5: ShowItemPage_Stickers(player); break;
      case 6: ShowItemPage_Flags(player); break;
      case 7: ShowItemPage_Propeller(player); break;
      case 8: ShowItemPage_RedstonePulser(player); break;
      case 9: ShowItemPage_CompressedRedstone(player); break;
      case 10: ShowItemPage_TeslaHelmetUpgrade(player); break;
    }
  }).catch((error) => console.error("Error showing form: " + error));
}

function ShowSupportPage(player) {
  const vehicles_menu = new ActionFormData()
    .title("bw_vv.guide_book.support_page.title")
    .body({ "rawtext": [{ "translate": "bw_vv.guide_book.support_page.description" }] })
    .divider()
    .label({ "rawtext": [
      { "translate": "bw_vv.guide_book.support_page.email.title" },
      { "text": "\n" },
      { "translate": "bw_vv.guide_book.support_page.email" },
      { "text": "\n" },
      { "translate": "bw_vv.guide_book.support_page.discord.title" },
      { "text": "\n" },
      { "translate": "bw_vv.guide_book.support_page.discord" },
      { "text": "\n" },
      { "translate": "bw_vv.guide_book.support_page.instagram.title" },
      { "text": "\n" },
      { "translate": "bw_vv.guide_book.support_page.instagram" },
      { "text": "\n" },
      { "translate": "bw_vv.guide_book.support_page.x.title" },
      { "text": "\n" },
      { "translate": "bw_vv.guide_book.support_page.x" },
      { "text": "\n" },
      { "translate": "bw_vv.guide_book.support_page.youtube.title" },
      { "text": "\n" },
      { "translate": "bw_vv.guide_book.support_page.youtube" },
    ] })
    .button("bw_vv.guide_book.button.back", "textures/bw/vv/ui/guide_book/back_icon");

  vehicles_menu.show(player).then((response) => {
    if (!response.canceled && response.selection !== 0) player.playSound("bw_vv.ui.guide_book.button");
    if (!response.canceled && response.selection == 0) player.playSound("bw_vv.ui.guide_book.button_back");
    if (response.canceled) { player.playSound("bw_vv.ui.guide_book.close"); return; }
    switch (response.selection) {
      case 0: showGuidebook(player); break;
    }
  }).catch((error) => console.error("Error showing form: " + error));
}

function ShowVehiclePage_Minecar(player) {
  const minecar_menu = new ActionFormData()
    .title("bw_vv.guide_book.vehicle_page.minecar.title")
    .body({ "rawtext": [{ "translate": "bw_vv.guide_book.vehicle_page.minecar.description" }] })
    .button("bw_vv.guide_book.button.back", "textures/bw/vv/ui/guide_book/back_icon")
    .divider()
    .label({ "rawtext": [{ "translate": "bw_vv.guide_book.vehicle_page.features" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.customisable.feature" }] })
    
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.smoke_bomb.feature" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.break.pickaxe" }] })
    .label({ "rawtext": [{ "translate": "bw_vv.guide_book.vehicle_page.crafting" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.minecar.crafting" }] })
    .label({ "rawtext": [{ "translate": "bw_vv.guide_book.vehicle_page.fuel" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.fuel.coal_based" }] });

  minecar_menu.show(player).then((response) => {
    if (!response.canceled && response.selection !== 0) player.playSound("bw_vv.ui.guide_book.button");
    if (!response.canceled && response.selection == 0) { player.playSound("bw_vv.ui.guide_book.button_back"); ShowVehiclesPage(player); }
    if (response.canceled) { player.playSound("bw_vv.ui.guide_book.close"); return; }
  }).catch((error) => console.error("Error showing form: " + error));
}

function ShowVehiclePage_Mobtruck(player) {
  const minecar_menu = new ActionFormData()
    .title("bw_vv.guide_book.vehicle_page.mobtruck.title")
    .body({ "rawtext": [{ "translate": "bw_vv.guide_book.vehicle_page.mobtruck.description" }] })
    .button("bw_vv.guide_book.button.back", "textures/bw/vv/ui/guide_book/back_icon")
    .divider()
    .label({ "rawtext": [{ "translate": "bw_vv.guide_book.vehicle_page.features" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.customisable.feature" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.mobtruck.features" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.smoke_bomb.feature" }] })
    
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.boost.feature" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.break.pickaxe" }] })
    .label({ "rawtext": [{ "translate": "bw_vv.guide_book.vehicle_page.crafting" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.mobtruck.crafting" }] })
    .label({ "rawtext": [{ "translate": "bw_vv.guide_book.vehicle_page.fuel" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.fuel.redstone_based" }] });

  minecar_menu.show(player).then((response) => {
    if (!response.canceled && response.selection !== 0) player.playSound("bw_vv.ui.guide_book.button");
    if (!response.canceled && response.selection == 0) { player.playSound("bw_vv.ui.guide_book.button_back"); ShowVehiclesPage(player); }
    if (response.canceled) { player.playSound("bw_vv.ui.guide_book.close"); return; }
  }).catch((error) => console.error("Error showing form: " + error));
}

function ShowVehiclePage_Logcar(player) {
  const minecar_menu = new ActionFormData()
    .title("bw_vv.guide_book.vehicle_page.logcar.title")
    .body({ "rawtext": [{ "translate": "bw_vv.guide_book.vehicle_page.logcar.description" }] })
    .button("bw_vv.guide_book.button.back", "textures/bw/vv/ui/guide_book/back_icon")
    .divider()
    .label({ "rawtext": [{ "translate": "bw_vv.guide_book.vehicle_page.features" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.logcar.features" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.chestable.feature" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.smoke_bomb.feature" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.break.axe" }] })
    .label({ "rawtext": [{ "translate": "bw_vv.guide_book.vehicle_page.crafting" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.logcar.crafting" }] })
    
    .label({ "rawtext": [{ "translate": "bw_vv.guide_book.vehicle_page.fuel" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.fuel.coal_based" }] });

  minecar_menu.show(player).then((response) => {
    if (!response.canceled && response.selection !== 0) player.playSound("bw_vv.ui.guide_book.button");
    if (!response.canceled && response.selection == 0) { player.playSound("bw_vv.ui.guide_book.button_back"); ShowVehiclesPage(player); }
    if (response.canceled) { player.playSound("bw_vv.ui.guide_book.close"); return; }
  }).catch((error) => console.error("Error showing form: " + error));
}

function ShowVehiclePage_Plane(player) {
  const minecar_menu = new ActionFormData()
    .title("bw_vv.guide_book.vehicle_page.plane.title")
    .body({ "rawtext": [{ "translate": "bw_vv.guide_book.vehicle_page.plane.description" }] })
    .button("bw_vv.guide_book.button.back", "textures/bw/vv/ui/guide_book/back_icon")
    .divider()
    .label({ "rawtext": [{ "translate": "bw_vv.guide_book.vehicle_page.features" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.plane.features" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.plane.features2" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.plane.features3" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.smoke_bomb.feature" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.boost.feature" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.customisable_dyes.feature" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.break.axe" }] })
    .label({ "rawtext": [{ "translate": "bw_vv.guide_book.vehicle_page.crafting" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.plane.crafting" }] })
    .label({ "rawtext": [{ "translate": "bw_vv.guide_book.vehicle_page.fuel" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.fuel.redstone_based" }] });

  minecar_menu.show(player).then((response) => {
    if (!response.canceled && response.selection !== 0) player.playSound("bw_vv.ui.guide_book.button");
    if (!response.canceled && response.selection == 0) { player.playSound("bw_vv.ui.guide_book.button_back"); ShowVehiclesPage(player); }
    if (response.canceled) { player.playSound("bw_vv.ui.guide_book.close"); return; }
  }).catch((error) => console.error("Error showing form: " + error));
}

function ShowVehiclePage_HotAirBalloon(player) {
  const minecar_menu = new ActionFormData()
    .title("bw_vv.guide_book.vehicle_page.hot_air_balloon.title")
    .body({ "rawtext": [{ "translate": "bw_vv.guide_book.vehicle_page.hot_air_balloon.description" }] })
    .button("bw_vv.guide_book.button.back", "textures/bw/vv/ui/guide_book/back_icon")
    .divider()
    .label({ "rawtext": [{ "translate": "bw_vv.guide_book.vehicle_page.features" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.hot_air_balloon.features" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.hot_air_balloon.features2" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.hot_air_balloon.features3" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.bundleable.feature" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.customisable_dyes.feature" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.break.axe" }] })
    .label({ "rawtext": [{ "translate": "bw_vv.guide_book.vehicle_page.crafting" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.hot_air_balloon.crafting" }] })
    .label({ "rawtext": [{ "translate": "bw_vv.guide_book.vehicle_page.fuel" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.fuel.flint_and_steel" }] });

  minecar_menu.show(player).then((response) => {
    if (!response.canceled && response.selection !== 0) player.playSound("bw_vv.ui.guide_book.button");
    if (!response.canceled && response.selection == 0) { player.playSound("bw_vv.ui.guide_book.button_back"); ShowVehiclesPage(player); }
    if (response.canceled) { player.playSound("bw_vv.ui.guide_book.close"); return; }
  }).catch((error) => console.error("Error showing form: " + error));
}

function ShowVehiclePage_Steamboat(player) {
  const minecar_menu = new ActionFormData()
    .title("bw_vv.guide_book.vehicle_page.steamboat.title")
    .body({ "rawtext": [{ "translate": "bw_vv.guide_book.vehicle_page.steamboat.description" }] })
    .button("bw_vv.guide_book.button.back", "textures/bw/vv/ui/guide_book/back_icon")
    .divider()
    .label({ "rawtext": [{ "translate": "bw_vv.guide_book.vehicle_page.features" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.steamboat.features" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.steamboat.features2" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.steamboat.features3" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.bundleable.feature" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.customisable_dyes.feature" }] })
    
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.smoke_bomb.feature" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.break.axe" }] })
    .label({ "rawtext": [{ "translate": "bw_vv.guide_book.vehicle_page.crafting" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.steamboat.crafting" }] })
    .label({ "rawtext": [{ "translate": "bw_vv.guide_book.vehicle_page.fuel" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.fuel.coal_based" }] });

  minecar_menu.show(player).then((response) => {
    if (!response.canceled && response.selection !== 0) player.playSound("bw_vv.ui.guide_book.button");
    if (!response.canceled && response.selection == 0) { player.playSound("bw_vv.ui.guide_book.button_back"); ShowVehiclesPage(player); }
    if (response.canceled) { player.playSound("bw_vv.ui.guide_book.close"); return; }
  }).catch((error) => console.error("Error showing form: " + error));
}

function ShowVehiclePage_Prismer(player) {
  const minecar_menu = new ActionFormData()
    .title("bw_vv.guide_book.vehicle_page.prismer.title")
    .body({ "rawtext": [{ "translate": "bw_vv.guide_book.vehicle_page.prismer.description" }] })
    .button("bw_vv.guide_book.button.back", "textures/bw/vv/ui/guide_book/back_icon")
    .divider()
    .label({ "rawtext": [{ "translate": "bw_vv.guide_book.vehicle_page.features" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.prismer.features" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.break.pickaxe" }] })
    .label({ "rawtext": [{ "translate": "bw_vv.guide_book.vehicle_page.crafting" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.prismer.crafting" }] })
    .label({ "rawtext": [{ "translate": "bw_vv.guide_book.vehicle_page.fuel" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.fuel.conduit" }] });

  minecar_menu.show(player).then((response) => {
    if (!response.canceled && response.selection !== 0) player.playSound("bw_vv.ui.guide_book.button");
    if (!response.canceled && response.selection == 0) { player.playSound("bw_vv.ui.guide_book.button_back"); ShowVehiclesPage(player); }
    if (response.canceled) { player.playSound("bw_vv.ui.guide_book.close"); return; }
  }).catch((error) => console.error("Error showing form: " + error));
}

function ShowVehiclePage_Mech(player) {
  const minecar_menu = new ActionFormData()
    .title("bw_vv.guide_book.vehicle_page.mech.title")
    .body({ "rawtext": [{ "translate": "bw_vv.guide_book.vehicle_page.mech.description" }] })
    .button("bw_vv.guide_book.button.back", "textures/bw/vv/ui/guide_book/back_icon")
    .divider()
    .label({ "rawtext": [{ "translate": "bw_vv.guide_book.vehicle_page.features" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.mech.features" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.mech.features2" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.mech.features3" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.chestable.feature" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.customisable_dyes.feature" }] })
    
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.break.pickaxe" }] })
    .label({ "rawtext": [{ "translate": "bw_vv.guide_book.vehicle_page.crafting" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.mech.crafting" }] })
    .label({ "rawtext": [{ "translate": "bw_vv.guide_book.vehicle_page.fuel" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.fuel.no_fuel" }] });

  minecar_menu.show(player).then((response) => {
    if (!response.canceled && response.selection !== 0) player.playSound("bw_vv.ui.guide_book.button");
    if (!response.canceled && response.selection == 0) { player.playSound("bw_vv.ui.guide_book.button_back"); ShowVehiclesPage(player); }
    if (response.canceled) { player.playSound("bw_vv.ui.guide_book.close"); return; }
  }).catch((error) => console.error("Error showing form: " + error));
}

// Utilities Pages
function ShowUtilityPage_RedstoneCompressor(player) {
  const redstone_compressor_menu = new ActionFormData()
    .title("bw_vv.guide_book.utility_page.redstone_compressor.title")
    .body({ "rawtext": [{ "translate": "bw_vv.guide_book.utility_page.redstone_compressor.description" }] })
    .button("bw_vv.guide_book.button.back", "textures/bw/vv/ui/guide_book/back_icon")
    .divider()
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.utility_page.redstone_compressor.features" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.break.pickaxe" }] })
    .label({ "rawtext": [{ "translate": "bw_vv.guide_book.vehicle_page.crafting" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.utility_page.redstone_compressor.crafting" }] });

  redstone_compressor_menu.show(player).then((response) => {
    if (!response.canceled && response.selection !== 0) player.playSound("bw_vv.ui.guide_book.button");
    if (!response.canceled && response.selection == 0) { player.playSound("bw_vv.ui.guide_book.button_back"); ShowUtilitiesPage(player); }
    if (response.canceled) { player.playSound("bw_vv.ui.guide_book.close"); return; }
  }).catch((error) => console.error("Error showing form: " + error));
}

function ShowUtilityPage_WindVane(player) {
  const wind_vane_menu = new ActionFormData()
    .title("bw_vv.guide_book.utility_page.wind_vane.title")
    .body({ "rawtext": [{ "translate": "bw_vv.guide_book.utility_page.wind_vane.description" }] })
    .button("bw_vv.guide_book.button.back", "textures/bw/vv/ui/guide_book/back_icon")
    .divider()
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.utility_page.wind_vane.features" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.utility_page.wind_vane.features2" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.vehicle_page.break.pickaxe" }] })
    .label({ "rawtext": [{ "translate": "bw_vv.guide_book.vehicle_page.crafting" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.utility_page.wind_vane.crafting" }] });

  wind_vane_menu.show(player).then((response) => {
    if (!response.canceled && response.selection !== 0) player.playSound("bw_vv.ui.guide_book.button");
    if (!response.canceled && response.selection == 0) { player.playSound("bw_vv.ui.guide_book.button_back"); ShowUtilitiesPage(player); }
    if (response.canceled) { player.playSound("bw_vv.ui.guide_book.close"); return; }
  }).catch((error) => console.error("Error showing form: " + error));
}

function ShowUtilityPage_SmokeBombs(player) {
  const smoke_bombs_menu = new ActionFormData()
    .title("bw_vv.guide_book.utility_page.smoke_bombs.title")
    .body({ "rawtext": [{ "translate": "bw_vv.guide_book.utility_page.smoke_bombs.description" }] })
    .button("bw_vv.guide_book.button.back", "textures/bw/vv/ui/guide_book/back_icon")
    .divider()
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.utility_page.smoke_bombs.features" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.utility_page.smoke_bombs.features2" }] })
    .label({ "rawtext": [{ "translate": "bw_vv.guide_book.vehicle_page.crafting" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.utility_page.smoke_bombs.crafting" }] });

  smoke_bombs_menu.show(player).then((response) => {
    if (!response.canceled && response.selection !== 0) player.playSound("bw_vv.ui.guide_book.button");
    if (!response.canceled && response.selection == 0) { player.playSound("bw_vv.ui.guide_book.button_back"); ShowUtilitiesPage(player); }
    if (response.canceled) { player.playSound("bw_vv.ui.guide_book.close"); return; }
  }).catch((error) => console.error("Error showing form: " + error));
}

function ShowUtilityPage_TeslaHelmet(player) {
  const tesla_helmet_menu = new ActionFormData()
    .title("bw_vv.guide_book.utility_page.tesla_helmet.title")
    .body({ "rawtext": [{ "translate": "bw_vv.guide_book.utility_page.tesla_helmet.description" }] })
    .button("bw_vv.guide_book.button.back", "textures/bw/vv/ui/guide_book/back_icon")
    .divider()
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.utility_page.tesla_helmet.features" }] })
    .label({ "rawtext": [{ "translate": "bw_vv.guide_book.vehicle_page.crafting" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.utility_page.tesla_helmet.crafting" }] });

  tesla_helmet_menu.show(player).then((response) => {
    if (!response.canceled && response.selection !== 0) player.playSound("bw_vv.ui.guide_book.button");
    if (!response.canceled && response.selection == 0) { player.playSound("bw_vv.ui.guide_book.button_back"); ShowUtilitiesPage(player); }
    if (response.canceled) { player.playSound("bw_vv.ui.guide_book.close"); return; }
  }).catch((error) => console.error("Error showing form: " + error));
}

// Systems Pages
function ShowSystemPage_Boosting(player) {
  const boosting_menu = new ActionFormData()
    .title("bw_vv.guide_book.system_page.boosting.title")
    .body({ "rawtext": [{ "translate": "bw_vv.guide_book.system_page.boosting.description" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.system_page.boosting.features" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.system_page.boosting.features2" }] })
    .divider()
    .button("bw_vv.guide_book.button.back", "textures/bw/vv/ui/guide_book/back_icon");

  boosting_menu.show(player).then((response) => {
    if (!response.canceled && response.selection !== 0) player.playSound("bw_vv.ui.guide_book.button");
    if (!response.canceled && response.selection == 0) { player.playSound("bw_vv.ui.guide_book.button_back"); ShowSystemsPage(player); }
    if (response.canceled) { player.playSound("bw_vv.ui.guide_book.close"); return; }
  }).catch((error) => console.error("Error showing form: " + error));
}

function ShowSystemPage_Customisation(player) {
  const customisation_menu = new ActionFormData()
    .title("bw_vv.guide_book.system_page.customisation.title")
    .body({ "rawtext": [{ "translate": "bw_vv.guide_book.system_page.customisation.description" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.system_page.customisation.features.1" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.system_page.customisation.features.2" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.system_page.customisation.features.3" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.system_page.customisation.features.4" }] })
    .divider()
    .button("bw_vv.guide_book.button.back", "textures/bw/vv/ui/guide_book/back_icon");

  customisation_menu.show(player).then((response) => {
    if (!response.canceled && response.selection !== 0) player.playSound("bw_vv.ui.guide_book.button");
    if (!response.canceled && response.selection == 0) { player.playSound("bw_vv.ui.guide_book.button_back"); ShowSystemsPage(player); }
    if (response.canceled) { player.playSound("bw_vv.ui.guide_book.close"); return; }
  }).catch((error) => console.error("Error showing form: " + error));
}

function ShowSystemPage_Wind(player) {
  const wind_menu = new ActionFormData()
    .title("bw_vv.guide_book.system_page.wind.title")
    .body({ "rawtext": [{ "translate": "bw_vv.guide_book.system_page.wind.description" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.system_page.wind.features" }] })
    .divider()
    .button("bw_vv.guide_book.button.back", "textures/bw/vv/ui/guide_book/back_icon");

  wind_menu.show(player).then((response) => {
    if (!response.canceled && response.selection !== 0) player.playSound("bw_vv.ui.guide_book.button");
    if (!response.canceled && response.selection == 0) { player.playSound("bw_vv.ui.guide_book.button_back"); ShowSystemsPage(player); }
    if (response.canceled) { player.playSound("bw_vv.ui.guide_book.close"); return; }
  }).catch((error) => console.error("Error showing form: " + error));
}

function ShowItemPage_Propeller(player) {
  const propeller_menu = new ActionFormData()
    .title("bw_vv.guide_book.item_page.propeller.title")
    .body({ "rawtext": [{ "translate": "bw_vv.guide_book.item_page.propeller.description" }] })
    .button("bw_vv.guide_book.button.back", "textures/bw/vv/ui/guide_book/back_icon")
    .divider()
    .label({ "rawtext": [{ "translate": "bw_vv.guide_book.vehicle_page.crafting" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.item_page.propeller.crafting" }] });

  propeller_menu.show(player).then((response) => {
    if (!response.canceled && response.selection !== 0) player.playSound("bw_vv.ui.guide_book.button");
    if (!response.canceled && response.selection == 0) { player.playSound("bw_vv.ui.guide_book.button_back"); OtherItemsPage(player); }
    if (response.canceled) { player.playSound("bw_vv.ui.guide_book.close"); return; }
  }).catch((error) => console.error("Error showing form: " + error));
}

function ShowItemPage_RedstonePulser(player) {
  const pulser_menu = new ActionFormData()
    .title("bw_vv.guide_book.item_page.redstone_pulser.title")
    .body({ "rawtext": [{ "translate": "bw_vv.guide_book.item_page.redstone_pulser.description" }] })
    .button("bw_vv.guide_book.button.back", "textures/bw/vv/ui/guide_book/back_icon")
    .divider()
    .label({ "rawtext": [{ "translate": "bw_vv.guide_book.vehicle_page.crafting" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.item_page.redstone_pulser.crafting" }] });

  pulser_menu.show(player).then((response) => {
    if (!response.canceled && response.selection !== 0) player.playSound("bw_vv.ui.guide_book.button");
    if (!response.canceled && response.selection == 0) { player.playSound("bw_vv.ui.guide_book.button_back"); OtherItemsPage(player); }
    if (response.canceled) { player.playSound("bw_vv.ui.guide_book.close"); return; }
  }).catch((error) => console.error("Error showing form: " + error));
}

function ShowItemPage_HotAirBalloonEnvelope(player) {
  const envelope_menu = new ActionFormData()
    .title("bw_vv.guide_book.item_page.hot_air_balloon.envelope.title")
    .body({ "rawtext": [{ "translate": "bw_vv.guide_book.item_page.hot_air_balloon.envelope.description" }] })
    .button("bw_vv.guide_book.button.back", "textures/bw/vv/ui/guide_book/back_icon")
    .divider()
    .label({ "rawtext": [{ "translate": "bw_vv.guide_book.vehicle_page.crafting" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.item_page.hot_air_balloon.envelope.blank.crafting" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.item_page.hot_air_balloon.envelope.creeper.crafting" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.item_page.hot_air_balloon.envelope.striped.crafting" }] });

  envelope_menu.show(player).then((response) => {
    if (!response.canceled && response.selection !== 0) player.playSound("bw_vv.ui.guide_book.button");
    if (!response.canceled && response.selection == 0) { player.playSound("bw_vv.ui.guide_book.button_back"); OtherItemsPage(player); }
    if (response.canceled) { player.playSound("bw_vv.ui.guide_book.close"); return; }
  }).catch((error) => console.error("Error showing form: " + error));
}

function ShowItemPage_SmokeBomb(player) {
  const smoke_bomb_menu = new ActionFormData()
    .title("bw_vv.guide_book.item_page.smoke_bomb.title")
    .body({ "rawtext": [{ "translate": "bw_vv.guide_book.item_page.smoke_bomb.description" }] })
    .button("bw_vv.guide_book.button.back", "textures/bw/vv/ui/guide_book/back_icon")
    .divider()
    .label({ "rawtext": [{ "translate": "bw_vv.guide_book.vehicle_page.crafting" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.item_page.smoke_bomb.crafting" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.item_page.smoke_bomb.crafting2" }] });

  smoke_bomb_menu.show(player).then((response) => {
    if (!response.canceled && response.selection !== 0) player.playSound("bw_vv.ui.guide_book.button");
    if (!response.canceled && response.selection == 0) { player.playSound("bw_vv.ui.guide_book.button_back"); OtherItemsPage(player); }
    if (response.canceled) { player.playSound("bw_vv.ui.guide_book.close"); return; }
  }).catch((error) => console.error("Error showing form: " + error));
}

function ShowItemPage_MechanicalHeart(player) {
  const mechanical_heart_menu = new ActionFormData()
    .title("bw_vv.guide_book.item_page.mechanical_heart.title")
    .body({ "rawtext": [{ "translate": "bw_vv.guide_book.item_page.mechanical_heart.description" }] })
    .button("bw_vv.guide_book.button.back", "textures/bw/vv/ui/guide_book/back_icon")
    .divider()
    .label({ "rawtext": [{ "translate": "bw_vv.guide_book.vehicle_page.crafting" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.item_page.mechanical_heart.crafting" }] });

  mechanical_heart_menu.show(player).then((response) => {
    if (!response.canceled && response.selection !== 0) player.playSound("bw_vv.ui.guide_book.button");
    if (!response.canceled && response.selection == 0) { player.playSound("bw_vv.ui.guide_book.button_back"); OtherItemsPage(player); }
    if (response.canceled) { player.playSound("bw_vv.ui.guide_book.close"); return; }
  }).catch((error) => console.error("Error showing form: " + error));
}

function ShowItemPage_Blueprints(player) {
  const blueprints_menu = new ActionFormData()
    .title("bw_vv.guide_book.item_page.blueprints.title")
    .body({ "rawtext": [{ "translate": "bw_vv.guide_book.item_page.blueprints.description" }] })
    .button("bw_vv.guide_book.button.back", "textures/bw/vv/ui/guide_book/back_icon")
    .divider()
    .label({ "rawtext": [{ "translate": "bw_vv.guide_book.vehicle_page.crafting" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.item_page.blueprints.crafting" }] });

  blueprints_menu.show(player).then((response) => {
    if (!response.canceled && response.selection !== 0) player.playSound("bw_vv.ui.guide_book.button");
    if (!response.canceled && response.selection == 0) { player.playSound("bw_vv.ui.guide_book.button_back"); OtherItemsPage(player); }
    if (response.canceled) { player.playSound("bw_vv.ui.guide_book.close"); return; }
  }).catch((error) => console.error("Error showing form: " + error));
}

function ShowItemPage_CompressedRedstone(player) {
  const compressed_redstone_menu = new ActionFormData()
    .title("bw_vv.guide_book.item_page.compressed_redstone.title")
    .body({ "rawtext": [{ "translate": "bw_vv.guide_book.item_page.compressed_redstone.description" }] })
    .button("bw_vv.guide_book.button.back", "textures/bw/vv/ui/guide_book/back_icon")
    .divider()
    .label({ "rawtext": [{ "translate": "bw_vv.guide_book.vehicle_page.crafting" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.item_page.compressed_redstone.crafting" }] });

  compressed_redstone_menu.show(player).then((response) => {
    if (!response.canceled && response.selection !== 0) player.playSound("bw_vv.ui.guide_book.button");
    if (!response.canceled && response.selection == 0) { player.playSound("bw_vv.ui.guide_book.button_back"); OtherItemsPage(player); }
    if (response.canceled) { player.playSound("bw_vv.ui.guide_book.close"); return; }
  }).catch((error) => console.error("Error showing form: " + error));
}

function ShowItemPage_Stickers(player) {
  const stickers_menu = new ActionFormData()
    .title("bw_vv.guide_book.item_page.stickers.title")
    .body({ "rawtext": [{ "translate": "bw_vv.guide_book.item_page.stickers.description" }] })
    .button("bw_vv.guide_book.button.back", "textures/bw/vv/ui/guide_book/back_icon")
    .divider()
    .label({ "rawtext": [{ "translate": "bw_vv.guide_book.vehicle_page.crafting" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.item_page.stickers.crafting" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.item_page.stickers.crafting2" }] });

  stickers_menu.show(player).then((response) => {
    if (!response.canceled && response.selection !== 0) player.playSound("bw_vv.ui.guide_book.button");
    if (!response.canceled && response.selection == 0) { player.playSound("bw_vv.ui.guide_book.button_back"); OtherItemsPage(player); }
    if (response.canceled) { player.playSound("bw_vv.ui.guide_book.close"); return; }
  }).catch((error) => console.error("Error showing form: " + error));
}

function ShowItemPage_Flags(player) {
  const flags_menu = new ActionFormData()
    .title("bw_vv.guide_book.item_page.flags.title")
    .body({ "rawtext": [{ "translate": "bw_vv.guide_book.item_page.flags.description" }] })
    .button("bw_vv.guide_book.button.back", "textures/bw/vv/ui/guide_book/back_icon")
    .divider()
    .label({ "rawtext": [{ "translate": "bw_vv.guide_book.vehicle_page.crafting" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.item_page.flags.crafting" }] });

  flags_menu.show(player).then((response) => {
    if (!response.canceled && response.selection !== 0) player.playSound("bw_vv.ui.guide_book.button");
    if (!response.canceled && response.selection == 0) { player.playSound("bw_vv.ui.guide_book.button_back"); OtherItemsPage(player); }
    if (response.canceled) { player.playSound("bw_vv.ui.guide_book.close"); return; }
  }).catch((error) => console.error("Error showing form: " + error));
}

function MobsPage(player) {
  const mobs_menu = new ActionFormData()
    .title("bw_vv.guide_book.mobs_page.title")
    .body({ "rawtext": [{ "translate": "bw_vv.guide_book.mobs_page.description" }] })
    .button("bw_vv.guide_book.button.back", "textures/bw/vv/ui/guide_book/back_icon")
    .divider()
    .button("bw_vv.guide_book.button.mechanic", "textures/bw/vv/items/mob_spawns/mechanic");

  mobs_menu.show(player).then((response) => {
    if (!response.canceled && response.selection !== 0) player.playSound("bw_vv.ui.guide_book.button");
    if (!response.canceled && response.selection == 0) player.playSound("bw_vv.ui.guide_book.button_back");
    if (response.canceled) { player.playSound("bw_vv.ui.guide_book.close"); return; }
    switch (response.selection) {
      case 0: showGuidebook(player); break;
      case 1: ShowMobPage_Mechanic(player); break;
    }
  }).catch((error) => console.error("Error showing form: " + error));
}

function ShowMobPage_Mechanic(player) {
  const mechanic_menu = new ActionFormData()
    .title("bw_vv.guide_book.mob_page.mechanic.title")
    .body({ "rawtext": [{ "translate": "bw_vv.guide_book.mob_page.mechanic.description" }] })
    .button("bw_vv.guide_book.button.back", "textures/bw/vv/ui/guide_book/back_icon")
    .divider()
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.mob_page.mechanic.trading" }] });

  mechanic_menu.show(player).then((response) => {
    if (!response.canceled && response.selection !== 0) player.playSound("bw_vv.ui.guide_book.button");
    if (!response.canceled && response.selection == 0) { player.playSound("bw_vv.ui.guide_book.button_back"); MobsPage(player); }
    if (response.canceled) { player.playSound("bw_vv.ui.guide_book.close"); return; }
  }).catch((error) => console.error("Error showing form: " + error));
}

function ShowItemPage_TeslaHelmetUpgrade(player) {
  const tesla_helmet_upgrade_menu = new ActionFormData()
    .title("bw_vv.guide_book.item_page.tesla_helmet_upgrade.title")
    .body({ "rawtext": [{ "translate": "bw_vv.guide_book.item_page.tesla_helmet_upgrade.description" }] })
    .button("bw_vv.guide_book.button.back", "textures/bw/vv/ui/guide_book/back_icon")
    .divider()
    .label({ "rawtext": [{ "translate": "bw_vv.guide_book.vehicle_page.crafting" }] })
    .label({ "rawtext": [{ "text": "■ " }, { "translate": "bw_vv.guide_book.item_page.tesla_helmet_upgrade.crafting" }] });

  tesla_helmet_upgrade_menu.show(player).then((response) => {
    if (!response.canceled && response.selection !== 0) player.playSound("bw_vv.ui.guide_book.button");
    if (!response.canceled && response.selection == 0) { player.playSound("bw_vv.ui.guide_book.button_back"); OtherItemsPage(player); }
    if (response.canceled) { player.playSound("bw_vv.ui.guide_book.close"); return; }
  }).catch((error) => console.error("Error showing form: " + error));
}
