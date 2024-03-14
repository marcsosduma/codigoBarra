package br.sf;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

public class BarcodeSearch {

    public static BufferedImage stringCode(BufferedImage image, StringBuilder code) {
        BufferedImage newImage = null;
        String valor = "";
        code.append("");
        for(int m = 1; m<3; m++) {
            try {      
            	 valor = "Not Found....";
            	// Rotacionar a imagem
                //BufferedImage rotatedImage = ImageRotation.rotateImage(image);
                //BufferedImage preprocessImage = preprocessImage(image);
                // Escalona a imagem para o dobro do tamanho original
                BufferedImage scaledImage = scaleImage(image, 10 * m);
                // Convertendo a imagem para tons de cinza
                BufferedImage grayscaleImage = convertToBlackAndWhite(scaledImage);
                // Filtragem de suaviza��o para redu��o de ru�do
                BufferedImage smoothedImage = applySmoothingFilter(grayscaleImage);
                // Remo��o de ru�do
                BufferedImage denoisedImage = removeNoise(smoothedImage);
                // Aprimoramento de contraste adaptativo
                BufferedImage enhancedImage = enhanceContrast(denoisedImage);
                // Detec��o de bordas
                newImage = detectEdges(enhancedImage);
                // Detec��o de c�digos de barras
                valor = detectBarcode(newImage);
                code.setLength(0);
                code = code.append(valor);
                break;
            } catch (ReaderException e) {
                System.out.println("No barcode found");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return newImage;
    }
    
    public static BufferedImage scaleImage(BufferedImage image, double scale) {
        int scaledWidth = (int) (image.getWidth() * scale);
        int scaledHeight = (int) (image.getHeight() * scale);
        BufferedImage scaledImage = new BufferedImage(scaledWidth, scaledHeight, image.getType());
        Graphics2D g2d = scaledImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(image, 0, 0, scaledWidth, scaledHeight, null);
        g2d.dispose();
        return scaledImage;
    }

    public static BufferedImage convertToBlackAndWhite(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage binaryImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        
        // Percorre cada pixel da imagem original
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Obt�m o valor de intensidade do pixel na imagem original
                int rgb = image.getRGB(x, y);
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = (rgb) & 0xFF;
                
                // Calcula o valor m�dio da intensidade para decidir se ser� branco ou preto
                int averageIntensity = (red + green + blue) / 3;

                // Define o pixel correspondente na imagem bin�ria como branco ou preto
                int binaryValue = (averageIntensity < 128) ? 0xFF000000 : 0xFFFFFFFF;
                binaryImage.setRGB(x, y, binaryValue);
            }
        }

        return binaryImage;
    }

    public static BufferedImage convertToGrayscale(BufferedImage image) {
        BufferedImage grayImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = grayImage.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();
        return grayImage;
    }

    public static BufferedImage applySmoothingFilter(BufferedImage image) {
        // Aplica um filtro de suaviza��o (m�dia ponderada) para reduzir o ru�do
        BufferedImage smoothedImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        int kernelSize = 3; // Tamanho do kernel
        float[] kernel = {
                1.0f / 16, 2.0f / 16, 1.0f / 16,
                2.0f / 16, 4.0f / 16, 2.0f / 16,
                1.0f / 16, 2.0f / 16, 1.0f / 16
        }; // Kernel com m�dia ponderada
        BufferedImageOp op = new ConvolveOp(new Kernel(kernelSize, kernelSize, kernel));
        op.filter(image, smoothedImage);
        return smoothedImage;
    }

    public static BufferedImage removeNoise(BufferedImage image) {
        // Aplicando um filtro de mediana adaptativo para remo��o de ru�do
        int width = image.getWidth();
        int height = image.getHeight();
        int kernelSize = 3; // Tamanho do kernel
        int radius = kernelSize / 2;
        BufferedImage denoisedImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        // Itera sobre cada pixel da imagem
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Calcula a mediana dos pixels na vizinhan�a do pixel atual
                int median = calculateAdaptiveMedian(image, x, y, kernelSize);
                // Define o valor do pixel na imagem denoisedImage
                denoisedImage.setRGB(x, y, median << 16 | median << 8 | median);
            }
        }

        return denoisedImage;
    }

    public static int calculateAdaptiveMedian(BufferedImage image, int x, int y, int kernelSize) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] values = new int[kernelSize * kernelSize];
        int index = 0;

        // Itera sobre a janela do kernel
        for (int ky = -kernelSize / 2; ky <= kernelSize / 2; ky++) {
            for (int kx = -kernelSize / 2; kx <= kernelSize / 2; kx++) {
                int nx = x + kx;
                int ny = y + ky;

                // Verifica os limites da imagem
                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                    // Obt�m o valor do pixel na posi��o (nx, ny)
                    values[index++] = image.getRGB(nx, ny) & 0xFF;
                }
            }
        }

        // Ordena os valores dos pixels
        Arrays.sort(values);

        // Calcula a mediana dos valores dos pixels
        int median;
        if (index % 2 == 0) {
            median = (values[index / 2] + values[index / 2 - 1]) / 2;
        } else {
            median = values[index / 2];
        }

        return median;
    }

    public static BufferedImage enhanceContrast(BufferedImage image) {
        // Aplica um simples esticamento de contraste

        BufferedImage enhancedImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        int minPixel = 255;
        int maxPixel = 0;

        // Encontra os valores m�nimo e m�ximo dos pixels na imagem
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getRGB(x, y) & 0xFF;
                if (pixel < minPixel) {
                    minPixel = pixel;
                }
                if (pixel > maxPixel) {
                    maxPixel = pixel;
                }
            }
        }

        // Realiza o esticamento de contraste
        double contrastFactor = 255.0 / (maxPixel - minPixel);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getRGB(x, y) & 0xFF;
                int newPixel = (int) ((pixel - minPixel) * contrastFactor);
                enhancedImage.setRGB(x, y, newPixel << 16 | newPixel << 8 | newPixel);
            }
        }

        return enhancedImage;
    }

    public static BufferedImage detectEdges(BufferedImage image) {
        // Implemente aqui o algoritmo de detec��o de bordas
        // Por exemplo, operador Sobel ou Canny
        // Veja: https://en.wikipedia.org/wiki/Edge_detection
        return image; // Retorno tempor�rio, voc� deve implementar a detec��o de bordas
    }

    private static String detectBarcode(BufferedImage image) throws Exception {
        LuminanceSource source = new BufferedImageLuminanceSource(image);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        
        MultiFormatReader reader = new MultiFormatReader();
        Map<DecodeHintType, Object> hints = new EnumMap(DecodeHintType.class);
        
        List<BarcodeFormat> formats = new ArrayList();
        for (BarcodeFormat format : BarcodeFormat.values()) {
            formats.add(format);
        }
        hints.put(DecodeHintType.POSSIBLE_FORMATS, formats);
        
        reader.setHints(hints);
        
        Result result = reader.decode(bitmap);
        return result.getText();
    }

    public static void main(String[] args) {
        try {
            File file = new File("C:\\docs\\codigo_barra_exemplo.png");
            BufferedImage image = ImageIO.read(file);
            StringBuilder result = new StringBuilder("");
            BufferedImage newImage = stringCode(image, result);
            System.out.println("Barcode text is " + result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
