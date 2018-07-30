package mcs;

public class FilePath {
    public static final String s = System.getProperty("file.separator");
    private static final String rootPath = s.equals("\\") ? "D:\\Project\\" : "Project/";

    public static String get(String path) {
        path = rootPath + path;
        return path.replace("\\",s).replace("/",s);
    }
}
