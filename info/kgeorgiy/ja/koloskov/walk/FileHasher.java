package info.kgeorgiy.ja.koloskov.walk;

import java.io.IOException;
import java.nio.file.Path;

public interface FileHasher {

    String getHash(Path file) throws IOException;

    int getHashLength();

    String getNullHash();
}
