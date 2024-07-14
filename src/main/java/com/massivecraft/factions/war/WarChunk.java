package com.massivecraft.factions.war;

import org.bukkit.Chunk;

public class WarChunk {
    boolean captured = false;
    long startCapturing;
    long endCapture;
    int progression = 0;
    Chunk chunk;

    public WarChunk(Chunk chunk) {
        startCapturing = System.currentTimeMillis();
    }
}
