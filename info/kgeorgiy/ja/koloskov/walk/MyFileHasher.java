package info.kgeorgiy.ja.koloskov.walk;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MyFileHasher implements FileHasher {

    private static final Logger logger = Logger.getLogger(MyFileHasher.class.getName());
    private static final int BUFFER_SIZE = 8192;
    private final MessageDigest digest;
    private final int hashLength;

    public MyFileHasher(final String algorithm, final int hashLength)
            throws NoSuchAlgorithmException {
        this.digest = MessageDigest.getInstance(algorithm);
        this.hashLength = hashLength < 0 ? digest.getDigestLength() : hashLength;
    }

    @Override
    public String getHash(final Path filePath) throws IOException {
        digest.reset();
        try (final FileChannel channel = FileChannel.open(filePath, StandardOpenOption.READ)) {
            final ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
            while (channel.read(buffer) != -1) {
                buffer.flip();
                digest.update(buffer);
                buffer.clear();
            }
        } catch (final IOException e) {
            logger.log(Level.WARNING, "Error reading file: " + filePath + ": " + e.getMessage());
            throw e;
        }
        return HexFormat.of().formatHex(digest.getDigestLength() == hashLength ? digest.digest()
                                                : Arrays.copyOf(digest.digest(), hashLength));
    }

    @Override
    public int getHashLength() {
        return hashLength;
    }

    @Override
    public String getNullHash() {
        return HexFormat.of().formatHex(new byte[getHashLength()]);
    }
}
