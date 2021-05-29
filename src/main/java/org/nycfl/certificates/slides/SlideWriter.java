package org.nycfl.certificates.slides;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.nycfl.certificates.Tournament;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.BadRequestException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RequestScoped
public class SlideWriter {
  @ConfigProperty(name="app.data.path")
  String dataPath;

  public String writeSlides(Tournament tournament,
                            Map<String, String> slides,
                            String prefix){
    try (
      FileOutputStream fileOutputStream =
        new FileOutputStream(getOutputFile(tournament, prefix));
      ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream)){
      for (Map.Entry<String, String> memoryFile : slides.entrySet()) {
        ZipEntry zipEntry = new ZipEntry(memoryFile.getKey() + ".svg");
        zipOutputStream.putNextEntry(zipEntry);
        zipOutputStream.write(memoryFile.getValue().getBytes());
        zipOutputStream.closeEntry();
      }
    } catch (IOException ioException){
      throw new BadRequestException("Could not create file");
    }
    return "\"OK\"";
  }

  private File getOutputFile(Tournament tournament, String prefix) throws IOException {
    Files.createDirectories(Paths.get(dataPath));
    return Paths.get(dataPath).resolve(
      String.format("%d_%s_%d.zip",
        tournament.getId(),
        prefix,
        System.currentTimeMillis())).toFile();
  }
}
