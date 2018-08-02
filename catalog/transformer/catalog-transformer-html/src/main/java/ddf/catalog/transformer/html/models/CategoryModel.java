package ddf.catalog.transformer.html.models;

import ddf.catalog.data.Metacard;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class CategoryModel {

  private String title;
  private Map<String, Object> attributes;

  public CategoryModel(Metacard metacard, String title, List<String> attributeList) {
    this.title = title;
    this.attributes = new TreeMap<>();

    mapAttributes(metacard, attributeList);
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public void setAttributes(Map<String, Object> attributes) {
    this.attributes = attributes;
  }

  public String getTitle() {
    return this.title;
  }

  public Map<String, Object> getAttributes() {
    return this.attributes;
  }

  private void mapAttributes(Metacard metacard, List<String> attributeList) {
    for (String attr : attributeList) {
      // TODO Figure out what the difference between getValue() is and getValues() and when to use which
      // TODO Replace the key with a human readable attribute value

      // this.attributes.put(attr, metacard.getAttribute(attr).getValue());
    }
  }
}
