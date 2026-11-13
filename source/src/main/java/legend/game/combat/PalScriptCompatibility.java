package legend.game.combat;

import legend.core.GamePaths;
import legend.core.TextFiles;
import org.json.JSONObject;
import java.nio.file.*;
import java.io.IOException;
import java.security.*;
import java.util.HexFormat;

/** Whole-script replacements independent of regional byte offsets; never writes installed disc data. */
public final class PalScriptCompatibility {
  private PalScriptCompatibility() { }
  private static String sha(final byte[] data) {
    try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data)); }
    catch(final NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
  }
  /** Install only exact, reviewed PAL scripts. Retain originals and never overwrite unknown data. */
  public static void install(final Path root) {
    final Path directory=GamePaths.resolve("gfx/compat/pal"), manifest=directory.resolve("scripts.json");
    if(!Files.isRegularFile(manifest))return;
    try {
      final JSONObject meta=new JSONObject(TextFiles.readString(manifest));
      final org.json.JSONArray scripts=meta.getJSONArray("scripts");
      final JSONObject player=scripts.getJSONObject(0);
      final String installedPlayer=sha(Files.readAllBytes(root.resolve("player_combat_script")));
      if(!installedPlayer.equals(player.getString("sourceSha256"))&&!installedPlayer.equals(player.getString("compiledSha256")))return;
      // Validate the entire payload before changing any installed data.
      for(int i=0;i<scripts.length();i++) {
        final JSONObject entry=scripts.getJSONObject(i);
        if(!sha(Files.readAllBytes(directory.resolve(entry.getString("file")))).equals(entry.getString("compiledSha256")))
          throw new IllegalStateException("PAL compatibility payload checksum mismatch");
      }
      for(int i=0;i<scripts.length();i++) {
        final JSONObject entry=scripts.getJSONObject(i);
        final Path target=root.resolve(entry.getString("path")).normalize();
        if(!target.startsWith(root.normalize()))throw new IllegalStateException("Invalid compatibility target");
        if(Files.exists(target)) {
          final byte[] original=Files.readAllBytes(target);final String hash=sha(original);
          if(hash.equals(entry.getString("compiledSha256")))continue;
          if(!entry.has("sourceSha256")||!hash.equals(entry.getString("sourceSha256")))continue;
          final Path backup=GamePaths.resolve("compat-originals/pal").resolve(entry.getString("path"));
          Files.createDirectories(backup.getParent());
          if(!Files.exists(backup))Files.write(backup,original);
        }else if(entry.has("sourceSha256"))continue;
        Files.createDirectories(target.getParent());
        final Path temporary=target.resolveSibling(target.getFileName()+".compat-tmp");
        Files.write(temporary,Files.readAllBytes(directory.resolve(entry.getString("file"))));
        Files.move(temporary,target,StandardCopyOption.REPLACE_EXISTING);
      }
    }catch(final IOException e){throw new IllegalStateException("Cannot install PAL combat compatibility scripts",e);}
  }
  public static byte[] playerCombat(final byte[] original) {
    final Path directory=GamePaths.resolve("gfx/compat/pal");
    final Path manifest=directory.resolve("player-combat.json");
    if(!Files.isRegularFile(manifest))return original;
    try {
      final JSONObject meta=new JSONObject(TextFiles.readString(manifest));
      if(!sha(original).equals(meta.getString("sourceSha256")))return original;
      final byte[] replacement=Files.readAllBytes(directory.resolve("player-combat.bin"));
      if(!sha(replacement).equals(meta.getString("compiledSha256")))throw new IllegalStateException("PAL combat script checksum mismatch");
      return replacement;
    }catch(final IOException e){throw new IllegalStateException("Cannot read PAL combat compatibility script",e);}
  }
}
