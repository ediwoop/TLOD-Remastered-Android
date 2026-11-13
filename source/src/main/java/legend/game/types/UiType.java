package legend.game.types;

import legend.core.renderer.Obj;
import legend.game.unpacker.FileData;

import java.util.Arrays;

public class UiType {
  public final UiPart[] entries_08;
  public Obj obj;

  public UiType(final UiPart[] entries_08) {
    this.entries_08 = entries_08;
  }

  public static UiType fromFile(final FileData data) {
    final int entryCount = data.readUShort(0x6);

    final FileData entryIndicesData = data.slice(0x8 + entryCount * 0x8);

    final UiPart[] entries_08 = new UiPart[entryCount];
    Arrays.setAll(entries_08, i -> UiPart.fromFile(data, data.slice(0x8 + i * 0x8, 0x8), entryIndicesData));

    return new UiType(entries_08);
  }

  /** Encoded extent, including all metric records referenced by this type. */
  static int byteSize(final FileData data) {
    final int entryCount = data.readUShort(0x6);
    final int indicesOffset = 0x8 + entryCount * 0x8;
    int size = indicesOffset;
    for(int i = 0; i < entryCount; i++) {
      final int index = data.readUShort(0x8 + i * 0x8);
      final int pointer = indicesOffset + index * 0x4;
      final int metricsOffset = data.readInt(pointer);
      final int metricsCount = data.readInt(metricsOffset);
      final long end = metricsOffset + 0x4L + metricsCount * 0x14L;
      if(metricsCount < 0 || metricsOffset < indicesOffset || end > data.size()) {
        throw new IllegalArgumentException("Invalid UI metric range");
      }
      size = Math.max(size, Math.max(pointer + 0x4, (int)end));
    }
    return size;
  }
}
