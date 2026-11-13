package legend.core.audio.output;
import java.util.List;import java.nio.IntBuffer;
import org.lwjgl.openal.*;import org.lwjgl.system.MemoryUtil;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.openal.ALC11.*;
import static org.lwjgl.openal.EXTDisconnect.ALC_CONNECTED;
/** Desktop-only OpenAL device/context management. */
public final class DesktopAudioDevice {
 private long device,context;private IntBuffer tmp;private ALCCapabilities alc;private String defaultName;
 public static List<String> devices(){return ALUtil.getStringList(0,ALC.getCapabilities().ALC_ENUMERATE_ALL_EXT?ALC_ALL_DEVICES_SPECIFIER:ALC_DEVICE_SPECIFIER);}
 public boolean open(String configured){List<String> names=devices();String selected=names.contains(configured)?configured:defaultName!=null?defaultName:names.isEmpty()?null:names.get(0);if(selected==null)return false;
  device=alcOpenDevice(selected);if(device==0)return false;context=alcCreateContext(device,new int[]{0});if(context==0){close();return false;}
  if(!alcMakeContextCurrent(context)){close();return false;}alc=ALC.createCapabilities(device);boolean supported=AL.createCapabilities(alc).OpenAL10;if(!supported){close();return false;}tmp=MemoryUtil.memAllocInt(1);defaultName=alcGetString(0,ALC_DEFAULT_ALL_DEVICES_SPECIFIER);return true;
 }
 public boolean refreshDefault(){alcGetString(0,ALC_ALL_DEVICES_SPECIFIER);String value=alcGetString(0,ALC_DEFAULT_ALL_DEVICES_SPECIFIER);if(value!=null&&!value.equals(defaultName)){defaultName=value;return true;}return false;}
 public boolean connected(){if(device==0)return false;if(alc==null||!alc.ALC_EXT_disconnect)return true;alcGetIntegerv(device,ALC_CONNECTED,tmp);return tmp.get(0)!=0;}
 public void close(){if(context!=0){alcMakeContextCurrent(0);alcDestroyContext(context);context=0;}if(device!=0){alcCloseDevice(device);device=0;}if(tmp!=null){MemoryUtil.memFree(tmp);tmp=null;}alc=null;}
}
