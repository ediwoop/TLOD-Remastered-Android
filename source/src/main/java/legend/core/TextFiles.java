package legend.core;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
/** UTF-8 text I/O using the Android-supported Files primitives. */
public final class TextFiles {
 private TextFiles(){}
 public static String readString(Path path)throws IOException{return new String(Files.readAllBytes(path),StandardCharsets.UTF_8);}
 public static Path writeString(Path path,CharSequence text,OpenOption... options)throws IOException{return Files.write(path,text.toString().getBytes(StandardCharsets.UTF_8),options);}
}
