import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Original code-drawn placeholder textures. Run from project root with Java 17. */
public class GenerateTextures {
    static final Path ROOT = Path.of("src/main/resources/assets/crowncinder/textures");
    static void pixel(BufferedImage image, int x, int y, int color) {
        if (x >= 0 && y >= 0 && x < image.getWidth() && y < image.getHeight()) image.setRGB(x,y,color);
    }
    static void write(BufferedImage image, String name) throws Exception {
        Path file = ROOT.resolve(name); Files.createDirectories(file.getParent());
        ImageIO.write(image,"png",file.toFile());
    }
    static void sword(String name, int width, boolean twin) throws Exception {
        BufferedImage image = new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);
        for(int i=3;i<14;i++) for(int w=0;w<width;w++) {
            pixel(image,i,15-i+w,0xff62757c);
            pixel(image,i,14-i+w,0xffc5e2e7);
        }
        if(twin) for(int i=4;i<12;i++) pixel(image,i+2,16-i,0xffacd8d5);
        for(int i=1;i<5;i++) pixel(image,i,15-i,0xff775134);
        for(int i=1;i<7;i++) pixel(image,i,8+i,0xffbd914b);
        pixel(image,1,14,0xffe0b967);
        write(image,"item/"+name+".png");
    }
    public static void main(String[] args) throws Exception {
        sword("longsword",1,false); sword("greatsword",3,false); sword("twinblade",1,true);
        BufferedImage goblin = new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<64;y++) for(int x=0;x<64;x++) {
            int color = y<16 ? 0xff62843d : y<32 ? 0xff624932 : 0xff3a432d;
            if((x*7+y*3)%13==0) color = y<16 ? 0xff769647 : 0xff806044;
            goblin.setRGB(x,y,color);
        }
        // Front face on the built-in zombie UV layout.
        for(int x : new int[]{9,10,13,14}) pixel(goblin,x,11,0xffe8bb43);
        pixel(goblin,10,11,0xff161c0f); pixel(goblin,13,11,0xff161c0f);
        for(int x=10;x<14;x++) pixel(goblin,x,14,0xff2e351c);
        pixel(goblin,10,14,0xffeee0b1); pixel(goblin,13,14,0xffeee0b1);
        // Transparent outer head layer: don't cover the face with a second opaque cube.
        for(int y=0;y<16;y++) for(int x=32;x<64;x++) goblin.setRGB(x,y,0);
        write(goblin,"entity/goblin.png");
        System.out.println("Generated 4 original placeholder textures");
    }
}
