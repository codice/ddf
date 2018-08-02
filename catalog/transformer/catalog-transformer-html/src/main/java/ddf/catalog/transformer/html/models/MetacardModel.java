package ddf.catalog.transformer.html.models;

import ddf.catalog.data.Metacard;
import java.util.List;

public class MetacardModel {

  private List<CategoryModel> categories;

  private Metacard metacard;

  public MetacardModel(Metacard metacard, List<CategoryModel> categories) {
    this.metacard = metacard;
    this.categories = categories;
  }

  public void setCategories(List<CategoryModel> categories) {
    this.categories = categories;
  }

  public List<CategoryModel> getCategories() {
    return this.categories;
  }

  public void addCategory(CategoryModel category) {
    if (contains(category.getTitle())) {
      // TODO Log that duplicate category entries are not allowed
      return;
    }

    this.categories.add(category);
  }

  public boolean contains(String title) {
    return false;
  }
}
