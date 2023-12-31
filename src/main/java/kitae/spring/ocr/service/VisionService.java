package kitae.spring.ocr.service;

import com.google.cloud.spring.vision.CloudVisionTemplate;
import com.google.cloud.vision.v1.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Service
public class VisionService {

    @Autowired
    private CloudVisionTemplate cloudVisionTemplate;

    @Autowired
    private ResourceLoader resourceLoader;

    /**
     * 이미지에서 텍스트를 추출
     * @param file
     * @return
     */
    public String extractTextFromImage(MultipartFile file) {
        String textFromImage = cloudVisionTemplate.
                extractTextFromImage(file.getResource());

        return textFromImage;
    }

    /**
     * 이미지에서 랜드마크를 추출
     * @param file
     * @return
     */
    public String getLandmarkFromImage(MultipartFile file) {
        AnnotateImageResponse response =
                cloudVisionTemplate.analyzeImage(file.getResource(),
                        Feature.Type.LANDMARK_DETECTION);

        return response.getLandmarkAnnotationsList().toString();
    }

    /**
     * 이미지에서 라벨을 추출
     * @param file
     * @return
     */
    public String detectLabelFromImage(MultipartFile file) {
        System.out.println("========================================");
        AnnotateImageResponse response =
                cloudVisionTemplate.analyzeImage(
                        file.getResource(), Feature.Type.DOCUMENT_TEXT_DETECTION);

        TextAnnotation annotation = response.getFullTextAnnotation();
        List<Page> pagesList = annotation.getPagesList();
        for (Page page : pagesList) {

            List<Block> blocksList = page.getBlocksList();
            for (Block block : blocksList) {
                System.out.println("\n======== Block ========");
                List<Paragraph> paragraphsList = block.getParagraphsList();
                for (Paragraph paragraph : paragraphsList) {
                    List<Word> wordsList = paragraph.getWordsList();
                    for (Word word : wordsList) {
                        List<Symbol> symbolsList = word.getSymbolsList();
                        for (Symbol symbol : symbolsList) {
                            System.out.print(symbol.getText());
                        }
                        System.out.print(" ");
                    }
                }
                System.out.println();
            }
        }



        return response.getFullTextAnnotation().getText().toString();
    }

    /**
     * PDF에서 텍스트를 추출
     * @param file
     * @return
     */
    public List<String> extractTextFromPdf(MultipartFile file) {
        List<String> texts =
                cloudVisionTemplate.extractTextFromPdf(file.getResource());

        return texts;
    }

    /**
     * 이미지에서 얼굴을 추출
     * @param file
     * @return
     * @throws IOException
     */
    public byte[] detectFaceFromImage(MultipartFile file) throws IOException {
        AnnotateImageResponse response = cloudVisionTemplate.analyzeImage(
                file.getResource(), Feature.Type.FACE_DETECTION);
        Resource outputImageResource = resourceLoader.
                getResource("file:src/main/resources/output.jpg");

        byte [] image = writeWithFaces(file,
                outputImageResource.getFile().toPath(),
                response.getFaceAnnotationsList());

        return image;
    }

    /**
     * 얼굴을 추출한 이미지를 리턴
     * @param file
     * @param outputPath
     * @param faces
     * @return
     * @throws IOException
     */
    private static byte[] writeWithFaces(MultipartFile file,
                                         Path outputPath, List<FaceAnnotation> faces)
            throws IOException {

        BufferedImage img = ImageIO.read(file.getInputStream());
        annotateWithFaces(img, faces);

        //Write file to resource folder, check resources folder
        ImageIO.write(img, "jpg", outputPath.toFile());

        //And
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        //BufferedImage to byte array
        byte[] image = baos.toByteArray();

        return image;
    }

    /**
     * 이미지에 얼굴을 표시
     * @param img
     * @param faces
     */
    public static void annotateWithFaces(BufferedImage img,
                                         List<FaceAnnotation> faces) {

        for (FaceAnnotation face : faces) {
            annotateWithFace(img, face);
        }
    }

    /**
     * 이미지에 얼굴을 표시
     * @param img
     * @param face
     */
    private static void annotateWithFace(BufferedImage img,
                                         FaceAnnotation face) {

        Graphics2D gfx = img.createGraphics();
        Polygon poly = new Polygon();
        for (Vertex
                vertex : face.getFdBoundingPoly().getVerticesList()) {
            poly.addPoint(vertex.getX(), vertex.getY());
        }
        gfx.setStroke(new BasicStroke(5));
        gfx.setColor(new Color(0xFFFF00));
        gfx.draw(poly);
    }
}

