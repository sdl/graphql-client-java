package com.sdl.web.pca.client.contentmodel;

import com.sdl.web.pca.client.contentmodel.pagemodeldata.ModelData;

import java.util.Dictionary;

/// <summary>
	/// Represents an item with content.
	/// </summary>
	class RawContent
	{
		private String charSet;
		private String content;
		private ModelData data;
		private String id;

		 public String getCharSet()
		 {
			 return charSet;
		 }
		 public void setCharSet(String charSet)
		 {
			 this.charSet = charSet;
		 }

		 public String getContent()
		 {
			 return content;
		 }
		 public void setContent(String content)
		 {
			 this.content = content;
		 }

		public ModelData getData() {
			return data;
		}

		public void setData(ModelData data) {
			this.data = data;
		}

		public String getId()
		 {
			 return id;
		 }
		 public void setId(String id)
		 {
			 this.id = id;
		 }
	}