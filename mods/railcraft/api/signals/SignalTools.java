/*******************************************************************************
 * Copyright (c) CovertJaguar, 2011-2016
 *
 * This work (the API) is licensed under the "MIT" License,
 * see LICENSE.md for details.
 ******************************************************************************/

package mods.railcraft.api.signals;

/**
 * @author CovertJaguar <http://www.railcraft.info>
 */
public abstract class SignalTools {

    public static boolean printSignalDebug;
    public static int signalUpdateInterval = 4;

    public static IPairEffectRenderer effectManager;
    public static ISignalPacketBuilder packetBuilder;
}
