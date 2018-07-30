package com.sdl.web.pca.client.contentmodel;

/// <summary>
	/// An edge in a connection
	/// </summary>
	class PublicationEdge
	{
		private Publication node;
		private String cursor;

		 public Publication getNode()
		 {
			 return node;
		 }
		 public void setNode(Publication node)
		 {
			 this.node = node;
		 }

		 public String getCursor()
		 {
			 return cursor;
		 }
		 public void setCursor(String cursor)
		 {
			 this.cursor = cursor;
		 }
	}
