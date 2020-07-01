package net.lamgc.pixiv.downloader.deepdanbooru;

import net.lamgc.pixiv.downloader.MetadataDatabase;
import org.apache.commons.codec.binary.Hex;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * DeepDanbooru训练集数据库对象
 */
public class DeepDanbooruDatabase implements MetadataDatabase {

    private final AtomicReference<Connection> database = new AtomicReference<>();
    private final File databaseFile;

    private final PreparedStatement insertStatement;

    public DeepDanbooruDatabase(File databaseFile) throws SQLException {
        this(databaseFile.getAbsolutePath());
    }
    
    public DeepDanbooruDatabase(String databasePath) throws SQLException {

        databaseFile = new File(databasePath);
        checkAndFixDataBase();
        insertStatement = database.get().prepareStatement("INSERT INTO posts VALUES (?, ?, ?, ?, ?)");
    }

    private void connectDataBase() throws SQLException {
        database.set(DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath()));
    }

    private void checkAndFixDataBase() {
         if(!validDataBase()) {
            databaseFile.delete();

         }

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
            String[] tags) throws SQLException {
        StringBuilder tagsStr = new StringBuilder();
        for (String tag : tags) {
            tagsStr.append(tag).append(" ");
        }

        ByteArrayOutputStream bufferStream = new ByteArrayOutputStream();
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
        insertStatement.setInt(1, id);
        insertStatement.setString(2, md5);
        insertStatement.setString(3, fileExtName);
        insertStatement.setString(4, tagsStr.toString());
        insertStatement.setInt(5, tags.length);

        if (!insertStatement.execute()) {
            throw new RuntimeException("Execute SQL statement failed: " + insertStatement.getWarnings().getMessage());
        }
        insertStatement.clearWarnings();
        insertStatement.clearParameters();
    }

    public void close() throws SQLException {
        database.get().close();
    }

}
