package org.yekashtalian.orderitemsreserver.model;

import lombok.Data;

import java.util.List;

@Data
public class OrderRequest {
	private String id;
	private String email;
	private List<Product> products;
	private String status;
	private boolean complete;

	@Data
	public static class Product {
		private Long id;
		private String name;
		private String photoURL;
		private Integer quantity;
		private Category category;
		private List<Tag> tags;
	}

	@Data
	public static class Category {
		private Long id;
		private String name;
	}

	@Data
	public static class Tag {
		private Long id;
		private String name;
	}
}
