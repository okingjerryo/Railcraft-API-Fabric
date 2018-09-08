/*------------------------------------------------------------------------------
 Copyright (c) CovertJaguar, 2011-2016

 This work (the API) is licensed under the "MIT" License,
 see LICENSE.md for details.
 -----------------------------------------------------------------------------*/
package mods.railcraft.api.carts;

import com.mojang.authlib.GameProfile;
import mods.railcraft.api.core.RailcraftConstantsAPI;
import mods.railcraft.api.core.RailcraftFakePlayer;
import mods.railcraft.api.items.IMinecartItem;
import net.minecraft.block.BlockRailBase;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemMinecart;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import net.minecraftforge.common.util.Constants.NBT;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@SuppressWarnings({"WeakerAccess", "unused"})
public final class CartToolsAPI {
    static ILinkageManager linkageManager = CartDummies.DUMMY_LINKAGE_MANAGER;
    static ITrainTransferHelper transferHelper = CartDummies.DUMMY_TRANSFER_HELPER;

    public static ITrainTransferHelper getTransferHelper() {
        return transferHelper;
    }

    /**
     * Returns an instance of ILinkageManager.
     * <p/>
     * Will return null if Railcraft is not installed.
     *
     * @return an instance of ILinkageManager
     */
    public static ILinkageManager getLinkageManager() {
        return linkageManager;
    }

    /**
     * Sets a carts owner.
     * <p/>
     * The is really only needed by the bukkit ports.
     */
    public static void setCartOwner(EntityMinecart cart, EntityPlayer owner) {
        setCartOwner(cart, owner.getGameProfile());
    }

    /**
     * Sets a carts owner.
     * <p/>
     * The is really only needed by the bukkit ports.
     */
    public static void setCartOwner(EntityMinecart cart, GameProfile owner) {
        if (!cart.getEntityWorld().isRemote) {
            NBTTagCompound data = cart.getEntityData();
            data.setTag("owner", NBTUtil.writeGameProfile(new NBTTagCompound(), owner));
//            if (owner.getName() != null)
//                data.setString("owner", owner.getName());
//            if (owner.getId() != null)
//                data.setString("ownerId", owner.getId().toString());
        }
    }

    /**
     * Gets a carts owner. (player.username)
     * <p/>
     * The is really only needed by the bukkit ports.
     */
    public static GameProfile getCartOwner(EntityMinecart cart) {
        NBTTagCompound data = cart.getEntityData();

        if (data.hasKey("owner", NBT.TAG_COMPOUND)) {
            GameProfile profile = NBTUtil.readGameProfileFromNBT(data.getCompoundTag("owner"));
            if (profile != null) {
                return profile;
            }
        }

        String ownerName = RailcraftConstantsAPI.UNKNOWN_PLAYER;
        if (data.hasKey("owner", NBT.TAG_STRING))
            ownerName = data.getString("owner");

        UUID ownerId = null;
        if (data.hasKey("ownerId"))
            ownerId = UUID.fromString(data.getString("ownerId"));
        return new GameProfile(ownerId, ownerName);
    }

    /**
     * Does the cart have a owner?
     * <p/>
     * The is really only needed by the bukkit ports.
     */
    public static boolean doesCartHaveOwner(EntityMinecart cart) {
        NBTTagCompound data = cart.getEntityData();
        return data.hasKey("owner");
    }

    /**
     * Spawns a new cart entity using the provided item.
     * <p/>
     * The backing item must implement <code>IMinecartItem</code> and/or extend
     * <code>ItemMinecart</code>.
     * <p/>
     * Generally Forge requires all cart items to extend ItemMinecart.
     *
     * @param owner The player name that should used as the owner
     * @param cart  An ItemStack containing a cart item, will not be changed by
     *              the function
     * @param world The World object
     * @return the cart placed or null if failed
     * @see IMinecartItem, ItemMinecart
     */
    @Nullable
    public static EntityMinecart placeCart(GameProfile owner, ItemStack cart, WorldServer world, BlockPos pos) {
        cart = cart.copy();
        if (cart.getItem() instanceof IMinecartItem) {
            IMinecartItem mi = (IMinecartItem) cart.getItem();
            return mi.placeCart(owner, cart, world, pos);
        } else if (cart.getItem() instanceof ItemMinecart)
            try {
                EnumActionResult placed = cart.getItem().onItemUse(RailcraftFakePlayer.get(world, pos, cart, EnumHand.MAIN_HAND), world, pos, EnumHand.MAIN_HAND, EnumFacing.DOWN, 0, 0, 0);
                if (placed == EnumActionResult.SUCCESS) {
                    List<EntityMinecart> carts = getMinecartsAt(world, pos, 0.3f);
                    if (carts.size() > 0) {
                        setCartOwner(carts.get(0), owner);
                        return carts.get(0);
                    }
                }
            } catch (Exception e) {
                return null;
            }

        return null;
    }

    public static boolean isMinecartOnRailAt(World world, BlockPos pos, float sensitivity) {
        return isMinecartOnRailAt(world, pos, sensitivity, null, true);
    }

    public static boolean isMinecartOnRailAt(World world, BlockPos pos, float sensitivity, @Nullable Class<? extends EntityMinecart> type, boolean subclass) {
        return BlockRailBase.isRailBlock(world, pos) && isMinecartAt(world, pos, sensitivity, type, subclass);
    }

    public static boolean isMinecartOnAnySide(World world, BlockPos pos, float sensitivity) {
        return isMinecartOnAnySide(world, pos, sensitivity, null, true);
    }

