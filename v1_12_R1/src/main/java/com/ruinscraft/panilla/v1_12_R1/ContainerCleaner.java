package com.ruinscraft.panilla.v1_12_R1;

import com.ruinscraft.panilla.api.IContainerCleaner;
import com.ruinscraft.panilla.api.IProtocolConstants;
import com.ruinscraft.panilla.api.config.PConfig;
import com.ruinscraft.panilla.api.nbt.checks.NbtChecks;
import com.ruinscraft.panilla.v1_12_R1.nbt.NbtTagCompound;
import net.minecraft.server.v1_12_R1.Container;
import net.minecraft.server.v1_12_R1.ItemStack;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class ContainerCleaner implements IContainerCleaner {

    private final PConfig config;
    private final IProtocolConstants protocolConstants;

    public ContainerCleaner(PConfig config, IProtocolConstants protocolConstants) {
        this.config = config;
        this.protocolConstants = protocolConstants;
    }

    @Override
    public void clean(Player player) {
        CraftPlayer craftPlayer = (CraftPlayer) player;
        Container container = craftPlayer.getHandle().activeContainer;

        for (int slot = 0; slot < container.slots.size(); slot++) {
            ItemStack itemStack = container.getSlot(slot).getItem();

            if (itemStack == null || !itemStack.hasTag()) continue;

            NBTTagCompound tag = itemStack.getTag();

            String failedNbt = NbtChecks.checkAll(new NbtTagCompound(tag),
                    itemStack.getItem().getClass().getSimpleName(), protocolConstants, config);

            if (failedNbt != null) {
                tag.remove(failedNbt);
                container.getSlot(slot).getItem().setTag(tag);
            }
        }
    }

}
