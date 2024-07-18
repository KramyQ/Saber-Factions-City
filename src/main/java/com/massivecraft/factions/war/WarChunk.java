package com.massivecraft.factions.war;

import org.bukkit.Chunk;

public class WarChunk {
    boolean captured = false;
    long startCapturing;
    long endCapture;
    float progression = 0;
    Chunk chunk;

    public WarChunk(Chunk targetChunk) {
        chunk = targetChunk;
        startCapturing = System.currentTimeMillis();
    }
}
