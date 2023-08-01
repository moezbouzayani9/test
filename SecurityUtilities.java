package com.valuephone.image.utilities;

import com.valuephone.image.exception.ImageContentHasXSSException;
import org.owasp.encoder.Encode;
import org.w3c.dom.NamedNodeMap;

import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author tcigler
 * @since 1.0
 */
public class SecurityUtilities {

    private final static Pattern pattern = Pattern.compile("(?s).*\\b(object|script|style|onclick|ondbclick|onmousedown|onmouseup|onmouseover|onmousemove|onmouseout|onkeypress|onkeydown|onkeyup)\\b.*");
    private SecurityUtilities() {
    }

    private static final Pattern PATTERN = Pattern.compile("[\\s]+");

    public static String replaceLineBreaksWithSpaces(String s) {
        Matcher matcher = PATTERN.matcher(s);
        String message = matcher.replaceAll(" ");
        return message.trim();
    }

    public static String sanitizeInputString(String str) {

        if (str != null) {

            String withoutLinebreaks = replaceLineBreaksWithSpaces(str);

            return Encode.forXml(withoutLinebreaks);

        } else {
            return null;
        }
    }

    public static void sanitizeImageMetadata(ImageReader reader) throws IOException {
        //preparation
        IIOMetadata metadata = reader.getImageMetadata(0);
        IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(metadata.getNativeMetadataFormatName());

        //check
        sanitizeMetadataNodeFromXSS(root);
    }

    public static void checkImageContentFromXSS(byte[] imageBytes) throws ImageContentHasXSSException {
        Matcher matcher = pattern.matcher(new String(imageBytes));

        if (matcher.matches()) {
            throw new ImageContentHasXSSException("During analyzing the content of image was found possible malicious code.");
        }
    }

    private static void sanitizeMetadataNodeFromXSS(IIOMetadataNode nodeToCheck) {
        // check the metadata value
        Object nodeValue = nodeToCheck.getUserObject();

        if (nodeValue != null) {
            String encodedValue = Encode.forHtml(nodeValue.toString());

            if (!nodeValue.toString().equals(encodedValue)) {
                nodeToCheck.setUserObject(encodedValue);
            }
        }

        // check the attributes of current node(metadata object)
        NamedNodeMap attributesOfNode = nodeToCheck.getAttributes();

        for (int i = 0; i < attributesOfNode.getLength(); i++) {
            String attributeValue = attributesOfNode.item(i).getNodeValue();
            String encodedValue = Encode.forHtml(attributeValue);

            if (!attributeValue.equals(encodedValue)) {
                attributesOfNode.item(i).setNodeValue(encodedValue);
            }
        }

        // recursively check the child nodes
        for (int i = 0; i < nodeToCheck.getLength(); i++) {
            sanitizeMetadataNodeFromXSS((IIOMetadataNode) nodeToCheck.item(i));
        }
    }

}
