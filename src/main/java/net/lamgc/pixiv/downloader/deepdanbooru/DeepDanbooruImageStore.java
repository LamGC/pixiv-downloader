package net.lamgc.pixiv.downloader.deepdanbooru;

import com.google.common.io.ByteStreams;
import net.lamgc.pixiv.downloader.ImageFileStore;
import net.lamgc.pixiv.downloader.util.MessageDigestUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

public class DeepDanbooruImageStore implements ImageFileStore {

    private final File storeDir;

    private final Map<String, File> hashDirMap = new HashMap<>();

    public DeepDanbooruImageStore(File storeDir) {
        this.storeDir = storeDir;
    }

    @Override
    public void save(int id, int pageIndex, String title, int userId, InputStream imageInputStream, String fileExtName)
            throws IOException {
        ByteArrayOutputStream bufferStream = new ByteArrayOutputStream();
        ByteStreams.copy(imageInputStream, bufferStream);
        String md5 = MessageDigestUtils.md5ToString(bufferStream.toByteArray(), true);
        File file = new File(getHashStoreDir(md5), md5 + "." + fileExtName);
        Files.write(file.toPath(), bufferStream.toByteArray(), StandardOpenOption.WRITE);
    }

    private File getHashStoreDir(String md5) throws IOException {
        String dirName = md5.substring(0, 2);
        if(!hashDirMap.containsKey(dirName)) {
            File dirFile = new File(storeDir, dirName);
            if(!dirFile.exists() && !dirFile.mkdirs()) {
                throw new IOException("Unable to create parent directories of " + dirFile.getAbsolutePath());
            }
            hashDirMap.put(dirName, dirFile);
        }

        return hashDirMap.get(dirName);
    }

}
