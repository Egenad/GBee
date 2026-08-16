import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.imageio.ImageIO;

/** Converts an image into deduplicated Game Boy 2bpp tiles and a Kotlin data object. */
public final class ConvertLogoToDmg {
    private static final int MAX_WIDTH = 128;
    private static final int MAX_HEIGHT = 64;
    private static final int SCREEN_WIDTH = 160;
    private static final int SCREEN_HEIGHT = 144;
    private static final int TILE_SIZE = 8;

    private static final int[] DMG_PALETTE = {
        0xFFE0F8D0,
        0xFF88C070,
        0xFF346856,
        0xFF081820,
    };

    private ConvertLogoToDmg() {}

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.err.println("Usage: ConvertLogoToDmg <input.png> <output.kt> <preview.png>");
            System.exit(1);
        }

        Path input = Path.of(args[0]);
        Path kotlinOutput = Path.of(args[1]);
        Path previewOutput = Path.of(args[2]);

        BufferedImage source = ImageIO.read(input.toFile());
        if (source == null) {
            throw new IOException("Unsupported image: " + input);
        }

        BufferedImage cropped = cropVisibleContent(source);
        BufferedImage scaled = scaleToFit(cropped, MAX_WIDTH, MAX_HEIGHT);

        int paddedWidth = roundUpToTile(scaled.getWidth());
        int paddedHeight = roundUpToTile(scaled.getHeight());
        int[][] pixels = quantizeAndPad(scaled, paddedWidth, paddedHeight);
        ConvertedLogo converted = convertToTiles(pixels, paddedWidth, paddedHeight);

        Files.createDirectories(kotlinOutput.getParent());
        Files.writeString(kotlinOutput, renderKotlin(converted, input));

        Files.createDirectories(previewOutput.getParent());
        ImageIO.write(renderPreview(pixels, paddedWidth, paddedHeight), "png", previewOutput.toFile());

        System.out.printf(
            Locale.ROOT,
            "Converted %s: source=%dx%d cropped=%dx%d scaled=%dx%d tiles=%dx%d unique=%d%n",
            input,
            source.getWidth(),
            source.getHeight(),
            cropped.getWidth(),
            cropped.getHeight(),
            scaled.getWidth(),
            scaled.getHeight(),
            converted.widthTiles,
            converted.heightTiles,
            converted.tiles.size()
        );
    }

    private static BufferedImage cropVisibleContent(BufferedImage source) {
        int minX = source.getWidth();
        int minY = source.getHeight();
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int alpha = source.getRGB(x, y) >>> 24;
                if (alpha > 8) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }

        if (maxX < minX || maxY < minY) {
            throw new IllegalArgumentException("The image is completely transparent");
        }

        return source.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private static BufferedImage scaleToFit(BufferedImage source, int maxWidth, int maxHeight) {
        double scale = Math.min(
            (double) maxWidth / source.getWidth(),
            (double) maxHeight / source.getHeight()
        );
        scale = Math.min(scale, 1.0);

        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = result.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.drawImage(source, 0, 0, width, height, null);
        graphics.dispose();
        return result;
    }

    private static int[][] quantizeAndPad(BufferedImage image, int width, int height) {
        int[][] pixels = new int[height][width];

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                int alpha = argb >>> 24;
                int red = (argb >>> 16) & 0xFF;
                int green = (argb >>> 8) & 0xFF;
                int blue = argb & 0xFF;

                red = compositeOnWhite(red, alpha);
                green = compositeOnWhite(green, alpha);
                blue = compositeOnWhite(blue, alpha);

                int luminance = (red * 299 + green * 587 + blue * 114) / 1000;
                pixels[y][x] = Math.min(3, Math.max(0, (255 - luminance + 42) / 85));
            }
        }

        return pixels;
    }

    private static int compositeOnWhite(int component, int alpha) {
        return (component * alpha + 255 * (255 - alpha)) / 255;
    }

    private static ConvertedLogo convertToTiles(int[][] pixels, int width, int height) {
        int widthTiles = width / TILE_SIZE;
        int heightTiles = height / TILE_SIZE;
        Map<Tile, Integer> uniqueTiles = new LinkedHashMap<>();
        List<Integer> tileMap = new ArrayList<>();

        for (int tileY = 0; tileY < heightTiles; tileY++) {
            for (int tileX = 0; tileX < widthTiles; tileX++) {
                byte[] bytes = new byte[16];

                for (int y = 0; y < TILE_SIZE; y++) {
                    int low = 0;
                    int high = 0;

                    for (int x = 0; x < TILE_SIZE; x++) {
                        int color = pixels[tileY * TILE_SIZE + y][tileX * TILE_SIZE + x];
                        int bit = 7 - x;
                        low |= (color & 1) << bit;
                        high |= ((color >>> 1) & 1) << bit;
                    }

                    bytes[y * 2] = (byte) low;
                    bytes[y * 2 + 1] = (byte) high;
                }

                Tile tile = new Tile(bytes);
                int index = uniqueTiles.computeIfAbsent(tile, ignored -> uniqueTiles.size());
                tileMap.add(index);
            }
        }

        if (uniqueTiles.size() > 256) {
            throw new IllegalArgumentException("The converted logo needs more than 256 unique tiles");
        }

        return new ConvertedLogo(widthTiles, heightTiles, new ArrayList<>(uniqueTiles.keySet()), tileMap);
    }

    private static BufferedImage renderPreview(int[][] pixels, int width, int height) {
        BufferedImage screen = new BufferedImage(SCREEN_WIDTH, SCREEN_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = screen.createGraphics();
        graphics.setColor(new Color(DMG_PALETTE[0], true));
        graphics.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
        graphics.dispose();

        int offsetX = ((SCREEN_WIDTH - width) / (2 * TILE_SIZE)) * TILE_SIZE;
        int offsetY = ((SCREEN_HEIGHT - height) / (2 * TILE_SIZE)) * TILE_SIZE;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                screen.setRGB(offsetX + x, offsetY + y, DMG_PALETTE[pixels[y][x]]);
            }
        }

        BufferedImage enlarged = new BufferedImage(SCREEN_WIDTH * 4, SCREEN_HEIGHT * 4, BufferedImage.TYPE_INT_ARGB);
        Graphics2D enlargedGraphics = enlarged.createGraphics();
        enlargedGraphics.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
        );
        enlargedGraphics.drawImage(screen, 0, 0, enlarged.getWidth(), enlarged.getHeight(), null);
        enlargedGraphics.dispose();
        return enlarged;
    }

    private static String renderKotlin(ConvertedLogo logo, Path source) {
        StringBuilder output = new StringBuilder();
        output.append("package es.atm.gbee.core.data.boot_roms\n\n");
        output.append("// Generated from ").append(source).append(" by tools/ConvertLogoToDmg.java.\n");
        output.append("object DmgBootLogo {\n");
        output.append("    const val WIDTH_TILES = ").append(logo.widthTiles).append("\n");
        output.append("    const val HEIGHT_TILES = ").append(logo.heightTiles).append("\n");
        output.append("    const val MAP_X = ").append((20 - logo.widthTiles) / 2).append("\n");
        output.append("    const val MAP_Y = ").append((18 - logo.heightTiles) / 2).append("\n\n");

        List<Integer> tileBytes = new ArrayList<>();
        for (Tile tile : logo.tiles) {
            for (byte value : tile.bytes) {
                tileBytes.add(value & 0xFF);
            }
        }

        output.append("    val tileData: ByteArray = byteArrayOf(\n");
        appendKotlinBytes(output, tileBytes);
        output.append("    )\n\n");
        output.append("    val tileMap: ByteArray = byteArrayOf(\n");
        appendKotlinBytes(output, logo.tileMap);
        output.append("    )\n");
        output.append("}\n");
        return output.toString();
    }

    private static void appendKotlinBytes(StringBuilder output, List<Integer> values) {
        for (int index = 0; index < values.size(); index++) {
            if (index % 8 == 0) {
                output.append("        ");
            }

            int value = values.get(index);
            output.append(String.format(Locale.ROOT, "0x%02X.toByte()", value));

            if (index + 1 < values.size()) {
                output.append(", ");
            }

            if (index % 8 == 7 || index + 1 == values.size()) {
                output.append("\n");
            }
        }
    }

    private static int roundUpToTile(int value) {
        return ((value + TILE_SIZE - 1) / TILE_SIZE) * TILE_SIZE;
    }

    private record ConvertedLogo(
        int widthTiles,
        int heightTiles,
        List<Tile> tiles,
        List<Integer> tileMap
    ) {}

    private static final class Tile {
        private final byte[] bytes;

        private Tile(byte[] bytes) {
            this.bytes = bytes;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Tile tile && Arrays.equals(bytes, tile.bytes);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(bytes);
        }
    }
}
