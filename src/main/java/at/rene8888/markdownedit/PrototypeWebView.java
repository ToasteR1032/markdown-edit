package at.rene8888.markdownedit;

import at.rene8888.markdownedit.serializers.FencedCodeBlockSerializer;
import com.lowagie.text.DocumentException;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.pegdown.Extensions;
import org.pegdown.LinkRenderer;
import org.pegdown.PegDownProcessor;
import org.pegdown.VerbatimSerializer;
import org.w3c.dom.Document;
import org.w3c.tidy.Tidy;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.*;
import java.util.Collections;
import java.util.function.IntFunction;

public class PrototypeWebView extends Application {

    private static final File DATA_FOLDER = new File("data");
    private static final File STYLE_FOLDER = new File(DATA_FOLDER, "style");

    private static String HTML_FRAMEWORK_FIRST_PART;
    private static String HTML_FRAMEWORK_SECOND_PART;

    private static final PegDownProcessor PEGDOWN_PROCESSOR = new PegDownProcessor(Extensions.ALL - Extensions.ANCHORLINKS);

    static {
        try {
            String[] cssFiles = {"gfm.css", "highlighting.css", "layout.css"};
            StringBuilder cssBilder = new StringBuilder();
            for (String cssFileString : cssFiles) {
                File cssFile = new File(STYLE_FOLDER, cssFileString);
                cssBilder.append("<link type=\"text/css\" rel=\"stylesheet\" href=\"file:///").append(cssFile.getAbsolutePath()).append("\" />\n");
            }

            String template = FileUtils.readFileToString(new File(DATA_FOLDER, "template.html"));
            HTML_FRAMEWORK_FIRST_PART = template.substring(0, template.indexOf("$CONTENT"));
            HTML_FRAMEWORK_FIRST_PART = HTML_FRAMEWORK_FIRST_PART.replace("$CSS", cssBilder.toString());
            HTML_FRAMEWORK_FIRST_PART = HTML_FRAMEWORK_FIRST_PART.replace("$HEADER", "Das ist der Header");
            HTML_FRAMEWORK_FIRST_PART = HTML_FRAMEWORK_FIRST_PART.replace("$FOOTER", "Das ist der Footer");
            HTML_FRAMEWORK_SECOND_PART = template.substring(template.indexOf("$CONTENT") + "$CONTENT".length(), template.length());
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private WebView webView;
    private Tidy tidy;
    private CodeArea codeArea;

    @Override
    public void start(Stage stage) throws IOException, InterruptedException {

        tidy = new Tidy();
        tidy.setShowErrors(0);
        tidy.setQuiet(true);

        BorderPane bp = new BorderPane();

        HBox hbox = new HBox();
        bp.setTop(hbox);

        SplitPane sp = new SplitPane();
        bp.setCenter(sp);

        codeArea = new CodeArea();
        IntFunction<String> format = (digits -> " %" + digits + "d ");
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea, format));
        codeArea.textProperty().addListener((obs, oldText, newText) -> {
            long start;
            long stop;
            start = System.nanoTime();
            System.out.println(getAsString());
            webView.getEngine().loadContent(getAsString());
            stop = System.nanoTime();
            long diff = stop - start;
            System.out.printf("Time to render: %dns, %fms%n", diff, diff / 1000000D);
        });
        webView = new WebView();
        sp.getItems().addAll(codeArea, webView);

        Button toPdf = new Button("To PDF");
        toPdf.setOnAction((event) -> {
            try {
                toPdf(new File("out.pdf"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        hbox.getChildren().add(toPdf);

        // create the scene
        stage.setTitle("Web View");
        Scene scene = new Scene(bp, 750, 500);
        stage.setScene(scene);
        stage.show();

        // updateContents(browser.getEngine(), FileUtils.readFileToString(new File("in.md")));
    }

    public Document getAsDocument() {
        return tidy.parseDOM(new StringReader(getAsString()), null);
    }

    public String getAsString() {
        String htmlMarkdown = PEGDOWN_PROCESSOR.markdownToHtml(codeArea.getText(), new LinkRenderer(), Collections.<String, VerbatimSerializer>singletonMap(VerbatimSerializer.DEFAULT, FencedCodeBlockSerializer.INSTANCE));
        return HTML_FRAMEWORK_FIRST_PART + htmlMarkdown + HTML_FRAMEWORK_SECOND_PART;
    }

    public void toPdf(File f) throws IOException, DocumentException {
        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocument(getAsDocument(), null);
        renderer.layout();
        OutputStream os = new FileOutputStream(f);
        renderer.createPDF(os);
        os.close();
    }

    public static void main(String[] args) throws Exception {
        launch(args);
    }
}