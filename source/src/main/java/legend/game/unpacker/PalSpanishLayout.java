package legend.game.unpacker;
import java.nio.file.*;import java.io.*;import java.security.*;import java.util.HexFormat;
/** Validated offsets for the supplied SCES03047 revision, not other PAL releases. */
public final class PalSpanishLayout {
 private PalSpanishLayout(){}
 private static final String DISC_SHA="b10ee3ea12c72de2e562344495811da575a285271ea4016f7d5d7a6096fc7a69";
 private static final String ENGINE_SHA="5c4b3938c4f5cc49f818b5f6bb7473ae7085ed0e8fc89f7aa35f1e8db148715d";
 private static String sha(InputStream in)throws IOException{try{MessageDigest d=MessageDigest.getInstance("SHA-256");byte[] b=new byte[262144];int n;while((n=in.read(b))!=-1)d.update(b,0,n);return HexFormat.of().formatHex(d.digest());}catch(NoSuchAlgorithmException e){throw new AssertionError(e);}}
 public static String sourceHash(){return DISC_SHA;}
 public static boolean isPrepared(Path root){
  try{
   if(!legend.core.TextFiles.readString(root.resolve("pal-source.sha256")).equals(DISC_SHA))return false;
   for(String p:new String[]{"font.tim","shadow.ctmd","shadow.anim","shadow.tim","encounters","player_combat_script","SECT/DRGN0.BIN/5718/mrg","SECT/DRGN0.BIN/5719","STR/DEMOH.IKI","STR/OPENH.IKI"})if(!Files.isRegularFile(root.resolve(p)))return false;
   byte[] font=Files.readAllBytes(root.resolve("font.tim"));return font.length==7264&&font[0]==0x10;
  }catch(IOException e){return false;}
 }

 /** Restore zero-entry DEFF archives omitted by old file-only ZIP packages, including nested package tables. */
 public static void restoreEmptyArchives(Path root) {
  Path directory=root.resolve("SECT/DRGN0.BIN"),catalog=directory.resolve("mrg");
  try(InputStream in=Files.newInputStream(catalog)) {
   if(!sha(in).equals("3c4901c46e2da0718ae1a86d64a2c2f5f99baac3460a0f0d5114094e1f377384"))return;
  }catch(IOException e){throw new UnpackerException(e);}
  try {
   for(String line:legend.core.TextFiles.readString(catalog).split("\\R")) {
    String[] entry=line.split("[=;]",-1);
    if(entry.length!=3||!entry[0].equals(entry[1]))continue;
    int id=Integer.parseInt(entry[0]);
    if(id<4000||id>=6000)continue;
    // Even entries are DEFF packages. Their child 0 is a DEFF/MRG resource table;
    // an eight-byte zero-entry archive also became an empty directory on extraction.
    if((id&1)==0) {
     Path packageDir=directory.resolve(entry[0]),map=packageDir.resolve("mrg");
     if(Files.isRegularFile(map)) {
      for(String child:legend.core.TextFiles.readString(map).split("\\R")) {
       if(!child.equals("0=0;8"))continue;
       Path emptyTable=packageDir.resolve("0");
       if(!Files.exists(emptyTable)) {
        Files.createDirectories(emptyTable);
        Files.write(emptyTable.resolve("mrg"),new byte[0]);
       }
      }
     }
     continue;
    }
    if(!entry[2].equals("8"))continue;
    Path target=directory.resolve(entry[0]);
    if(!Files.exists(target)) {
     Files.createDirectories(target);
     Files.write(target.resolve("mrg"),new byte[0]);
    }
   }
  }catch(IOException e){throw new UnpackerException(e);}
 }
 public static void verifyDisc(Path path){try(InputStream in=Files.newInputStream(path)){if(Files.size(path)!=462393792L||!sha(in).equals(DISC_SHA))throw new IllegalArgumentException("Unsupported SCES03047 disc revision");}catch(IOException e){throw new UnpackerException(e);}}
 public static void verifyEngine(FileData data){try{if(data.size()!=921600||!sha(new ByteArrayInputStream(data.getBytes())).equals(ENGINE_SHA)||data.readInt(0x3e8)!=0x41||data.readInt(0x550)!=0x10||data.readInt(0xb6a6c)!=0x10)throw new IllegalArgumentException("PAL engine layout mismatch");}catch(IOException e){throw new UnpackerException(e);}}
}
