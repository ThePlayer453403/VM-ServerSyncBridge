package com.tp4.syncbridge.mixin;


import io.netty.channel.ChannelHandlerContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import static com.tp4.syncbridge.VMServerSyncBridge.logger;


@Mixin(targets = {"io.netty.channel.AbstractChannelHandlerContext"})
public abstract class PacketMixin {

    /**
     * @author ThePlayer453403
     * @reason Prevent player been kick out the server when adds a mod not on the server
     */
    @Overwrite
    public ChannelHandlerContext fireExceptionCaught(final Throwable cause) {
        logger.error(String.valueOf(cause));
        return (ChannelHandlerContext) this;
    }
}