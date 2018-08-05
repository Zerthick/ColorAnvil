/*
 * Copyright (C) 2018  Zerthick
 *
 * This file is part of ColorAnvil.
 *
 * ColorAnvil is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * ColorAnvil is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ColorAnvil.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.zerthick.coloranvil;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.item.inventory.ClickInventoryEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.InventoryArchetypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.property.SlotIndex;
import org.spongepowered.api.item.inventory.slot.InputSlot;
import org.spongepowered.api.item.inventory.slot.OutputSlot;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Plugin(
        id = "coloranvil",
        name = "ColorAnvil",
        description = "Name Items With Color!",
        authors = {
                "Zerthick"
        }
)
public class ColorAnvil {

    @Inject
    private Logger logger;
    @Inject
    private PluginContainer instance;

    private Pattern formatCodePattern = Pattern.compile("&([a-z0-9])");

    @Listener
    public void onServerStart(GameStartedServerEvent event) {

        // Log Start Up to Console
        logger.info(
                instance.getName() + " version " + instance.getVersion().orElse("")
                        + " enabled!");
    }


    @Listener
    public void onItemForge(ClickInventoryEvent event, @First Player player, @Getter("getTargetInventory") Inventory inventory) {

        // If the inventory in question is an anvil, and the player has the appropriate permissions
        if (inventory.getArchetype() == InventoryArchetypes.ANVIL && player.hasPermission("coloranvil.use")) {

            // Loop through the transactions for this event, there should really only be 1
            event.getTransactions().forEach(slotTransaction -> {

                // If the player clicked on an output slot, then we know they've forged a new item
                if (slotTransaction.getSlot() instanceof OutputSlot &&
                        slotTransaction.getOriginal().equals(event.getCursorTransaction().getDefault())) {

                    // Get the original itemstack from the transaction, as the final should be air if crafting a new item
                    ItemStack originalStack = slotTransaction.getOriginal().createStack();

                    // Check to make sure that the player didn't click on the empty output slot with an item
                    if (originalStack.getType() != ItemTypes.AIR) {

                        // Grab the display name for the item
                        Text itemName = originalStack.get(Keys.DISPLAY_NAME).orElse(Text.of(originalStack.getTranslation()));
                        String itemNamePlain = itemName.toPlain();

                        // Loop through the plain text and filter out any format codes the player doesn't have permission to use
                        Matcher matcher = formatCodePattern.matcher(itemNamePlain);
                        StringBuffer sb = new StringBuffer();
                        while (matcher.find()) {
                            if (!player.hasPermission("coloranvil.color." + matcher.group(1))) {
                                matcher.appendReplacement(sb, "");
                            }
                        }
                        matcher.appendTail(sb);
                        itemNamePlain = sb.toString();

                        // Deserialize the plain text, this time account for formatting codes, offer it back to the item
                        itemName = TextSerializers.FORMATTING_CODE.deserialize(itemNamePlain);
                        originalStack.offer(Keys.DISPLAY_NAME, itemName);

                        // Update the item in the cursor transaction for this event to reflect the itemstack with the colored name
                        event.getCursorTransaction().setCustom(originalStack.createSnapshot());
                    }
                } else if (slotTransaction.getSlot() instanceof InputSlot &&
                        slotTransaction.getFinal().equals(event.getCursorTransaction().getOriginal())) { // If the player places an item in the first input slot

                    slotTransaction.getSlot().getProperties(SlotIndex.class).stream().findFirst().ifPresent(slotIndex -> {
                        if (slotIndex.getValue() != null && slotIndex.getValue() == 0) { // If they clicked on the first input slot
                            slotTransaction.getSlot().peek().ifPresent(itemStack -> {

                                // Grab the display name for the item
                                Text itemName = itemStack.get(Keys.DISPLAY_NAME).orElse(Text.of(itemStack.getTranslation()));

                                // Serialize it to preserve formatting codes
                                String itemNameSerialized = TextSerializers.FORMATTING_CODE.serialize(itemName);
                                itemStack.offer(Keys.DISPLAY_NAME, Text.of(itemNameSerialized));

                                // Update the item in the slot transaction for this event to reflect the itemstack with the serialized name
                                slotTransaction.setCustom(itemStack);
                            });
                        }
                    });
                }
            });

        }
    }
}
