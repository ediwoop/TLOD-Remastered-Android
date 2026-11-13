package legend.game.types;

import legend.game.unpacker.FileData;

public record UiFile(UiType uiElements_0000, UiType itemIcons_c6a4, UiType portraits_cfac, UiType _d2d8) {
  public static UiFile fromFile(final FileData data) {
    // The localized element metrics change the following sections' offsets.
    // Each type's metric pointers delimit its size; retail US offsets are not universal.
    final UiType[] types = new UiType[4];
    int offset = 0;
    for(int i = 0; i < types.length; i++) {
      final FileData section = data.slice(offset);
      final int size = UiType.byteSize(section);
      types[i] = UiType.fromFile(section.slice(0, size));
      offset += size;
    }

    return new UiFile(types[0], types[1], types[2], types[3]);
  }

  public void delete() {
    if(this.uiElements_0000.obj != null) {
      this.uiElements_0000.obj.delete();
      this.uiElements_0000.obj = null;
    }

    if(this.itemIcons_c6a4.obj != null) {
      this.itemIcons_c6a4.obj.delete();
      this.itemIcons_c6a4.obj = null;
    }

    if(this.portraits_cfac.obj != null) {
      this.portraits_cfac.obj.delete();
      this.portraits_cfac.obj = null;
    }

    if(this._d2d8.obj != null) {
      this._d2d8.obj.delete();
      this._d2d8.obj = null;
    }
  }
}
