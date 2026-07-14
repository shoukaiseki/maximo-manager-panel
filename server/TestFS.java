import java.nio.file.*;
public class TestFS {
    public static void main(String[] args) throws Exception {
        Path p = Paths.get(System.getProperty("user.home"), ".sks", "maximo-manager-panel", "test", "java-test");
        if (!Files.exists(p)) {
            Files.createDirectories(p);
        }
        Path f = p.resolve("test.txt");
        Files.write(f, "hello".getBytes());
        System.out.println("OK: " + f);
    }
}