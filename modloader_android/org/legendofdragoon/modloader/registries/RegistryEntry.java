package org.legendofdragoon.modloader.registries;

public class RegistryEntry {
  private Registry<?> registry;
  private RegistryId id;

  void setRegistry(final Registry<?> registry, final RegistryId id) {
    this.registry = registry;
    this.id = id;
  }

  public <EntryType extends RegistryEntry, RegistryType extends Registry<EntryType>> RegistryType getRegistry() {
    //noinspection unchecked
    return (RegistryType)this.registry;
  }

  public RegistryId getRegistryId() {
    return this.id;
  }

  public String getTranslationKey() {
    return this.getTranslationKey("name");
  }

  public String getTranslationKey(final String type) {
    return this.id.modId() + '.' + this.registry.id.entryId() + '.' + this.id.entryId() + '.' + type;
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + ' ' + this.id;
  }
}
