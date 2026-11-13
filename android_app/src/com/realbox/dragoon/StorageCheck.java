package com.realbox.dragoon;

import java.nio.file.*;
import legend.core.GameEngine;
import legend.core.IoHelper;
import legend.game.saves.*;

/** Real ART check: naming resources and storage are independent of process cwd. */
public final class StorageCheck {
 public static void main(String[] args){try{
  CoreCheck.main(args);
  String ascii=IoHelper.slugName("New Campaign"),spanish=IoHelper.slugName("La leyenda de Dragón");
  if(!ascii.equals("new_campaign")||!spanish.equals("la_leyenda_de_dragon"))throw new AssertionError("Campaign naming: "+ascii+" / "+spanish);
  System.out.println("PASS: Android campaign naming loads ICU and slugify resources including Spanish accents");
  Path root=Path.of(args[1]);ConfigPresetManager.loadPresetList();
  if(!Files.isDirectory(root.resolve("config")))throw new AssertionError("Preset directory escaped game root");
  ConfigStorage.saveConfig(GameEngine.CONFIG,ConfigStorageLocation.GLOBAL,Path.of("checks/config.dcnf"));
  if(Files.size(root.resolve("checks/config.dcnf"))<4)throw new AssertionError("Config was not written inside game root");
  System.out.println("PASS: Android preset/config files use private game root rather than process cwd");
  System.exit(0);
 }catch(Throwable error){error.printStackTrace();System.exit(1);}}
}
