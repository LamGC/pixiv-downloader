package net.lamgc.pixiv.downloader.deepdanbooru;

import com.google.common.io.ByteStreams;
import net.lamgc.pixiv.downloader.MetadataDatabase;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * DeepDanbooru训练集数据库对象
 */
public class DeepDanbooruDatabase implements MetadataDatabase {

    private final static Logger log = LoggerFactory.getLogger(DeepDanbooruDatabase.class);
    private final AtomicReference<Connection> database = new AtomicReference<>();
    private final File databaseFile;

    private final PreparedStatement insertStatement;

    public DeepDanbooruDatabase(File databaseFile) throws SQLException {
        this(databaseFile.getAbsolutePath());
    }
    
    public DeepDanbooruDatabase(String databasePath) throws SQLException {
        databaseFile = new File(databasePath);
        connectDataBase();
        checkAndFixDataBase();
        insertStatement = database.get().prepareStatement("INSERT INTO posts VALUES (?, ?, ?, ?, ?)");
    }

    private void connectDataBase() throws SQLException {
        database.set(DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath()));
    }

    private void checkAndFixDataBase() {
        try {
            if(!hasTable()) {
                createTable();
            }
        } catch (SQLException throwable) {
            throw new IllegalStateException();
        }
    }

    private boolean hasTable() throws SQLException {
        Connection databaseConn = database.get();
        ResultSet resultSet = databaseConn.createStatement().executeQuery("SELECT tbl_name FROM sqlite_master WHERE type='table' AND name='posts';");
        return resultSet.next();
    }

    private boolean validDataBase() {
        try {
            ResultSet resultSet = database.get().createStatement().executeQuery("PRAGMA integrity_check;");
            return resultSet.next() && resultSet.getString(1).equalsIgnoreCase("ok");
        } catch (SQLException e) {
            return false;
        }
    }

    private boolean createTable() throws SQLException {
        return database.get().createStatement().execute("CREATE TABLE posts (\n" +
                "    id                INTEGER PRIMARY KEY\n" +
                "                              UNIQUE\n" +
                "                              NOT NULL,\n" +
                "    md5               TEXT    NOT NULL\n" +
                "                              UNIQUE,\n" +
                "    file_ext          TEXT    NOT NULL,\n" +
                "    tag_string        TEXT,\n" +
                "    tag_count_general INTEGER NOT NULL\n" +
                "                              CHECK (tag_count_general >= 0) \n" +
                ");\n");
    }

    @Override
    public synchronized void putImageMetaData(
            int id,
            int pageIndex,
            String title, 
            String description, 
            int userId, 
            InputStream imageInputStream, 
            String fileExtName, 
            String[] tags) throws SQLException, IOException {
        StringBuilder tagsStr = new StringBuilder();
        for (String tag : tags) {
            tagsStr.append(tag.replaceAll(" ", "_")).append(" ");
        }

        ByteArrayOutputStream bufferStream = new ByteArrayOutputStream();
        long readLength = ByteStreams.copy(imageInputStream, bufferStream);
        log.info("已读取的图片长度: {}b", readLength);
        String md5;
        try {
            md5 = Hex.encodeHexString(
                    MessageDigest
                            .getInstance("MD5")
                            .digest(bufferStream.toByteArray())
            );
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        try {
            //PreparedStatement insertStatement = database.get().prepareStatement("INSERT INTO posts VALUES (?, ?, ?, ?, ?)");
            insertStatement.setInt(1, id);
            insertStatement.setString(2, md5);
            insertStatement.setString(3, fileExtName);
            insertStatement.setString(4, tagsStr.toString());
            insertStatement.setInt(5, tags.length);

            insertStatement.execute();
        } finally {
            insertStatement.clearWarnings();
            insertStatement.clearParameters();
        }
    }

    @Override
    public Set<String> getTags() throws SQLException {
        Statement statement = database.get().createStatement();
        ResultSet tagsResult = statement.executeQuery("SELECT tag_string FROM posts");
        Set<String> tagsSet = new HashSet<>();
        while(tagsResult.next()) {
            String tagsStr = tagsResult.getString(1);
            tagsSet.addAll(Arrays.asList(tagsStr.split(" ")));
        }
        return tagsSet;
    }

    public void close() throws SQLException {
        insertStatement.close();
        database.get().close();
    }

}
