package fradiani.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import javax.sound.sampled.Clip;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Tool to batch copy the files from network nodes to the base_csv
 */
public class FileBot {

  public static String BASE_CSV = "/Users/alx/Downloads/(0)BASE_CSV/";

  public static void copyDirContent(File[] files) throws Exception {
    for (File f : files) {
      if (f.isDirectory()) //enter that directory
      {
        copyDirContent(f.listFiles());
      } else //is a file, copy it
      {
        copyFile(f);
      }
    }
  }

  public static void copyFile(File afile) throws Exception {
    InputStream inStream;
    OutputStream outStream;

    String[] froute = afile.getName().split("_");
    File direc = new File(BASE_CSV + froute[0] + "/" + froute[1]);
    File bfile = new File(BASE_CSV + froute[0] + "/" + froute[1] + "/" + afile.getName());

    if (bfile.exists() || afile.getName().substring(0, 1).equals(".")) //file already copied
    {
      return;
    }
    if (!direc.exists()) {
      direc.mkdirs();
    }

    inStream = new FileInputStream(afile);
    outStream = new FileOutputStream(bfile);

    byte[] buffer = new byte[1024];
    int length;
    //copy the file content in bytes 
    while ((length = inStream.read(buffer)) > 0) {
      outStream.write(buffer, 0, length);
    }

    inStream.close();
    outStream.close();
    System.out.println("copied: " + afile.getAbsolutePath());
  }

  public static void play(String filename) {
    File file = new File("/Users/alx/Documents/" + filename);
    try {
      URL url = file.toURI().toURL();
      Clip clip = AudioSystem.getClip();

      AudioInputStream ais = AudioSystem.getAudioInputStream(url);
      clip.open(ais);
      clip.start();
    } catch (LineUnavailableException | UnsupportedAudioFileException | IOException e) {
      System.err.println(e);
    }
  }

  public static void main(String[] args) {
    String source_path = "/Volumes/alx/shared/BASE_CSV/TICK_FACTORY/";

    //loop through source path and copy all files
    File[] files = new File(source_path).listFiles();
    try {
      FileBot.copyDirContent(files);
    } catch (Exception e) {
      System.err.println(e);
      FileBot.play("failure.wav");
    }

    FileBot.play("success.wav");
  }
}