    public static boolean isMinecartOnAnySide(World world, BlockPos pos, float sensitivity, @Nullable Class<? extends EntityMinecart> type, boolean subclass) {
        List<EntityMinecart> list = new ArrayList<>();
        for (EnumFacing side : EnumFacing.VALUES) {
            list.addAll(getMinecartsOnSide(world, pos, sensitivity, side));
        }

        if (type == null)
            return !list.isEmpty();
        else
            for (EntityMinecart cart : list) {
                if ((subclass && type.isInstance(cart)) || cart.getClass() == type)
                    return true;
            }
        return false;
    }

    public static boolean isMinecartAt(World world, BlockPos pos, float sensitivity) {
        return isMinecartAt(world, pos, sensitivity, null, true);
    }

    public static boolean isMinecartAt(World world, BlockPos pos, float sensitivity, @Nullable Class<? extends EntityMinecart> type, boolean subclass) {
        List<EntityMinecart> list = getMinecartsAt(world, pos, sensitivity);

        if (type == null)
            return !list.isEmpty();
        else
            for (EntityMinecart cart : list) {
                if ((subclass && type.isInstance(cart)) || cart.getClass() == type)
                    return true;
            }
        return false;
    }

    public static List<EntityMinecart> getMinecartsOnAllSides(World world, BlockPos pos, float sensitivity) {
        List<EntityMinecart> carts = new ArrayList<>();
        for (EnumFacing side : EnumFacing.VALUES) {
            carts.addAll(getMinecartsOnSide(world, pos, sensitivity, side));
        }

        return carts;
    }

    public static List<EntityMinecart> getMinecartsOnAllSides(World world, BlockPos pos, float sensitivity, Class<? extends EntityMinecart> type, boolean subclass) {
        List<EntityMinecart> list = new ArrayList<>();
        List<EntityMinecart> carts = new ArrayList<>();
        for (EnumFacing side : EnumFacing.VALUES) {
            list.addAll(getMinecartsOnSide(world, pos, sensitivity, side));
        }

        for (EntityMinecart cart : list) {
            if ((subclass && type.isInstance(cart)) || cart.getClass() == type)
                carts.add(cart);
        }
        return carts;
    }

    public static List<EntityMinecart> getMinecartsOnSide(World world, BlockPos pos, float sensitivity, EnumFacing side) {
        return getMinecartsAt(world, pos.offset(side), sensitivity);
    }

    public static boolean isMinecartOnSide(World world, BlockPos pos, float sensitivity, EnumFacing side) {
        return getMinecartOnSide(world, pos, sensitivity, side) != null;
    }

    @Nullable
    public static EntityMinecart getMinecartOnSide(World world, BlockPos pos, float sensitivity, EnumFacing side) {
        List<EntityMinecart> carts = getMinecartsOnSide(world, pos, sensitivity, side);
        if (!carts.isEmpty())
            return carts.get(0);
        return null;
    }

    public static boolean isMinecartOnSide(World world, BlockPos pos, float sensitivity, EnumFacing side, @Nullable Class<? extends EntityMinecart> type, boolean subclass) {
        return getMinecartOnSide(world, pos, sensitivity, side, type, subclass) != null;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static <T extends EntityMinecart> T getMinecartOnSide(World world, BlockPos pos, float sensitivity, EnumFacing side, @Nullable Class<T> type, boolean subclass) {
        for (EntityMinecart cart : getMinecartsOnSide(world, pos, sensitivity, side)) {
            if (type == null || (subclass && type.isInstance(cart)) || cart.getClass() == type)
                return (T) cart;
        }
        return null;
    }

    /**
     * @param sensitivity Controls the size of the search box, ranges from
     *                    (-inf, 0.49].
     */
    public static List<EntityMinecart> getMinecartsAt(World world, BlockPos pos, float sensitivity) {
        sensitivity = Math.min(sensitivity, 0.49f);
        return world.getEntitiesWithinAABB(EntityMinecart.class, new AxisAlignedBB(pos.getX() + sensitivity, pos.getY(), pos.getZ() + sensitivity,
                pos.getX() + 1 - sensitivity, pos.getY() + 1 - sensitivity, pos.getZ() + 1 - sensitivity), EntitySelectors.IS_ALIVE);
    }

    public static List<EntityMinecart> getMinecartsIn(World world, BlockPos p1, BlockPos p2) {
        return world.getEntitiesWithinAABB(EntityMinecart.class, new AxisAlignedBB(p1.getX(), p1.getY(), p1.getZ(), p2.getX(), p2.getY(), p2.getZ())).stream()
                .filter(cart -> !cart.isDead).collect(Collectors.toList());
    }

    /**
     * Returns the cart's "speed". It is not capped by the carts max speed, it
     * instead returns the cart's "potential" speed. Used by collision and
     * linkage logic. Do not use this to determine how fast a cart is currently
     * moving.
     *
     * @return speed
     */
    public static double getCartSpeedUncapped(EntityMinecart cart) {
        return Math.sqrt(cart.motionX * cart.motionX + cart.motionZ * cart.motionZ);
    }

    public static double getCartSpeedUncappedSquared(EntityMinecart cart) {
        return cart.motionX * cart.motionX + cart.motionZ * cart.motionZ;
    }

    public static boolean cartVelocityIsLessThan(EntityMinecart cart, float vel) {
        return Math.abs(cart.motionX) < vel && Math.abs(cart.motionZ) < vel;
    }

    private CartToolsAPI() {
    }
}
