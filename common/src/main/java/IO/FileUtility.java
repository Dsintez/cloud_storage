package IO;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileUtility {

    public static void createFile(File file) throws IOException {
        File parent = new File(file.getParent());
        if (!parent.exists()) {
            createDir(parent);
        }
        if (!file.exists() || file.isDirectory()) {
            file.createNewFile();
        }
    }

    public static void createDir(File file) {
        File parent = new File(file.getParent());
        if (!parent.exists()) {
            createDir(parent);
        }
        if (!file.exists() || file.isFile()) {
            file.mkdir();
        }
    }



    public static void move(File dir, File file) throws IOException {
        String path = dir.getAbsolutePath() + "/" + file.getName();
        createFile(new File(path));
        InputStream is = new FileInputStream(file);
        try(OutputStream os = new FileOutputStream(new File(path))) {
            byte [] buffer = new byte[8192];
            while (is.available() > 0) {
                int readBytes = is.read(buffer);
                System.out.println(readBytes);
                os.write(buffer, 0, readBytes);
            }
        }
    }

    public static List<String> getListFiles(File homeDirectory, String path) {
        File file = new File(homeDirectory, path);
        ArrayList<String> list = new ArrayList();
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File file1 : files) {
                list.add((file1.isFile()? "File " : "Directory") + file1.getName());
            }
        } else {
            System.out.println("Выбран файл");
        }
        return list;
    }


}
