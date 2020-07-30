/**
 *    Copyright 2009-2020 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.parsing;

import org.w3c.dom.CharacterData;
import org.w3c.dom.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;

/**
 * @author Clinton Begin
 */
public class XNode {

  private final Node node;
  private final String name;
  private final String body;
  private final Properties attributes;
  private final Properties variables;
  private final XPathParser xpathParser;

  // 在构造器中, 解析当前节点的属性和内容体
  public XNode(XPathParser xpathParser, Node node, Properties variables) {
    this.xpathParser = xpathParser;
    this.node = node;
    this.name = node.getNodeName();

    // 这个variables里面装着从properties里面解析出来的键值对
    this.variables = variables;

    // 解析节点的属性, 这里会使用variables的键值去替换所有属性${xxx}占位符
    this.attributes = parseAttributes(node);

    // 解析当前节点的CDATA_SECTION_NODE或者TEXT_NODE即文本内容, 非节点, 或者无文本时, 解析第一个子节点的CDATA_SECTION_NODE或者TEXT_NODE
    // 这里会使用variables的键值去替换所有文本${xxx}占位符
    this.body = parseBody(node);
  }

  public XNode newXNode(Node node) {
    return new XNode(xpathParser, node, variables);
  }

  public XNode getParent() {
    Node parent = node.getParentNode();
    if (!(parent instanceof Element)) {
      return null;
    } else {
      return new XNode(xpathParser, parent, variables);
    }
  }

  public String getPath() {
    StringBuilder builder = new StringBuilder();
    Node current = node;
    while (current instanceof Element) {
      if (current != node) {
        builder.insert(0, "/");
      }
      builder.insert(0, current.getNodeName());
      current = current.getParentNode();
    }
    return builder.toString();
  }

  public String getValueBasedIdentifier() {
    StringBuilder builder = new StringBuilder();
    XNode current = this;
    while (current != null) {
      if (current != this) {
        builder.insert(0, "_");
      }
      String value = current.getStringAttribute("id",
          current.getStringAttribute("value",
              current.getStringAttribute("property", (String) null)));
      if (value != null) {
        value = value.replace('.', '_');
        builder.insert(0, "]");
        builder.insert(0,
            value);
        builder.insert(0, "[");
      }
      builder.insert(0, current.getName());
      current = current.getParent();
    }
    return builder.toString();
  }

  public String evalString(String expression) {
    return xpathParser.evalString(node, expression);
  }

  public Boolean evalBoolean(String expression) {
    return xpathParser.evalBoolean(node, expression);
  }

  public Double evalDouble(String expression) {
    return xpathParser.evalDouble(node, expression);
  }

  public List<XNode> evalNodes(String expression) {
    return xpathParser.evalNodes(node, expression);
  }

  public XNode evalNode(String expression) {
    return xpathParser.evalNode(node, expression);
  }

  public Node getNode() {
    return node;
  }

  public String getName() {
    return name;
  }

  public String getStringBody() {
    return getStringBody(null);
  }

  public String getStringBody(String def) {
    if (body == null) {
      return def;
    } else {
      return body;
    }
  }

  public Boolean getBooleanBody() {
    return getBooleanBody(null);
  }

  public Boolean getBooleanBody(Boolean def) {
    if (body == null) {
      return def;
    } else {
      return Boolean.valueOf(body);
    }
  }

  public Integer getIntBody() {
    return getIntBody(null);
  }

  public Integer getIntBody(Integer def) {
    if (body == null) {
      return def;
    } else {
      return Integer.parseInt(body);
    }
  }

  public Long getLongBody() {
    return getLongBody(null);
  }

  public Long getLongBody(Long def) {
    if (body == null) {
      return def;
    } else {
      return Long.parseLong(body);
    }
  }

  public Double getDoubleBody() {
    return getDoubleBody(null);
  }

  public Double getDoubleBody(Double def) {
    if (body == null) {
      return def;
    } else {
      return Double.parseDouble(body);
    }
  }

  public Float getFloatBody() {
    return getFloatBody(null);
  }

  public Float getFloatBody(Float def) {
    if (body == null) {
      return def;
    } else {
      return Float.parseFloat(body);
    }
  }

  public <T extends Enum<T>> T getEnumAttribute(Class<T> enumType, String name) {
    return getEnumAttribute(enumType, name, null);
  }

  public <T extends Enum<T>> T getEnumAttribute(Class<T> enumType, String name, T def) {
    String value = getStringAttribute(name);
    if (value == null) {
      return def;
    } else {
      return Enum.valueOf(enumType, value);
    }
  }

  /**
   * Return a attribute value as String.
   *
   * <p>
   * If attribute value is absent, return value that provided from supplier of default value.
   *
   * @param name
   *          attribute name
   * @param defSupplier
   *          a supplier of default value
   * @return the string attribute
   * @since 3.5.4
   */
  public String getStringAttribute(String name, Supplier<String> defSupplier) {
    String value = attributes.getProperty(name);
    return value == null ? defSupplier.get() : value;
  }

  public String getStringAttribute(String name) {
    return getStringAttribute(name, (String) null);
  }

  /**
   * 获取属性值
   *
   * @param name 键名
   * @param def 根据键名无法获取键值时, 默认返回def
   * @return 返回键值或者def
   */
  public String getStringAttribute(String name, String def) {
    String value = attributes.getProperty(name);
    if (value == null) {
      return def;
    } else {
      return value;
    }
  }

  public Boolean getBooleanAttribute(String name) {
    return getBooleanAttribute(name, null);
  }

