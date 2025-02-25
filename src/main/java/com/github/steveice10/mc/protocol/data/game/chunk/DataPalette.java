package com.github.steveice10.mc.protocol.data.game.chunk;

import com.github.steveice10.mc.protocol.data.game.chunk.palette.*;
import com.github.steveice10.packetlib.io.NetInput;
import com.github.steveice10.packetlib.io.NetOutput;
import lombok.*;

import java.io.IOException;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class DataPalette {
    public static final int GLOBAL_PALETTE_BITS_PER_ENTRY = 14;

    private @NonNull Palette palette;
    private BitStorage storage;
    private final PaletteType paletteType;
    private final int globalPaletteBits;

    public static DataPalette createForChunk() {
        return createForChunk(GLOBAL_PALETTE_BITS_PER_ENTRY);
    }

    public static DataPalette createForChunk(int globalPaletteBits) {
        return createEmpty(PaletteType.CHUNK, globalPaletteBits);
    }

    public static DataPalette createForBiome(int globalPaletteBits) {
        return createEmpty(PaletteType.BIOME, globalPaletteBits);
    }

    public static DataPalette createEmpty(PaletteType paletteType, int globalPaletteBits) {
        return new DataPalette(new ListPalette(paletteType.getMinBitsPerEntry()),
                new BitStorage(paletteType.getMinBitsPerEntry(), paletteType.getStorageSize()), paletteType, globalPaletteBits);
    }

    public static DataPalette read(NetInput in, PaletteType paletteType, int globalPaletteBits) throws IOException {
        int bitsPerEntry = in.readByte();
        Palette palette = readPalette(paletteType, bitsPerEntry, in);
        BitStorage storage;
        if (!(palette instanceof SingletonPalette)) {
            int length = in.readVarInt();
            storage = new BitStorage(bitsPerEntry, paletteType.getStorageSize(), in.readLongs(length));
        } else {
            in.readVarInt();
            storage = null;
        }

        return new DataPalette(palette, storage, paletteType, globalPaletteBits);
    }

    public static void write(NetOutput out, DataPalette palette) throws IOException {
        if (palette.palette instanceof SingletonPalette) {
            out.writeByte(0); // Bits per entry
            out.writeVarInt(palette.palette.idToState(0));
            out.writeVarInt(0); // Data length
            return;
        }

        out.writeByte(palette.storage.getBitsPerEntry());

        if (!(palette.palette instanceof GlobalPalette)) {
            int paletteLength = palette.palette.size();
            out.writeVarInt(paletteLength);
            for (int i = 0; i < paletteLength; i++) {
                out.writeVarInt(palette.palette.idToState(i));
            }
        }

        long[] data = palette.storage.getData();
        out.writeVarInt(data.length);
        out.writeLongs(data);
    }

    public int get(int x, int y, int z) {
        if (storage != null) {
            int id = this.storage.get(index(x, y, z));
            return this.palette.idToState(id);
        } else {
            return this.palette.idToState(0);
        }
    }

    /**
     * @return the old value present in the storage.
     */
    public int set(int x, int y, int z, int state) {
        int id = this.palette.stateToId(state);
        if (id == -1) {
            resize();
            id = this.palette.stateToId(state);
        }

        if (this.storage != null) {
            int index = index(x, y, z);
            int curr = this.storage.get(index);

            this.storage.set(index, id);
            return curr;
        } else {
            // Singleton palette and the block has not changed because the palette hasn't resized
            return state;
        }
    }

    private static Palette readPalette(PaletteType paletteType, int bitsPerEntry, NetInput in) throws IOException {
        if (bitsPerEntry > paletteType.getMaxBitsPerEntry()) {
            return new GlobalPalette();
        }
        if (bitsPerEntry == 0) {
            return new SingletonPalette(in);
        }
        if (bitsPerEntry <= paletteType.getMinBitsPerEntry()) {
            return new ListPalette(bitsPerEntry, in);
        } else {
            return new MapPalette(bitsPerEntry, in);
        }
    }

    private int sanitizeBitsPerEntry(int bitsPerEntry) {
        if (bitsPerEntry <= this.paletteType.getMaxBitsPerEntry()) {
            return Math.max(this.paletteType.getMinBitsPerEntry(), bitsPerEntry);
        } else {
            return GLOBAL_PALETTE_BITS_PER_ENTRY;
        }
    }

    private void resize() {
        Palette oldPalette = this.palette;
        BitStorage oldData = this.storage;

        int bitsPerEntry = sanitizeBitsPerEntry(oldPalette instanceof SingletonPalette ? 1 : oldData.getBitsPerEntry() + 1);
        this.palette = createPalette(bitsPerEntry, paletteType);
        this.storage = new BitStorage(bitsPerEntry, paletteType.getStorageSize());

        if (oldPalette instanceof SingletonPalette) {
            for (int i = 0; i < paletteType.getStorageSize(); i++) {
                // TODO necessary?
                this.storage.set(i, 0);
            }
        } else {
            for (int i = 0; i < paletteType.getStorageSize(); i++) {
                this.storage.set(i, this.palette.stateToId(oldPalette.idToState(oldData.get(i))));
            }
        }
    }

    private static Palette createPalette(int bitsPerEntry, PaletteType paletteType) {
        if (bitsPerEntry <= paletteType.getMinBitsPerEntry()) {
            return new ListPalette(bitsPerEntry);
        } else if (bitsPerEntry <= paletteType.getMaxBitsPerEntry()) {
            return new MapPalette(bitsPerEntry);
        } else {
            return new GlobalPalette();
        }
    }

    private static int index(int x, int y, int z) {
        return y << 8 | z << 4 | x;
    }
}
