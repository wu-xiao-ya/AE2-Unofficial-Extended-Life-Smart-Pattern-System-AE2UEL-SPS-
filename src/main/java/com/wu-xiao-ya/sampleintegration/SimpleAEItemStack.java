package com.lwx1145.sampleintegration;


import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.IStorageChannel;
import appeng.api.config.FuzzyMode;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import io.netty.buffer.ByteBuf;
import java.io.IOException;

/**
 * Simple implementation of IAEItemStack for AE2UEL compatibility
 */
public class SimpleAEItemStack implements IAEItemStack {

    private ItemStack itemStack;
    private long stackSize;
    private long countRequestable;
    private boolean craftable;
    private ItemStack cachedItemStack;

    public SimpleAEItemStack(ItemStack itemStack) {
        this.itemStack = itemStack.copy();
        this.stackSize = itemStack.getCount();
        this.countRequestable = 0;
        this.craftable = false;
    }

    public SimpleAEItemStack(ItemStack itemStack, long stackSize) {
        this.itemStack = itemStack.copy();
        this.stackSize = stackSize;
        this.countRequestable = 0;
        this.craftable = false;
    }

    @Override
    public ItemStack createItemStack() {
        ItemStack copy = itemStack.copy();
        copy.setCount((int) Math.min(stackSize, Integer.MAX_VALUE));
        return copy;
    }

    @Override
    public boolean hasTagCompound() {
        return itemStack.hasTagCompound();
    }

    @Override
    public ItemStack getDefinition() {
        return itemStack.copy();
    }

    @Override
    public boolean isSameType(ItemStack other) {
        return other != null && ItemStack.areItemsEqual(itemStack, other) &&
               ItemStack.areItemStackTagsEqual(itemStack, other);
    }

    @Override
    public boolean isSameType(IAEItemStack other) {
        return other != null && ItemStack.areItemsEqual(itemStack, other.createItemStack()) &&
               ItemStack.areItemStackTagsEqual(itemStack, other.createItemStack());
    }

    @Override
    public void add(IAEItemStack other) {
        if (other != null && isSameType(other)) {
            this.stackSize += other.getStackSize();
            this.countRequestable += other.getCountRequestable();
        }
    }

    @Override
    public IAEItemStack copy() {
        SimpleAEItemStack copy = new SimpleAEItemStack(itemStack, stackSize);
        copy.countRequestable = this.countRequestable;
        copy.craftable = this.craftable;
        return copy;
    }

    @Override
    public boolean sameOre(IAEItemStack other) {
        return other != null && ItemStack.areItemsEqualIgnoreDurability(itemStack, other.createItemStack());
    }

    @Override
    public Item getItem() {
        return itemStack.getItem();
    }

    @Override
    public int getItemDamage() {
        return itemStack.getItemDamage();
    }

    @Override
    public long getStackSize() {
        return stackSize;
    }

    @Override
    public IAEItemStack setStackSize(long stackSize) {
        this.stackSize = stackSize;
        return this;
    }

    @Override
    public long getCountRequestable() {
        return countRequestable;
    }

    @Override
    public IAEItemStack setCountRequestable(long countRequestable) {
        this.countRequestable = countRequestable;
        return this;
    }

    @Override
    public boolean isCraftable() {
        return craftable;
    }

    @Override
    public IAEItemStack setCraftable(boolean craftable) {
        this.craftable = craftable;
        return this;
    }

    @Override
    public IAEItemStack reset() {
        stackSize = 0;
        countRequestable = 0;
        craftable = false;
        return this;
    }

    @Override
    public boolean isMeaningful() {
        return stackSize > 0 || countRequestable > 0;
    }

    @Override
    public void incStackSize(long delta) {
        this.stackSize += delta;
    }

    @Override
    public void decStackSize(long delta) {
        this.stackSize -= delta;
    }

    @Override
    public void incCountRequestable(long delta) {
        this.countRequestable += delta;
    }

    @Override
    public void decCountRequestable(long delta) {
        this.countRequestable -= delta;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        // Simple implementation - not used in our case
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof IAEItemStack) {
            return isSameType((IAEItemStack) obj);
        }
        return false;
    }

    @Override
    public boolean equals(ItemStack other) {
        return isSameType(other);
    }

    @Override
    public boolean fuzzyComparison(IAEItemStack other, FuzzyMode mode) {
        return isSameType(other); // Simple implementation
    }

    @Override
    public void writeToPacket(ByteBuf buf) throws IOException {
        // Simple implementation - not used in our case
    }

    @Override
    public ItemStack getCachedItemStack(long count) {
        if (cachedItemStack == null || cachedItemStack.getCount() != (int) Math.min(count, Integer.MAX_VALUE)) {
            cachedItemStack = itemStack.copy();
            cachedItemStack.setCount((int) Math.min(count, Integer.MAX_VALUE));
        }
        return cachedItemStack;
    }

    @Override
    public void setCachedItemStack(ItemStack stack) {
        this.cachedItemStack = stack != null ? stack.copy() : null;
    }

    @Override
    public IAEItemStack empty() {
        return new SimpleAEItemStack(itemStack, 0);
    }

    @Override
    public boolean isItem() {
        return true;
    }

    @Override
    public boolean isFluid() {
        return false;
    }

    @Override
    public IStorageChannel<IAEItemStack> getChannel() {
        // This would require importing AE2's item channel - for now return null
        return null;
    }

    @Override
    public ItemStack asItemStackRepresentation() {
        return createItemStack();
    }

    public boolean isEmpty() {
        return stackSize <= 0 && countRequestable <= 0;
    }
}

