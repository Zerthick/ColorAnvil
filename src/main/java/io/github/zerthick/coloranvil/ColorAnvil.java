package io.github.zerthick.coloranvil;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.item.inventory.ClickInventoryEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.InventoryArchetypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.slot.OutputSlot;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

@Plugin(
        id = "coloranvil",
        name = "ColorAnvil",
        version = "1.0.0",
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

    @Listener
    public void onServerStart(GameStartedServerEvent event) {

        // Log Start Up to Console
        logger.info(
                instance.getName() + " version " + instance.getVersion().orElse("")
                        + " enabled!");
    }


    @Listener
    public void onItemForge(ClickInventoryEvent event, @Root Player player, @Getter("getTargetInventory") Inventory inventory) {

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
                    if (originalStack.getItem() != ItemTypes.AIR) {

                        // Grab the display name for the item
                        Text itemName = originalStack.get(Keys.DISPLAY_NAME).orElse(Text.of(originalStack.getTranslation()));
                        String itemNamePlain = itemName.toPlain();

                        // Deserialize the plain text, this time account for formatting codes, offer it back to the item
                        itemName = TextSerializers.FORMATTING_CODE.deserialize(itemNamePlain);
                        originalStack.offer(Keys.DISPLAY_NAME, itemName);

                        // Update the item in the cursor transaction for this event to reflect the itemstack with the colored name
                        event.getCursorTransaction().setCustom(originalStack.createSnapshot());
                    }
                }
            });

        }
    }

}