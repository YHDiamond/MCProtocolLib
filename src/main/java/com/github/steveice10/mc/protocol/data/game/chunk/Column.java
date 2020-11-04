package com.github.steveice10.mc.protocol.data.game.chunk;

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import lombok.Data;
import lombok.NonNull;

import java.util.Arrays;

@Data
public class Column {
    private final int x;
    private final int z;
    private final @NonNull Chunk[] chunks;
    private final @NonNull CompoundTag[] tileEntities;
    private final @NonNull CompoundTag heightMaps;
    private final @NonNull int[] biomeData;

    public Column(int x, int z, @NonNull Chunk[] chunks, @NonNull CompoundTag[] tileEntities, @NonNull CompoundTag heightMaps) {
        this(x, z, chunks, tileEntities, heightMaps, new int[0]);
    }

    public Column(int x, int z, @NonNull Chunk[] chunks, @NonNull CompoundTag[] tileEntities, @NonNull CompoundTag heightMaps, @NonNull int[] biomeData) {
        if(chunks.length != 16) {
            throw new IllegalArgumentException("Chunk array length must be 16.");
        }

        this.x = x;
        this.z = z;
        this.chunks = Arrays.copyOf(chunks, chunks.length);
        this.biomeData = biomeData != null ? Arrays.copyOf(biomeData, biomeData.length) : null;
        this.tileEntities = tileEntities != null ? tileEntities : new CompoundTag[0];
        this.heightMaps = heightMaps;
    }
}
