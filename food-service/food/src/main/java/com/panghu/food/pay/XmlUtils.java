package com.panghu.food.pay;

import com.alibaba.fastjson.JSONObject;
import com.panghu.food.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

public final class XmlUtils {
    private XmlUtils() {
    }

    public static JSONObject parseXmlToJson(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setExpandEntityReferences(false);
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            document.getDocumentElement().normalize();
            return nodeToJson(document.getDocumentElement());
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "虚拟支付回调XML解析失败");
        }
    }

    private static JSONObject nodeToJson(Node node) {
        JSONObject result = new JSONObject();
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            if (hasElementChildren(child)) {
                result.put(child.getNodeName(), nodeToJson(child));
            } else {
                result.put(child.getNodeName(), child.getTextContent());
            }
        }
        return result;
    }

    private static boolean hasElementChildren(Node node) {
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                return true;
            }
        }
        return false;
    }
}