  public Boolean getBooleanAttribute(String name, Boolean def) {
    String value = attributes.getProperty(name);
    if (value == null) {
      return def;
    } else {
      return Boolean.valueOf(value);
    }
  }

  public Integer getIntAttribute(String name) {
    return getIntAttribute(name, null);
  }

  public Integer getIntAttribute(String name, Integer def) {
    String value = attributes.getProperty(name);
    if (value == null) {
      return def;
    } else {
      return Integer.parseInt(value);
    }
  }

  public Long getLongAttribute(String name) {
    return getLongAttribute(name, null);
  }

  public Long getLongAttribute(String name, Long def) {
    String value = attributes.getProperty(name);
    if (value == null) {
      return def;
    } else {
      return Long.parseLong(value);
    }
  }

  public Double getDoubleAttribute(String name) {
    return getDoubleAttribute(name, null);
  }

  public Double getDoubleAttribute(String name, Double def) {
    String value = attributes.getProperty(name);
    if (value == null) {
      return def;
    } else {
      return Double.parseDouble(value);
    }
  }

  public Float getFloatAttribute(String name) {
    return getFloatAttribute(name, null);
  }

  public Float getFloatAttribute(String name, Float def) {
    String value = attributes.getProperty(name);
    if (value == null) {
      return def;
    } else {
      return Float.parseFloat(value);
    }
  }

  // 将第三方w3c中Node转为mybatis自己的XNode, 并且将一级子节点的占位符替换为具体的值
  public List<XNode> getChildren() {
    List<XNode> children = new ArrayList<>();

    // 获取w3c的nodeList
    NodeList nodeList = node.getChildNodes();
    if (nodeList != null) {
      for (int i = 0, n = nodeList.getLength(); i < n; i++) {
        Node node = nodeList.item(i);

        // 得到为element节点的node
        if (node.getNodeType() == Node.ELEMENT_NODE) {

          // 将每一个element节点元素转为mybatis自己写XNode, 并且一个个添加到XNode集合中
          children.add(new XNode(xpathParser, node, variables));
        }
      }
    }
    return children;
  }

  public Properties getChildrenAsProperties() {
    Properties properties = new Properties();

    // getChildren方法会解析
    for (XNode child : getChildren()) {
      String name = child.getStringAttribute("name");
      String value = child.getStringAttribute("value");
      if (name != null && value != null) {
        properties.setProperty(name, value);
      }
    }
    return properties;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();

    // 调用重载的toString方法
    toString(builder, 0);
    return builder.toString();
  }

  // 递归解析当前元素及子元素的占位$
  private void toString(StringBuilder builder, int level) {
    builder.append("<");

    // 当前的节点名称
    builder.append(name);

    // 当前节点的属性及内容体在构造函数中已经解析过占位符了, attributes存取着已经解析后的属性值
    for (Map.Entry<Object, Object> entry : attributes.entrySet()) {
      builder.append(" ");
      builder.append(entry.getKey());
      builder.append("=\"");
      builder.append(entry.getValue());
      builder.append("\"");
    }

    // 这个getChildren方法, 会解析一级子节点的属性和内容体占位符
    // 在IDEA中要是查看XNode对象信息的话, 会调用的toString方法, 看到的都是已经替换占位符后的结果, 但是上并没有解析占位符
    // 容易误以为SQL中的${username}已经被解析了, 实际上, SQL中的${username}是在parseDynamicTags方法中被解析的, toString解析后的结果只是作为一个返回值, 跟实际上的SQL并无关系
    List<XNode> children = getChildren();
    if (!children.isEmpty()) {
      builder.append(">\n");
      for (XNode child : children) {
        indent(builder, level + 1);

        // 递归调用子类的toString方法
        child.toString(builder, level + 1);
      }
      indent(builder, level);
      builder.append("</");
      builder.append(name);
      builder.append(">");
    } else if (body != null) {
      builder.append(">");
      builder.append(body);
      builder.append("</");
      builder.append(name);
      builder.append(">");
    } else {
      builder.append("/>");
      indent(builder, level);
    }
    builder.append("\n");
  }

  // 根据level来拼接多少空格符的问题
  private void indent(StringBuilder builder, int level) {
    for (int i = 0; i < level; i++) {
      builder.append("    ");
    }
  }

  // 将该节点属性中的占位符进行替换解析
  private Properties parseAttributes(Node n) {
    Properties attributes = new Properties();
    NamedNodeMap attributeNodes = n.getAttributes();
    if (attributeNodes != null) {
      for (int i = 0; i < attributeNodes.getLength(); i++) {
        Node attribute = attributeNodes.item(i);

        // 通过PropertyParser.parse替换属性占位符
        String value = PropertyParser.parse(attribute.getNodeValue(), variables);

        // 属性用name=value键值对返回properties
        attributes.put(attribute.getNodeName(), value);
      }
    }
    return attributes;
  }

  private String parseBody(Node node) {

    // 返回的数据: 已经用variables去替换占位符${xxx}后的数据
    String data = getBodyData(node);
    if (data == null) {
      NodeList children = node.getChildNodes();
      for (int i = 0; i < children.getLength(); i++) {
        Node child = children.item(i);
        data = getBodyData(child);
        if (data != null) {
          break;
        }
      }
    }
    return data;
  }

  // 只解析文本和CDATA节, 用variables去替换所有占位符
  private String getBodyData(Node child) {
    if (child.getNodeType() == Node.CDATA_SECTION_NODE
        || child.getNodeType() == Node.TEXT_NODE) {
      String data = ((CharacterData) child).getData();

      // 调用PropertyParser.parse来解析${xxx}
      data = PropertyParser.parse(data, variables);
      return data;
    }
    return null;
  }

}
