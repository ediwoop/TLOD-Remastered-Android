package legend.game.unpacker.scripts;

import legend.game.unpacker.Loader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ScriptFinder {
  private static final Logger LOGGER = LogManager.getFormatterLogger(ScriptFinder.class);

  public void find() throws IOException {
    final ScriptSearcher searcher = new ScriptSearcher();
    final List<String> all = Files.readAllLines(Path.of("files.txt"));
    final List<String> matches = new ArrayList<>();
    final List<String> unmatched = new ArrayList<>(all);
    final List<String> messages = new ArrayList<>();

    // SECT/DRGN21.BIN/39/0
    for(final String path : all) {
      if(searcher.compare(searcher.pattern1, Loader.resolve("SECT" + path.substring(1, path.length() - 4)))) {
        matches.add(path);
      }
    }

    messages.add("Pattern 1: found %d matches: %s".formatted(matches.size(), matches));
    unmatched.removeAll(matches);

    matches.clear();

    for(final String path : all) {
      if(searcher.compare(searcher.pattern2, Loader.resolve("SECT" + path.substring(1, path.length() - 4)))) {
        matches.add(path);
      }
    }

    messages.add("Pattern 2: found %d matches: %s".formatted(matches.size(), matches));
    unmatched.removeAll(matches);

    matches.clear();

    for(final String path : all) {
      if(searcher.compare(searcher.pattern3, Loader.resolve("SECT" + path.substring(1, path.length() - 4)))) {
        matches.add(path);
      }
    }

    messages.add("Pattern 3: found %d matches: %s".formatted(matches.size(), matches));
    unmatched.removeAll(matches);

    matches.clear();

    for(final String path : all) {
      if(searcher.compare(searcher.pattern4, Loader.resolve("SECT" + path.substring(1, path.length() - 4)))) {
        matches.add(path);
      }
    }

    messages.add("Pattern 4: found %d matches: %s".formatted(matches.size(), matches));
    unmatched.removeAll(matches);

    matches.clear();

    for(final String path : all) {
      if(searcher.compare(searcher.pattern5, Loader.resolve("SECT" + path.substring(1, path.length() - 4)))) {
        matches.add(path);
      }
    }

    messages.add("Pattern 5: found %d matches: %s".formatted(matches.size(), matches));
    unmatched.removeAll(matches);

    matches.clear();

    for(final String path : all) {
      if(searcher.compare(searcher.pattern6, Loader.resolve("SECT" + path.substring(1, path.length() - 4)))) {
        matches.add(path);
      }
    }

    messages.add("Pattern 6: found %d matches: %s".formatted(matches.size(), matches));
    unmatched.removeAll(matches);

    matches.clear();

    for(final String path : all) {
      if(searcher.compare(searcher.pattern7, Loader.resolve("SECT" + path.substring(1, path.length() - 4)))) {
        matches.add(path);
      }
    }

    messages.add("Pattern 7: found %d matches: %s".formatted(matches.size(), matches));
    unmatched.removeAll(matches);

    matches.clear();

    for(final String path : all) {
      if(searcher.compare(searcher.pattern8, Loader.resolve("SECT" + path.substring(1, path.length() - 4)))) {
        matches.add(path);
      }
    }

    messages.add("Pattern 8: found %d matches: %s".formatted(matches.size(), matches));
    unmatched.removeAll(matches);

    matches.clear();

    for(final String path : all) {
      if(searcher.compare(searcher.pattern9, Loader.resolve("SECT" + path.substring(1, path.length() - 4)))) {
        matches.add(path);
      }
    }

    messages.add("Pattern 9: found %d matches: %s".formatted(matches.size(), matches));
    unmatched.removeAll(matches);

    matches.clear();

    for(final String path : all) {
      if(searcher.compare(searcher.pattern10, Loader.resolve("SECT" + path.substring(1, path.length() - 4)))) {
        matches.add(path);
      }
    }

    messages.add("Pattern 10: found %d matches: %s".formatted(matches.size(), matches));
    unmatched.removeAll(matches);

    messages.forEach(LOGGER::info);
    LOGGER.info("%d did not match: %s", unmatched.size(), unmatched);
  }
}
