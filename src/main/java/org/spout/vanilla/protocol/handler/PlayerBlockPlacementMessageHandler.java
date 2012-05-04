/*
 * This file is part of Vanilla (http://www.spout.org/).
 *
 * Vanilla is licensed under the SpoutDev License Version 1.
 *
 * Vanilla is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * In addition, 180 days after any changes are published, you can use the
 * software, incorporating those changes, under the terms of the MIT license,
 * as described in the SpoutDev License Version 1.
 *
 * Vanilla is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License,
 * the MIT license and the SpoutDev License Version 1 along with this program.
 * If not, see <http://www.gnu.org/licenses/> for the GNU Lesser General Public
 * License and see <http://www.spout.org/SpoutDevLicenseV1.txt> for the full license,
 * including the MIT license.
 */
package org.spout.vanilla.protocol.handler;

import org.spout.api.entity.PlayerController;
import org.spout.api.event.EventManager;
import org.spout.api.event.player.PlayerInteractEvent;
import org.spout.api.event.player.PlayerInteractEvent.Action;
import org.spout.api.geo.World;
import org.spout.api.geo.cuboid.Block;
import org.spout.api.inventory.Inventory;
import org.spout.api.inventory.ItemStack;
import org.spout.api.material.BlockMaterial;
import org.spout.api.material.Material;
import org.spout.api.material.block.BlockFace;
import org.spout.api.player.Player;
import org.spout.api.protocol.MessageHandler;
import org.spout.api.protocol.Session;
import org.spout.vanilla.material.VanillaMaterials;
import org.spout.vanilla.material.block.generic.VanillaBlockMaterial;
import org.spout.vanilla.protocol.msg.BlockChangeMessage;
import org.spout.vanilla.protocol.msg.PlayerBlockPlacementMessage;
import org.spout.vanilla.util.VanillaMessageHandlerUtils;

public final class PlayerBlockPlacementMessageHandler extends MessageHandler<PlayerBlockPlacementMessage> {
	@Override
	public void handleServer(Session session, Player player, PlayerBlockPlacementMessage message) {
		EventManager eventManager = session.getGame().getEventManager();
		World world = player.getEntity().getWorld();
		Inventory inventory = player.getEntity().getInventory();
		ItemStack holding = inventory.getCurrentItem();
		Material holdingMat = holding == null ? null : holding.getSubMaterial();

		/**
		 * The notch client's packet sending is weird. Here's how it works: If
		 * the client is clicking a block not in range, sends a packet with
		 * x=-1,y=255,z=-1 If the client is clicking a block in range with an
		 * item in hand (id > 255) Sends both the normal block placement packet
		 * and a (-1,255,-1) one If the client is placing a block in range with
		 * a block in hand, only one normal packet is sent That is how it
		 * usually happens. Sometimes it doesn't happen like that. Therefore, a
		 * hacky workaround.
		 */

		if (message.getDirection() == 255) {
			// Right clicked air with an item.
			PlayerInteractEvent event = eventManager.callEvent(new PlayerInteractEvent(player, null, holding, Action.RIGHT_CLICK, true));
			if (!event.isCancelled() && holdingMat != null) {
				holdingMat.onInteract(player.getEntity(), Action.RIGHT_CLICK);
			}
		} else {
			//TODO: Validate the x/y/z coordinates of the message to check if it is in range of the player
			//This is an anti-hack requirement (else hackers can load far-away chunks and crash the server)

			//Get clicked block and validated face against it was placed
			Block clickedBlock = world.getBlock(message.getX(), message.getY(), message.getZ(), player.getEntity());
			BlockFace clickedFace = VanillaMessageHandlerUtils.messageToBlockFace(message.getDirection());
			if (clickedFace == BlockFace.THIS) {
				return;
			}
			//Perform interaction event
			PlayerInteractEvent interactEvent = eventManager.callEvent(new PlayerInteractEvent(player, clickedBlock.getPosition(), inventory.getCurrentItem(), Action.RIGHT_CLICK, false));

			//Get the target block and validate 
			Block target;
			BlockMaterial clickedMaterial = clickedBlock.getSubMaterial();
			BlockFace targetFace;
			if (clickedMaterial.isPlacementObstacle()) {
				target = clickedBlock.translate(clickedFace);
				targetFace = clickedFace.getOpposite();
			} else {
				target = clickedBlock;
				targetFace = BlockFace.BOTTOM; //face is no longer valid at this point
			}
			if (target.getY() >= world.getHeight() || target.getY() < 0) {
				return;
			}

			clickedMaterial.onInteract(player.getEntity(), clickedBlock, Action.RIGHT_CLICK, clickedFace);

			//check if the interaction can possibly result in placement
			if (!interactEvent.isCancelled()) {

				//perform interaction on the server
				if (holdingMat != null) {
					holdingMat.onInteract(player.getEntity(), clickedBlock, Action.RIGHT_CLICK, clickedFace);
				}

				if (clickedMaterial instanceof VanillaBlockMaterial && ((VanillaBlockMaterial) clickedMaterial).isPlacementSuppressed()) {
					return; //prevent placement if the material suppresses this
				}

				//if the material is actually a block, place it
				if (holdingMat != null && holdingMat instanceof BlockMaterial) {
					short placedData = holding.getData(); //TODO: shouldn't the sub-material deal with this?
					BlockMaterial oldBlock = target.getMaterial();
					BlockMaterial newBlock = (BlockMaterial) holdingMat;

					//check if placement is even possible and handle the destruction of the old block
					if (!oldBlock.isPlacementObstacle() && newBlock.canPlace(target, placedData, targetFace)) {
						if (!oldBlock.equals(VanillaMaterials.AIR)) {
							oldBlock.onDestroy(target);
						}

						//perform actual placement
						if (newBlock.onPlacement(target, placedData, targetFace)) {
							//Remove block from inventory if not in creative mode.
							if (!((PlayerController) player.getEntity().getController()).hasInfiniteResources()) {
								holding.setAmount(holding.getAmount() - 1);
								inventory.setItem(holding, inventory.getCurrentSlot());
							}
							return; //prevent undoing our placement
						}
					}
				}
			}

			//refresh the client just in case it assumed something
			int x = target.getX();
			int y = target.getY();
			int z = target.getZ();
			player.getSession().send(new BlockChangeMessage(x, y, z, target.getMaterial().getId(), target.getData()));
			inventory.setItem(holding, inventory.getCurrentSlot());
			return;
		}
	}
}
