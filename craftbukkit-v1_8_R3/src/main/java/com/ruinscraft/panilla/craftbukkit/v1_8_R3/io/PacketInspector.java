package com.ruinscraft.panilla.craftbukkit.v1_8_R3.io;

import com.ruinscraft.panilla.api.IPanilla;
import com.ruinscraft.panilla.api.IPanillaPlayer;
import com.ruinscraft.panilla.api.exception.FailedNbt;
import com.ruinscraft.panilla.api.exception.LegacyEntityNbtNotPermittedException;
import com.ruinscraft.panilla.api.exception.NbtNotPermittedException;
import com.ruinscraft.panilla.api.io.IPacketInspector;
import com.ruinscraft.panilla.api.nbt.INbtTagCompound;
import com.ruinscraft.panilla.api.nbt.checks.NbtChecks;
import com.ruinscraft.panilla.craftbukkit.v1_8_R3.nbt.NbtTagCompound;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;

import java.lang.reflect.Field;
import java.util.UUID;

public class PacketInspector implements IPacketInspector {

    private final IPanilla panilla;

    public PacketInspector(IPanilla panilla) {
        this.panilla = panilla;
    }

    @Override
    public void checkPacketPlayInSetCreativeSlot(Object _packet) throws NbtNotPermittedException {
        if (_packet instanceof PacketPlayInSetCreativeSlot) {
            PacketPlayInSetCreativeSlot packet = (PacketPlayInSetCreativeSlot) _packet;

            int slot = packet.a();
            ItemStack itemStack = packet.getItemStack();

            if (itemStack == null || !itemStack.hasTag()) return;

            NbtTagCompound tag = new NbtTagCompound(itemStack.getTag());
            String itemClass = itemStack.getItem().getClass().getSimpleName();
            String packetClass = packet.getClass().getSimpleName();

            NbtChecks.checkPacketPlayIn(slot, tag, itemClass, packetClass, panilla);
        }
    }

    @Override
    public void checkPacketPlayOutSetSlot(Object _packet) throws NbtNotPermittedException {
        if (_packet instanceof PacketPlayOutSetSlot) {
            PacketPlayOutSetSlot packet = (PacketPlayOutSetSlot) _packet;

            try {
                Field slotField = PacketPlayOutSetSlot.class.getDeclaredField("b");
                Field itemStackField = PacketPlayOutSetSlot.class.getDeclaredField("c");

                slotField.setAccessible(true);
                itemStackField.setAccessible(true);

                int slot = (int) slotField.get(packet);
                ItemStack itemStack = (ItemStack) itemStackField.get(packet);

                if (itemStack == null || !itemStack.hasTag()) return;

                NbtTagCompound tag = new NbtTagCompound(itemStack.getTag());
                String itemClass = itemStack.getClass().getSimpleName();
                String packetClass = packet.getClass().getSimpleName();

                NbtChecks.checkPacketPlayOut(slot, tag, itemClass, packetClass, panilla);
            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void checkPacketPlayOutSpawnEntity(Object _packet) throws LegacyEntityNbtNotPermittedException {
        if (_packet instanceof PacketPlayOutSpawnEntity) {
            PacketPlayOutSpawnEntity packet = (PacketPlayOutSpawnEntity) _packet;

            try {
                Field idField = PacketPlayOutSpawnEntity.class.getDeclaredField("a");

                idField.setAccessible(true);

                int entityId = (int) idField.get(packet);
                Entity entity = getEntityById(entityId);

                if (entity != null) {
                    if (entity instanceof EntityItem) {
                        EntityItem item = (EntityItem) entity;

                        if (item.getItemStack() == null) {
                            return;
                        }

                        if (!item.getItemStack().hasTag()) {
                            return;
                        }

                        INbtTagCompound tag = new NbtTagCompound(item.getItemStack().getTag());
                        FailedNbt failedNbt = NbtChecks.checkAll(tag, item.getItemStack().getClass().getSimpleName(), panilla);

                        if (FailedNbt.fails(failedNbt)) {
                            throw new LegacyEntityNbtNotPermittedException(packet.getClass().getSimpleName(), false, failedNbt, entityId);
                        }
                    }
                }
            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void sendPacketPlayOutSetSlotAir(IPanillaPlayer player, int slot) {
        CraftPlayer craftPlayer = (CraftPlayer) player.getHandle();
        EntityPlayer entityPlayer = craftPlayer.getHandle();
        PacketPlayOutSetSlot packet = new PacketPlayOutSetSlot(0, slot, new ItemStack(Blocks.AIR));
        entityPlayer.playerConnection.sendPacket(packet);
    }

    @Override
    public void removeEntity(UUID entityId) {
        throw new IllegalStateException("Cannot use #removeEntity on 1.8");
    }

    @Override
    public void removeEntityLegacy(int entityId) {
        Entity entity = getEntityById(entityId);

        if (entity != null) {
            entity.dead = true; // TODO: test
        }
    }

    private Entity getEntityById(int entityId) {
        for (Entity entity : MinecraftServer.getServer().getWorld().entityList) {
            if (entity == null) {
                continue;
            }

            if (entity.getId() == entityId) {
                return entity;
            }
        }
        return null;
    }

}
