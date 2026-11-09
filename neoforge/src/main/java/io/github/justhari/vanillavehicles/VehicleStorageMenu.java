package io.github.justhari.vanillavehicles;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public class VehicleStorageMenu extends AbstractContainerMenu {
    private final net.minecraft.world.entity.Entity owner;
    private final SimpleContainer inventory;
    private final int rows, storageSlots;
    public VehicleStorageMenu(int id,Inventory playerInventory,VehicleEntity vehicle){
        this(id,playerInventory,vehicle,vehicle.inventory);
    }
    public VehicleStorageMenu(int id,Inventory playerInventory,net.minecraft.world.entity.Entity owner,SimpleContainer inventory){
        super(inventory.getContainerSize()<=9?MenuType.GENERIC_9x1:MenuType.GENERIC_9x3,id);
        this.owner=owner;this.inventory=inventory;rows=inventory.getContainerSize()<=9?1:3;storageSlots=rows*9;
        var disabled=new SimpleContainer(Math.max(1,storageSlots-inventory.getContainerSize()));
        for(int i=0;i<storageSlots;i++){
            if(i<inventory.getContainerSize())addSlot(new Slot(inventory,i,8+(i%9)*18,18+(i/9)*18));
            else addSlot(new Slot(disabled,i-inventory.getContainerSize(),8+(i%9)*18,18){@Override public boolean mayPlace(ItemStack stack){return false;}@Override public boolean mayPickup(Player player){return false;}});
        }
        int offset=(rows-4)*18;
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(playerInventory,col+row*9+9,8+col*18,103+row*18+offset));
        for(int col=0;col<9;col++)addSlot(new Slot(playerInventory,col,8+col*18,161+offset));
    }
    @Override public boolean stillValid(Player player){return !owner.isRemoved()&&player.distanceToSqr(owner)<64;}
    @Override public ItemStack quickMoveStack(Player player,int index){
        var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;
        var original=slot.getItem();var result=original.copy();
        if(index<storageSlots){if(!moveItemStackTo(original,storageSlots,slots.size(),true))return ItemStack.EMPTY;}
        else if(!moveItemStackTo(original,0,inventory.getContainerSize(),false))return ItemStack.EMPTY;
        if(original.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();
        return result;
    }
}
