package br.com.murilo.liberthia.capture;

import net.minecraft.core.BlockPos;

import java.util.UUID;

public class CapturedPlayerState {

    private final UUID capturedPlayerId;
    private final UUID captorId;
    private final BlockPos prisonAnchor;
    private final long capturedAtGameTime;

    public CapturedPlayerState(UUID capturedPlayerId, UUID captorId, BlockPos prisonAnchor, long capturedAtGameTime) {
        this.capturedPlayerId = capturedPlayerId;
        this.captorId = captorId;
        this.prisonAnchor = prisonAnchor;
        this.capturedAtGameTime = capturedAtGameTime;
    }

    public UUID capturedPlayerId() {
        return capturedPlayerId;
    }

    public UUID captorId() {
        return captorId;
    }

    public BlockPos prisonAnchor() {
        return prisonAnchor;
    }

    public long capturedAtGameTime() {
        return capturedAtGameTime;
    }
}