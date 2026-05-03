package com.chtrembl.petstoreapp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tag model used for pets and products.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tag {
	private Long id;
	private String name;

  @Override
  public String toString() {
    return "Tag{" + "id=" + id + ", name='" + name + '\'' + '}';
  }
}
